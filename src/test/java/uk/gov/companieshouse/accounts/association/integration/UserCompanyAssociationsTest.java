package uk.gov.companieshouse.accounts.association.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.ResponseBodyPost;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

@AutoConfigureMockMvc
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class UserCompanyAssociationsTest extends BaseMongoIntegration {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private  MockMvc mockMvc;

    @MockBean
    private CompanyService companyService;

    @MockBean
    private UsersService usersService;

    @MockBean
    private EmailProducer emailProducer;

    @MockBean
    private KafkaProducerFactory kafkaProducerFactory;

    @Autowired
    private AssociationsRepository associationsRepository;

    private TestDataManager testDataManager = TestDataManager.getInstance();

    private static final String DEFAULT_KIND = "association";

    @Value( "${invitation.url}")
    private String COMPANY_INVITATIONS_URL;

    private final LocalDateTime now = LocalDateTime.now();

    private CountDownLatch latch;

    private Mockers mockers;

    private ComparisonUtils comparisonUtils = new ComparisonUtils();

    private void setEmailProducerCountDownLatch( int countdown ){
        latch = new CountDownLatch( countdown );
        doAnswer( invocation -> {
            latch.countDown();
            return null;
        } ).when( emailProducer ).sendEmail( any(), any() );
    }

    @BeforeEach
    public void setup() throws IOException, URIValidationException {
        mockers = new Mockers( null, emailProducer, companyService, usersService );
    }

    static Stream<Arguments> withoutXRequestIdTestData(){
        return Stream.of(
                Arguments.of( "/associations" ),
                Arguments.of( "/associations/1" ),
                Arguments.of( "/associations/invitations?page_index=1&items_per_page=1" )
        );
    }
    @Test
    void fetchAssociationsByWithoutEricIdentityReturnsForbidden() throws Exception {
        mockMvc.perform( get( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isForbidden() );
    }

    static Stream<Arguments> malformedQueryParametersTestData(){
        return Stream.of(
                Arguments.of( "/associations?page_index=-1" ),
                Arguments.of( "/associations?items_per_page=0" ),
                Arguments.of( "/associations?company_number=$$$$$$" ),
                Arguments.of( "/associations/$" ),
                Arguments.of( "/associations/invitations?page_index=-1&items_per_page=1" ),
                Arguments.of( "/associations/invitations?page_index=0&items_per_page=-1" )
        );
    }

    @ParameterizedTest
    @MethodSource( "malformedQueryParametersTestData" )
    void endpointsWithMalformedQueryParametersReturnBadRequest( final String uri ) throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "9999" );

        mockMvc.perform( get( uri )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchAssociationsByWithNonExistentUserReturnsForbidden() throws Exception {
        mockers.mockUsersServiceFetchUserDetailsNotFound( "9191" );

        mockMvc.perform( get( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isForbidden() );
    }

    @Test
    void fetchAssociationsByWithNonexistentCompanyReturnsNotFound() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "17" ) );
        mockers.mockUsersServiceFetchUserDetails( "8888" );
        mockers.mockCompanyServiceFetchCompanyProfileNotFound( "222222" );

        mockMvc.perform( get( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "8888")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isNotFound() );
    }

    @Test
    void fetchAssociationsByWithInvalidStatusReturnsZeroResults() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "9999" );

        mockMvc.perform( get( "/associations?status=$$$$" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchAssociationsByUsesDefaultsIfValuesAreNotProvided() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33" ) );
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333", "444444", "555555", "666666", "777777" );

        final var response =
                mockMvc.perform( get( "/associations" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() );

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "18", "19", "20", "21", "22" ) ) );
        Assertions.assertEquals( "/associations?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 5, associationsList.getTotalResults() );
        Assertions.assertEquals( 1, associationsList.getTotalPages() );
    }

    @Test
    void fetchAssociationsByWithOneStatusAppliesStatusFilterCorrectly() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33" ) );
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "x777777", "x888888", "x999999" );

        final var response =
                mockMvc.perform( get( "/associations?status=removed" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() );

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "31", "32", "33" ) ) );
        Assertions.assertEquals( "/associations?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 3, associationsList.getTotalResults() );
        Assertions.assertEquals( 1, associationsList.getTotalPages() );
    }

    @Test
    void fetchAssociationsByWithMultipleStatusesAppliesStatusFilterCorrectly() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33" ) );
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333", "444444", "555555", "666666", "777777", "x777777", "x888888", "x999999" );

        final var response =
                mockMvc.perform( get( "/associations?status=confirmed&status=removed" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() );

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "18", "19", "20", "21", "22", "31", "32", "33" ) ) );
        Assertions.assertEquals( "/associations?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 8, associationsList.getTotalResults() );
        Assertions.assertEquals( 1, associationsList.getTotalPages() );
    }

    @Test
    void fetchAssociationsByImplementsPaginationCorrectly() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33" ) );
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "999999", "x111111", "x222222" );

        final var response =
                mockMvc.perform( get( "/associations?status=confirmed&status=awaiting-approval&status=removed&page_index=2&items_per_page=3" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() );

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.containsAll( List.of ( "24", "25", "26" ) ) );
        Assertions.assertEquals( "/associations?page_index=2&items_per_page=3", links.getSelf() );
        Assertions.assertEquals( "/associations?page_index=3&items_per_page=3", links.getNext() );
        Assertions.assertEquals( 2, associationsList.getPageNumber() );
        Assertions.assertEquals( 3, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 16, associationsList.getTotalResults() );
        Assertions.assertEquals( 6, associationsList.getTotalPages() );
    }

    @Test
    void fetchAssociationsByFiltersBasedOnCompanyNumberCorrectly() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33" ) );
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );

        final var response =
                mockMvc.perform( get( "/associations?company_number=333333" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() );

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var links = associationsList.getLinks();

        final var items =
                associationsList.getItems()
                        .stream()
                        .map( uk.gov.companieshouse.api.accounts.associations.model.Association::getId )
                        .toList();

        Assertions.assertTrue( items.contains( "18" ) );
        Assertions.assertEquals( "/associations?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 1, associationsList.getTotalResults() );
        Assertions.assertEquals( 1, associationsList.getTotalPages() );
    }

    @Test
    void fetchAssociationsByDoesMappingCorrectly() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18" ) );
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );

        final var response =
                mockMvc.perform( get( "/associations?company_number=333333" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() );


        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var associations = associationsList.getItems();
        final var associationOne = associations.getFirst();

        Assertions.assertEquals( "aa", associationOne.getEtag() );
        Assertions.assertEquals( "18", associationOne.getId() );
        Assertions.assertEquals( "9999", associationOne.getUserId() );
        Assertions.assertEquals( "scrooge.mcduck@disney.land", associationOne.getUserEmail() );
        Assertions.assertEquals( "Scrooge McDuck", associationOne.getDisplayName() );
        Assertions.assertEquals( "333333", associationOne.getCompanyNumber() );
        Assertions.assertEquals( "Tesco", associationOne.getCompanyName() );
        Assertions.assertEquals( StatusEnum.CONFIRMED, associationOne.getStatus() );
        Assertions.assertNotNull( associationOne.getCreatedAt() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(1) ), localDateTimeToNormalisedString( associationOne.getApprovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(2) ), localDateTimeToNormalisedString( associationOne.getRemovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( DEFAULT_KIND, associationOne.getKind() );
        Assertions.assertEquals( ApprovalRouteEnum.AUTH_CODE, associationOne.getApprovalRoute() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.plusDays(3) ), reduceTimestampResolution( associationOne.getApprovalExpiryAt() ) );
        Assertions.assertEquals( "/associations/18", associationOne.getLinks().getSelf() );
    }

    @Test
    void fetchAssociationsByCanFetchMigratedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockUsersServiceFetchUserDetails( "MKUser001" );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );

        final var response =
                mockMvc.perform( get( "/associations?status=migrated" )
                                .header( "X-Request-Id", "theId123" )
                                .header( "Eric-identity", "MKUser001" )
                                .header( "ERIC-Identity-Type", "oauth2" ) )
                        .andExpect( status().isOk() );

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var associations = associationsList.getItems();
        final var associationOne = associations.getFirst();

        Assertions.assertEquals( "MKAssociation001", associationOne.getId() );
        Assertions.assertEquals( "migrated", associationOne.getStatus().getValue() );
        Assertions.assertEquals( "migration", associationOne.getApprovalRoute().getValue() );
    }

    @Test
    void fetchAssociationsByCanFetchUnauthorisedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation004" ) );
        mockers.mockUsersServiceFetchUserDetails( "MKUser004" );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );

        final var response =
                mockMvc.perform( get( "/associations?status=unauthorised" )
                                .header( "X-Request-Id", "theId123" )
                                .header( "Eric-identity", "MKUser004" )
                                .header( "ERIC-Identity-Type", "oauth2" ) )
                        .andExpect( status().isOk() );

        final var associationsList = parseResponseTo( response, AssociationsList.class );
        final var associations = associationsList.getItems();
        final var associationOne = associations.getFirst();

        Assertions.assertEquals( "MKAssociation004", associationOne.getId() );
        Assertions.assertEquals( StatusEnum.UNAUTHORISED, associationOne.getStatus() );
        Assertions.assertNotNull( associationOne.getUnauthorisedAt() );
    }

    @Test
    void addAssociationWithoutEricIdentityReturnsForbidden() throws Exception {
        mockMvc.perform(post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"111111\"}" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void addAssociationWithoutRequestBodyReturnsBadRequest() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "9999" );

        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addAssociationWithEmptyBodyReturnsBadRequest() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "9999" );

        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addAssociationWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "9999" );

        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"$$$$$$\", \"user_id\":\"9999\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addAssociationWithMalformedUserIdReturnsBadRequest() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "9999" );

        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\", \"user_id\":\"$$$$\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addAssociationWithExistingAssociationReturnsBadRequest() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18" ) );
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );

        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\", \"user_id\":\"9999\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addAssociationWithNonexistentCompanyNumberReturnsNotFound() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18" ) );
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfileNotFound( "919191" );

        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"919191\", \"user_id\":\"9999\"}" ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void addAssociationCreatesNewAssociationCorrectlyAndReturnsAssociationIdWithCreatedHttpStatus() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18" ) );
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );

        final var responseJson =
                mockMvc.perform(post( "/associations" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "key")
                                .header("ERIC-Authorised-Key-Roles", "*")
                                .contentType( MediaType.APPLICATION_JSON )
                                .content( "{\"company_number\":\"111111\", \"user_id\":\"9999\"}" ) )
                        .andExpect( status().isCreated() );

        final var response = parseResponseTo( responseJson, ResponseBodyPost.class );

        final var associationLink = response.getAssociationLink();
        final var associationOptional = associationsRepository.findById( associationLink.substring( associationLink.lastIndexOf( "/" )+1 ) );
        Assertions.assertTrue( associationOptional.isPresent() );

        final var association = associationOptional.get();
        Assertions.assertEquals( "111111", association.getCompanyNumber() );
        Assertions.assertEquals( "9999", association.getUserId() );
        Assertions.assertEquals( StatusEnum.CONFIRMED.getValue(), association.getStatus() );
        Assertions.assertEquals( ApprovalRouteEnum.AUTH_CODE.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getEtag() );
    }

    @Test
    void addAssociationWithNonExistentUserReturnsNotFound() throws Exception {
        mockers.mockUsersServiceFetchUserDetailsNotFound( "9191" );

        mockMvc.perform( post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"111111\", \"user_id\":\"9191\"}" ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void addAssociationWithUserThatHasDisplayNameUsesDisplayName() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "19" ) );
        mockers.mockUsersServiceFetchUserDetails( "111", "9999" );
        mockers.mockUsersServiceToFetchUserDetailsRequest( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "444444" );

        setEmailProducerCountDownLatch( 1 );

        mockMvc.perform(post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\", \"user_id\":\"111\"}" ) )
                .andExpect( status().isCreated() );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.authCodeConfirmationEmailMatcher( "scrooge.mcduck@disney.land", "Sainsbury's", "Batman" ) ), eq( AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void addAssociationReturnsCreatedWhenEmailIsNotSentOut() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "19" ) );
        mockers.mockUsersServiceFetchUserDetails( "111", "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "444444" );
        mockers.mockEmailSendingFailure( AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue() );

        mockMvc.perform(post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "key")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\", \"user_id\":\"111\"}" ) )
                .andExpect( status().isCreated() );
    }

    @Test
    void addAssociationCanBeAppliedToMigratedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002" ) );
        mockers.mockUsersServiceFetchUserDetails( "MKUser001", "MKUser002" );
        mockers.mockUsersServiceToFetchUserDetailsRequest( "MKUser001", "MKUser002" );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );

        setEmailProducerCountDownLatch( 1 );

        mockMvc.perform( post( "/associations" )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-Identity", "MKUser001" )
                        .header( "ERIC-Identity-Type", "key" )
                        .header( "ERIC-Authorised-Key-Roles", "*" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"MKCOMP001\", \"user_id\":\"MKUser001\"}" ) )
                .andExpect( status().isCreated() );

        final var updatedAssociation = associationsRepository.findById( "MKAssociation001" ).get();

        Assertions.assertEquals( "confirmed", updatedAssociation.getStatus() );
        Assertions.assertEquals( "auth_code", updatedAssociation.getApprovalRoute() );
        Assertions.assertNotNull( updatedAssociation.getEtag() );
        Assertions.assertEquals( 1, updatedAssociation.getPreviousStates().size() );
        Assertions.assertEquals( "migrated", updatedAssociation.getPreviousStates().getFirst().getStatus() );
        Assertions.assertEquals( COMPANIES_HOUSE, updatedAssociation.getPreviousStates().getFirst().getChangedBy() );
        Assertions.assertNotNull( updatedAssociation.getPreviousStates().getFirst().getChangedAt() );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.authCodeConfirmationEmailMatcher( "luigi@mushroom.kingdom", "Mushroom Kingdom", "Mario" ) ), eq( AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue() ) );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }

}