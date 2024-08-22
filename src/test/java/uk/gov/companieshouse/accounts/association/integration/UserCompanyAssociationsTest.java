package uk.gov.companieshouse.accounts.association.integration;

import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationAcceptedEmailBuilder;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.rest.AccountsUserEndpoint;
import uk.gov.companieshouse.accounts.association.rest.CompanyProfileEndpoint;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut;
import uk.gov.companieshouse.api.accounts.associations.model.ResponseBodyPost;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_ACCEPTED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITE_MESSAGE_TYPE;

@AutoConfigureMockMvc
@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class UserCompanyAssociationsTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private  MockMvc mockMvc;

    @MockBean
    private CompanyProfileEndpoint companyProfileEndpoint;

    @MockBean
    private AccountsUserEndpoint accountsUserEndpoint;

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
        mockers = new Mockers( accountsUserEndpoint, companyProfileEndpoint, emailProducer, null, null );
    }

    @Test
    void fetchAssociationsByWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( get( "/associations" )
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchAssociationsByWithoutEricIdentityReturnsUnauthorised() throws Exception {
        mockMvc.perform( get( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isUnauthorized() );
    }

    @Test
    void fetchAssociationsByWithInvalidPageIndexReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( get( "/associations?page_index=-1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchAssociationsByWithInvalidItemsPerPageReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( get( "/associations?items_per_page=0" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchAssociationsByWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( get( "/associations?company_number=$$$$$$" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchAssociationsByWithNonExistentUserReturnsForbidden() throws Exception {
        mockers.mockGetUserDetailsNotFound( "9191" );

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
        mockers.mockGetUserDetails( "8888" );
        mockers.mockFetchCompanyProfileNotFound( "222222" );

        mockMvc.perform( get( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "8888")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isNotFound() );
    }

    @Test
    void fetchAssociationsByWithInvalidStatusReturnsZeroResults() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        final var response =
                mockMvc.perform( get( "/associations?status=$$$$" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() );

        final var associationsList = parseResponseTo( response, AssociationsList.class );

        Assertions.assertEquals( 0, associationsList.getTotalResults() );
    }

    @Test
    void fetchAssociationsByUsesDefaultsIfValuesAreNotProvided() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "17", "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33" ) );
        mockers.mockGetUserDetails( "9999" );
        mockers.mockFetchCompanyProfile( "333333", "444444", "555555", "666666", "777777" );

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
        mockers.mockGetUserDetails( "9999" );
        mockers.mockFetchCompanyProfile( "x777777", "x888888", "x999999" );

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
        mockers.mockGetUserDetails( "9999" );
        mockers.mockFetchCompanyProfile( "333333", "444444", "555555", "666666", "777777", "x777777", "x888888", "x999999" );

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
        mockers.mockGetUserDetails( "9999" );
        mockers.mockFetchCompanyProfile( "999999", "x111111", "x222222" );

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
        mockers.mockGetUserDetails( "9999" );
        mockers.mockFetchCompanyProfile( "333333" );

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
        mockers.mockGetUserDetails( "9999" );
        mockers.mockFetchCompanyProfile( "333333" );

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
    void getAssociationDetailsWithoutPathVariableReturnsNotFound() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( get( "/associations/" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isNotFound() );
    }

    @Test
    void getAssociationsDetailsWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( get( "/associations/1" )
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAssociationDetailsWithMalformedInputReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( get( "/associations/$" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getAssociationUserDetailsWithNonexistentIdReturnsNotFound() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( get( "/associations/1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isNotFound() );
    }


    @Test
    void getAssociationDetailsFetchesAssociationDetails() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18" ) );
        mockers.mockGetUserDetails( "9999" );
        mockers.mockFetchCompanyProfile( "333333" );

        final var response =
                mockMvc.perform( get( "/associations/18" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() );

        final var association = parseResponseTo( response, Association.class );

        Assertions.assertEquals( "9999", association.getUserId());
    }

    @Test
    void addAssociationWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"111111\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addAssociationWithoutEricIdentityReturnsUnauthorised() throws Exception {
        mockMvc.perform(post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"111111\"}" ) )
                .andExpect( status().isUnauthorized() );
    }

    @Test
    void addAssociationWithoutRequestBodyReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addAssociationWithoutCompanyNumberReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addAssociationWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"$$$$$$\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addAssociationWithExistingAssociationReturnsBadRequest() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18" ) );
        mockers.mockGetUserDetails( "9999" );
        mockers.mockFetchCompanyProfile( "333333" );

        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void addAssociationWithNonexistentCompanyNumberReturnsNotFound() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18" ) );
        mockers.mockGetUserDetails( "9999" );
        mockers.mockFetchCompanyProfileNotFound( "919191" );

        mockMvc.perform(post( "/associations" )
                        .header("Eric-identity", "9999")
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"919191\"}" ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void addAssociationCreatesNewAssociationCorrectlyAndReturnsAssociationIdWithCreatedHttpStatus() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18" ) );
        mockers.mockGetUserDetails( "9999" );
        mockers.mockFetchCompanyProfile( "111111" );

        final var responseJson =
                mockMvc.perform(post( "/associations" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "9999")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*")
                                .contentType( MediaType.APPLICATION_JSON )
                                .content( "{\"company_number\":\"111111\"}" ) )
                        .andExpect( status().isCreated() );

        final var response = parseResponseTo( responseJson, ResponseBodyPost.class );

        final var associationOptional = associationsRepository.findById( response.getAssociationId() );
        Assertions.assertTrue( associationOptional.isPresent() );

        final var association = associationOptional.get();
        Assertions.assertEquals( "111111", association.getCompanyNumber() );
        Assertions.assertEquals( "9999", association.getUserId() );
        Assertions.assertEquals( StatusEnum.CONFIRMED.getValue(), association.getStatus() );
        Assertions.assertEquals( ApprovalRouteEnum.AUTH_CODE.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getEtag() );
    }

    @Test
    void addAssociationWithNonExistentUserReturnsForbidden() throws Exception {
        mockers.mockGetUserDetailsNotFound( "9191" );

        mockMvc.perform( post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"111111\"}" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void addAssociationWithUserThatHasDisplayNameUsesDisplayName() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "19" ) );
        mockers.mockGetUserDetails( "111", "9999" );
        mockers.mockFetchCompanyProfile( "444444" );

        setEmailProducerCountDownLatch( 1 );

        mockMvc.perform(post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\"}" ) )
                .andExpect( status().isCreated() );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.authCodeConfirmationEmailMatcher( "scrooge.mcduck@disney.land", "Sainsbury's", "Batman" ) ), eq( AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void addAssociationReturnsCreatedWhenEmailIsNotSentOut() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "19" ) );
        mockers.mockGetUserDetails( "111", "9999" );
        mockers.mockFetchCompanyProfile( "444444" );
        mockers.mockEmailSendingFailure( AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue() );

        mockMvc.perform(post( "/associations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\"}" ) )
                .andExpect( status().isCreated() );
    }

    @Test
    void updateAssociationStatusForIdWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails(  "9999" );

        mockMvc.perform( patch( "/associations/18" )
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithMalformedAssociationIdReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails(  "9999" );

        mockMvc.perform( patch( "/associations/$$$" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithoutRequestBodyReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails(  "9999" );

        mockMvc.perform( patch( "/associations/18" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithoutStatusReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails(  "9999" );

        mockMvc.perform( patch( "/associations/18" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithMalformedStatusReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails(  "9999" );

        mockMvc.perform( patch( "/associations/18")
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"complicated\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithNonexistentAssociationIdReturnsNotFound() throws Exception {
        mockers.mockGetUserDetails(  "9999" );

        mockMvc.perform( patch( "/associations/9191" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void updateAssociationStatusForIdWithRemovedUpdatesAssociationStatus() throws Exception {
        final var oldAssociationData = testDataManager.fetchAssociationDaos( "18" ).getFirst();

        associationsRepository.insert( oldAssociationData );
        mockers.mockGetUserDetails(  "9999" );
        mockers.mockFetchCompanyProfile( "333333" );

        mockMvc.perform( patch( "/associations/18" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        final var newAssociationData = associationsRepository.findById("18").get();
        Assertions.assertEquals( RequestBodyPut.StatusEnum.REMOVED.getValue(), newAssociationData.getStatus() );
        Assertions.assertEquals( localDateTimeToNormalisedString( oldAssociationData.getApprovedAt() ), localDateTimeToNormalisedString( newAssociationData.getApprovedAt() ) );
        Assertions.assertNotEquals( oldAssociationData.getRemovedAt(), newAssociationData.getRemovedAt() );
        Assertions.assertNotEquals( oldAssociationData.getEtag(), newAssociationData.getEtag() );
        Assertions.assertEquals( oldAssociationData.getUserEmail(), newAssociationData.getUserEmail() );
        Assertions.assertEquals( oldAssociationData.getUserId(), newAssociationData.getUserId() );
    }

    @Test
    void updateAssociationStatusForIdWithConfirmedUpdatesAssociationStatus() throws Exception {
        final var oldAssociationData = testDataManager.fetchAssociationDaos( "18" ).getFirst();

        associationsRepository.insert( oldAssociationData );
        mockers.mockGetUserDetails(  "9999" );
        mockers.mockFetchCompanyProfile( "333333" );

        mockMvc.perform( patch( "/associations/18" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );

        final var newAssociationData = associationsRepository.findById("18").get();
        Assertions.assertEquals( RequestBodyPut.StatusEnum.CONFIRMED.getValue(), newAssociationData.getStatus() );
        Assertions.assertNotEquals( oldAssociationData.getApprovedAt(), newAssociationData.getApprovedAt() );
        Assertions.assertEquals( localDateTimeToNormalisedString( oldAssociationData.getRemovedAt() ), localDateTimeToNormalisedString( newAssociationData.getRemovedAt() ) );
        Assertions.assertNotEquals( oldAssociationData.getEtag(), newAssociationData.getEtag() );
        Assertions.assertEquals( oldAssociationData.getUserEmail(), newAssociationData.getUserEmail() );
        Assertions.assertEquals( oldAssociationData.getUserId(), newAssociationData.getUserId() );
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndNonexistentUserAndConfirmedReturnsBadRequest() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "17" ) );
        mockers.mockSearchUserDetailsNotFound( "mr.blobby@nightmare.com" );
        mockers.mockGetUserDetails( "222" );

        mockMvc.perform( patch( "/associations/17" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "222")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndExistingUserAndRemovedUpdatesAssociationStatus() throws Exception {
        final var associationDaos = testDataManager.fetchAssociationDaos( "1", "34" );
        final var oldAssociationData = associationDaos.getLast();

        associationsRepository.insert(associationDaos);
        mockers.mockSearchUserDetails( "000" );
        mockers.mockGetUserDetails( "111" );
        mockers.mockFetchCompanyProfile( "111111" );

        mockMvc.perform( patch( "/associations/34" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        final var newAssociationData = associationsRepository.findById("34").get();
        Assertions.assertEquals( RequestBodyPut.StatusEnum.REMOVED.getValue(), newAssociationData.getStatus() );
        Assertions.assertEquals( localDateTimeToNormalisedString( oldAssociationData.getApprovedAt() ), localDateTimeToNormalisedString( newAssociationData.getApprovedAt() ) );
        Assertions.assertNotEquals( localDateTimeToNormalisedString( oldAssociationData.getRemovedAt() ), localDateTimeToNormalisedString( newAssociationData.getRemovedAt() ) );
        Assertions.assertNotEquals( oldAssociationData.getEtag(), newAssociationData.getEtag() );
        Assertions.assertNotEquals( oldAssociationData.getUserEmail(), newAssociationData.getUserEmail() );
        Assertions.assertNotEquals( oldAssociationData.getUserId(), newAssociationData.getUserId() );
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndNonexistentUserAndRemovedUpdatesAssociationStatus() throws Exception {
        final var associationDaos = testDataManager.fetchAssociationDaos( "1", "34" );
        final var oldAssociationData = associationDaos.getLast();

        associationsRepository.insert(associationDaos);
        mockers.mockSearchUserDetailsNotFound( "light.yagami@death.note" );
        mockers.mockGetUserDetails( "111" );
        mockers.mockFetchCompanyProfile( "111111" );

        mockMvc.perform( patch( "/associations/34" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        final var newAssociationData = associationsRepository.findById("34").get();
        Assertions.assertEquals( RequestBodyPut.StatusEnum.REMOVED.getValue(), newAssociationData.getStatus() );
        Assertions.assertEquals( localDateTimeToNormalisedString( oldAssociationData.getApprovedAt() ), localDateTimeToNormalisedString( newAssociationData.getApprovedAt() ) );
        Assertions.assertNotEquals( localDateTimeToNormalisedString( oldAssociationData.getRemovedAt() ), localDateTimeToNormalisedString( newAssociationData.getRemovedAt() ) );
        Assertions.assertNotEquals( oldAssociationData.getEtag(), newAssociationData.getEtag() );
        Assertions.assertEquals( oldAssociationData.getUserEmail(), newAssociationData.getUserEmail() );
        Assertions.assertEquals( oldAssociationData.getUserId(), newAssociationData.getUserId() );
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereTargetUserIdExistsSendsNotification() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18", "35" ) );
        mockers.mockGetUserDetails( "9999", "000" );
        mockers.mockFetchCompanyProfile( "333333" );

        mockMvc.perform( patch( "/associations/35" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereTargetUserIdDoesNotExistSendsNotification() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "34" ) );
        mockers.mockGetUserDetails( "111" );
        mockers.mockSearchUserDetails( "000" );
        mockers.mockFetchCompanyProfile( "111111" );

        setEmailProducerCountDownLatch( 1 );

        mockMvc.perform( patch( "/associations/34" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        latch.await( 10, TimeUnit.SECONDS );

        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.authorisationRemovedEmailMatcher( "Batman", "light.yagami@death.note", "Wayne Enterprises", "bruce.wayne@gotham.city" ) ), eq( AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereUserCannotBeFoundSendsNotification() throws Exception {
        final var associationDaos = testDataManager.fetchAssociationDaos( "1", "34" );

        associationsRepository.insert(associationDaos);
        mockers.mockSearchUserDetailsNotFound( "light.yagami@death.note" );
        mockers.mockGetUserDetails( "111" );
        mockers.mockFetchCompanyProfile( "111111" );

        setEmailProducerCountDownLatch( 1 );

        mockMvc.perform( patch( "/associations/34" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "111")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.authorisationRemovedEmailMatcher( "Batman", "light.yagami@death.note", "Wayne Enterprises", "bruce.wayne@gotham.city" ) ), eq( AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void updateAssociationStatusForIdNotificationsUsesDisplayNamesWhenAvailable() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "3", "4" ) );
        mockers.mockGetUserDetails( "333" );
        mockers.mockSearchUserDetails( "444" );
        mockers.mockFetchCompanyProfile( "111111" );

        mockMvc.perform( patch( "/associations/4" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );
    }

    @Test
    void updateAssociationToConfirmedStatusForOtherUserShouldThrow400Error() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "3", "4" ) );
        mockers.mockGetUserDetails( "333" );
        mockers.mockSearchUserDetails( "444" );
        mockers.mockFetchCompanyProfile( "111111" );

        mockMvc.perform( patch( "/associations/4" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "333")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdUserAcceptedInvitationNotificationsSendsNotification() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "2", "4", "6" ) );
        mockers.mockFetchCompanyProfile( "111111" );
        mockers.mockSearchUserDetails( "666" );
        mockers.mockGetUserDetails( "222", "444", "666" );

        setEmailProducerCountDownLatch( 3 );

        mockMvc.perform( patch( "/associations/6"  )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "666")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );

        latch.await( 10, TimeUnit.SECONDS );

        final var expectedBaseEmail = new InvitationAcceptedEmailBuilder()
                        .setInviterDisplayName( "the.joker@gotham.city" )
                        .setInviteeDisplayName( "homer.simpson@springfield.com" )
                        .setCompanyName( "Wayne Enterprises" );

        Mockito.verify( emailProducer, times( 3 ) ).sendEmail( argThat( comparisonUtils.invitationAcceptedEmailDataMatcher( List.of( "the.joker@gotham.city", "robin@gotham.city", "homer.simpson@springfield.com" ), expectedBaseEmail ) ), eq( INVITATION_ACCEPTED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void inviteUserWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void inviteUserWithoutEricIdentityReturnsUnauthorised() throws Exception {
        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isUnauthorized() );
    }

    @Test
    void inviteUserWithMalformedEricIdentityReturnsForbidden() throws Exception {
        mockers.mockGetUserDetailsNotFound( "$$$$" );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "$$$$")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void inviteUserWithNonexistentEricIdentityReturnsForbidden() throws Exception {
        mockers.mockGetUserDetailsNotFound( "9191" );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void inviteUserWithoutRequestBodyReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void inviteUserWithoutCompanyNumberReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void inviteUserWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"$$$$$$\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void inviteUserWithNonexistentCompanyNumberReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );
        mockers.mockFetchCompanyProfileNotFound( "919191" );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"919191\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void inviteUserWithoutInviteeEmailIdReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void inviteUserWithMalformedInviteeEmailIdReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"$$$\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeEmailAndCompanyNumberExistsAndInviteeUserIsFoundPerformsSwapAndUpdateOperations() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "19", "36" ) );
        mockers.mockGetUserDetails( "9999" );
        mockers.mockSearchUserDetails( "000" );
        mockers.mockFetchCompanyProfile( "444444" );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}" ) )
                .andExpect( status().isCreated() );

        final var association = associationsRepository.findById( "36" ).get();
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertNull( association.getUserEmail() );
        Assertions.assertEquals( "000", association.getUserId() );
        Assertions.assertEquals( "444444", association.getCompanyNumber() );
        Assertions.assertEquals( StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus() );
        Assertions.assertEquals( ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getApprovalExpiryAt() );
        Assertions.assertNotNull( association.getEtag() );
        Assertions.assertNotEquals( "qq", association.getEtag() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertEquals( "9999", invitation.getInvitedBy() );
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeEmailAndCompanyNumberExistsAndInviteeUserIsNotFoundDoesNotPerformSwapButDoesPerformUpdateOperation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "19", "36" ) );
        mockers.mockGetUserDetails( "9999" );
        mockers.mockSearchUserDetailsNotFound( "light.yagami@death.note" );
        mockers.mockFetchCompanyProfile( "444444" );

        setEmailProducerCountDownLatch( 2 );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}" ) )
                .andExpect( status().isCreated() );

        final var association = associationsRepository.findById( "36" ).get();
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertEquals( "light.yagami@death.note", association.getUserEmail() );
        Assertions.assertNull( association.getUserId() );
        Assertions.assertEquals( "444444", association.getCompanyNumber() );
        Assertions.assertEquals( StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus() );
        Assertions.assertEquals( ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getApprovalExpiryAt() );
        Assertions.assertNotNull( association.getEtag() );
        Assertions.assertNotEquals( "qq", association.getEtag() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertEquals( "9999", invitation.getInvitedBy() );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer, times( 2 ) ).sendEmail( argThat( comparisonUtils.invitationAndInviteEmailDataMatcher( "scrooge.mcduck@disney.land", "Scrooge McDuck", "light.yagami@death.note", "light.yagami@death.note", "Sainsbury's", COMPANY_INVITATIONS_URL ) ), argThat( messageType -> List.of( INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue() ).contains( messageType ) ) );
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberExistsDoesNotPerformSwapButDoesPerformUpdateOperation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "19", "36" ) );
        mockers.mockGetUserDetails( "9999" );
        mockers.mockSearchUserDetails( "000" );
        mockers.mockFetchCompanyProfile( "444444" );

        setEmailProducerCountDownLatch( 2 );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}" ) )
                .andExpect( status().isCreated() );

        final var association = associationsRepository.findById( "36" ).get();
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertNull( association.getUserEmail() );
        Assertions.assertEquals( "000", association.getUserId() );
        Assertions.assertEquals( "444444", association.getCompanyNumber() );
        Assertions.assertEquals( StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus() );
        Assertions.assertEquals( ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getApprovalExpiryAt() );
        Assertions.assertNotNull( association.getEtag() );
        Assertions.assertNotEquals( "rr", association.getEtag() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertEquals( "9999", invitation.getInvitedBy() );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer, times( 2 ) ).sendEmail( argThat( comparisonUtils.invitationAndInviteEmailDataMatcher( "scrooge.mcduck@disney.land", "Scrooge McDuck", "light.yagami@death.note", "light.yagami@death.note", "Sainsbury's", COMPANY_INVITATIONS_URL ) ), argThat( messageType -> List.of( INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue() ).contains( messageType ) ) );
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberDoesNotExistCreatesNewAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "19" ) );
        mockers.mockGetUserDetails( "9999" );
        mockers.mockSearchUserDetails( "000" );
        mockers.mockFetchCompanyProfile( "444444" );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}" ) )
                .andExpect( status().isCreated() );

        final var association = associationsRepository.fetchAssociationForCompanyNumberAndUserId("444444", "000").get();
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertNull( association.getUserEmail() );
        Assertions.assertEquals( "000", association.getUserId() );
        Assertions.assertEquals( "444444", association.getCompanyNumber() );
        Assertions.assertEquals( ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getEtag() );
        Assertions.assertEquals( StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus() );
        Assertions.assertNotNull( association.getApprovalExpiryAt() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertEquals( "9999", invitation.getInvitedBy() );
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeUserEmailAndCompanyNumberDoesNotExistAndInviteeUserIsNotFoundCreatesNewAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "19" ) );
        mockers.mockGetUserDetails( "9999" );
        mockers.mockSearchUserDetailsNotFound( "light.yagami@death.note" );
        mockers.mockFetchCompanyProfile( "444444" );

        setEmailProducerCountDownLatch( 2 );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}" ) )
                .andExpect( status().isCreated() );

        final var association = associationsRepository.fetchAssociationForCompanyNumberAndUserEmail("444444", "light.yagami@death.note").get();
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertEquals( "light.yagami@death.note", association.getUserEmail() );
        Assertions.assertNull( association.getUserId() );
        Assertions.assertEquals( "444444", association.getCompanyNumber() );
        Assertions.assertEquals( ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getEtag() );
        Assertions.assertEquals( StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus() );
        Assertions.assertNotNull( association.getApprovalExpiryAt() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertEquals( "9999", invitation.getInvitedBy() );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer, times( 2 ) ).sendEmail( argThat( comparisonUtils.invitationAndInviteEmailDataMatcher( "scrooge.mcduck@disney.land", "Scrooge McDuck", "light.yagami@death.note", "light.yagami@death.note", "Sainsbury's", COMPANY_INVITATIONS_URL ) ), argThat( messageType -> List.of( INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue() ).contains( messageType ) ) );
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberExistsDoesNotPerformSwapButThrowsBadRequest() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18", "35" ) );
        mockers.mockGetUserDetails( "9999" );
        mockers.mockSearchUserDetails( "000" );
        mockers.mockFetchCompanyProfile( "333333" );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"light.yagami@death.note\"}" ) )
                .andExpect( status().isBadRequest() ).andReturn();
    }

    @Test
    void inviteUserWithUserThatHasDisplayNameUsesDisplayName()  throws Exception {

        associationsRepository.insert( testDataManager.fetchAssociationDaos( "19" ) );
        mockers.mockGetUserDetails( "9999" );
        mockers.mockSearchUserDetails( "111" );
        mockers.mockFetchCompanyProfile( "444444" );

        setEmailProducerCountDownLatch( 2 );

        mockMvc.perform(post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isCreated() );

        latch.await( 10, TimeUnit.SECONDS );
        Mockito.verify( emailProducer, times( 2 ) ).sendEmail( argThat( comparisonUtils.invitationAndInviteEmailDataMatcher( "scrooge.mcduck@disney.land", "Scrooge McDuck", "bruce.wayne@gotham.city", "Batman", "Sainsbury's", COMPANY_INVITATIONS_URL ) ), argThat( messageType -> List.of( INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue() ).contains( messageType ) ) );
    }

    @Test
    void fetchActiveInvitationsForUserWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchActiveInvitationsForUserWithoutEricIdentityReturnsUnauthorised() throws Exception {
        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isUnauthorized() );
    }

    @Test
    void fetchActiveInvitationsForUserWithMalformedEricIdentityReturnsForbidden() throws Exception {
        mockers.mockGetUserDetailsNotFound( "$$$$" );

        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "$$$$")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isForbidden() );
    }

    @Test
    void fetchActiveInvitationsForUserWithNonexistentEricIdentityReturnsForbidden() throws Exception {
        mockers.mockGetUserDetailsNotFound( "9191" );

        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isForbidden() );
    }

    @Test
    void fetchActiveInvitationsForUserWithUnacceptablePageIndexReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( get( "/associations/invitations?page_index=-1&items_per_page=1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchActiveInvitationsForUserWithUnacceptableItemsPerPageReturnsBadRequest() throws Exception {
        mockers.mockGetUserDetails( "9999" );

        mockMvc.perform( get( "/associations/invitations?page_index=0&items_per_page=-1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void fetchActiveInvitationsForUserRetrievesActiveInvitationsInCorrectOrderAndPaginatesCorrectly() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "37", "38" ) );
        mockers.mockGetUserDetails(  "000", "444" );

        final var response =
                mockMvc.perform( get( "/associations/invitations?page_index=0&items_per_page=1" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "000")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() );

        final var invitations = parseResponseTo( response, InvitationsList.class ).getItems();
        final var invitation = invitations.getFirst();

        Assertions.assertEquals( 1, invitations.size() );
        Assertions.assertEquals( "robin@gotham.city", invitation.getInvitedBy() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertEquals( "37", invitation.getAssociationId() );
        Assertions.assertTrue( invitation.getIsActive() );
    }

    @Test
    void fetchActiveInvitationsForUserWithoutActiveInvitationsReturnsEmptyList() throws Exception {
        mockers.mockGetUserDetails(  "111" );

        final var response =
                mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                                .header("X-Request-Id", "theId123")
                                .header("Eric-identity", "111")
                                .header("ERIC-Identity-Type", "oauth2")
                                .header("ERIC-Authorised-Key-Roles", "*") )
                        .andExpect( status().isOk() );

        final var invitations = parseResponseTo( response, InvitationsList.class );
        Assertions.assertTrue( invitations.getItems().isEmpty() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }

}