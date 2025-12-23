package uk.gov.companieshouse.accounts.association.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.accounts.association.client.EmailClient;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_READ_PERMISSION;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ASSOCIATIONS;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ASSOCIATIONS_COMPANIES;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_AUTHORISED_ROLES;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_IDENTITY;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_ID_VALUE;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.KEY_ROLES_VALUE;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.MK_USER_001;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.NON_EXISTING_COMPANY;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.OAUTH_2;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.X_REQUEST_ID;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.X_REQUEST_ID_VALUE;

@AutoConfigureMockMvc
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class AssociationsListForCompanyControllerTest extends BaseMongoIntegration {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CompanyService companyService;

    @MockitoBean
    private UsersService usersService;
    @Mock
    private EmailClient emailClient;
    @Autowired
    private AssociationsRepository associationsRepository;

    private static final String DEFAULT_KIND = "association";
    private static final String DEFAULT_DISPLAY_NAME = "Not provided";

    private final LocalDateTime now = LocalDateTime.now();

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private Mockers mockers;

    @BeforeEach
    void setup(){
        mockers = new Mockers(null, emailClient, companyService, usersService);
    }

    @Test
    void getAssociationsForCompanyWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "$$$$$$")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyWithNonexistentCompanyReturnsForbidden() throws Exception {
        mockers.mockCompanyServiceFetchCompanyProfileNotFound(NON_EXISTING_COMPANY);
        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + NON_EXISTING_COMPANY)
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAssociationsForCompanyWithoutQueryParamsUsesDefaults() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
        mockers.mockUsersServiceFetchUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "111111")
                                .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                                .header(ERIC_IDENTITY, "111")
                                .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items = associationsList.getItems()
                .stream()
                .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" ) ) );
        Assertions.assertEquals(ASSOCIATIONS_COMPANIES + "111111?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 13, associationsList.getTotalResults() );
        Assertions.assertEquals( 1, associationsList.getTotalPages() );
    }

    @Test
    void getAssociationsForCompanySupportsRequestsFromAdminUsers() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockUsersServiceFetchUserDetails(MK_USER_001);
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "MKCOMP001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_ROLES, ADMIN_READ_PERMISSION))
                .andExpect( status().isOk() );
    }

    @Test
    void getAssociationsForCompanySupportsRequestsFromAPIKey() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockUsersServiceFetchUserDetails(MK_USER_001);
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );

        final var response = mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "MKCOMP001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect( status().isOk() );

        final var associationsList = parseResponseTo( response, AssociationsList.class );

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.contains(  "MKAssociation001" ) );
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedFalseAppliesFilter() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
        mockers.mockUsersServiceFetchUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "111111?include_removed=false")
                                .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                                .header(ERIC_IDENTITY, "111")
                                .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" ) ) );
        Assertions.assertEquals(ASSOCIATIONS_COMPANIES + "111111?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 13, associationsList.getTotalResults() );
        Assertions.assertEquals( 1, associationsList.getTotalPages() );
    }

    @Test
    void getAssociationsForCompanyWithIncludeRemovedTrueDoesNotApplyFilter() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
        mockers.mockUsersServiceFetchUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "111111?include_removed=true")
                                .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                                .header(ERIC_IDENTITY, "111")
                                .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15" ) ) );
        Assertions.assertEquals(ASSOCIATIONS_COMPANIES + "111111?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals(ASSOCIATIONS_COMPANIES + "111111?page_index=1&items_per_page=15", links.getNext());
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 16, associationsList.getTotalResults() );
        Assertions.assertEquals( 2, associationsList.getTotalPages() );
    }

    @Test
    void getAssociationsForCompanyPaginatesCorrectly() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
        mockers.mockUsersServiceFetchUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "111111?include_removed=true&items_per_page=3&page_index=2")
                                .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                                .header(ERIC_IDENTITY, "111")
                                .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "7", "8", "9" ) ) );
        Assertions.assertEquals(ASSOCIATIONS_COMPANIES + "111111?page_index=2&items_per_page=3", links.getSelf());
        Assertions.assertEquals(ASSOCIATIONS_COMPANIES + "111111?page_index=3&items_per_page=3", links.getNext());
        Assertions.assertEquals( 2, associationsList.getPageNumber() );
        Assertions.assertEquals( 3, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 16, associationsList.getTotalResults() );
        Assertions.assertEquals( 6, associationsList.getTotalPages() );
    }

    @Test
    void getAssociationsForCompanyWhereAccountsUserEndpointCannotFindUserReturnsNotFound() throws Exception {
        final var associations = testDataManager.fetchAssociationDaos( "1" );
        associations.add( testDataManager.fetchAssociationDaos( "18" ).getFirst().companyNumber( "111111" ) );

        associationsRepository.insert( associations );
        mockers.mockUsersServiceFetchUserDetails(ERIC_ID_VALUE);
        mockers.mockUsersServiceFetchUserDetailsNotFound( "111" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "111111")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_ID_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationsForCompanyWhereCompanyProfileEndpointCannotFindCompanyReturnsNotFound() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        mockers.mockUsersServiceFetchUserDetails( "111" );
        mockers.mockCompanyServiceFetchCompanyProfileNotFound( "111111" );

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "111111")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationsForCompanyDoesMappingCorrectly() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
        mockers.mockUsersServiceFetchUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "111111?include_removed=true&items_per_page=2&page_index=0")
                                .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                                .header(ERIC_IDENTITY, "111")
                                .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );

        final var associations = associationsList.getItems();
        final var associationOne = associations.getFirst();

        Assertions.assertEquals( "a", associationOne.getEtag() );
        Assertions.assertEquals( "1", associationOne.getId() );
        Assertions.assertEquals( "111", associationOne.getUserId() );
        Assertions.assertEquals( "bruce.wayne@gotham.city", associationOne.getUserEmail() );
        Assertions.assertEquals( "Batman", associationOne.getDisplayName() );
        Assertions.assertEquals( "111111", associationOne.getCompanyNumber() );
        Assertions.assertEquals( "Wayne Enterprises", associationOne.getCompanyName() );
        Assertions.assertEquals( StatusEnum.CONFIRMED, associationOne.getStatus() );
        Assertions.assertNotNull( associationOne.getCreatedAt() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(1) ), localDateTimeToNormalisedString( associationOne.getApprovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(2) ), localDateTimeToNormalisedString( associationOne.getRemovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( DEFAULT_KIND, associationOne.getKind() );
        Assertions.assertEquals( ApprovalRouteEnum.AUTH_CODE, associationOne.getApprovalRoute() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(3) ), reduceTimestampResolution( associationOne.getApprovalExpiryAt() ) );
        Assertions.assertEquals(ASSOCIATIONS + "/1", associationOne.getLinks().getSelf());

        final var associationTwo = associations.get( 1 );
        Assertions.assertEquals( "222", associationTwo.getUserId() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, associationTwo.getDisplayName() );
        Assertions.assertEquals( "Wayne Enterprises", associationTwo.getCompanyName() );
    }

    @Test
    void getAssociationsForCompanyWithUnacceptablePaginationParametersShouldReturnBadRequest() throws Exception {
        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "111111?include_removed=true&items_per_page=1&page_index=-1")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "111111?include_removed=true&items_per_page=0&page_index=0")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationsForCompanyFetchesAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
        mockers.mockUsersServiceFetchUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "111111")
                                .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                                .header(ERIC_IDENTITY, "111")
                                .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                        .andExpect(status().isOk());

        final var associationsList = parseResponseTo( response, AssociationsList.class );

        Assertions.assertEquals( 13, associationsList.getTotalResults() );
    }

    @Test
    void getAssociationsForCompanyCanRetrieveMigratedAssociations() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002", "MKAssociation003" ) );
        mockers.mockUsersServiceFetchUserDetails(MK_USER_001, "MKUser002", "MKUser003");
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );

        final var response = mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "MKCOMP001?include_removed=true")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
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
    void getAssociationsForCompanyReturnsForbiddenWhenCalledByAUserThatIsNotAMemberOfCompanyOrAdmin() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
        mockers.mockUsersServiceFetchUserDetails("111", "222", "333", "444", "555", "666", "777", "888", "999", "1111", "2222", "3333", "4444", "5555", "6666", "7777", MK_USER_001);
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );

        mockMvc.perform(get(ASSOCIATIONS_COMPANIES + "111111")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, MK_USER_001)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect( status().isForbidden() );
    }

    @Test
    void getAssociationsForCompanyUserAndStatusWithUserIdFetchesAssociations() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation002" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doReturn( testDataManager.fetchUserDtos("MKUser002" ).getFirst() ).when( usersService ).retrieveUserDetails("MKUser002", null );

        final var response = mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "MKCOMP001/search")
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
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation002" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doReturn( testDataManager.fetchUserDtos("MKUser002" ).getFirst() ).when( usersService ).retrieveUserDetails(null, "luigi@mushroom.kingdom" );

        final var response = mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "MKCOMP001/search")
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
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doReturn( null ).when( usersService ).retrieveUserDetails(null, "mario@mushroom.kingdom" );

        final var response = mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "test")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content(  "{\"user_email\":\"mario@mushroom.kingdom\" , \"status\":[\"migrated\"] }" ) )
                .andExpect( status().isOk() );

        final var association = parseResponseTo( response, Association.class );
        Assertions.assertEquals( "MKAssociation001", association.getId() );
    }

    static Stream<Arguments> getAssociationsForCompanyUserMalformedScenarios(){
        return Stream.of(
                Arguments.of( "$$$$$" ,"MKCOMP001", ", \"status\":[\"confirmed\", \"removed\"]" ),
                Arguments.of( "MKUser002" ,"$$$$$", ", \"status\":[\"confirmed\", \"removed\"]" ),
                Arguments.of( "MKUser002" ,"MKCOMP001", ", \"status\":[\"$$$$$$\", \"removed\"]" )

        );
    }

    @ParameterizedTest
    @MethodSource("getAssociationsForCompanyUserMalformedScenarios")
    void getAssociationsForCompanyUserAndStatusMalformedReturnsBadRequests(final String userId, final String companyNumber, final String status) throws Exception {
        mockMvc.perform(post(String.format(ASSOCIATIONS_COMPANIES + "%s/search", companyNumber))
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header( "ERIC-Identity", "test" )
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( String.format( "{\"user_id\":\"%s\" %s }", userId , status ) ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAssociationsForCompanyUserAndStatusWithMalformedEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header( "ERIC-Identity", "test" )
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( "{\"user_email\":\"$\\(){}$$$$@mushroomkingdom\" }" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAssociationsForCompanyUserAndStatusWithoutUserReturnsBadRequest() throws Exception {
        mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header( "ERIC-Identity", "test" )
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( "{ \"status\":[\"confirmed\", \"removed\"]} " ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAssociationsForCompanyUserAndStatusWithMalformedUserEmailReturnsBadRequest() throws Exception {
        mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header( "ERIC-Identity", "test" )
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( "\"user_email\":\"$$$$$@mushroomkingdom\" ,\"status\":[\"confirmed\", \"removed\"]} " ) )
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
        mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header( "ERIC-Identity", "test" )
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( body ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAssociationsForCompanyWithNonExistentCompanyNumberReturnsNotFound() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation002" ) );
        mockers.mockCompanyServiceFetchCompanyProfileNotFound( "MKCOMP001" );
        Mockito.doReturn( testDataManager.fetchUserDtos("MKUser002" ).getFirst() ).when( usersService ).retrieveUserDetails("MKUser002", null );

        mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header( "ERIC-Identity", "test" )
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( "{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} " ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void getAssociationsForCompanyWithNonExistentUserIdReturnsNotFound() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation002" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doThrow(new NotFoundRuntimeException(X_REQUEST_ID_VALUE, "Test", new Exception())).when(usersService).retrieveUserDetails("MKUser002", null);

        mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header( "ERIC-Identity", "test" )
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( "{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} " ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void getAssociationsForCompanyNonExistentAssociationsReturnsNotFound() throws Exception {
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doReturn( testDataManager.fetchUserDtos("MKUser002" ).getFirst() ).when( usersService ).retrieveUserDetails("MKUser002", null );

        mockMvc.perform(post(ASSOCIATIONS_COMPANIES + "MKCOMP001/search")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header( "ERIC-Identity", "test" )
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType(MediaType.APPLICATION_JSON )
                        .content( "{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} " ) )
                .andExpect( status().isNotFound() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }

}