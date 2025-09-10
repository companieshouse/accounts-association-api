//package uk.gov.companieshouse.accounts.association.integration;
//
//import org.junit.jupiter.api.*;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.junit.jupiter.params.ParameterizedTest;
//import org.junit.jupiter.params.provider.Arguments;
//import org.junit.jupiter.params.provider.MethodSource;
//import org.mockito.Mockito;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.data.mongodb.core.MongoTemplate;
//import org.springframework.http.MediaType;
//import org.springframework.test.web.servlet.MockMvc;
//import uk.gov.companieshouse.accounts.association.common.Mockers;
//import uk.gov.companieshouse.accounts.association.common.TestDataManager;
//import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
//import uk.gov.companieshouse.accounts.association.models.AssociationDao;
//import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
//import uk.gov.companieshouse.accounts.association.service.CompanyService;
//import uk.gov.companieshouse.accounts.association.service.UsersService;
//import uk.gov.companieshouse.api.accounts.associations.model.Association;
//import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
//import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
//import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
//import uk.gov.companieshouse.email_producer.EmailProducer;
//import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.stream.Stream;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
//import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
//import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
//import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_READ_PERMISSION;
//
//@AutoConfigureMockMvc
//@SpringBootTest
//@ExtendWith(MockitoExtension.class)
//@Tag("integration-test")
//class AssociationsListForCompanyControllerTest {
//
//    @Autowired
//    private MongoTemplate mongoTemplate;
//
//    @Autowired
//    private MockMvc mockMvc;
//
//    @MockBean
//    private CompanyService companyService;
//
//    @MockBean
//    private UsersService usersService;
//
//    @MockBean
//    private EmailProducer emailProducer;
//
//    @MockBean
//    private KafkaProducerFactory kafkaProducerFactory;
//
//    @Autowired
//    private AssociationsRepository associationsRepository;
//
//    private static final String DEFAULT_KIND = "association";
//    private static final String DEFAULT_DISPLAY_NAME = "Not provided";
//
//    private final LocalDateTime now = LocalDateTime.now();
//
//    private static final TestDataManager testDataManager = TestDataManager.getInstance();
//
//    private Mockers mockers;
//
//    @BeforeEach
//    void setup(){
//        mockers = new Mockers( null, emailProducer, companyService, usersService );
//    }
//
//    @Test
//    void getAssociationsForCompanyWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
//        mockMvc.perform( get( "/associations/companies/$$$$$$" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "111" )
//                        .header( "ERIC-Identity-Type", "oauth2" ) )
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void getAssociationsForCompanyWithNonexistentCompanyReturnsForbidden() throws Exception {
//        mockers.mockCompanyServiceFetchCompanyProfileNotFound( "919191" );
//        mockMvc.perform( get( "/associations/companies/919191" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "111" )
//                        .header( "ERIC-Identity-Type", "oauth2" ) )
//                .andExpect(status().isForbidden());
//    }
//
//    @Test
//    void getAssociationsForCompanyWithoutQueryParamsUsesDefaults() throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
//        mockers.mockUsersServiceFetchUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
//        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
//
//        final var response =
//                mockMvc.perform( get( "/associations/companies/111111" )
//                                .header("X-Request-Id", "theId123")
//                                .header( "ERIC-Identity", "111" )
//                                .header( "ERIC-Identity-Type", "oauth2" ) )
//                        .andExpect(status().isOk());
//
//        final var associationsList = parseResponseTo( response, AssociationsList.class );
//        final var links = associationsList.getLinks();
//
//        final var items = associationsList.getItems()
//                .stream()
//                .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
//                .toList();
//
//        Assertions.assertTrue( items.containsAll( List.of ( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" ) ) );
//        Assertions.assertEquals( "/associations/companies/111111?page_index=0&items_per_page=15", links.getSelf() );
//        Assertions.assertEquals( "", links.getNext() );
//        Assertions.assertEquals( 0, associationsList.getPageNumber() );
//        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
//        Assertions.assertEquals( 13, associationsList.getTotalResults() );
//        Assertions.assertEquals( 1, associationsList.getTotalPages() );
//    }
//
//    @Test
//    void getAssociationsForCompanySupportsRequestsFromAdminUsers() throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
//        mockers.mockUsersServiceFetchUserDetails( "MKUser001" );
//        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
//
//        mockMvc.perform( get( "/associations/companies/MKCOMP001" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "111" )
//                        .header( "ERIC-Identity-Type", "oauth2" )
//                        .header( "ERIC-Authorised-Roles", ADMIN_READ_PERMISSION ) )
//                .andExpect( status().isOk() );
//    }
//
//    @Test
//    void getAssociationsForCompanySupportsRequestsFromAPIKey() throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
//        mockers.mockUsersServiceFetchUserDetails( "MKUser001" );
//        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
//
//        final var response = mockMvc.perform( get( "/associations/companies/MKCOMP001" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "111" )
//                        .header( "ERIC-Identity-Type", "key" )
//                        .header("ERIC-Authorised-Key-Roles", "*") )
//                .andExpect( status().isOk() );
//
//        final var associationsList = parseResponseTo( response, AssociationsList.class );
//
//        final var items =
//                associationsList.getItems()
//                        .stream()
//                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
//                        .toList();
//
//        Assertions.assertTrue( items.contains(  "MKAssociation001" ) );
//    }
//
//    @Test
//    void getAssociationsForCompanyWithIncludeRemovedFalseAppliesFilter() throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
//        mockers.mockUsersServiceFetchUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
//        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
//
//        final var response =
//                mockMvc.perform( get( "/associations/companies/111111?include_removed=false" )
//                                .header("X-Request-Id", "theId123")
//                                .header( "ERIC-Identity", "111" )
//                                .header( "ERIC-Identity-Type", "oauth2" ) )
//                        .andExpect(status().isOk());
//
//        final var associationsList = parseResponseTo( response, AssociationsList.class );
//        final var links = associationsList.getLinks();
//
//        final var items =
//                associationsList.getItems()
//                        .stream()
//                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
//                        .toList();
//
//        Assertions.assertTrue( items.containsAll( List.of ( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" ) ) );
//        Assertions.assertEquals( "/associations/companies/111111?page_index=0&items_per_page=15", links.getSelf() );
//        Assertions.assertEquals( "", links.getNext() );
//        Assertions.assertEquals( 0, associationsList.getPageNumber() );
//        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
//        Assertions.assertEquals( 13, associationsList.getTotalResults() );
//        Assertions.assertEquals( 1, associationsList.getTotalPages() );
//    }
//
//    @Test
//    void getAssociationsForCompanyWithIncludeRemovedTrueDoesNotApplyFilter() throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
//        mockers.mockUsersServiceFetchUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
//        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
//
//        final var response =
//                mockMvc.perform( get( "/associations/companies/111111?include_removed=true" )
//                                .header("X-Request-Id", "theId123")
//                                .header( "ERIC-Identity", "111" )
//                                .header( "ERIC-Identity-Type", "oauth2" ) )
//                        .andExpect(status().isOk());
//
//        final var associationsList = parseResponseTo( response, AssociationsList.class );
//        final var links = associationsList.getLinks();
//
//        final var items =
//                associationsList.getItems()
//                        .stream()
//                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
//                        .toList();
//
//        Assertions.assertTrue( items.containsAll( List.of ( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15" ) ) );
//        Assertions.assertEquals( "/associations/companies/111111?page_index=0&items_per_page=15", links.getSelf() );
//        Assertions.assertEquals( "/associations/companies/111111?page_index=1&items_per_page=15", links.getNext() );
//        Assertions.assertEquals( 0, associationsList.getPageNumber() );
//        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
//        Assertions.assertEquals( 16, associationsList.getTotalResults() );
//        Assertions.assertEquals( 2, associationsList.getTotalPages() );
//    }
//
//    @Test
//    void getAssociationsForCompanyPaginatesCorrectly() throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
//        mockers.mockUsersServiceFetchUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
//        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
//
//        final var response =
//                mockMvc.perform( get( "/associations/companies/111111?include_removed=true&items_per_page=3&page_index=2" )
//                                .header("X-Request-Id", "theId123")
//                                .header( "ERIC-Identity", "111" )
//                                .header( "ERIC-Identity-Type", "oauth2" ) )
//                        .andExpect(status().isOk());
//
//        final var associationsList = parseResponseTo( response, AssociationsList.class );
//        final var links = associationsList.getLinks();
//
//        final var items =
//                associationsList.getItems()
//                        .stream()
//                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
//                        .toList();
//
//        Assertions.assertTrue( items.containsAll( List.of ( "7", "8", "9" ) ) );
//        Assertions.assertEquals( "/associations/companies/111111?page_index=2&items_per_page=3", links.getSelf() );
//        Assertions.assertEquals( "/associations/companies/111111?page_index=3&items_per_page=3", links.getNext() );
//        Assertions.assertEquals( 2, associationsList.getPageNumber() );
//        Assertions.assertEquals( 3, associationsList.getItemsPerPage() );
//        Assertions.assertEquals( 16, associationsList.getTotalResults() );
//        Assertions.assertEquals( 6, associationsList.getTotalPages() );
//    }
//
//    @Test
//    void getAssociationsForCompanyWhereAccountsUserEndpointCannotFindUserReturnsNotFound() throws Exception {
//        final var associations = testDataManager.fetchAssociationDaos( "1" );
//        associations.add( testDataManager.fetchAssociationDaos( "18" ).getFirst().companyNumber( "111111" ) );
//
//        associationsRepository.insert( associations );
//        mockers.mockUsersServiceFetchUserDetails( "9999" );
//        mockers.mockUsersServiceFetchUserDetailsNotFound( "111" );
//        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
//
//        mockMvc.perform( get( "/associations/companies/111111" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "9999" )
//                        .header( "ERIC-Identity-Type", "oauth2" ) )
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void getAssociationsForCompanyWhereCompanyProfileEndpointCannotFindCompanyReturnsNotFound() throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
//        mockers.mockUsersServiceFetchUserDetails( "111" );
//        mockers.mockCompanyServiceFetchCompanyProfileNotFound( "111111" );
//
//        mockMvc.perform( get( "/associations/companies/111111" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "111" )
//                        .header( "ERIC-Identity-Type", "oauth2" ) )
//                .andExpect(status().isNotFound());
//    }
//
//    @Test
//    void getAssociationsForCompanyDoesMappingCorrectly() throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
//        mockers.mockUsersServiceFetchUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
//        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
//
//        final var response =
//                mockMvc.perform( get( "/associations/companies/111111?include_removed=true&items_per_page=2&page_index=0" )
//                                .header("X-Request-Id", "theId123")
//                                .header( "ERIC-Identity", "111" )
//                                .header( "ERIC-Identity-Type", "oauth2" ) )
//                        .andExpect(status().isOk());
//
//        final var associationsList = parseResponseTo( response, AssociationsList.class );
//
//        final var associations = associationsList.getItems();
//        final var associationOne = associations.getFirst();
//
//        Assertions.assertEquals( "a", associationOne.getEtag() );
//        Assertions.assertEquals( "1", associationOne.getId() );
//        Assertions.assertEquals( "111", associationOne.getUserId() );
//        Assertions.assertEquals( "bruce.wayne@gotham.city", associationOne.getUserEmail() );
//        Assertions.assertEquals( "Batman", associationOne.getDisplayName() );
//        Assertions.assertEquals( "111111", associationOne.getCompanyNumber() );
//        Assertions.assertEquals( "Wayne Enterprises", associationOne.getCompanyName() );
//        Assertions.assertEquals( StatusEnum.CONFIRMED, associationOne.getStatus() );
//        Assertions.assertNotNull( associationOne.getCreatedAt() );
//        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(1) ), localDateTimeToNormalisedString( associationOne.getApprovedAt().toLocalDateTime() ) );
//        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(2) ), localDateTimeToNormalisedString( associationOne.getRemovedAt().toLocalDateTime() ) );
//        Assertions.assertEquals( DEFAULT_KIND, associationOne.getKind() );
//        Assertions.assertEquals( ApprovalRouteEnum.AUTH_CODE, associationOne.getApprovalRoute() );
//        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(3) ), reduceTimestampResolution( associationOne.getApprovalExpiryAt() ) );
//        Assertions.assertEquals( "/associations/1", associationOne.getLinks().getSelf() );
//
//        final var associationTwo = associations.get( 1 );
//        Assertions.assertEquals( "222", associationTwo.getUserId() );
//        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, associationTwo.getDisplayName() );
//        Assertions.assertEquals( "Wayne Enterprises", associationTwo.getCompanyName() );
//    }
//
//    @Test
//    void getAssociationsForCompanyWithUnacceptablePaginationParametersShouldReturnBadRequest() throws Exception {
//        mockMvc.perform( get( "/associations/companies/111111?include_removed=true&items_per_page=1&page_index=-1" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "111" )
//                        .header( "ERIC-Identity-Type", "oauth2" ) )
//                .andExpect(status().isBadRequest());
//
//        mockMvc.perform( get( "/associations/companies/111111?include_removed=true&items_per_page=0&page_index=0" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "111" )
//                        .header( "ERIC-Identity-Type", "oauth2" ) )
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void getAssociationsForCompanyFetchesAssociation() throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
//        mockers.mockUsersServiceFetchUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777" );
//        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
//
//        final var response =
//                mockMvc.perform( get( "/associations/companies/111111" )
//                                .header("X-Request-Id", "theId123")
//                                .header( "ERIC-Identity", "111" )
//                                .header( "ERIC-Identity-Type", "oauth2" ) )
//                        .andExpect(status().isOk());
//
//        final var associationsList = parseResponseTo( response, AssociationsList.class );
//
//        Assertions.assertEquals( 13, associationsList.getTotalResults() );
//    }
//
//    @Test
//    void getAssociationsForCompanyCanRetrieveMigratedAssociations() throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002", "MKAssociation003" ) );
//        mockers.mockUsersServiceFetchUserDetails( "MKUser001", "MKUser002", "MKUser003" );
//        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
//
//        final var response = mockMvc.perform( get( "/associations/companies/MKCOMP001?include_removed=true" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "MKUser002" )
//                        .header( "ERIC-Identity-Type", "oauth2" ) )
//                .andExpect( status().isOk() );
//
//        final var associationsList = parseResponseTo( response, AssociationsList.class );
//        final var associations = associationsList.getItems();
//
//        Assertions.assertEquals( 3, associationsList.getTotalResults() );
//
//        for ( final Association association: associations ){
//            final var expectedStatus = switch ( association.getId() ){
//                case "MKAssociation001" -> "migrated";
//                case "MKAssociation002" -> "confirmed";
//                case "MKAssociation003" -> "removed";
//                default -> "unknown";
//            };
//
//            final var expectedApprovalRoute = switch ( association.getId() ){
//                case "MKAssociation001", "MKAssociation003" -> "migration";
//                case "MKAssociation002" -> "auth_code";
//                default -> "unknown";
//            };
//
//            Assertions.assertEquals( expectedStatus, association.getStatus().getValue() );
//            Assertions.assertEquals( expectedApprovalRoute, association.getApprovalRoute().getValue() );
//        }
//    }
//
//    @Test
//    void getAssociationsForCompanyReturnsForbiddenWhenCalledByAUserThatIsNotAMemberOfCompanyOrAdmin() throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10","11","12","13","14","15","16","17" ) );
//        mockers.mockUsersServiceFetchUserDetails( "111", "222", "333", "444", "555", "666", "777", "888", "999" ,"1111", "2222", "3333", "4444", "5555", "6666", "7777", "MKUser001" );
//        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
//
//        mockMvc.perform( get( "/associations/companies/111111" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "MKUser001" )
//                        .header( "ERIC-Identity-Type", "oauth2" ) )
//                .andExpect( status().isForbidden() );
//    }
//
//    @Test
//    void getAssociationsForCompanyUserAndStatusWithUserIdFetchesAssociations() throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation002" ) );
//        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
//        Mockito.doReturn( testDataManager.fetchUserDtos("MKUser002" ).getFirst() ).when( usersService ).retrieveUserDetails("MKUser002", null );
//
//        final var response = mockMvc.perform( post( "/associations/companies/MKCOMP001/search" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "test" )
//                        .header( "ERIC-Identity-Type", "key" )
//                        .header( "ERIC-Authorised-Key-Roles", "*" )
//                        .contentType(MediaType.APPLICATION_JSON )
//                        .content( "{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} " ) )
//                .andExpect( status().isOk() );
//
//        final var association = parseResponseTo( response, Association.class );
//        Assertions.assertEquals( "MKAssociation002", association.getId() );
//    }
//
//    static Stream<Arguments> getAssociationsForCompanyUserAndStatusHappyCaseScenarios(){
//        return Stream.of(
//                Arguments.of( ", \"status\":[\"confirmed\", \"removed\"]" ),
//                Arguments.of( ", \"status\":[]" ),
//                Arguments.of( "" )
//
//        );
//    }
//
//    @ParameterizedTest
//    @MethodSource("getAssociationsForCompanyUserAndStatusHappyCaseScenarios")
//    void getAssociationsForCompanyUserAndStatusWithUserEmailFetchesAssociations( final String status ) throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation002" ) );
//        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
//        Mockito.doReturn( testDataManager.fetchUserDtos("MKUser002" ).getFirst() ).when( usersService ).retrieveUserDetails(null, "luigi@mushroom.kingdom" );
//
//        final var response = mockMvc.perform( post( "/associations/companies/MKCOMP001/search" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "test" )
//                        .header( "ERIC-Identity-Type", "key" )
//                        .header( "ERIC-Authorised-Key-Roles", "*" )
//                        .contentType(MediaType.APPLICATION_JSON )
//                        .content( String.format( "{\"user_email\":\"luigi@mushroom.kingdom\" %s} ", status) ) )
//                .andExpect( status().isOk() );
//
//        final var association = parseResponseTo( response, Association.class );
//        Assertions.assertEquals( "MKAssociation002", association.getId() );
//    }
//
//    @Test
//    void getAssociationsForCompanyUserWithNonExistentUserEmailFetchesAssociations( ) throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
//        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
//        Mockito.doReturn( null ).when( usersService ).retrieveUserDetails(null, "mario@mushroom.kingdom" );
//
//        final var response = mockMvc.perform( post( "/associations/companies/MKCOMP001/search" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "test" )
//                        .header( "ERIC-Identity-Type", "key" )
//                        .header( "ERIC-Authorised-Key-Roles", "*" )
//                        .contentType(MediaType.APPLICATION_JSON )
//                        .content(  "{\"user_email\":\"mario@mushroom.kingdom\" , \"status\":[\"migrated\"] }" ) )
//                .andExpect( status().isOk() );
//
//        final var association = parseResponseTo( response, Association.class );
//        Assertions.assertEquals( "MKAssociation001", association.getId() );
//    }
//
//    static Stream<Arguments> getAssociationsForCompanyUserMalformedScenarios(){
//        return Stream.of(
//                Arguments.of( "$$$$$" ,"MKCOMP001", ", \"status\":[\"confirmed\", \"removed\"]" ),
//                Arguments.of( "MKUser002" ,"$$$$$", ", \"status\":[\"confirmed\", \"removed\"]" ),
//                Arguments.of( "MKUser002" ,"MKCOMP001", ", \"status\":[\"$$$$$$\", \"removed\"]" )
//
//        );
//    }
//
//    @ParameterizedTest
//    @MethodSource("getAssociationsForCompanyUserMalformedScenarios")
//    void getAssociationsForCompanyUserAndStatusMalformedReturnsBadRequests(final String userId, final String companyNumber, final String status) throws Exception {
//        mockMvc.perform( post( String.format("/associations/companies/%s/search", companyNumber ) )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "test" )
//                        .header( "ERIC-Identity-Type", "key" )
//                        .header( "ERIC-Authorised-Key-Roles", "*" )
//                        .contentType(MediaType.APPLICATION_JSON )
//                        .content( String.format( "{\"user_id\":\"%s\" %s }", userId , status ) ) )
//                .andExpect( status().isBadRequest() );
//    }
//
//    @Test
//    void getAssociationsForCompanyUserAndStatusWithMalformedEmailReturnsBadRequest() throws Exception {
//        mockMvc.perform( post( "/associations/companies/MKCOMP001/search"  )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "test" )
//                        .header( "ERIC-Identity-Type", "key" )
//                        .header( "ERIC-Authorised-Key-Roles", "*" )
//                        .contentType(MediaType.APPLICATION_JSON )
//                        .content( "{\"user_email\":\"$\\(){}$$$$@mushroomkingdom\" }" ) )
//                .andExpect( status().isBadRequest() );
//    }
//
//    @Test
//    void getAssociationsForCompanyUserAndStatusWithoutUserReturnsBadRequest() throws Exception {
//         mockMvc.perform( post( "/associations/companies/MKCOMP001/search" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "test" )
//                        .header( "ERIC-Identity-Type", "key" )
//                        .header( "ERIC-Authorised-Key-Roles", "*" )
//                        .contentType(MediaType.APPLICATION_JSON )
//                        .content( "{ \"status\":[\"confirmed\", \"removed\"]} " ) )
//                .andExpect( status().isBadRequest() );
//    }
//
//    @Test
//    void getAssociationsForCompanyUserAndStatusWithMalformedUserEmailReturnsBadRequest() throws Exception {
//        mockMvc.perform( post( "/associations/companies/MKCOMP001/search" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "test" )
//                        .header( "ERIC-Identity-Type", "key" )
//                        .header( "ERIC-Authorised-Key-Roles", "*" )
//                        .contentType(MediaType.APPLICATION_JSON )
//                        .content( "\"user_email\":\"$$$$$@mushroomkingdom\" ,\"status\":[\"confirmed\", \"removed\"]} " ) )
//                .andExpect( status().isBadRequest() );
//    }
//
//    static Stream<Arguments> getAssociationsForCompanyMalformedBodyScenarios(){
//        return Stream.of(
//                Arguments.of( "" ),
//                Arguments.of( "{ \"user_email\":\"111@mushroom.kingdom\", \"user_id\":\"111\" }"  )
//
//        );
//    }
//
//    @ParameterizedTest
//    @MethodSource("getAssociationsForCompanyMalformedBodyScenarios")
//    void getAssociationsForCompanyMalformedBodyScenarios( final String body ) throws Exception{
//        mockMvc.perform( post( "/associations/companies/MKCOMP001/search" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "test" )
//                        .header( "ERIC-Identity-Type", "key" )
//                        .header( "ERIC-Authorised-Key-Roles", "*" )
//                        .contentType(MediaType.APPLICATION_JSON )
//                        .content( body ) )
//                .andExpect( status().isBadRequest() );
//    }
//
//    @Test
//    void getAssociationsForCompanyWithNonExistentCompanyNumberReturnsNotFound() throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation002" ) );
//        mockers.mockCompanyServiceFetchCompanyProfileNotFound( "MKCOMP001" );
//        Mockito.doReturn( testDataManager.fetchUserDtos("MKUser002" ).getFirst() ).when( usersService ).retrieveUserDetails("MKUser002", null );
//
//        mockMvc.perform( post( "/associations/companies/MKCOMP001/search" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "test" )
//                        .header( "ERIC-Identity-Type", "key" )
//                        .header( "ERIC-Authorised-Key-Roles", "*" )
//                        .contentType(MediaType.APPLICATION_JSON )
//                        .content( "{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} " ) )
//                .andExpect( status().isNotFound() );
//    }
//
//    @Test
//    void getAssociationsForCompanyWithNonExistentUserIdReturnsNotFound() throws Exception {
//        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation002" ) );
//        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
//        Mockito.doThrow( new NotFoundRuntimeException( "Test", new Exception() ) ).when( usersService ).retrieveUserDetails("MKUser002", null );
//
//        mockMvc.perform( post( "/associations/companies/MKCOMP001/search" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "test" )
//                        .header( "ERIC-Identity-Type", "key" )
//                        .header( "ERIC-Authorised-Key-Roles", "*" )
//                        .contentType(MediaType.APPLICATION_JSON )
//                        .content( "{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} " ) )
//                .andExpect( status().isNotFound() );
//    }
//
//    @Test
//    void getAssociationsForCompanyNonExistentAssociationsReturnsNotFound() throws Exception {
//        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
//        Mockito.doReturn( testDataManager.fetchUserDtos("MKUser002" ).getFirst() ).when( usersService ).retrieveUserDetails("MKUser002", null );
//
//        mockMvc.perform( post( "/associations/companies/MKCOMP001/search" )
//                        .header("X-Request-Id", "theId123")
//                        .header( "ERIC-Identity", "test" )
//                        .header( "ERIC-Identity-Type", "key" )
//                        .header( "ERIC-Authorised-Key-Roles", "*" )
//                        .contentType(MediaType.APPLICATION_JSON )
//                        .content( "{\"user_id\":\"MKUser002\", \"status\":[\"confirmed\", \"removed\"]} " ) )
//                .andExpect( status().isNotFound() );
//    }
//
//    @AfterEach
//    public void after() {
//        mongoTemplate.dropCollection(AssociationDao.class);
//    }
//
//}