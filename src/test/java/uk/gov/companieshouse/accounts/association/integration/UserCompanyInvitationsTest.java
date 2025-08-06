package uk.gov.companieshouse.accounts.association.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITE_MESSAGE_TYPE;

import java.io.IOException;
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
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

@AutoConfigureMockMvc
@SpringBootTest
@ExtendWith( MockitoExtension.class )
@Tag( "integration-test" )
class UserCompanyInvitationsTest {

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

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @Value( "${invitation.url}")
    private String COMPANY_INVITATIONS_URL;

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
    void fetchActiveInvitationsForUserWithoutEricIdentityReturnsForbidden() throws Exception {
        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isForbidden() );
    }

    @Test
    void fetchActiveInvitationsForUserWithMalformedEricIdentityReturnsForbidden() throws Exception {
        mockers.mockUsersServiceFetchUserDetailsNotFound( "$$$$" );

        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "$$$$")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isForbidden() );
    }

    @Test
    void fetchActiveInvitationsForUserWithNonexistentEricIdentityReturnsForbidden() throws Exception {
        mockers.mockUsersServiceFetchUserDetailsNotFound( "9191" );

        mockMvc.perform( get( "/associations/invitations?page_index=1&items_per_page=1" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9191")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*") )
                .andExpect( status().isForbidden() );
    }

    @Test
    void fetchActiveInvitationsForUserRetrievesActiveInvitationsInCorrectOrderAndPaginatesCorrectly() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "37", "38" ) );
        mockers.mockUsersServiceFetchUserDetails(  "000", "444" );

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
        mockers.mockUsersServiceFetchUserDetails(  "111" );

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

    @Test
    void inviteUserWithoutXRequestIdReturnsBadRequest() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "9999" );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void inviteUserWithoutEricIdentityReturnsForbidden() throws Exception {
        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}" ) )
                .andExpect( status().isForbidden() );
    }

    @Test
    void inviteUserWithMalformedEricIdentityReturnsForbidden() throws Exception {
        mockers.mockUsersServiceFetchUserDetailsNotFound( "$$$$" );

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
        mockers.mockUsersServiceFetchUserDetailsNotFound( "9191" );

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
        mockers.mockUsersServiceFetchUserDetails( "9999" );

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
        mockers.mockUsersServiceFetchUserDetails( "9999" );

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
        mockers.mockUsersServiceFetchUserDetails( "9999" );

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
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockCompanyServiceFetchCompanyProfileNotFound( "919191" );

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
        mockers.mockUsersServiceFetchUserDetails( "9999" );

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
        mockers.mockUsersServiceFetchUserDetails( "9999" );

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
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockUsersServiceSearchUserDetails( "000" );
        mockers.mockCompanyServiceFetchCompanyProfile( "444444" );

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
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockUsersServiceToFetchUserDetailsRequest( "9999" );
        mockers.mockUsersServiceSearchUserDetailsEmptyList( "light.yagami@death.note" );
        mockers.mockCompanyServiceFetchCompanyProfile( "444444" );

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
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockUsersServiceToFetchUserDetailsRequest( "9999" );
        mockers.mockUsersServiceSearchUserDetails( "000" );
        mockers.mockCompanyServiceFetchCompanyProfile( "444444" );

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
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockUsersServiceSearchUserDetails( "000" );
        mockers.mockCompanyServiceFetchCompanyProfile( "444444" );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}" ) )
                .andExpect( status().isCreated() );

        final var association = associationsRepository.fetchAssociation("444444", "000", null).get();
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
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockUsersServiceToFetchUserDetailsRequest( "9999" );
        mockers.mockUsersServiceSearchUserDetailsEmptyList( "light.yagami@death.note" );
        mockers.mockCompanyServiceFetchCompanyProfile( "444444" );

        setEmailProducerCountDownLatch( 2 );

        mockMvc.perform( post( "/associations/invitations" )
                        .header("X-Request-Id", "theId123")
                        .header("Eric-identity", "9999")
                        .header("ERIC-Identity-Type", "oauth2")
                        .header("ERIC-Authorised-Key-Roles", "*")
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}" ) )
                .andExpect( status().isCreated() );

        final var association = associationsRepository.fetchAssociation("444444", null,"light.yagami@death.note").get();
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
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockUsersServiceSearchUserDetails( "000" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );

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
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockUsersServiceToFetchUserDetailsRequest( "9999" );
        mockers.mockUsersServiceSearchUserDetails( "111" );
        mockers.mockCompanyServiceFetchCompanyProfile( "444444" );

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

    private static Stream<Arguments> inviteUserAppliedToMigratedAssociationScenarios(){
         return Stream.of(
                 Arguments.of( testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst() ),
                 Arguments.of( testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst().userId( null ).userEmail( "mario@mushroom.kingdom" ) )
         );
    }

    @ParameterizedTest
    @MethodSource( "inviteUserAppliedToMigratedAssociationScenarios" )
    void inviteUserCanBeAppliedToMigratedAssociations( final Association targetAssociation ) throws Exception {
        final var requestingAssociation = testDataManager.fetchAssociationDaos( "MKAssociation002" ).getFirst();

        associationsRepository.insert( List.of( requestingAssociation, targetAssociation ) );
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        mockers.mockUsersServiceSearchUserDetails( "MKUser001" );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );

        mockMvc.perform( post( "/associations/invitations" )
                        .header( "X-Request-Id", "theId123" )
                        .header( "Eric-identity", "MKUser002" )
                        .header( "ERIC-Identity-Type", "oauth2" )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"company_number\":\"MKCOMP001\",\"invitee_email_id\":\"mario@mushroom.kingdom\"}" ) )
                .andExpect( status().isCreated() );

        final var updatedAssociation = associationsRepository.findById( "MKAssociation001" ).get();
        Assertions.assertEquals( StatusEnum.AWAITING_APPROVAL.getValue(), updatedAssociation.getStatus() );
        Assertions.assertEquals( 1, updatedAssociation.getPreviousStates().size() );
        Assertions.assertEquals( StatusEnum.MIGRATED.getValue(), updatedAssociation.getPreviousStates().getFirst().getStatus() );
        Assertions.assertEquals( "MKUser002", updatedAssociation.getPreviousStates().getFirst().getChangedBy() );
        Assertions.assertNotNull( updatedAssociation.getPreviousStates().getFirst().getChangedAt() );
        Assertions.assertNotNull( updatedAssociation.getEtag() );
        Assertions.assertNotNull( updatedAssociation.getApprovalExpiryAt() );
        Assertions.assertEquals( 1, updatedAssociation.getInvitations().size() );
        Assertions.assertEquals( "MKUser002", updatedAssociation.getInvitations().getFirst().getInvitedBy() );
        Assertions.assertNotNull( updatedAssociation.getInvitations().getFirst().getInvitedAt() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Association.class);
    }

}
