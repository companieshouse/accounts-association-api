package uk.gov.companieshouse.accounts.association.controller;

import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.configuration.WebSecurityConfig;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.*;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;

import static org.mockito.ArgumentMatchers.*;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_READ_PERMISSION;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.fetchAllStatusesWithout;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;

@AutoConfigureMockMvc
@WebMvcTest(AssociationsListForCompanyController.class)
@ExtendWith(MockitoExtension.class)
@Import(WebSecurityConfig.class)
@Tag("unit-test")
class AssociationsListForCompanyControllerTest {

    @Value("${internal.api.url}")
    private String internalApiUrl;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context;

    @MockBean
    private AssociationsService associationsService;

    @MockBean
    private CompanyService companyService;

    @MockBean
    private UsersService usersService;

    @MockBean
    private StaticPropertyUtil staticPropertyUtil;

    private static final String DEFAULT_KIND = "association";
    private static final String DEFAULT_DISPLAY_NAME = "Not provided";

    private static final TestDataManager testDataManager = TestDataManager.getInstance();
    private Mockers mockers;

    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    public void setup() {
        this.mockers = new Mockers( null, null, companyService, usersService );
        mockMvc = MockMvcBuilders.webAppContextSetup( context )
                .apply( SecurityMockMvcConfigurers.springSecurity() )
                .build();
        ReflectionTestUtils.setField( staticPropertyUtil, "APPLICATION_NAMESPACE", "acsp-manage-users-api" );
    }

