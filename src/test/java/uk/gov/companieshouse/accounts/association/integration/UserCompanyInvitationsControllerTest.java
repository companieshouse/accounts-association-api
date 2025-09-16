package uk.gov.companieshouse.accounts.association.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_IDENTITY;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_IDENTITY_TYPE_OAUTH;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.X_REQUEST_ID;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITE_MESSAGE_TYPE;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.service.client.CompanyClient;
import uk.gov.companieshouse.accounts.association.service.client.UserClient;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@TestPropertySource(locations = "classpath:application-test.properties")
class UserCompanyInvitationsControllerTest extends AbstractBaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private AssociationsRepository associationsRepository;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private UsersService usersService;

    // Mock Kafka
    // TODO: Replace with testcontainer instance
    @MockitoBean
    private KafkaProducerFactory kafkaProducerFactory;
    @MockitoBean
    private EmailProducer emailProducer;

    // Mock external service client layer
    @MockitoBean
    private CompanyClient companyClient;
    @MockitoBean
    private UserClient userClient;

    @Value("${invitation.url}")
    private String COMPANY_INVITATIONS_URL;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();
    private ComparisonUtils comparisonUtils = new ComparisonUtils();

    private CountDownLatch latch;

    private final String ASSOCIATIONS_INVITATIONS_URL = "/associations/invitations";

    private void setEmailProducerCountDownLatch(int countdown){
        latch = new CountDownLatch(countdown);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(emailProducer).sendEmail(any(), any());
    }

    @BeforeEach
    public void setup() {
    }

    @Test
    void fetchActiveInvitationsForUserWithoutEricIdentityReturnsForbidden() throws Exception {
        mockMvc.perform(get(ASSOCIATIONS_INVITATIONS_URL + "?page_index=1&items_per_page=1")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                .andExpect(status().isForbidden());
    }

    @Ignore
    @Test
    void fetchActiveInvitationsForUserWithMalformedEricIdentityReturnsForbidden() throws Exception {
        final var malformedUserId = "$$$$";
        // There's no regex on the interface for this test, so nothing to validate what "malformed" means
        // TODO: confirm against regex impl once available and remove these comments
        mockMvc.perform(get(ASSOCIATIONS_INVITATIONS_URL + "?page_index=1&items_per_page=1")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, malformedUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                .andExpect(status().isForbidden());
    }

    @Test
    void fetchActiveInvitationsForUserWithNonexistentEricIdentityReturnsForbidden() throws Exception {
        final var requestingUserId = "9191";

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("User not found", new Exception()));

        mockMvc.perform(get(ASSOCIATIONS_INVITATIONS_URL + "?page_index=1&items_per_page=1")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                .andExpect(status().isForbidden());
    }

    @Test
    void fetchActiveInvitationsForUserRetrievesActiveInvitationsInCorrectOrderAndPaginatesCorrectly() throws Exception {
        final var requestingUserId = "000";
        final var targetUserId = "444";

        associationsRepository.insert(testDataManager.fetchAssociationDaos("37", "38"));

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetails(targetUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(targetUserId).getFirst());

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_INVITATIONS_URL + "?page_index=0&items_per_page=1")
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, requestingUserId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                                .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                        .andExpect(status().isOk());

        final var invitations = parseResponseTo(response, InvitationsList.class).getItems();
        final var invitation = invitations.getFirst();

        Assertions.assertEquals(1, invitations.size());
        Assertions.assertEquals("robin@gotham.city", invitation.getInvitedBy());
        Assertions.assertNotNull(invitation.getInvitedAt());
        Assertions.assertEquals("37", invitation.getAssociationId());
        Assertions.assertTrue(invitation.getIsActive());
    }

    @Test
    void fetchActiveInvitationsForUserWithoutActiveInvitationsReturnsEmptyList() throws Exception {
        final var requestingUserId = "111";

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_INVITATIONS_URL + "?page_index=1&items_per_page=1")
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, requestingUserId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                                .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                        .andExpect(status().isOk());

        final var invitations = parseResponseTo(response, InvitationsList.class);
        Assertions.assertTrue(invitations.getItems().isEmpty());
    }

    @Test
    void inviteUserWithoutXRequestIdReturnsBadRequest() throws Exception {
        final var requestingUserId = "9999";

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithoutEricIdentityReturnsForbidden() throws Exception {
        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @Ignore
    void inviteUserWithMalformedEricIdentityReturnsForbidden() throws Exception {
        final var malformedRequestingUserId = "$$$$";

        //TODO: should this be treated as malformed or just passed along to not be found?
//        when(userClient.requestUserDetails(malformedRequestingUserId, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("User not found", new Exception()));

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, malformedRequestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void inviteUserWithNonexistentEricIdentityReturnsForbidden() throws Exception {
        final var requestingUserId = "9191";

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("User not found", new Exception()));

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void inviteUserWithoutRequestBodyReturnsBadRequest() throws Exception {
        final var requestingUserId = "9999";

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithoutCompanyNumberReturnsBadRequest() throws Exception {
        final var requestingUserId = "9999";

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithMalformedCompanyNumberReturnsBadRequest() throws Exception {
        final var requestingUserId = "9999";

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"$$$$$$\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithNonexistentCompanyNumberReturnsBadRequest() throws Exception {
        final var requestingUserId = "9999";
        final var companyNumber = "919191";

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("Company not found", new Exception()));

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "9999")
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"919191\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithoutInviteeEmailIdReturnsBadRequest() throws Exception {
        final var requestingUserId = "9999";

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWithMalformedInviteeEmailIdReturnsBadRequest() throws Exception {
        final var requestingUserId = "9999";

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"$$$\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeEmailAndCompanyNumberExistsAndInviteeUserIsFoundPerformsSwapAndUpdateOperations() throws Exception {
        final var requestingUserId = "9999";
        final var targetUserId = "000";
        final var companyNumber = "444444";
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var usersList = new UsersList();
        usersList.add(targetUserDetails);

        associationsRepository.insert(testDataManager.fetchAssociationDaos("19", "36"));

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(targetUserDetails.getEmail(), X_REQUEST_ID.value)).thenReturn(usersList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isCreated());

        final var association = associationsRepository.findById("36").get();
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertNull(association.getUserEmail());
        Assertions.assertEquals("000", association.getUserId());
        Assertions.assertEquals("444444", association.getCompanyNumber());
        Assertions.assertEquals(StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus());
        Assertions.assertEquals(ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute());
        Assertions.assertNotNull(association.getApprovalExpiryAt());
        Assertions.assertNotNull(association.getEtag());
        Assertions.assertNotEquals("qq", association.getEtag());
        Assertions.assertNotNull(invitation.getInvitedAt());
        Assertions.assertEquals("9999", invitation.getInvitedBy());
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeEmailAndCompanyNumberExistsAndInviteeUserIsNotFoundDoesNotPerformSwapButDoesPerformUpdateOperation() throws Exception {
        final var requestingUserId = "9999";
        final var targetUserId = "000";
        final var companyNumber = "444444";
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var usersList = new UsersList();

        associationsRepository.insert(testDataManager.fetchAssociationDaos("19", "36"));

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(targetUserDetails.getEmail(), X_REQUEST_ID.value)).thenReturn(usersList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        setEmailProducerCountDownLatch(2);

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isCreated());

        final var association = associationsRepository.findById("36").get();
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertEquals("light.yagami@death.note", association.getUserEmail());
        Assertions.assertNull(association.getUserId());
        Assertions.assertEquals("444444", association.getCompanyNumber());
        Assertions.assertEquals(StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus());
        Assertions.assertEquals(ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute());
        Assertions.assertNotNull(association.getApprovalExpiryAt());
        Assertions.assertNotNull(association.getEtag());
        Assertions.assertNotEquals("qq", association.getEtag());
        Assertions.assertNotNull(invitation.getInvitedAt());
        Assertions.assertEquals("9999", invitation.getInvitedBy());

        latch.await(10, TimeUnit.SECONDS);
        Mockito.verify(emailProducer, times(2)).sendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("scrooge.mcduck@disney.land", "Scrooge McDuck", "light.yagami@death.note", "light.yagami@death.note", "Sainsbury's", COMPANY_INVITATIONS_URL)), argThat(messageType -> List.of(INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue()).contains(messageType)));
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberExistsDoesNotPerformSwapButDoesPerformUpdateOperation() throws Exception {
        final var requestingUserId = "9999";
        final var targetUserId = "000";
        final var companyNumber = "444444";
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var usersList = new UsersList();
        usersList.add(targetUserDetails);

        associationsRepository.insert(testDataManager.fetchAssociationDaos("19", "36"));

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(targetUserDetails.getEmail(), X_REQUEST_ID.value)).thenReturn(usersList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        setEmailProducerCountDownLatch(2);

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isCreated());

        final var association = associationsRepository.findById("36").get();
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertNull(association.getUserEmail());
        Assertions.assertEquals("000", association.getUserId());
        Assertions.assertEquals("444444", association.getCompanyNumber());
        Assertions.assertEquals(StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus());
        Assertions.assertEquals(ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute());
        Assertions.assertNotNull(association.getApprovalExpiryAt());
        Assertions.assertNotNull(association.getEtag());
        Assertions.assertNotEquals("rr", association.getEtag());
        Assertions.assertNotNull(invitation.getInvitedAt());
        Assertions.assertEquals("9999", invitation.getInvitedBy());

        latch.await(10, TimeUnit.SECONDS);
        Mockito.verify(emailProducer, times(2)).sendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("scrooge.mcduck@disney.land", "Scrooge McDuck", "light.yagami@death.note", "light.yagami@death.note", "Sainsbury's", COMPANY_INVITATIONS_URL)), argThat(messageType -> List.of(INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue()).contains(messageType)));
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberDoesNotExistCreatesNewAssociation() throws Exception {
        final var requestingUserId = "9999";
        final var targetUserId = "000";
        final var companyNumber = "444444";
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var usersList = new UsersList();
        usersList.add(targetUserDetails);

        associationsRepository.insert(testDataManager.fetchAssociationDaos("19"));

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(targetUserDetails.getEmail(), X_REQUEST_ID.value)).thenReturn(usersList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isCreated());

        final var association = associationsRepository.fetchAssociation("444444", "000", null).get();
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertNull(association.getUserEmail());
        Assertions.assertEquals("000", association.getUserId());
        Assertions.assertEquals("444444", association.getCompanyNumber());
        Assertions.assertEquals(ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute());
        Assertions.assertNotNull(association.getEtag());
        Assertions.assertEquals(StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus());
        Assertions.assertNotNull(association.getApprovalExpiryAt());
        Assertions.assertNotNull(invitation.getInvitedAt());
        Assertions.assertEquals("9999", invitation.getInvitedBy());
    }

    @Test
    void inviteUserWhereAssociationBetweenInviteeUserEmailAndCompanyNumberDoesNotExistAndInviteeUserIsNotFoundCreatesNewAssociation() throws Exception {
        final var requestingUserId = "9999";
        final var targetUserId = "000";
        final var companyNumber = "444444";
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var usersList = new UsersList();

        associationsRepository.insert(testDataManager.fetchAssociationDaos("19"));

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(targetUserDetails.getEmail(), X_REQUEST_ID.value)).thenReturn(usersList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        setEmailProducerCountDownLatch(2);

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "9999")
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isCreated());

        final var association = associationsRepository.fetchAssociation("444444", null,"light.yagami@death.note").get();
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertEquals("light.yagami@death.note", association.getUserEmail());
        Assertions.assertNull(association.getUserId());
        Assertions.assertEquals("444444", association.getCompanyNumber());
        Assertions.assertEquals(ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute());
        Assertions.assertNotNull(association.getEtag());
        Assertions.assertEquals(StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus());
        Assertions.assertNotNull(association.getApprovalExpiryAt());
        Assertions.assertNotNull(invitation.getInvitedAt());
        Assertions.assertEquals("9999", invitation.getInvitedBy());

        latch.await(10, TimeUnit.SECONDS);
        Mockito.verify(emailProducer, times(2)).sendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("scrooge.mcduck@disney.land", "Scrooge McDuck", "light.yagami@death.note", "light.yagami@death.note", "Sainsbury's", COMPANY_INVITATIONS_URL)), argThat(messageType -> List.of(INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue()).contains(messageType)));
    }

    @Test
    void inviteUserWhereInviteeUserIsFoundAndAssociationBetweenInviteeUserIdAndCompanyNumberExistsDoesNotPerformSwapButThrowsBadRequest() throws Exception {
        final var requestingUserId = "9999";
        final var targetUserId = "000";
        final var companyNumber = "333333";
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var usersList = new UsersList();
        usersList.add(targetUserDetails);

        associationsRepository.insert(testDataManager.fetchAssociationDaos("18", "35"));

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(targetUserDetails.getEmail(), X_REQUEST_ID.value)).thenReturn(usersList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"333333\",\"invitee_email_id\":\"light.yagami@death.note\"}"))
                .andExpect(status().isBadRequest()).andReturn();
    }

    @Test
    void inviteUserWithUserThatHasDisplayNameUsesDisplayName()  throws Exception {
        final var requestingUserId = "9999";
        final var targetUserId = "111";
        final var companyNumber = "444444";
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var usersList = new UsersList();
        usersList.add(targetUserDetails);

        associationsRepository.insert(testDataManager.fetchAssociationDaos("19"));

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(targetUserDetails.getEmail(), X_REQUEST_ID.value)).thenReturn(usersList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        setEmailProducerCountDownLatch(2);

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"444444\",\"invitee_email_id\":\"bruce.wayne@gotham.city\"}"))
                .andExpect(status().isCreated());

        latch.await(10, TimeUnit.SECONDS);
        Mockito.verify(emailProducer, times(2)).sendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("scrooge.mcduck@disney.land", "Scrooge McDuck", "bruce.wayne@gotham.city", "Batman", "Sainsbury's", COMPANY_INVITATIONS_URL)), argThat(messageType -> List.of(INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue()).contains(messageType)));
    }

    private static Stream<Arguments> inviteUserAppliedToMigratedAssociationScenarios(){
         return Stream.of(
                 Arguments.of(testDataManager.fetchAssociationDaos("MKAssociation001").getFirst()),
                 Arguments.of(testDataManager.fetchAssociationDaos("MKAssociation001").getFirst().userId(null).userEmail("mario@mushroom.kingdom"))
       );
    }

    @ParameterizedTest
    @MethodSource("inviteUserAppliedToMigratedAssociationScenarios")
    void inviteUserCanBeAppliedToMigratedAssociations(final AssociationDao targetAssociation) throws Exception {
        final var requestingUserId = "MKUser002";
        final var targetUserId = "MKUser001";
        final var companyNumber = "MKCOMP001";
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var usersList = new UsersList();
        usersList.add(targetUserDetails);
        final var requestingAssociation = testDataManager.fetchAssociationDaos("MKAssociation002").getFirst();

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(targetUserDetails.getEmail(), X_REQUEST_ID.value)).thenReturn(usersList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        associationsRepository.insert(List.of(requestingAssociation, targetAssociation));

        mockMvc.perform(post(ASSOCIATIONS_INVITATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"company_number\":\"MKCOMP001\",\"invitee_email_id\":\"mario@mushroom.kingdom\"}"))
                .andExpect(status().isCreated());

        final var updatedAssociation = associationsRepository.findById("MKAssociation001").get();
        Assertions.assertEquals(StatusEnum.AWAITING_APPROVAL.getValue(), updatedAssociation.getStatus());
        Assertions.assertEquals(1, updatedAssociation.getPreviousStates().size());
        Assertions.assertEquals(StatusEnum.MIGRATED.getValue(), updatedAssociation.getPreviousStates().getFirst().getStatus());
        Assertions.assertEquals("MKUser002", updatedAssociation.getPreviousStates().getFirst().getChangedBy());
        Assertions.assertNotNull(updatedAssociation.getPreviousStates().getFirst().getChangedAt());
        Assertions.assertNotNull(updatedAssociation.getEtag());
        Assertions.assertNotNull(updatedAssociation.getApprovalExpiryAt());
        Assertions.assertEquals(1, updatedAssociation.getInvitations().size());
        Assertions.assertEquals("MKUser002", updatedAssociation.getInvitations().getFirst().getInvitedBy());
        Assertions.assertNotNull(updatedAssociation.getInvitations().getFirst().getInvitedAt());
    }

    @AfterEach
    public void after() {
        associationsRepository.deleteAll();
    }

}
