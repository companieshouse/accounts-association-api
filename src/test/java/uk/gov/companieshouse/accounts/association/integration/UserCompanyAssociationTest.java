package uk.gov.companieshouse.accounts.association.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_READ_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_UPDATE_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_ACCEPTED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_CANCELLED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITE_CANCELLED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.AWAITING_APPROVAL;

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
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.PreviousStatesList;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

@AutoConfigureMockMvc
@SpringBootTest
@ExtendWith( MockitoExtension.class )
@Tag( "integration-test" )
class UserCompanyAssociationTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private MockMvc mockMvc;

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

    @Test
    void getAssociationDetailsWithoutPathVariableReturnsNotFound() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "9999" );

        mockMvc.perform( get( "/associations/" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isNotFound() );
    }

    @Test
    void getAssociationUserDetailsWithNonexistentIdReturnsNotFound() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "9999" );

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
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );

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
    void getAssociationForIdCanFetchMigratedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockUsersServiceFetchUserDetails( "MKUser001" );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );

        final var response =
                mockMvc.perform( get( "/associations/MKAssociation001" )
                                .header( "X-Request-Id", "theId123" )
                                .header( "Eric-identity", "MKUser001" )
                                .header( "ERIC-Identity-Type", "oauth2" ) )
                        .andExpect( status().isOk() );

        final var association = parseResponseTo( response, Association.class );

        Assertions.assertEquals( "MKAssociation001", association.getId() );
        Assertions.assertEquals( "migrated", association.getStatus().getValue() );
        Assertions.assertEquals( "migration", association.getApprovalRoute().getValue() );
    }

    @Test
    void getAssociationForIdCanBeCalledByAdmin() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockUsersServiceFetchUserDetails( "MKUser001", "111" );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );

        mockMvc.perform( get( "/associations/MKAssociation001" )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "111" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .header( "Eric-Authorised-Roles", ADMIN_READ_PERMISSION ) )
                .andExpect( status().isOk() );
    }

    @Test
    void updateAssociationStatusForIdWithMalformedAssociationIdReturnsBadRequest() throws Exception {
        mockers.mockUsersServiceFetchUserDetails(  "9999" );

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
        mockers.mockUsersServiceFetchUserDetails(  "9999" );

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
        mockers.mockUsersServiceFetchUserDetails(  "9999" );

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
        mockers.mockUsersServiceFetchUserDetails(  "9999" );

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
        mockers.mockUsersServiceFetchUserDetails(  "9999" );

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
        mockers.mockUsersServiceFetchUserDetails(  "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "9999" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

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
        mockers.mockUsersServiceFetchUserDetails(  "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "9999" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

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
        mockers.mockUsersServiceFetchUserDetails( "222" );
        Mockito.doReturn( null ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );


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
        mockers.mockUsersServiceFetchUserDetails( "111" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "000" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

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
        mockers.mockUsersServiceFetchUserDetails( "111" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( null ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

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
        Assertions.assertEquals( 1, newAssociationData.getPreviousStates().size() );
        Assertions.assertEquals( "awaiting-approval", newAssociationData.getPreviousStates().getFirst().getStatus() );
        Assertions.assertEquals( "111", newAssociationData.getPreviousStates().getFirst().getChangedBy() );
        Assertions.assertNotNull( newAssociationData.getPreviousStates().getFirst().getChangedAt() );
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereTargetUserIdExistsSendsNotification() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18", "35" ) );
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "000" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

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
        mockers.mockUsersServiceFetchUserDetails( "111" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "000" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

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

        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher( "null", "Batman", "null", "Wayne Enterprises", "light.yagami@death.note"  ) ), eq( INVITE_CANCELLED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereUserCannotBeFoundSendsNotification() throws Exception {
        final var associationDaos = testDataManager.fetchAssociationDaos( "1", "34" );

        associationsRepository.insert(associationDaos);
        mockers.mockUsersServiceFetchUserDetails( "111" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "000" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

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
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher( "null", "Batman", "null", "Wayne Enterprises",  "light.yagami@death.note"  ) ), eq( INVITE_CANCELLED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void updateAssociationStatusForIdNotificationsUsesDisplayNamesWhenAvailable() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "3", "4" ) );
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "444" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

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
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "444" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

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
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        mockers.mockUsersServiceFetchUserDetails( "222", "444", "666" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "666" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

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
    void updateAssociationStatusForIdThrowsBadRequestWhenUserTriesToConfirmOwnMigratedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        mockers.mockUsersServiceFetchUserDetails( "MKUser001" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "MKUser001" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

        mockMvc.perform( patch( "/associations/MKAssociation001"  )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "MKUser001" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdUpdatesAssociationWhenUserTriesToRemoveOwnMigratedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        mockers.mockUsersServiceFetchUserDetails( "MKUser001" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "MKUser001" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

        mockMvc.perform( patch( "/associations/MKAssociation001"  )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "MKUser001" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        final var updatedAssociation = associationsRepository.findById( "MKAssociation001" ).get();

        Assertions.assertEquals( "removed", updatedAssociation.getStatus() );
        Assertions.assertNotNull( updatedAssociation.getRemovedAt() );
        Assertions.assertNotNull( updatedAssociation.getEtag() );
        Assertions.assertEquals( 1, updatedAssociation.getPreviousStates().size() );
        Assertions.assertEquals( "migrated", updatedAssociation.getPreviousStates().getFirst().getStatus() );
        Assertions.assertEquals( "MKUser001", updatedAssociation.getPreviousStates().getFirst().getChangedBy() );
        Assertions.assertNotNull(  updatedAssociation.getPreviousStates().getFirst().getChangedAt() );
        Assertions.assertNull( updatedAssociation.getUserEmail() );
        Assertions.assertEquals( "MKUser001", updatedAssociation.getUserId() );
    }

    @Test
    void updateAssociationStatusForIdUpdatesAssociationWhenUserTriesToConfirmedAnotherUsersMigratedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "MKUser001" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

        mockMvc.perform( patch( "/associations/MKAssociation001"  )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "MKUser002" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );

        final var updatedAssociation = associationsRepository.findById( "MKAssociation001" ).get();

        Assertions.assertEquals( "confirmed", updatedAssociation.getStatus() );
        Assertions.assertNotNull( updatedAssociation.getApprovedAt() );
        Assertions.assertNotNull( updatedAssociation.getEtag() );
        Assertions.assertEquals( 1, updatedAssociation.getPreviousStates().size() );
        Assertions.assertEquals( "migrated", updatedAssociation.getPreviousStates().getFirst().getStatus() );
        Assertions.assertEquals( "MKUser002", updatedAssociation.getPreviousStates().getFirst().getChangedBy() );
        Assertions.assertNotNull(  updatedAssociation.getPreviousStates().getFirst().getChangedAt() );
        Assertions.assertNull( updatedAssociation.getUserEmail() );
        Assertions.assertEquals( "MKUser001", updatedAssociation.getUserId() );
    }

    @Test
    void updateAssociationStatusForIdUpdatesAssociationWhenUserTriesToRemoveAnotherUsersMigratedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "MKUser001" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

        mockMvc.perform( patch( "/associations/MKAssociation001"  )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "MKUser002" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        final var updatedAssociation = associationsRepository.findById( "MKAssociation001" ).get();

        Assertions.assertEquals( "removed", updatedAssociation.getStatus() );
        Assertions.assertNotNull( updatedAssociation.getRemovedAt() );
        Assertions.assertNotNull( updatedAssociation.getEtag() );
        Assertions.assertEquals( 1, updatedAssociation.getPreviousStates().size() );
        Assertions.assertEquals( "migrated", updatedAssociation.getPreviousStates().getFirst().getStatus() );
        Assertions.assertEquals( "MKUser002", updatedAssociation.getPreviousStates().getFirst().getChangedBy() );
        Assertions.assertNotNull(  updatedAssociation.getPreviousStates().getFirst().getChangedAt() );
        Assertions.assertNull( updatedAssociation.getUserEmail() );
        Assertions.assertEquals( "MKUser001", updatedAssociation.getUserId() );
    }

    @Test
    void updateAssociationStatusForIdSendInvitationWhenUserTriesToConfirmNonexistentUsersMigratedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        Mockito.doReturn( null ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

        mockMvc.perform( patch( "/associations/MKAssociation001"  )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "MKUser002" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );

        final var updatedAssociation = associationsRepository.findById( "MKAssociation001" ).get();

        Assertions.assertEquals( "awaiting-approval", updatedAssociation.getStatus() );
        Assertions.assertNotNull( updatedAssociation.getApprovalExpiryAt() );
        Assertions.assertEquals( 1, updatedAssociation.getInvitations().size() );
        Assertions.assertEquals( "MKUser002", updatedAssociation.getInvitations().getFirst().getInvitedBy() );
        Assertions.assertNotNull( updatedAssociation.getInvitations().getFirst().getInvitedAt() );
        Assertions.assertNotNull( updatedAssociation.getEtag() );
        Assertions.assertEquals( 1, updatedAssociation.getPreviousStates().size() );
        Assertions.assertEquals( "migrated", updatedAssociation.getPreviousStates().getFirst().getStatus() );
        Assertions.assertEquals( "MKUser002", updatedAssociation.getPreviousStates().getFirst().getChangedBy() );
        Assertions.assertNotNull(  updatedAssociation.getPreviousStates().getFirst().getChangedAt() );
        Assertions.assertEquals( "mario@mushroom.kingdom", updatedAssociation.getUserEmail() );
        Assertions.assertNull( updatedAssociation.getUserId() );
    }

    @Test
    void updateAssociationStatusForIdUpdatesAssociationWhenUserTriesToRemoveNonexistentUsersMigratedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        Mockito.doReturn( null ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

        mockMvc.perform( patch( "/associations/MKAssociation001"  )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "MKUser002" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        final var updatedAssociation = associationsRepository.findById( "MKAssociation001" ).get();

        Assertions.assertEquals( "removed", updatedAssociation.getStatus() );
        Assertions.assertNotNull( updatedAssociation.getRemovedAt() );
        Assertions.assertNotNull( updatedAssociation.getEtag() );
        Assertions.assertEquals( 1, updatedAssociation.getPreviousStates().size() );
        Assertions.assertEquals( "migrated", updatedAssociation.getPreviousStates().getFirst().getStatus() );
        Assertions.assertEquals( "MKUser002", updatedAssociation.getPreviousStates().getFirst().getChangedBy() );
        Assertions.assertNotNull(  updatedAssociation.getPreviousStates().getFirst().getChangedAt() );
        Assertions.assertEquals( "mario@mushroom.kingdom", updatedAssociation.getUserEmail() );
        Assertions.assertNull( updatedAssociation.getUserId() );
    }

    @Test
    void updateAssociationStatusForIdAllowsAdminUserToRemoveAuthorisation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2" ) );
        mockers.mockUsersServiceFetchUserDetails( "9999", "111", "222" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "111" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

        setEmailProducerCountDownLatch( 2 );

        mockMvc.perform( patch( "/associations/1"  )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "9999" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .header( "Eric-Authorised-Roles", ADMIN_UPDATE_PERMISSION )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        latch.await( 10, TimeUnit.SECONDS );

        final var updatedAssociation = associationsRepository.findById( "1" ).get();

        Assertions.assertEquals( "removed", updatedAssociation.getStatus() );

        Mockito.verify( emailProducer, times( 2 ) ).sendEmail( argThat( comparisonUtils.authorisationRemovedAndYourAuthorisationRemovedEmailMatcher( COMPANIES_HOUSE, "Batman", "Wayne Enterprises", "the.joker@gotham.city", "bruce.wayne@gotham.city" ) ), argThat( messageType -> List.of( AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue(), YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() ).contains( messageType ) ) );
    }

    @Test
    void updateAssociationStatusForIdAllowsAdminUserToCancelInvitation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "6", "2" ) );
        mockers.mockUsersServiceFetchUserDetails( "9999", "666", "222" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn( testDataManager.fetchUserDtos( "666" ).getFirst() ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );

        setEmailProducerCountDownLatch( 2 );

        mockMvc.perform( patch( "/associations/6"  )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "9999" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .header( "Eric-Authorised-Roles", ADMIN_UPDATE_PERMISSION )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );

        latch.await( 10, TimeUnit.SECONDS );

        final var updatedAssociation = associationsRepository.findById( "6" ).get();

        Assertions.assertEquals( "removed", updatedAssociation.getStatus() );

        Mockito.verify( emailProducer, times( 2 ) ).sendEmail( argThat( comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher( "the.joker@gotham.city", "Companies House", "homer.simpson@springfield.com", "Wayne Enterprises", "homer.simpson@springfield.com" ) ), argThat( messageType -> List.of( INVITATION_CANCELLED_MESSAGE_TYPE.getValue(), INVITE_CANCELLED_MESSAGE_TYPE.getValue() ).contains( messageType ) ) );
    }

    @Test
    void getPreviousStatesForAssociationRetrievesData() throws Exception {
        final var now = LocalDateTime.now();

        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation003" ) );
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );

        final var response = mockMvc.perform( get( "/associations/MKAssociation003/previous-states?page_index=1&items_per_page=1" )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "MKUser002" )
                        .header( "ERIC-Identity-Type", "oauth2" ) )
                .andExpect( status().isOk() );

        final var previousStatesList = parseResponseTo( response, PreviousStatesList.class );
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();

        Assertions.assertEquals( 1, previousStatesList.getItemsPerPage() );
        Assertions.assertEquals( 1, previousStatesList.getPageNumber() );
        Assertions.assertEquals( 4, previousStatesList.getTotalResults() );
        Assertions.assertEquals( 4, previousStatesList.getTotalPages() );
        Assertions.assertEquals( "/associations/MKAssociation003/previous-states?page_index=1&items_per_page=1", links.getSelf() );
        Assertions.assertEquals( "/associations/MKAssociation003/previous-states?page_index=2&items_per_page=1", links.getNext() );
        Assertions.assertEquals( 1, items.size() );
        Assertions.assertEquals( AWAITING_APPROVAL, items.getFirst().getStatus() );
        Assertions.assertEquals( "MKUser003", items.getFirst().getChangedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.minusDays( 7L ) ), reduceTimestampResolution( items.getFirst().getChangedAt() ) );
    }

    @Test
    void getPreviousStatesForAssociationUsesDefaults() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );

        final var response = mockMvc.perform( get( "/associations/MKAssociation001/previous-states" )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "MKUser002" )
                        .header( "ERIC-Identity-Type", "oauth2" ) )
                .andExpect( status().isOk() );

        final var previousStatesList = parseResponseTo( response, PreviousStatesList.class );
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();

        Assertions.assertEquals( 15, previousStatesList.getItemsPerPage() );
        Assertions.assertEquals( 0, previousStatesList.getPageNumber() );
        Assertions.assertEquals( 0, previousStatesList.getTotalResults() );
        Assertions.assertEquals( 0, previousStatesList.getTotalPages() );
        Assertions.assertEquals( "/associations/MKAssociation001/previous-states?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertTrue( items.isEmpty() );
    }

    private static Stream<Arguments> getPreviousStatesForAssociationMalformedScenarios(){
        return Stream.of(
                Arguments.of( "/associations/$$$/previous-states" ),
                Arguments.of( "/associations/MKAssociation003/previous-states?page_index=-1" ),
                Arguments.of( "/associations/MKAssociation003/previous-states?items_per_page=-1" )
        );
    }

    @ParameterizedTest
    @MethodSource( "getPreviousStatesForAssociationMalformedScenarios" )
    void getPreviousStatesForAssociationWithMalformedAssociationIdReturnsBadRequest( final String uri ) throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        mockMvc.perform( get( uri )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "MKUser002" )
                        .header( "ERIC-Identity-Type", "oauth2" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getPreviousStatesForAssociationWithNonexistentAssociationReturnsNotFound() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        mockMvc.perform( get( "/associations/404MKAssociation/previous-states" )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "MKUser002" )
                        .header( "ERIC-Identity-Type", "oauth2" ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void getPreviousStatesForAssociationCanBeCalledByAdmin() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockUsersServiceFetchUserDetails( "111" );

        mockMvc.perform( get( "/associations/MKAssociation001/previous-states" )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "111" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .header( "Eric-Authorised-Roles", ADMIN_READ_PERMISSION ) )
                .andExpect( status().isOk() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }

}