    @Test
    void getAssociationsForCompanyWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/companies/$$$$$$" )
                        .header("X-Request-Id", "theId123")
                        .header( "ERIC-Identity", "111" )
                        .header( "ERIC-Identity-Type", "oauth2" ) )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyWithNonexistentCompanyReturnsNotFound() throws Exception {
        mockers.mockCompanyServiceFetchCompanyProfileNotFound( "919191" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "919191", "111" );

        mockMvc.perform( get( "/associations/companies/919191" )
                        .header("X-Request-Id", "theId123")
                        .header( "ERIC-Identity", "111" )
                        .header( "ERIC-Identity-Type", "oauth2" ) )
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationsForCompanyWithoutQueryParamsUsesDefaults() throws Exception {
        final var batmanUser = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var batmanAssociation = testDataManager.fetchAssociationDto( "1", batmanUser );
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();

        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "111111", "111" );

        final var associationsList = new AssociationsList()
                .totalResults( 1 ).totalPages( 1 ).pageNumber( 0 ).itemsPerPage( 15 )
                .links( new Links().self(String.format("%s/associations", internalApiUrl)).next("") )
                .items(List.of( batmanAssociation ));

        Mockito.doReturn(associationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( any(), eq( fetchAllStatusesWithout( Set.of( StatusEnum.REMOVED ) ) ),  isNull(), isNull(), eq( 0 ), eq( 15 ) );

        mockMvc.perform( get( "/associations/companies/111111" )
                        .header("X-Request-Id", "theId123")
                        .header( "ERIC-Identity", "111" )
                        .header( "ERIC-Identity-Type", "oauth2" ) )
                .andExpect(status().isOk());

        Mockito.verify( associationsService ).fetchUnexpiredAssociationsForCompanyAndStatuses( eq( companyDetails ) ,  eq( fetchAllStatusesWithout( Set.of( StatusEnum.REMOVED ) ) ),  isNull(), isNull(), eq( 0 ), eq(15 ) );
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedFalseAppliesFilter() throws Exception {
        final var batmanUser = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var batmanAssociation = testDataManager.fetchAssociationDto( "1", batmanUser );
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();

        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "111111", "111" );

        final var expectedAssociationsList = new AssociationsList()
                .totalResults( 1 ).totalPages( 1 ).pageNumber( 0 ).itemsPerPage( 15 )
                .links( new Links().self(String.format("%s/associations", internalApiUrl)).next("") )
                .items(List.of( batmanAssociation ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( any(), eq( fetchAllStatusesWithout( Set.of( StatusEnum.REMOVED ) ) ), isNull(), isNull(), eq( 0 ), eq( 15 ) );

        mockMvc.perform( get( "/associations/companies/111111?include_removed=false" )
                        .header("X-Request-Id", "theId123")
                        .header( "ERIC-Identity", "111" )
                        .header( "ERIC-Identity-Type", "oauth2" ) )
                .andExpect(status().isOk());

        Mockito.verify( associationsService ).fetchUnexpiredAssociationsForCompanyAndStatuses( eq( companyDetails ) , eq( fetchAllStatusesWithout( Set.of( StatusEnum.REMOVED ) ) ),  isNull(), isNull(), eq( 0 ), eq( 15 ) );
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedTrueDoesNotApplyFilter() throws Exception {
        final var batmanUser = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var batmanAssociation = testDataManager.fetchAssociationDto( "1", batmanUser );
        final var jokerUser = testDataManager.fetchUserDtos( "222" ).getFirst();
        final var jokerAssociation = testDataManager.fetchAssociationDto( "2", jokerUser );
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();

        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "111111", "111" );

        final var expectedAssociationsList = new AssociationsList()
                .totalResults( 2 ).totalPages( 1 ).pageNumber( 0 ).itemsPerPage( 15 )
                .links( new Links().self(String.format("%s/associations", internalApiUrl)).next("") )
                .items(List.of( batmanAssociation, jokerAssociation ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( any(), eq( fetchAllStatusesWithout( Set.of() ) ),  isNull(), isNull(), eq( 0 ), eq( 15 ) );

        mockMvc.perform( get( "/associations/companies/111111?include_removed=true" )
                        .header("X-Request-Id", "theId123")
                        .header( "ERIC-Identity", "111" )
                        .header( "ERIC-Identity-Type", "oauth2" ) )
                .andExpect(status().isOk());

        Mockito.verify( associationsService ).fetchUnexpiredAssociationsForCompanyAndStatuses( eq( companyDetails ), eq( fetchAllStatusesWithout( Set.of() ) ),  isNull(), isNull(), eq ( 0 ), eq( 15 ) );
    }

    @Test
    void getAssociationsForCompanyPaginatesCorrectly() throws Exception {
        final var jokerUser = testDataManager.fetchUserDtos( "222" ).getFirst();
        final var jokerAssociation = testDataManager.fetchAssociationDto( "2", jokerUser );

        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "111111", "111" );

        final var expectedAssociationsList = new AssociationsList()
                .totalResults( 2 ).totalPages( 2 ).pageNumber( 1 ).itemsPerPage( 1 )
                .links( new Links().self(String.format("%s/associations", internalApiUrl)).next("") )
                .items(List.of( jokerAssociation ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( any(), eq( fetchAllStatusesWithout( Set.of() ) ),  isNull(), isNull(), eq( 1 ), eq( 1 ) );

        final var response =
                mockMvc.perform( get( "/associations/companies/111111?include_removed=true&items_per_page=1&page_index=1" )
                                .header("X-Request-Id", "theId123")
                                .header( "ERIC-Identity", "111" )
                                .header( "ERIC-Identity-Type", "oauth2" ) )
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( Association::getId )
                        .toList();

        Assertions.assertTrue( items.contains( "2" ) );
        Assertions.assertEquals( String.format("%s/associations", internalApiUrl), links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 1, associationsList.getPageNumber() );
        Assertions.assertEquals( 1, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 2, associationsList.getTotalResults() );
        Assertions.assertEquals( 2, associationsList.getTotalPages() );
    }

    @Test
    void getAssociationsForCompanyDoesMappingCorrectly() throws Exception {
        final var batmanUser = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var batmanAssociation = testDataManager.fetchAssociationDto( "1", batmanUser );
        final var jokerUser = testDataManager.fetchUserDtos( "222" ).getFirst();
        final var jokerAssociation = testDataManager.fetchAssociationDto( "2", jokerUser );

        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "111111", "111" );

        final var expectedAssociationsList = new AssociationsList()
                .totalResults( 2 ).totalPages( 1 ).pageNumber( 0 ).itemsPerPage( 2 )
                .links( new Links().self(String.format("%s/associations", internalApiUrl)).next("") )
                .items(List.of( batmanAssociation, jokerAssociation ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( any(), eq( fetchAllStatusesWithout( Set.of() ) ),  isNull(), isNull(), eq( 0 ), eq( 2 ) );

        final var response =
                mockMvc.perform( get( "/associations/companies/111111?include_removed=true&items_per_page=2&page_index=0" ).header("X-Request-Id", "theId123")
                                .header( "ERIC-Identity", "111" )
                                .header( "ERIC-Identity-Type", "oauth2" ) )
                        .andExpect(status().isOk());
        final var associationsList = parseResponseTo( response, AssociationsList.class );

        final var associations = associationsList.getItems();
        final var firstAssociation = associations.getFirst();

        Assertions.assertEquals( "a", firstAssociation.getEtag() );
        Assertions.assertEquals( "1", firstAssociation.getId() );
        Assertions.assertEquals( "111", firstAssociation.getUserId() );
        Assertions.assertEquals( "bruce.wayne@gotham.city", firstAssociation.getUserEmail() );
        Assertions.assertEquals( "Batman", firstAssociation.getDisplayName() );
        Assertions.assertEquals( "111111", firstAssociation.getCompanyNumber() );
        Assertions.assertEquals( "Wayne Enterprises", firstAssociation.getCompanyName() );
        Assertions.assertEquals( CONFIRMED, firstAssociation.getStatus() );
        Assertions.assertNull( firstAssociation.getCreatedAt() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(1) ), localDateTimeToNormalisedString( firstAssociation.getApprovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(2) ), localDateTimeToNormalisedString( firstAssociation.getRemovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( DEFAULT_KIND, firstAssociation.getKind() );
        Assertions.assertEquals( ApprovalRouteEnum.AUTH_CODE, firstAssociation.getApprovalRoute() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(3) ), reduceTimestampResolution( firstAssociation.getApprovalExpiryAt() ) );
        Assertions.assertEquals( "/associations/1", firstAssociation.getLinks().getSelf() );

        final var secondAssociation = associations.get( 1 );
        Assertions.assertEquals( "222", secondAssociation.getUserId() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, secondAssociation.getDisplayName() );
        Assertions.assertEquals( "Wayne Enterprises", secondAssociation.getCompanyName() );
    }

    @Test
    void getAssociationsForCompanyWithUnacceptablePaginationParametersShouldReturnBadRequest() throws Exception {
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "111111", "111" );

        mockMvc.perform( get( "/associations/companies/111111?include_removed=true&items_per_page=1&page_index=-1" )
                        .header("X-Request-Id", "theId123")
                        .header( "ERIC-Identity", "111" )
                        .header( "ERIC-Identity-Type", "oauth2" ) )
                .andExpect(status().isBadRequest());

        mockMvc.perform( get( "/associations/companies/111111?include_removed=true&items_per_page=0&page_index=0" )
                        .header("X-Request-Id", "theId123")
                        .header( "ERIC-Identity", "111" )
                        .header( "ERIC-Identity-Type", "oauth2" ) )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyFetchesAssociation() throws Exception {
        final var batmanUser = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var associationOne = testDataManager.fetchAssociationDto( "1", batmanUser );
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();

        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "111111", "111" );

        final var expectedAssociationsList = new AssociationsList().totalResults( 1 ).items(List.of( associationOne ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( eq( companyDetails ), eq( fetchAllStatusesWithout( Set.of( StatusEnum.REMOVED ) ) ),  isNull(), isNull(), eq( 0 ), eq(15 ) );

        final var response =
                mockMvc.perform( get( "/associations/companies/{company_number}?user_email=bruce.wayne@gotham.city", "111111" ).header("X-Request-Id", "theId123")
                                .header( "ERIC-Identity", "111" )
                                .header( "ERIC-Identity-Type", "oauth2" ) )
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );

        Assertions.assertEquals( 1, associationsList.getTotalResults() );
        Assertions.assertEquals( "1", associationsList.getItems().getFirst().getId() );
    }

    @Test
    void getAssociationsForCompanyWithNonexistentUserEmailFetchesEmptyList() throws Exception {
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();

        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );

        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "111111", "111" );

        final var expectedAssociationsList = new AssociationsList().totalResults( 0 ).items(List.of());
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( eq( companyDetails ), eq( fetchAllStatusesWithout ( Set.of( StatusEnum.REMOVED ) ) ),  isNull(), isNull(), eq(0 ) , eq(15 ) );

        final var response =
                mockMvc.perform( get( "/associations/companies/111111?user_email=the.void@space.com" ).header("X-Request-Id", "theId123")
                                .header( "ERIC-Identity", "111" )
                                .header( "ERIC-Identity-Type", "oauth2" ) )
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );

        Assertions.assertEquals( 0, associationsList.getTotalResults() );
    }

    @Test
    void getAssociationsForCompanyCanRetrieveMigratedAssociations() throws Exception {
        final var marioUser = testDataManager.fetchUserDtos( "MKUser001" ).getFirst();
        final var luigiUser = testDataManager.fetchUserDtos( "MKUser002" ).getFirst();
        final var peachUser = testDataManager.fetchUserDtos( "MKUser003" ).getFirst();
        final var marioAssociation = testDataManager.fetchAssociationDto( "MKAssociation001", marioUser );
        final var luigiAssociation = testDataManager.fetchAssociationDto( "MKAssociation002", luigiUser );
        final var peachAssociation = testDataManager.fetchAssociationDto( "MKAssociation003", peachUser );
        final var mushroomKingdomCompany = testDataManager.fetchCompanyDetailsDtos( "MKCOMP001" ).getFirst();

        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "MKCOMP001", "MKUser002" );

        final var expectedAssociationsList = new AssociationsList().totalResults( 3 ).items( List.of( marioAssociation, luigiAssociation, peachAssociation ) );
        Mockito.doReturn( expectedAssociationsList ).when( associationsService ).fetchUnexpiredAssociationsForCompanyAndStatuses( eq(mushroomKingdomCompany ), eq( fetchAllStatusesWithout( Set.of() ) ),  isNull(), isNull(), eq( 0 ), eq( 15 ) );

        final var response = mockMvc.perform( get( "/associations/companies/{company_number}?include_removed=true", "MKCOMP001" )
                        .header( "X-Request-Id", "theId123" )
                        .header( "ERIC-Identity", "MKUser002" )
                        .header( "ERIC-Identity-Type", "oauth2" ) )
                .andExpect( status().isOk() );

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var associations = associationsList.getItems();

        Assertions.assertEquals( 3, associationsList.getTotalResults() );

        for ( final Association association: associations ){
            final var expectedStatus = switch ( association.getId() ){
                case "MKAssociation001" -> "migrated";
                case "MKAssociation002" -> "confirmed";
                case "MKAssociation003" -> "removed";
                default -> "unknown";
            };

            final var expectedApprovalRoute = switch ( association.getId() ){
                case "MKAssociation001", "MKAssociation003" -> "migration";
                case "MKAssociation002" -> "auth_code";
                default -> "unknown";
            };

            Assertions.assertEquals( expectedStatus, association.getStatus().getValue() );
            Assertions.assertEquals( expectedApprovalRoute, association.getApprovalRoute().getValue() );
        }
    }

    @Test
    void getAssociationsForCompanyCanBeCalledByAdminUser() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "MKUser001" );
        Mockito.doReturn( false ).when( associationsService ).confirmedAssociationExists( "111111", "MKUser001" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );

        mockMvc.perform( get( "/associations/companies/111111" )
                        .header("X-Request-Id", "theId123")
                        .header( "ERIC-Identity", "MKUser001" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .header( "Eric-Authorised-Roles", ADMIN_READ_PERMISSION ) )
                .andExpect( status().isOk() );
    }

    @Test
    void getAssociationsForCompanyReturnsForbiddenWhenCalledByAUserThatIsNotAMemberOfCompanyOrAdmin() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "MKUser001" );
        Mockito.doReturn( false ).when( associationsService ).confirmedAssociationExists( "111111", "MKUser001" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );

        mockMvc.perform( get( "/associations/companies/111111" )
                        .header("X-Request-Id", "theId123")
                        .header( "ERIC-Identity", "MKUser001" )
                        .header( "ERIC-Identity-Type", "oauth2" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void getAssociationsForCompanyWithEmailReturnsData() throws Exception {
        final var companyProfile = testDataManager.fetchCompanyDetailsDtos( "MICOMP001" ).getFirst();
        final var user = testDataManager.fetchUserDtos( "MiUser002" ).getFirst();
        final var association = testDataManager.fetchAssociationDto( "MiAssociation002", user );
        final var expectedList = new AssociationsList();
        expectedList.addItemsItem( association );

        mockers.mockUsersServiceFetchUserDetails( "MiUser001" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "MICOMP001", "MiUser001" );
        mockers.mockUsersServiceSearchUserDetails( "MiUser002" );
        mockers.mockCompanyServiceFetchCompanyProfile( "MICOMP001" );
        Mockito.doReturn( expectedList ).when( associationsService ).fetchUnexpiredAssociationsForCompanyAndStatuses( companyProfile, Set.of( CONFIRMED, AWAITING_APPROVAL, MIGRATED ), "MiUser002", "lechuck.monkey.island@inugami-example.com", 0, 15 );

        final var response = mockMvc
                .perform( get( "/associations/companies/MICOMP001" )
                        .header("X-Request-Id", "theId123" )
                        .header( "ERIC-Identity", "MiUser001" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .header( "user_email", "lechuck.monkey.island@inugami-example.com" ) )
                .andExpect( status().isOk() );

        final var associationsList = parseResponseTo( response, AssociationsList.class );

        Assertions.assertEquals( 1, associationsList.getItems().size() );
        Assertions.assertEquals( "MiAssociation002", associationsList.getItems().getFirst().getId() );
    }

    @Test
    void getAssociationsForCompanyWithNonexistentUserReturnsEmptyList() throws Exception {
        final var companyProfile = testDataManager.fetchCompanyDetailsDtos( "MICOMP001" ).getFirst();
        final var expectedList = new AssociationsList();
        expectedList.setItems( List.of() );

        mockers.mockUsersServiceFetchUserDetails( "MiUser001" );
        Mockito.doReturn( true ).when( associationsService ).confirmedAssociationExists( "MICOMP001", "MiUser001" );
        mockers.mockUsersServiceSearchUserDetailsEmptyList( "MiUser002" );
        mockers.mockCompanyServiceFetchCompanyProfile( "MICOMP001" );
        Mockito.doReturn( expectedList ).when( associationsService ).fetchUnexpiredAssociationsForCompanyAndStatuses( companyProfile, Set.of( CONFIRMED, AWAITING_APPROVAL, MIGRATED ), null, "lechuck.monkey.island@inugami-example.com", 0, 15 );

        final var response = mockMvc
                .perform( get( "/associations/companies/MICOMP001" )
                        .header("X-Request-Id", "theId123" )
                        .header( "ERIC-Identity", "MiUser001" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .header( "user_email", "lechuck.monkey.island@inugami-example.com" ) )
                .andExpect( status().isOk() );

        final var associationsList = parseResponseTo( response, AssociationsList.class );

        Assertions.assertTrue( associationsList.getItems().isEmpty() );
    }

    @Test
    void getAssociationsForCompanyWithMalformedEmailReturnsBadRequest() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "MiUser001" );

        mockMvc.perform( get( "/associations/companies/MICOMP001" )
                        .header("X-Request-Id", "theId123" )
                        .header( "ERIC-Identity", "MiUser001" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .header( "user_email", "$$$@inugami-example.com" ) )
                .andExpect( status().isBadRequest() );
    }

}