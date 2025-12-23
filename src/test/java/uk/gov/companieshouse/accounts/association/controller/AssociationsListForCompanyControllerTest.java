package uk.gov.companieshouse.accounts.association.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.configuration.WebSecurityConfig;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.Links;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_READ_PERMISSION;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.fetchAllStatusesWithout;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ASSOCIATIONS;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ASSOCIATIONS_COMPANIES;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_AUTHORISED_ROLES;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_IDENTITY;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_IDENTITY_VALUE;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.IDS;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.KEY_ROLES_VALUE;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.MKCOMP_001;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.MK_ASSOCIATION_001;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.MK_USER_001;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.NON_EXISTING_COMPANY;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.OAUTH_2;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.WRONG_COMPANY_ID;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.X_REQUEST_ID;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.X_REQUEST_ID_VALUE;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.REMOVED;

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

    @MockitoBean
    private AssociationsService associationsService;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private UsersService usersService;

    @MockitoBean
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
        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + WRONG_COMPANY_ID)
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyWithNonexistentCompanyReturnsNotFound() throws Exception {
        String id = NON_EXISTING_COMPANY;
        mockers.mockCompanyServiceFetchCompanyProfileNotFound(id);
        Mockito.doReturn(true).when(associationsService).confirmedAssociationExists(id, ERIC_IDENTITY_VALUE);

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "/" + NON_EXISTING_COMPANY)
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationsForCompanyWithoutQueryParamsUsesDefaults() throws Exception {
        final var batmanUser = testDataManager.fetchUserDtos(ERIC_IDENTITY_VALUE).getFirst();
        final var batmanAssociation = testDataManager.fetchAssociationDto( "1", batmanUser );
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos(IDS).getFirst();

        mockers.mockCompanyServiceFetchCompanyProfile(IDS);
        Mockito.doReturn(true).when(associationsService).confirmedAssociationExists(IDS, ERIC_IDENTITY_VALUE);

        final var associationsList = new AssociationsList()
                .totalResults( 1 ).totalPages( 1 ).pageNumber( 0 ).itemsPerPage( 15 )
                .links(new Links().self(String.format("%s" + ASSOCIATIONS, internalApiUrl)).next(""))
                .items(List.of( batmanAssociation ));

        Mockito.doReturn(associationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( any(), eq( fetchAllStatusesWithout( Set.of( StatusEnum.REMOVED ) ) ),  isNull(), isNull(), eq( 0 ), eq( 15 ) );

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + IDS)
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isOk());

        Mockito.verify( associationsService ).fetchUnexpiredAssociationsForCompanyAndStatuses( eq( companyDetails ) ,  eq( fetchAllStatusesWithout( Set.of( StatusEnum.REMOVED ) ) ),  isNull(), isNull(), eq( 0 ), eq(15 ) );
    }

    @Test
    void getAssociationsForCompanySupportsRequestsFromAdminUsers() throws Exception {
        final var marioUser = testDataManager.fetchUserDtos(MK_USER_001).getFirst();
        final var marioAssociation = testDataManager.fetchAssociationDto(MK_ASSOCIATION_001, marioUser);
        final var associationsList = new AssociationsList()
                .totalResults( 1 ).totalPages( 1 ).pageNumber( 0 ).itemsPerPage( 15 )
                .links(new Links().self(String.format("%s" + ASSOCIATIONS, internalApiUrl)).next(""))
                .items( List.of( marioAssociation ) );

        mockers.mockCompanyServiceFetchCompanyProfile(MKCOMP_001);
        Mockito.doReturn(true).when(associationsService).confirmedAssociationExists(MKCOMP_001, "111");
        Mockito.doReturn(associationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( any(), eq( fetchAllStatusesWithout( Set.of( StatusEnum.REMOVED ) ) ),  isNull(), isNull(), eq( 0 ), eq( 15 ) );

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + MKCOMP_001)
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_ROLES, ADMIN_READ_PERMISSION))
                .andExpect( status().isOk() );
    }

    @Test
    void getAssociationsForCompanySupportsRequestsFromAPIKey() throws Exception {
        final var marioUser = testDataManager.fetchUserDtos(MK_USER_001).getFirst();
        final var marioAssociation = testDataManager.fetchAssociationDto(MK_ASSOCIATION_001, marioUser);
        final var associationsList = new AssociationsList()
                .totalResults( 1 ).totalPages( 1 ).pageNumber( 0 ).itemsPerPage( 15 )
                .links(new Links().self(String.format("%s" + ASSOCIATIONS, internalApiUrl)).next(""))
                .items( List.of( marioAssociation ) );

        mockers.mockCompanyServiceFetchCompanyProfile(MKCOMP_001);
        Mockito.doReturn(true).when(associationsService).confirmedAssociationExists(MKCOMP_001, ERIC_IDENTITY_VALUE);
        Mockito.doReturn(associationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( any(), eq( fetchAllStatusesWithout( Set.of( StatusEnum.REMOVED ) ) ),  isNull(), isNull(), eq( 0 ), eq( 15 ) );

        final var response = mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "/MKCOMP001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect( status().isOk() );

        final var associations = parseResponseTo( response, AssociationsList.class );

        final var items =
                associations.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue(items.contains(MK_ASSOCIATION_001));
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedFalseAppliesFilter() throws Exception {
        final var batmanUser = testDataManager.fetchUserDtos(ERIC_IDENTITY_VALUE).getFirst();
        final var batmanAssociation = testDataManager.fetchAssociationDto( "1", batmanUser );
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos(IDS).getFirst();

        mockers.mockCompanyServiceFetchCompanyProfile(IDS);
        Mockito.doReturn(true).when(associationsService).confirmedAssociationExists(IDS, ERIC_IDENTITY_VALUE);

        final var expectedAssociationsList = new AssociationsList()
                .totalResults( 1 ).totalPages( 1 ).pageNumber( 0 ).itemsPerPage( 15 )
                .links(new Links().self(String.format("%s" + ASSOCIATIONS, internalApiUrl)).next(""))
                .items(List.of( batmanAssociation ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( any(), eq( fetchAllStatusesWithout( Set.of( StatusEnum.REMOVED ) ) ), isNull(), isNull(), eq( 0 ), eq( 15 ) );

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "/111111?include_removed=false")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isOk());

        Mockito.verify( associationsService ).fetchUnexpiredAssociationsForCompanyAndStatuses( eq( companyDetails ) , eq( fetchAllStatusesWithout( Set.of( StatusEnum.REMOVED ) ) ),  isNull(), isNull(), eq( 0 ), eq( 15 ) );
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedTrueDoesNotApplyFilter() throws Exception {
        final var batmanUser = testDataManager.fetchUserDtos(ERIC_IDENTITY_VALUE).getFirst();
        final var batmanAssociation = testDataManager.fetchAssociationDto( "1", batmanUser );
        final var jokerUser = testDataManager.fetchUserDtos( "222" ).getFirst();
        final var jokerAssociation = testDataManager.fetchAssociationDto( "2", jokerUser );
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos(IDS).getFirst();

        mockers.mockCompanyServiceFetchCompanyProfile(IDS);
        Mockito.doReturn(true).when(associationsService).confirmedAssociationExists(IDS, ERIC_IDENTITY_VALUE);

        final var expectedAssociationsList = new AssociationsList()
                .totalResults( 2 ).totalPages( 1 ).pageNumber( 0 ).itemsPerPage( 15 )
                .links(new Links().self(String.format("%s" + ASSOCIATIONS, internalApiUrl)).next(""))
                .items(List.of( batmanAssociation, jokerAssociation ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( any(), eq( fetchAllStatusesWithout( Set.of() ) ),  isNull(), isNull(), eq( 0 ), eq( 15 ) );

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "/111111?include_removed=true")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isOk());

        Mockito.verify( associationsService ).fetchUnexpiredAssociationsForCompanyAndStatuses( eq( companyDetails ), eq( fetchAllStatusesWithout( Set.of() ) ),  isNull(), isNull(), eq ( 0 ), eq( 15 ) );
    }

    @Test
    void getAssociationsForCompanyPaginatesCorrectly() throws Exception {
        final var jokerUser = testDataManager.fetchUserDtos( "222" ).getFirst();
        final var jokerAssociation = testDataManager.fetchAssociationDto( "2", jokerUser );

        mockers.mockCompanyServiceFetchCompanyProfile(IDS);
        Mockito.doReturn(true).when(associationsService).confirmedAssociationExists(IDS, ERIC_IDENTITY_VALUE);

        final var expectedAssociationsList = new AssociationsList()
                .totalResults( 2 ).totalPages( 2 ).pageNumber( 1 ).itemsPerPage( 1 )
                .links(new Links().self(String.format("%s" + ASSOCIATIONS, internalApiUrl)).next(""))
                .items(List.of( jokerAssociation ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( any(), eq( fetchAllStatusesWithout( Set.of() ) ),  isNull(), isNull(), eq( 1 ), eq( 1 ) );

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "/111111?include_removed=true&items_per_page=1&page_index=1")
                                .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                                .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                                .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( Association::getId )
                        .toList();

        Assertions.assertTrue( items.contains( "2" ) );
        Assertions.assertEquals(String.format("%s" + ASSOCIATIONS, internalApiUrl), links.getSelf());
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 1, associationsList.getPageNumber() );
        Assertions.assertEquals( 1, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 2, associationsList.getTotalResults() );
        Assertions.assertEquals( 2, associationsList.getTotalPages() );
    }

    @Test
    void getAssociationsForCompanyDoesMappingCorrectly() throws Exception {
        final var batmanUser = testDataManager.fetchUserDtos(ERIC_IDENTITY_VALUE).getFirst();
        final var batmanAssociation = testDataManager.fetchAssociationDto( "1", batmanUser );
        final var jokerUser = testDataManager.fetchUserDtos( "222" ).getFirst();
        final var jokerAssociation = testDataManager.fetchAssociationDto( "2", jokerUser );

        mockers.mockCompanyServiceFetchCompanyProfile(IDS);
        Mockito.doReturn(true).when(associationsService).confirmedAssociationExists(IDS, ERIC_IDENTITY_VALUE);

        final var expectedAssociationsList = new AssociationsList()
                .totalResults( 2 ).totalPages( 1 ).pageNumber( 0 ).itemsPerPage( 2 )
                .links(new Links().self(String.format("%s" + ASSOCIATIONS, internalApiUrl)).next(""))
                .items(List.of( batmanAssociation, jokerAssociation ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( any(), eq( fetchAllStatusesWithout( Set.of() ) ),  isNull(), isNull(), eq( 0 ), eq( 2 ) );

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "/111111?include_removed=true&items_per_page=2&page_index=0").header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                                .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                                .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                        .andExpect(status().isOk());
        final var associationsList = parseResponseTo( response, AssociationsList.class );

        final var associations = associationsList.getItems();
        final var firstAssociation = associations.getFirst();

        Assertions.assertEquals( "a", firstAssociation.getEtag() );
        Assertions.assertEquals( "1", firstAssociation.getId() );
        Assertions.assertEquals(ERIC_IDENTITY_VALUE, firstAssociation.getUserId());
        Assertions.assertEquals( "bruce.wayne@gotham.city", firstAssociation.getUserEmail() );
        Assertions.assertEquals( "Batman", firstAssociation.getDisplayName() );
        Assertions.assertEquals(IDS, firstAssociation.getCompanyNumber());
        Assertions.assertEquals( "Wayne Enterprises", firstAssociation.getCompanyName() );
        Assertions.assertEquals( CONFIRMED, firstAssociation.getStatus() );
        Assertions.assertNull( firstAssociation.getCreatedAt() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(1) ), localDateTimeToNormalisedString( firstAssociation.getApprovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(2) ), localDateTimeToNormalisedString( firstAssociation.getRemovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( DEFAULT_KIND, firstAssociation.getKind() );
        Assertions.assertEquals( ApprovalRouteEnum.AUTH_CODE, firstAssociation.getApprovalRoute() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(3) ), reduceTimestampResolution( firstAssociation.getApprovalExpiryAt() ) );
        Assertions.assertEquals(ASSOCIATIONS + "/1", firstAssociation.getLinks().getSelf());

        final var secondAssociation = associations.get( 1 );
        Assertions.assertEquals( "222", secondAssociation.getUserId() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, secondAssociation.getDisplayName() );
        Assertions.assertEquals( "Wayne Enterprises", secondAssociation.getCompanyName() );
    }

    @Test
    void getAssociationsForCompanyWithUnacceptablePaginationParametersShouldReturnBadRequest() throws Exception {
        Mockito.doReturn(true).when(associationsService).confirmedAssociationExists(IDS, ERIC_IDENTITY_VALUE);

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "/111111?include_removed=true&items_per_page=1&page_index=-1")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "/111111?include_removed=true&items_per_page=0&page_index=0")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyFetchesAssociation() throws Exception {
        final var batmanUser = testDataManager.fetchUserDtos(ERIC_IDENTITY_VALUE).getFirst();
        final var associationOne = testDataManager.fetchAssociationDto( "1", batmanUser );
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos(IDS).getFirst();

        mockers.mockCompanyServiceFetchCompanyProfile(IDS);
        Mockito.doReturn(true).when(associationsService).confirmedAssociationExists(IDS, ERIC_IDENTITY_VALUE);

        final var expectedAssociationsList = new AssociationsList().totalResults( 1 ).items(List.of( associationOne ));
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( eq( companyDetails ), eq( fetchAllStatusesWithout( Set.of( StatusEnum.REMOVED ) ) ),  isNull(), isNull(), eq( 0 ), eq(15 ) );

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "/{company_number}?user_email=bruce.wayne@gotham.city", IDS).header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                                .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                                .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );

        Assertions.assertEquals( 1, associationsList.getTotalResults() );
        Assertions.assertEquals( "1", associationsList.getItems().getFirst().getId() );
    }

    @Test
    void getAssociationsForCompanyWithNonexistentUserEmailFetchesEmptyList() throws Exception {
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos(IDS).getFirst();

        mockers.mockCompanyServiceFetchCompanyProfile(IDS);

        Mockito.doReturn(true).when(associationsService).confirmedAssociationExists(IDS, ERIC_IDENTITY_VALUE);

        final var expectedAssociationsList = new AssociationsList().totalResults( 0 ).items(List.of());
        Mockito.doReturn(expectedAssociationsList).when(associationsService).fetchUnexpiredAssociationsForCompanyAndStatuses( eq( companyDetails ), eq( fetchAllStatusesWithout ( Set.of( StatusEnum.REMOVED ) ) ),  isNull(), isNull(), eq(0 ) , eq(15 ) );

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "/111111?user_email=the.void@space.com").header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                                .header(ERIC_IDENTITY, ERIC_IDENTITY_VALUE)
                                .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );

        Assertions.assertEquals( 0, associationsList.getTotalResults() );
    }

    @Test
    void getAssociationsForCompanyCanRetrieveMigratedAssociations() throws Exception {
        final var marioUser = testDataManager.fetchUserDtos(MK_USER_001).getFirst();
        final var luigiUser = testDataManager.fetchUserDtos( "MKUser002" ).getFirst();
        final var peachUser = testDataManager.fetchUserDtos( "MKUser003" ).getFirst();
        final var marioAssociation = testDataManager.fetchAssociationDto(MK_ASSOCIATION_001, marioUser);
        final var luigiAssociation = testDataManager.fetchAssociationDto( "MKAssociation002", luigiUser );
        final var peachAssociation = testDataManager.fetchAssociationDto( "MKAssociation003", peachUser );
        final var mushroomKingdomCompany = testDataManager.fetchCompanyDetailsDtos(MKCOMP_001).getFirst();

        mockers.mockCompanyServiceFetchCompanyProfile(MKCOMP_001);
        Mockito.doReturn(true).when(associationsService).confirmedAssociationExists(MKCOMP_001, "MKUser002");

        final var expectedAssociationsList = new AssociationsList().totalResults( 3 ).items( List.of( marioAssociation, luigiAssociation, peachAssociation ) );
        Mockito.doReturn( expectedAssociationsList ).when( associationsService ).fetchUnexpiredAssociationsForCompanyAndStatuses( eq(mushroomKingdomCompany ), eq( fetchAllStatusesWithout( Set.of() ) ),  isNull(), isNull(), eq( 0 ), eq( 15 ) );

        final var response = mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "/{company_number}?include_removed=true", MKCOMP_001)
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect( status().isOk() );

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var associations = associationsList.getItems();

        Assertions.assertEquals( 3, associationsList.getTotalResults() );

        for ( final Association association: associations ){
            final var expectedStatus = switch ( association.getId() ){
                case MK_ASSOCIATION_001 -> "migrated";
                case "MKAssociation002" -> "confirmed";
                case "MKAssociation003" -> "removed";
                default -> "unknown";
            };

            final var expectedApprovalRoute = switch ( association.getId() ){
                case MK_ASSOCIATION_001, "MKAssociation003" -> "migration";
                case "MKAssociation002" -> "auth_code";
                default -> "unknown";
            };

            Assertions.assertEquals( expectedStatus, association.getStatus().getValue() );
            Assertions.assertEquals( expectedApprovalRoute, association.getApprovalRoute().getValue() );
        }
    }

    @Test
    void getAssociationsForCompanyReturnsForbiddenWhenCalledByAUserThatIsNotAMemberOfCompanyOrAdmin() throws Exception {
        mockers.mockUsersServiceFetchUserDetails(MK_USER_001);
        Mockito.doReturn(false).when(associationsService).confirmedAssociationExists(IDS, MK_USER_001);
        mockers.mockCompanyServiceFetchCompanyProfile(IDS);

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + IDS)
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, MK_USER_001)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect( status().isForbidden() );
    }


    @Test
    void getAssociationsForCompanyUserAndStatusWithUserIdFetchesAssociations() throws Exception {
        final var user = testDataManager.fetchUserDtos( "MKUser002" ).getFirst();
        final var company = testDataManager.fetchCompanyDetailsDtos(MKCOMP_001).getFirst();
        mockers.mockCompanyServiceFetchCompanyProfile(MKCOMP_001);
        Mockito.doReturn( testDataManager.fetchUserDtos("MKUser002" ).getFirst() ).when( usersService ).retrieveUserDetails("MKUser002", null );
        Mockito.doReturn(Optional.of( testDataManager.fetchAssociationDto( "MKAssociation002" , user ) ) ).when( associationsService ).fetchUnexpiredAssociationsForCompanyUserAndStatuses( company, Set.of( CONFIRMED, REMOVED ), user, "luigi@mushroom.kingdom" );

        final var response = mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "/MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "test")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( "{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} " ) )
                .andExpect( status().isOk() );

        final var association = parseResponseTo( response, Association.class );
        Assertions.assertEquals( "MKAssociation002", association.getId() );
    }

    static Stream<Arguments> getAssociationsForCompanyUserAndStatusHappyCaseScenarios(){
        return Stream.of(
                Arguments.of( ", \"status\":[\"confirmed\", \"removed\"]" ),
                Arguments.of( ", \"status\":[]" ),
                Arguments.of( "" )

        );
    }

    @ParameterizedTest
    @MethodSource("getAssociationsForCompanyUserAndStatusHappyCaseScenarios")
    void getAssociationsForCompanyUserAndStatusWithUserEmailFetchesAssociations( final String status ) throws Exception {
        final var user = testDataManager.fetchUserDtos( "MKUser002" ).getFirst();
        final var company = testDataManager.fetchCompanyDetailsDtos(MKCOMP_001).getFirst();
        mockers.mockCompanyServiceFetchCompanyProfile(MKCOMP_001);
        Mockito.doReturn( testDataManager.fetchUserDtos("MKUser002" ).getFirst() ).when( usersService ).retrieveUserDetails(null, "luigi@mushroom.kingdom" );
        Mockito.doReturn(Optional.of( testDataManager.fetchAssociationDto( "MKAssociation002" , user ) ) ).when( associationsService ).fetchUnexpiredAssociationsForCompanyUserAndStatuses( eq( company ), any(), eq( user ), eq( "luigi@mushroom.kingdom" ) );

        final var response = mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "/MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "test")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( String.format( "{\"user_email\":\"luigi@mushroom.kingdom\" %s} ", status) ) )
                .andExpect( status().isOk() );

        final var association = parseResponseTo( response, Association.class );
        Assertions.assertEquals( "MKAssociation002", association.getId() );
    }

    @Test
    void getAssociationsForCompanyUserWithNonExistentUserEmailFetchesAssociations( ) throws Exception {
        final var user = testDataManager.fetchUserDtos(MK_USER_001).getFirst();
        final var company = testDataManager.fetchCompanyDetailsDtos(MKCOMP_001).getFirst();
        mockers.mockCompanyServiceFetchCompanyProfile(MKCOMP_001);
        Mockito.doReturn( null ).when( usersService ).retrieveUserDetails(null, "mario@mushroom.kingdom" );
        Mockito.doReturn(Optional.of(testDataManager.fetchAssociationDto(MK_ASSOCIATION_001, user))).when(associationsService).fetchUnexpiredAssociationsForCompanyUserAndStatuses(company, Set.of(MIGRATED), null, "mario@mushroom.kingdom");
        final var response = mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "/MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "test")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content(  "{\"user_email\":\"mario@mushroom.kingdom\" , \"status\":[\"migrated\"] }" ) )
                .andExpect( status().isOk() );

        final var association = parseResponseTo( response, Association.class );
        Assertions.assertEquals(MK_ASSOCIATION_001, association.getId());
    }

    static Stream<Arguments> getAssociationsForCompanyUserMalformedScenarios(){
        return Stream.of(
                Arguments.of("$$$$$", MKCOMP_001, ", \"status\":[\"confirmed\", \"removed\"]"),
                Arguments.of( "MKUser002" ,"$$$$$", ", \"status\":[\"confirmed\", \"removed\"]" ),
                Arguments.of("MKUser002", MKCOMP_001, ", \"status\":[\"" + WRONG_COMPANY_ID + "\", \"removed\"]")

        );
    }

    @ParameterizedTest
    @MethodSource("getAssociationsForCompanyUserMalformedScenarios")
    void getAssociationsForCompanyUserAndStatusMalformedReturnsBadRequests(final String userId, final String companyNumber, final String status) throws Exception {
        mockMvc.perform(post(String.format(ASSOCIATIONS_COMPANIES + "/%s/search", companyNumber))
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "test")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( String.format( "{\"user_id\":\"%s\" %s }", userId , status ) ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAssociationsForCompanyUserAndStatusWithMalformedEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "/MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "test")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( "{\"user_email\":\"$\\(){}$$$$@mushroomkingdom\" " ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAssociationsForCompanyUserAndStatusWithoutUserReturnsBadRequest() throws Exception {
        mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "/MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "test")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( "{ \"status\":[\"confirmed\", \"removed\"]} " ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAssociationsForCompanyUserAndStatusWithMalformedUserEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "/MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "test")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( "\"user_email\":\"$$$$$@mushroom.kingdom\" ,\"status\":[\"confirmed\", \"removed\"]} " ) )
                .andExpect( status().isBadRequest() );
    }

    static Stream<Arguments> getAssociationsForCompanyMalformedBodyScenarios(){
        return Stream.of(
                Arguments.of( "" ),
                Arguments.of( "{ \"user_email\":\"111@mushroom.kingdom\", \"user_id\":\"111\" }"  )

        );
    }

    @ParameterizedTest
    @MethodSource("getAssociationsForCompanyMalformedBodyScenarios")
    void getAssociationsForCompanyMalformedBodyScenarios( final String body ) throws Exception{
        mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "/MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "test")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( body ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAssociationsForCompanyWithNonExistentCompanyNumberReturnsNotFound() throws Exception {
        final var user = testDataManager.fetchUserDtos( "MKUser002" ).getFirst();
        final var company = testDataManager.fetchCompanyDetailsDtos(MKCOMP_001).getFirst();
        mockers.mockCompanyServiceFetchCompanyProfileNotFound(MKCOMP_001);
        Mockito.doReturn( testDataManager.fetchUserDtos("MKUser002" ).getFirst() ).when( usersService ).retrieveUserDetails("MKUser002", null );
        Mockito.doReturn(Optional.of( testDataManager.fetchAssociationDto( "MKAssociation002" , user ) ) ).when( associationsService ).fetchUnexpiredAssociationsForCompanyUserAndStatuses( company, Set.of( CONFIRMED, REMOVED ), user, "luigi@mushroom.kingdom" );

        mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "/MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "test")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( "{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} " ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void getAssociationsForCompanyWithNonExistentUserIdReturnsNotFound() throws Exception {
        final var user = testDataManager.fetchUserDtos( "MKUser002" ).getFirst();
        final var company = testDataManager.fetchCompanyDetailsDtos(MKCOMP_001).getFirst();
        mockers.mockCompanyServiceFetchCompanyProfile(MKCOMP_001);
        Mockito.doThrow(new NotFoundRuntimeException(X_REQUEST_ID_VALUE, "Test", new Exception())).when(usersService).retrieveUserDetails("MKUser002", null);
        Mockito.doReturn(Optional.of( testDataManager.fetchAssociationDto( "MKAssociation002" , user ) ) ).when( associationsService ).fetchUnexpiredAssociationsForCompanyUserAndStatuses( company, Set.of( CONFIRMED, REMOVED ), user, "luigi@mushroom.kingdom" );

        mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "/MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "test")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( "{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} " ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void getAssociationsForCompanyNonExistentAssociationsReturnsNotFound() throws Exception {
        final var user = testDataManager.fetchUserDtos( "MKUser002" ).getFirst();
        final var company = testDataManager.fetchCompanyDetailsDtos(MKCOMP_001).getFirst();
        mockers.mockCompanyServiceFetchCompanyProfile(MKCOMP_001);
        Mockito.doReturn( testDataManager.fetchUserDtos("MKUser002" ).getFirst() ).when( usersService ).retrieveUserDetails("MKUser002", null );
        Mockito.doReturn(Optional.empty( ) ).when( associationsService ).fetchUnexpiredAssociationsForCompanyUserAndStatuses( company, Set.of( CONFIRMED, REMOVED ), user, "luigi@mushroom.kingdom" );

        mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "/MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "test")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( "{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} " ) )
                .andExpect( status().isNotFound() );
    }
}