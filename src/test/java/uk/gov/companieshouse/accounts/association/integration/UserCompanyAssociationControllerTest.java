package uk.gov.companieshouse.accounts.association.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.parseResponseTo;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_ADMIN_READ_PERMISSION;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_ADMIN_UPDATE_PERMISSION;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_IDENTITY;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_IDENTITY_TYPE_API_KEY;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.ERIC_IDENTITY_TYPE_OAUTH;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.X_REQUEST_ID;
import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.DELEGATED_REMOVAL_OF_MIGRATED;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.DELEGATED_REMOVAL_OF_MIGRATED_BATCH;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_ACCEPTED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_CANCELLED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITE_CANCELLED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITE_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.REMOVAL_OF_OWN_MIGRATED;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonFrom;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.UNAUTHORISED;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.context.annotation.ComponentScan;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_BODY_ASSOCIATION_STATUS;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationAcceptedEmailBuilder;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.service.client.CompanyClient;
import uk.gov.companieshouse.accounts.association.service.client.UserClient;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.PreviousStatesList;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
@ComponentScan(basePackages = "uk.gov.companieshouse.email_producer")
@TestPropertySource(locations = "classpath:application-test.properties")
class UserCompanyAssociationControllerTest extends AbstractBaseIntegrationTest {

    @Value("${invitation.url}")
    private String COMPANY_INVITATIONS_URL;

    @Autowired
    private StaticPropertyUtil staticPropertyUtil;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private EmailService emailService;
    @Autowired
    private CompanyService companyService;
    @Autowired
    private UsersService usersService;
    @Autowired
    private AssociationsRepository associationsRepository;

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

    private final TestDataManager testDataManager = TestDataManager.getInstance();
    private final ComparisonUtils comparisonUtils = new ComparisonUtils();

    private final String ASSOCIATIONS_URL = "/associations/";
    private CountDownLatch latch;

    private void setEmailProducerCountDownLatch(int countdown){
        latch = new CountDownLatch(countdown);
        doAnswer(invocation -> {
            latch.countDown();
            return null;
        }).when(emailProducer).sendEmail(any(), any());
    }

    @Test
    void getAssociationDetailsWithoutPathVariableReturnsNotFound() throws Exception {
        final var userId = "9999";

        mockMvc.perform(get(ASSOCIATIONS_URL)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                .andExpect(status().isNotFound());
    }

    @Test
    void getAssociationUserDetailsWithNonexistentIdReturnsNotFound() throws Exception {
        final var userId = "9999";
        final var associationId = "1";

        mockMvc.perform(get(ASSOCIATIONS_URL + associationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                .andExpect(status().isNotFound());
    }


    @Test
    void getAssociationDetailsFetchesAssociationDetails() throws Exception {
        final var userId = "9999";
        final var companyNumber = "333333";
        final var associationId = "18";

        associationsRepository.insert(testDataManager.fetchAssociationDaos(associationId));
        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_URL + associationId)
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, userId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                                .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                        .andExpect(status().isOk());

        final var association = parseResponseTo(response, Association.class);

        Assertions.assertEquals(userId, association.getUserId());
    }

    @Test
    void getAssociationForIdCanFetchMigratedAssociation() throws Exception {
        final var userId = "MKUser001";
        final var companyNumber = "MKCOMP001";
        final var associationId = "MKAssociation001";

        associationsRepository.insert(testDataManager.fetchAssociationDaos(associationId));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_URL + associationId)
                                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                                .header(ERIC_IDENTITY.key, userId)
                                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                        .andExpect(status().isOk());

        final var association = parseResponseTo(response, Association.class);

        Assertions.assertEquals(associationId, association.getId());
        Assertions.assertEquals(StatusEnum.MIGRATED.getValue(), association.getStatus().getValue());
        Assertions.assertEquals(ApprovalRouteEnum.MIGRATION.getValue(), association.getApprovalRoute().getValue());
    }

    @Test
    void getAssociationForIdWithAPIKeyRequest() throws Exception {
        final var userId = "MKUser001";
        final var companyNumber = "MKCOMP001";
        final var associationId = "MKAssociation001";

        associationsRepository.insert(testDataManager.fetchAssociationDaos(associationId));
        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        final var response = mockMvc.perform(get(ASSOCIATIONS_URL + associationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value))
                .andExpect(status().isOk());

        final var association = parseResponseTo(response, Association.class);

        Assertions.assertEquals(associationId, association.getId());
    }

    @Test
    void getAssociationForIdCanBeCalledByAdmin() throws Exception {
        final var userId = "111";
        final var companyNumber = "MKCOMP001";
        final var associationId = "MKAssociation001";

        associationsRepository.insert(testDataManager.fetchAssociationDaos(associationId));
        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(get(ASSOCIATIONS_URL + associationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_ADMIN_READ_PERMISSION.key, ERIC_ADMIN_READ_PERMISSION.value))
                .andExpect(status().isOk());
    }

    @Test
    void getAssociationForIdCanRetrieveUnauthorisedAssociation() throws Exception {
        final var requestUserId = "111";
        final var requestedUserId = "MKUser004";
        final var companyNumber = "MKCOMP001";
        final var associationId = "MKAssociation004";

        associationsRepository.insert(testDataManager.fetchAssociationDaos(associationId));

        when(userClient.requestUserDetails(requestUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestUserId).getFirst());
        when(userClient.requestUserDetails(requestedUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestedUserId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        final var response =
                mockMvc.perform(get(ASSOCIATIONS_URL + associationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, "111")
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_ADMIN_READ_PERMISSION.key, ERIC_ADMIN_READ_PERMISSION.value))
                .andExpect(status().isOk());

        final var association = parseResponseTo(response, Association.class);
        Assertions.assertEquals(associationId, association.getId());
        Assertions.assertEquals(StatusEnum.UNAUTHORISED, association.getStatus());
        Assertions.assertNotNull(association.getUnauthorisedAt());
    }

    @Test
    void updateAssociationStatusForIdWithMalformedAssociationIdReturnsBadRequest() throws Exception {
        final var userId = "9999";
        final var malformedAssociationId = "$$$";
        final var requestBody = parseJsonFrom(StatusEnum.REMOVED);

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + malformedAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithoutRequestBodyReturnsBadRequest() throws Exception {
        final var userId = "9999";
        final var associationId = "18";

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + associationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithoutStatusReturnsBadRequest() throws Exception {
        final var userId = "9999";
        final var associationId = "18";
        final var badRequestBody = "{}";

        mockMvc.perform(patch(ASSOCIATIONS_URL + associationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badRequestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithMalformedStatusReturnsBadRequest() throws Exception {
        final var userId = "9999";
        final var associationId = "18";
        final var badRequestBody = "{\"status\":\"complicated\"}";

        mockMvc.perform(patch(ASSOCIATIONS_URL + associationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badRequestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithNonexistentAssociationIdReturnsNotFound() throws Exception {
        final var userId = "9999";

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + "9191")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.CONFIRMED.value))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAssociationStatusForIdWithRemovedUpdatesAssociationStatus() throws Exception {
        final var userId = "9999";
        final var companyId = "333333";
        final var oldAssociationId = "18";
        final var oldAssociationData = testDataManager.fetchAssociationDaos(oldAssociationId).getFirst();

        associationsRepository.insert(oldAssociationData);

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyId).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + oldAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.REMOVED.value))
                .andExpect(status().isOk());

        final var newAssociationData = associationsRepository.findById(oldAssociationId).get();
        Assertions.assertEquals(RequestBodyPut.StatusEnum.REMOVED.getValue(), newAssociationData.getStatus());
        Assertions.assertEquals(localDateTimeToNormalisedString(oldAssociationData.getApprovedAt()), localDateTimeToNormalisedString(newAssociationData.getApprovedAt()));
        Assertions.assertNotEquals(oldAssociationData.getRemovedAt(), newAssociationData.getRemovedAt());
        Assertions.assertNotEquals(oldAssociationData.getEtag(), newAssociationData.getEtag());
        Assertions.assertEquals(oldAssociationData.getUserEmail(), newAssociationData.getUserEmail());
        Assertions.assertEquals(oldAssociationData.getUserId(), newAssociationData.getUserId());
    }

    @Test
    void updateAssociationStatusForIdWithConfirmedUpdatesAssociationStatus() throws Exception {
        final var userId = "9999";
        final var companyId = "333333";
        final var oldAssociationId = "18";
        final var oldAssociationData = testDataManager.fetchAssociationDaos(oldAssociationId).getFirst();

        associationsRepository.insert(oldAssociationData);

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyId).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + oldAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.CONFIRMED.value))
                .andExpect(status().isOk());

        final var newAssociationData = associationsRepository.findById(oldAssociationId).get();
        Assertions.assertEquals(RequestBodyPut.StatusEnum.CONFIRMED.getValue(), newAssociationData.getStatus());
        Assertions.assertNotEquals(oldAssociationData.getApprovedAt(), newAssociationData.getApprovedAt());
        Assertions.assertEquals(localDateTimeToNormalisedString(oldAssociationData.getRemovedAt()), localDateTimeToNormalisedString(newAssociationData.getRemovedAt()));
        Assertions.assertNotEquals(oldAssociationData.getEtag(), newAssociationData.getEtag());
        Assertions.assertEquals(oldAssociationData.getUserEmail(), newAssociationData.getUserEmail());
        Assertions.assertEquals(oldAssociationData.getUserId(), newAssociationData.getUserId());
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndNonexistentUserAndConfirmedReturnsBadRequest() throws Exception {
        final var userId = "222";
        final var associationId = "17";

        associationsRepository.insert(testDataManager.fetchAssociationDaos(associationId));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + associationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.CONFIRMED.value))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndExistingUserAndRemovedUpdatesAssociationStatus() throws Exception {
        final var userId = "111";
        final var nullUser = "000";
        final var companyId = "111111";
        final var oldAssociationId = "34";
        final var associationId = "1";
        final UsersList returnedNullUser = new UsersList();
        returnedNullUser.add(testDataManager.fetchUserDtos(nullUser).getFirst());
        final var associationDaos = testDataManager.fetchAssociationDaos(associationId, oldAssociationId);
        final var oldAssociationData = associationDaos.getLast();

        associationsRepository.insert(associationDaos);

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(userClient.requestUserDetailsByEmail(oldAssociationData.getUserEmail(), X_REQUEST_ID.value)).thenReturn(returnedNullUser);
        when(companyClient.requestCompanyProfile(companyId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyId).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + oldAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.REMOVED.value))
                .andExpect(status().isOk());

        final var newAssociationData = associationsRepository.findById(oldAssociationId).get();
        Assertions.assertEquals(RequestBodyPut.StatusEnum.REMOVED.getValue(), newAssociationData.getStatus());
        Assertions.assertEquals(localDateTimeToNormalisedString(oldAssociationData.getApprovedAt()), localDateTimeToNormalisedString(newAssociationData.getApprovedAt()));
        Assertions.assertNotEquals(localDateTimeToNormalisedString(oldAssociationData.getRemovedAt()), localDateTimeToNormalisedString(newAssociationData.getRemovedAt()));
        Assertions.assertNotEquals(oldAssociationData.getEtag(), newAssociationData.getEtag());
        Assertions.assertNotEquals(oldAssociationData.getUserEmail(), newAssociationData.getUserEmail());
        Assertions.assertNotEquals(oldAssociationData.getUserId(), newAssociationData.getUserId());
    }

    @Test
    void updateAssociationStatusForIdWithNullUserIdAndNonexistentUserAndRemovedUpdatesAssociationStatus() throws Exception {
        final var userId = "111";
        final var companyId = "111111";
        final var oldAssociationId = "34";
        final var associationId = "1";
        final var associationDaos = testDataManager.fetchAssociationDaos(associationId, oldAssociationId);
        final var oldAssociationData = associationDaos.getLast();
        final var nonexistentUserList = new UsersList();

        associationsRepository.insert(associationDaos);

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(userClient.requestUserDetailsByEmail(oldAssociationData.getUserEmail(), X_REQUEST_ID.value)).thenReturn(nonexistentUserList);
        when(companyClient.requestCompanyProfile(companyId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyId).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + oldAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.REMOVED.value))
                .andExpect(status().isOk());

        final var newAssociationData = associationsRepository.findById(oldAssociationId).get();
        Assertions.assertEquals(RequestBodyPut.StatusEnum.REMOVED.getValue(), newAssociationData.getStatus());
        Assertions.assertEquals(localDateTimeToNormalisedString(oldAssociationData.getApprovedAt()), localDateTimeToNormalisedString(newAssociationData.getApprovedAt()));
        Assertions.assertNotEquals(localDateTimeToNormalisedString(oldAssociationData.getRemovedAt()), localDateTimeToNormalisedString(newAssociationData.getRemovedAt()));
        Assertions.assertNotEquals(oldAssociationData.getEtag(), newAssociationData.getEtag());
        Assertions.assertEquals(oldAssociationData.getUserEmail(), newAssociationData.getUserEmail());
        Assertions.assertEquals(oldAssociationData.getUserId(), newAssociationData.getUserId());
        Assertions.assertEquals(1, newAssociationData.getPreviousStates().size());
        Assertions.assertEquals(StatusEnum.AWAITING_APPROVAL.getValue(), newAssociationData.getPreviousStates().getFirst().getStatus());
        Assertions.assertEquals(userId, newAssociationData.getPreviousStates().getFirst().getChangedBy());
        Assertions.assertNotNull(newAssociationData.getPreviousStates().getFirst().getChangedAt());
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereTargetUserIdExistsSendsNotification() throws Exception {
        final var userId = "9999";
        final var companyNumber = "333333";
        final var firstAssociationId = "18";
        final var secondAssociationId = "35";
        final var nullUser = "000";
        final UsersList returnedNullUser = new UsersList();
        final var oldAssociationData = testDataManager.fetchAssociationDaos(secondAssociationId).getFirst();
        returnedNullUser.add(testDataManager.fetchUserDtos(nullUser).getFirst());

        associationsRepository.insert(testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(userClient.requestUserDetails(nullUser, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(nullUser).getFirst());
        when(userClient.requestUserDetailsByEmail(oldAssociationData.getUserEmail(), X_REQUEST_ID.value)).thenReturn(returnedNullUser);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + secondAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.REMOVED.value))
                .andExpect(status().isOk());
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereTargetUserIdDoesNotExistSendsNotification() throws Exception {
        final var userId = "111";
        final var companyNumber = "111111";
        final var firstAssociationId = "1";
        final var secondAssociationId = "34";
        final var targetUserId = "000";
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var targetUsersList = new UsersList();
        targetUsersList.add(targetUserDetails);

        associationsRepository.insert(testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(userClient.requestUserDetailsByEmail(targetUserDetails.getEmail(), X_REQUEST_ID.value)).thenReturn(targetUsersList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        setEmailProducerCountDownLatch(1);

        mockMvc.perform(patch(ASSOCIATIONS_URL + secondAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.REMOVED.value))
                .andExpect(status().isOk());

        latch.await(10, TimeUnit.SECONDS);

         Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher("null", "Batman", "null", "Wayne Enterprises", "light.yagami@death.note")), eq(INVITE_CANCELLED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereUserCannotBeFoundSendsNotification() throws Exception {
        final var userId = "111";
        final var companyNumber = "111111";
        final var associationId = "34";
        final var targetUserId = "000";
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var targetUsersList = new UsersList();
        targetUsersList.add(targetUserDetails);

        final var associationDaos = testDataManager.fetchAssociationDaos("1", "34");

        associationsRepository.insert(associationDaos);

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(userClient.requestUserDetailsByEmail(targetUserDetails.getEmail(), X_REQUEST_ID.value)).thenReturn(targetUsersList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        setEmailProducerCountDownLatch(1);

        mockMvc.perform(patch(ASSOCIATIONS_URL + associationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.REMOVED.value))
                .andExpect(status().isOk());

        latch.await(10, TimeUnit.SECONDS);
         Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher("null", "Batman", "null", "Wayne Enterprises",  "light.yagami@death.note")), eq(INVITE_CANCELLED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void updateAssociationStatusForIdNotificationsUsesDisplayNamesWhenAvailable() throws Exception {
        final var userId = "333";
        final var companyNumber = "111111";
        final var firstAssociationId = "3";
        final var secondAssociationId = "4";
        final var targetUserId = "444";
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var targetUsersList = new UsersList();
        targetUsersList.add(targetUserDetails);

        associationsRepository.insert(testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(userClient.requestUserDetailsByEmail(targetUserDetails.getEmail(), X_REQUEST_ID.value)).thenReturn(targetUsersList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + secondAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.REMOVED.value))
                .andExpect(status().isOk());
    }

    @Test
    void updateAssociationToConfirmedStatusForOtherUserShouldThrow400Error() throws Exception {
        final var userId = "333";
        final var companyNumber = "111111";
        final var firstAssociationId = "3";
        final var secondAssociationId = "4";
        final var targetUserId = "444";
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var targetUsersList = new UsersList();
        targetUsersList.add(targetUserDetails);

        associationsRepository.insert(testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(userClient.requestUserDetailsByEmail(targetUserDetails.getEmail(), X_REQUEST_ID.value)).thenReturn(targetUsersList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + secondAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.CONFIRMED.value))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdUserAcceptedInvitationNotificationsSendsNotification() throws Exception {
        final var firstUserId = "222";
        final var secondUserId = "444";
        final var thirdUserId = "666";
        final var companyNumber = "111111";
        final var firstAssociationId = "2";
        final var secondAssociationId = "4";
        final var thirdAssociationId = "6";

        associationsRepository.insert(testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId, thirdAssociationId));

        when(userClient.requestUserDetails(firstUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(firstUserId).getFirst());
        when(userClient.requestUserDetails(secondUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(secondUserId).getFirst());
        when(userClient.requestUserDetails(thirdUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(thirdUserId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        setEmailProducerCountDownLatch(3);

        mockMvc.perform(patch(ASSOCIATIONS_URL + thirdAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, thirdUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.CONFIRMED.value))
                .andExpect(status().isOk());

        latch.await(10, TimeUnit.SECONDS);

        final var expectedBaseEmail = new InvitationAcceptedEmailBuilder()
                .setInviterDisplayName("the.joker@gotham.city")
                .setInviteeDisplayName("homer.simpson@springfield.com")
                .setCompanyName("Wayne Enterprises");

         Mockito.verify(emailProducer, times(3)).sendEmail(argThat(comparisonUtils.invitationAcceptedEmailDataMatcher(
                 List.of("the.joker@gotham.city", "robin@gotham.city", "homer.simpson@springfield.com"), expectedBaseEmail)), eq(INVITATION_ACCEPTED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void updateAssociationStatusForIdThrowsBadRequestWhenUserTriesToConfirmOwnMigratedAssociation() throws Exception {
        final var userId = "MKUser001";
        final var companyNumber = "MKCOMP001";
        final var associationId = "MKAssociation001";

        associationsRepository.insert(testDataManager.fetchAssociationDaos(associationId));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + associationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.CONFIRMED.value))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdUpdatesAssociationWhenUserTriesToRemoveOwnMigratedAssociation() throws Exception {
        final var userId = "MKUser001";
        final var companyNumber = "MKCOMP001";
        final var associationId = "MKAssociation001";

        associationsRepository.insert(testDataManager.fetchAssociationDaos(associationId));

        when(userClient.requestUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + associationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, userId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.REMOVED.value))
                .andExpect(status().isOk());

        final var updatedAssociation = associationsRepository.findById("MKAssociation001").get();

        Assertions.assertEquals("removed", updatedAssociation.getStatus());
        Assertions.assertNotNull(updatedAssociation.getRemovedAt());
        Assertions.assertNotNull(updatedAssociation.getEtag());
        Assertions.assertEquals(1, updatedAssociation.getPreviousStates().size());
        Assertions.assertEquals("migrated", updatedAssociation.getPreviousStates().getFirst().getStatus());
        Assertions.assertEquals("MKUser001", updatedAssociation.getPreviousStates().getFirst().getChangedBy());
        Assertions.assertNotNull(updatedAssociation.getPreviousStates().getFirst().getChangedAt());
        Assertions.assertNull(updatedAssociation.getUserEmail());
        Assertions.assertEquals("MKUser001", updatedAssociation.getUserId());
    }

    @Test
    void updateAssociationStatusForIdUpdatesAssociationWhenUserTriesToConfirmedAnotherUsersMigratedAssociation() throws Exception {
        final var firstUserId = "MKUser001";
        final var secondUserId = "MKUser002";
        final var companyNumber = "MKCOMP001";
        final var firstAssociationId = "MKAssociation001";
        final var secondAssociationId = "MKAssociation002";
        final var targetUserDetails = testDataManager.fetchUserDtos(firstUserId).getFirst();
        final var targetUserList = new UsersList();
        targetUserList.add(targetUserDetails);

        associationsRepository.insert(testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId));

        when(userClient.requestUserDetails(firstUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(firstUserId).getFirst());
        when(userClient.requestUserDetails(secondUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(secondUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(testDataManager.fetchAssociationDaos(firstAssociationId).getFirst().getUserEmail(), X_REQUEST_ID.value)).thenReturn(targetUserList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + firstAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, secondUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.CONFIRMED.value))
                .andExpect(status().isOk());

        final var updatedAssociation = associationsRepository.findById(firstAssociationId).get();
        final var invitation = updatedAssociation.getInvitations().getFirst();

        Assertions.assertNotNull(updatedAssociation.getEtag());
        Assertions.assertEquals(1, updatedAssociation.getPreviousStates().size());
        Assertions.assertEquals("migrated", updatedAssociation.getPreviousStates().getFirst().getStatus());
        Assertions.assertEquals("MKUser002", updatedAssociation.getPreviousStates().getFirst().getChangedBy());
        Assertions.assertNotNull(updatedAssociation.getPreviousStates().getFirst().getChangedAt());
        Assertions.assertEquals("MKUser001", updatedAssociation.getUserId());
        Assertions.assertNull(updatedAssociation.getUserEmail());
        Assertions.assertEquals("MKUser002", invitation.getInvitedBy());
        Assertions.assertNotNull(invitation.getInvitedAt());
        Assertions.assertNotNull(invitation.getExpiredAt());
        Assertions.assertEquals("awaiting-approval", updatedAssociation.getStatus());
        Assertions.assertNotNull(updatedAssociation.getApprovalExpiryAt());
    }

    @Test
    void updateAssociationStatusForIdUpdatesAssociationWhenUserTriesToRemoveAnotherUsersMigratedAssociation() throws Exception {
        final var firstUserId = "MKUser001";
        final var secondUserId = "MKUser002";
        final var companyNumber = "MKCOMP001";
        final var firstAssociationId = "MKAssociation001";
        final var secondAssociationId = "MKAssociation002";
        final var targetUserDetails = testDataManager.fetchUserDtos(firstUserId).getFirst();
        final var targetUserList = new UsersList();
        targetUserList.add(targetUserDetails);

        associationsRepository.insert(testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId));

        when(userClient.requestUserDetails(firstUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(firstUserId).getFirst());
        when(userClient.requestUserDetails(secondUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(secondUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(testDataManager.fetchAssociationDaos(firstAssociationId).getFirst().getUserEmail(), X_REQUEST_ID.value)).thenReturn(targetUserList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + firstAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, secondUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.REMOVED.value))
                .andExpect(status().isOk());

        final var updatedAssociation = associationsRepository.findById("MKAssociation001").get();

        Assertions.assertEquals("removed", updatedAssociation.getStatus());
        Assertions.assertNotNull(updatedAssociation.getRemovedAt());
        Assertions.assertNotNull(updatedAssociation.getEtag());
        Assertions.assertEquals(1, updatedAssociation.getPreviousStates().size());
        Assertions.assertEquals("migrated", updatedAssociation.getPreviousStates().getFirst().getStatus());
        Assertions.assertEquals("MKUser002", updatedAssociation.getPreviousStates().getFirst().getChangedBy());
        Assertions.assertNotNull(updatedAssociation.getPreviousStates().getFirst().getChangedAt());
        Assertions.assertNull(updatedAssociation.getUserEmail());
        Assertions.assertEquals("MKUser001", updatedAssociation.getUserId());
    }

    @Test
    void updateAssociationStatusForIdSendInvitationWhenUserTriesToConfirmNonexistentUsersMigratedAssociation() throws Exception {
        final var firstUserId = "MKUser001";
        final var secondUserId = "MKUser002";
        final var companyNumber = "MKCOMP001";
        final var firstAssociationId = "MKAssociation001";
        final var secondAssociationId = "MKAssociation002";
        final var targetUserDetails = testDataManager.fetchUserDtos(firstUserId).getFirst();
        final var targetUserList = new UsersList();
        targetUserList.add(targetUserDetails);

        associationsRepository.insert(testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId));

        when(userClient.requestUserDetails(firstUserId, X_REQUEST_ID.value)).thenReturn(null);
        when(userClient.requestUserDetails(secondUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(secondUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(testDataManager.fetchAssociationDaos(firstAssociationId).getFirst().getUserEmail(), X_REQUEST_ID.value)).thenReturn(null);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + firstAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, secondUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.CONFIRMED.value))
                .andExpect(status().isOk());

        final var updatedAssociation = associationsRepository.findById("MKAssociation001").get();

        Assertions.assertEquals("awaiting-approval", updatedAssociation.getStatus());
        Assertions.assertNotNull(updatedAssociation.getApprovalExpiryAt());
        Assertions.assertEquals(1, updatedAssociation.getInvitations().size());
        Assertions.assertEquals("MKUser002", updatedAssociation.getInvitations().getFirst().getInvitedBy());
        Assertions.assertNotNull(updatedAssociation.getInvitations().getFirst().getInvitedAt());
        Assertions.assertNotNull(updatedAssociation.getEtag());
        Assertions.assertEquals(1, updatedAssociation.getPreviousStates().size());
        Assertions.assertEquals("migrated", updatedAssociation.getPreviousStates().getFirst().getStatus());
        Assertions.assertEquals("MKUser002", updatedAssociation.getPreviousStates().getFirst().getChangedBy());
        Assertions.assertNotNull(updatedAssociation.getPreviousStates().getFirst().getChangedAt());
        Assertions.assertEquals("mario@mushroom.kingdom", updatedAssociation.getUserEmail());
        Assertions.assertNull(updatedAssociation.getUserId());
    }

    @Test
    void updateAssociationStatusForIdUpdatesAssociationWhenUserTriesToRemoveNonexistentUsersMigratedAssociation() throws Exception {
        final var firstUserId = "MKUser001";
        final var secondUserId = "MKUser002";
        final var companyNumber = "MKCOMP001";
        final var firstAssociationId = "MKAssociation001";
        final var secondAssociationId = "MKAssociation002";
        final var targetUserDetails = testDataManager.fetchUserDtos(firstUserId).getFirst();
        final var targetUserList = new UsersList();
        targetUserList.add(targetUserDetails);

        associationsRepository.insert(testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId));

        when(userClient.requestUserDetails(firstUserId, X_REQUEST_ID.value)).thenReturn(null);
        when(userClient.requestUserDetails(secondUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(secondUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(testDataManager.fetchAssociationDaos(firstAssociationId).getFirst().getUserEmail(), X_REQUEST_ID.value)).thenReturn(null);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + "MKAssociation001")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, secondUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.REMOVED.value))
                .andExpect(status().isOk());

        final var updatedAssociation = associationsRepository.findById("MKAssociation001").get();

        Assertions.assertEquals("removed", updatedAssociation.getStatus());
        Assertions.assertNotNull(updatedAssociation.getRemovedAt());
        Assertions.assertNotNull(updatedAssociation.getEtag());
        Assertions.assertEquals(1, updatedAssociation.getPreviousStates().size());
        Assertions.assertEquals("migrated", updatedAssociation.getPreviousStates().getFirst().getStatus());
        Assertions.assertEquals("MKUser002", updatedAssociation.getPreviousStates().getFirst().getChangedBy());
        Assertions.assertNotNull(updatedAssociation.getPreviousStates().getFirst().getChangedAt());
        Assertions.assertEquals("mario@mushroom.kingdom", updatedAssociation.getUserEmail());
        Assertions.assertNull(updatedAssociation.getUserId());
    }

    @Test
    void updateAssociationStatusForIdAllowsAdminUserToRemoveAuthorisation() throws Exception {
        final var firstUserId = "9999";
        final var secondUserId = "111";
        final var thirdUserId = "222";
        final var companyNumber = "111111";
        final var firstAssociationId = "1";
        final var secondAssociationId = "2";
        final var targetUserDetails = testDataManager.fetchUserDtos(firstUserId).getFirst();
        final var targetUserList = new UsersList();
        targetUserList.add(targetUserDetails);

        associationsRepository.insert(testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId));

        when(userClient.requestUserDetails(firstUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(firstUserId).getFirst());
        when(userClient.requestUserDetails(secondUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(secondUserId).getFirst());
        when(userClient.requestUserDetails(thirdUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(thirdUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(testDataManager.fetchAssociationDaos(firstAssociationId).getFirst().getUserEmail(), X_REQUEST_ID.value)).thenReturn(targetUserList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        setEmailProducerCountDownLatch(2);

        mockMvc.perform(patch(ASSOCIATIONS_URL + firstAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, firstUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_ADMIN_UPDATE_PERMISSION.key, ERIC_ADMIN_UPDATE_PERMISSION.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.REMOVED.value))
                .andExpect(status().isOk());

        latch.await(10, TimeUnit.SECONDS);

        final var updatedAssociation = associationsRepository.findById("1").get();

        Assertions.assertEquals("removed", updatedAssociation.getStatus());

         Mockito.verify(emailProducer, times(2)).sendEmail(argThat(comparisonUtils.authorisationRemovedAndYourAuthorisationRemovedEmailMatcher(COMPANIES_HOUSE, "Batman", "Wayne Enterprises", "the.joker@gotham.city", "bruce.wayne@gotham.city")), argThat(messageType -> List.of(AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue(), YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue()).contains(messageType)));
    }

    @Test
    void updateAssociationStatusForIdAllowsAdminUserToCancelInvitation() throws Exception {
        final var firstUserId = "9999";
        final var secondUserId = "666";
        final var thirdUserId = "222";
        final var companyNumber = "111111";
        final var firstAssociationId = "6";
        final var secondAssociationId = "2";
        final var targetUserDetails = testDataManager.fetchUserDtos(secondUserId).getFirst();
        final var targetUserList = new UsersList();
        targetUserList.add(targetUserDetails);

        associationsRepository.insert(testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId));

        when(userClient.requestUserDetails(firstUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(firstUserId).getFirst());
        when(userClient.requestUserDetails(secondUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(secondUserId).getFirst());
        when(userClient.requestUserDetails(thirdUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(thirdUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(testDataManager.fetchAssociationDaos(firstAssociationId).getFirst().getUserEmail(), X_REQUEST_ID.value)).thenReturn(targetUserList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        setEmailProducerCountDownLatch(2);

        mockMvc.perform(patch(ASSOCIATIONS_URL + firstAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, firstUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_ADMIN_UPDATE_PERMISSION.key, ERIC_ADMIN_UPDATE_PERMISSION.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.REMOVED.value))
                .andExpect(status().isOk());

        latch.await(10, TimeUnit.SECONDS);

        final var updatedAssociation = associationsRepository.findById("6").get();

        Assertions.assertEquals("removed", updatedAssociation.getStatus());

        Mockito.verify(emailProducer, times(2)).sendEmail(argThat(comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher("the.joker@gotham.city", "Companies House", "homer.simpson@springfield.com", "Wayne Enterprises", "homer.simpson@springfield.com")), argThat(messageType -> List.of(INVITATION_CANCELLED_MESSAGE_TYPE.getValue(), INVITE_CANCELLED_MESSAGE_TYPE.getValue()).contains(messageType)));
    }

    @Test
    void updateAssociationStatusForIdWithAPIKeyCanSetStatusToUnauthorised() throws Exception {
        final var requestingUserId = "9999";
        final var targetUserId = "MKUser002";
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var targetUserList = new UsersList();
        targetUserList.add(targetUserDetails);
        final var companyNumber = "MKCOMP001";
        final var firstAssociationId = "MKAssociation002";

        associationsRepository.insert(testDataManager.fetchAssociationDaos(firstAssociationId));

        when(userClient.requestUserDetailsByEmail(testDataManager.fetchAssociationDaos(firstAssociationId).getFirst().getUserEmail(), X_REQUEST_ID.value)).thenReturn(targetUserList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + firstAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.UNAUTHORISED.value))
                .andExpect(status().isOk());

        final var updatedAssociation = associationsRepository.findById("MKAssociation002").get();

        Assertions.assertEquals(UNAUTHORISED.getValue(), updatedAssociation.getStatus());
        Assertions.assertNotNull(updatedAssociation.getUnauthorisedAt());
        Assertions.assertEquals(COMPANIES_HOUSE, updatedAssociation.getUnauthorisedBy());
    }

    private static Stream<Arguments> updateAssociationStatusForIdWithAPIKeyBadRequestScenarios(){
        return Stream.of(
                Arguments.of("confirmed"),
                Arguments.of("removed")
      );
    }

    @ParameterizedTest
    @MethodSource("updateAssociationStatusForIdWithAPIKeyBadRequestScenarios")
    void updateAssociationStatusForIdWithAPIKeyReturnsBadRequestWhenBodyContainsConfirmedOrRemoved(final String status) throws Exception {
        final var requestingUserId = "9999";
        final var firstAssociationId = "MKAssociation002";
        final var targetAssociation = testDataManager.fetchAssociationDaos(firstAssociationId).getFirst();

        associationsRepository.insert(targetAssociation);

        mockMvc.perform(patch(ASSOCIATIONS_URL + firstAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"status\":\"%s\"}", status)))
                .andExpect(status().isBadRequest());
    }

    private static Stream<Arguments> updateAssociationStatusForIdAPIRequestsThatChangeUnauthorisedOrMigratedAssociationsToConfirmedScenarios(){
        return Stream.of(
                Arguments.of("MKAssociation004", "MKUser004"),
                Arguments.of("MKAssociation001", "MKUser001")
      );
    }

    @ParameterizedTest
    @MethodSource("updateAssociationStatusForIdAPIRequestsThatChangeUnauthorisedOrMigratedAssociationsToConfirmedScenarios")
    void updateAssociationStatusForIdSupportsAPIRequestsThatChangeUnauthorisedOrMigratedAssociationsToConfirmed(final String targetAssociationId, final String targetUserId) throws Exception {
        final var requestingUserId = "9999";
        final var associations = testDataManager.fetchAssociationDaos(targetAssociationId).getFirst();
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var targetUserList = new UsersList();
        targetUserList.add(targetUserDetails);

        associationsRepository.insert(associations);

        when(userClient.requestUserDetails(targetUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(targetUserId).getFirst());
        when(usersService.fetchUserDetailsByEmail(associations.getUserEmail(), X_REQUEST_ID.value)).thenReturn(targetUserList);
        when(companyService.fetchCompanyProfile(associations.getCompanyNumber(), X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(associations.getCompanyNumber()).getFirst());

        setEmailProducerCountDownLatch(1);

        mockMvc.perform(patch(ASSOCIATIONS_URL + targetAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_API_KEY.key, ERIC_IDENTITY_TYPE_API_KEY.value)
                        .header(ERIC_AUTHORISED_KEY_ROLES.key, ERIC_AUTHORISED_KEY_ROLES.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.CONFIRMED.value))
                .andExpect(status().isOk());

        latch.await(10, TimeUnit.SECONDS);

        Assertions.assertEquals(CONFIRMED.getValue(), associationsRepository.findById(targetAssociationId).get().getStatus());

         Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.authCodeConfirmationEmailMatcher(targetUserDetails.getEmail(), "Mushroom Kingdom", targetUserDetails.getDisplayName())), argThat(messageType -> List.of(AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue(), AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue()).contains(messageType)));
    }

    private static Stream<Arguments> updateAssociationStatusForIdSameUserUnauthorisedBadRequestScenarios(){
        return Stream.of(
                Arguments.of("confirmed"),
                Arguments.of("unauthorised")
      );
    }

    @ParameterizedTest
    @MethodSource("updateAssociationStatusForIdSameUserUnauthorisedBadRequestScenarios")
    void updateAssociationStatusForIdWhereUserTriesToConfirmOwnUnauthorisedAssociationOrSetStatusToUnauthorisedReturnsBadRequest(final String status) throws Exception {
        final var requestingUserId = "MKUser004";
        final var associationId = "MKAssociation004";
        final var targetAssociation = testDataManager.fetchAssociationDaos("MKAssociation004").getFirst();

        associationsRepository.insert(targetAssociation);

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());


        mockMvc.perform(patch(ASSOCIATIONS_URL + associationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(String.format("{\"status\":\"%s\"}", status)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdWhereDifferentUserAttemptsToConfirmAnotherUsersUnauthorisedAssociationSendsInvitation() throws Exception {
        final var firstAssociationId = "MKAssociation004";
        final var secondAssociationId = "MKAssociation002";
        final var associations = testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId);
        final var companyNumber = "MKCOMP001";

        final var requestingUserId = "MKUser002";
        final var targetUserId = "MKUser004";

        associationsRepository.insert(associations);

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetails(targetUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(targetUserId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());;

        mockMvc.perform(patch(ASSOCIATIONS_URL + firstAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.CONFIRMED.value))
                .andExpect(status().isOk());

        Assertions.assertEquals(AWAITING_APPROVAL.getValue(), associationsRepository.findById("MKAssociation004").get().getStatus());
    }

    @Test
    void updateAssociationStatusForIdWhereDifferentUserAttemptsToSetAnotherUsersAssociationToUnauthorisedReturnsBadRequest() throws Exception {
        final var firstAssociationId = "MKAssociation004";
        final var secondAssociationId = "MKAssociation002";
        final var associations = testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId);

        final var requestingUserId = "MKUser002";

        associationsRepository.insert(associations);

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());

        mockMvc.perform(patch(ASSOCIATIONS_URL + firstAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.UNAUTHORISED.value))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForIdSendsEmailWhenOneUserRemovesAnotherUsersMigratedAssociation() throws Exception {
        final var firstAssociationId = "MKAssociation001";
        final var secondAssociationId = "MKAssociation002";
        final var associations = testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId);
        final var companyNumber = "MKCOMP001";

        final var requestingUserId = "MKUser002";
        final var targetUserId = "MKUser001";
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var targetUserList = new UsersList();
        targetUserList.add(targetUserDetails);

        associationsRepository.insert(associations);

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(testDataManager.fetchAssociationDaos(firstAssociationId).getFirst().getUserEmail(), X_REQUEST_ID.value)).thenReturn(targetUserList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        setEmailProducerCountDownLatch(2);

        mockMvc.perform(patch(ASSOCIATIONS_URL + firstAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.REMOVED.value))
                .andExpect(status().isOk());

        latch.await(10, TimeUnit.SECONDS);

         Mockito.verify(emailProducer, times(2)).sendEmail(argThat(comparisonUtils.delegatedRemovalOfMigratedAndBatchEmailMatcher("mario@mushroom.kingdom", "Mario", "luigi@mushroom.kingdom", "Luigi", "Mushroom Kingdom")), argThat(messageType -> List.of(DELEGATED_REMOVAL_OF_MIGRATED.getValue(), DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue()).contains(messageType)));
    }

    @Test
    void updateAssociationStatusForIdSendsEmailWhenOneUserRemovesTheirOwnMigratedAssociation() throws Exception {
        final var firstAssociationId = "MKAssociation001";
        final var secondAssociationId = "MKAssociation002";
        final var associations = testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId);

        final var requestingUserId = "MKUser001";
        final var targetUserId = "MKUser002";
        final var companyNumber = "MKCOMP001";

        associationsRepository.insert(associations);

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetails(targetUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(targetUserId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        setEmailProducerCountDownLatch(2);

        mockMvc.perform(patch(ASSOCIATIONS_URL + firstAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.REMOVED.value))
                .andExpect(status().isOk());

        latch.await(10, TimeUnit.SECONDS);

        Mockito.verify(emailProducer, times(2)).sendEmail(argThat(comparisonUtils.removalOfOwnMigratedEmailAndBatchMatcher("mario@mushroom.kingdom", "Mario", "luigi@mushroom.kingdom", "Mario", "Mushroom Kingdom")), argThat(messageType -> List.of(REMOVAL_OF_OWN_MIGRATED.getValue(), DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue()).contains(messageType)));
    }

    @Test
    void updateAssociationStatusForIdSendsEmailWhenOneUserConfirmsAnotherUsersMigratedAssociation() throws Exception {
        final var firstAssociationId = "MKAssociation001";
        final var secondAssociationId = "MKAssociation002";
        final var associations = testDataManager.fetchAssociationDaos(firstAssociationId, secondAssociationId);

        final var targetUserId = "MKUser001";
        final var requestingUserId = "MKUser002";
        final var targetUserDetails = testDataManager.fetchUserDtos(targetUserId).getFirst();
        final var targetUserList = new UsersList();
        targetUserList.add(targetUserDetails);

        final var companyNumber = "MKCOMP001";

        associationsRepository.insert(associations);

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetailsByEmail(testDataManager.fetchAssociationDaos(firstAssociationId).getFirst().getUserEmail(), X_REQUEST_ID.value)).thenReturn(targetUserList);
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        setEmailProducerCountDownLatch(2);

        mockMvc.perform(patch(ASSOCIATIONS_URL + firstAssociationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.CONFIRMED.value))
                .andExpect(status().isOk());

        latch.await(10, TimeUnit.SECONDS);

         Mockito.verify(emailProducer, times(2)).sendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("luigi@mushroom.kingdom", "Luigi", "mario@mushroom.kingdom", "Mario", "Mushroom Kingdom", COMPANY_INVITATIONS_URL)), argThat(messageType -> List.of(INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue()).contains(messageType)));
    }

    @Test
    void updateAssociationStatusForIdSendsEmailWhenAPIKeyConfirmsAnUnauthorisedAssociation() throws Exception {
        final var associationId = "MKAssociation004";
        final var associations = testDataManager.fetchAssociationDaos("MKAssociation004", "MKAssociation002");

        associationsRepository.insert(associations);

        final var requestingUserId = "MKUser002";
        final var targetUserId = "MKUser004";

        final var companyNumber = "MKCOMP001";

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        when(userClient.requestUserDetails(targetUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(targetUserId).getFirst());
        when(companyClient.requestCompanyProfile(companyNumber, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchCompanyDetailsDtos(companyNumber).getFirst());

        setEmailProducerCountDownLatch(2);

        mockMvc.perform(patch(ASSOCIATIONS_URL + associationId)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(REQUEST_BODY_ASSOCIATION_STATUS.CONFIRMED.value))
                .andExpect(status().isOk());

        latch.await(10, TimeUnit.SECONDS);

         Mockito.verify(emailProducer, times(2)).sendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("luigi@mushroom.kingdom", "Luigi", "bowser@mushroom.kingdom", "Bowser", "Mushroom Kingdom", COMPANY_INVITATIONS_URL)), argThat(messageType -> List.of(INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue()).contains(messageType)));
    }

    @Test
    void getPreviousStatesForAssociationRetrievesData() throws Exception {
        final var now = LocalDateTime.now();

        final var requestingUserId = "MKUser002";

        final var associationId = "MKAssociation003";

        associationsRepository.insert(testDataManager.fetchAssociationDaos(associationId));

        final var response = mockMvc.perform(get(ASSOCIATIONS_URL + associationId + "/previous-states?page_index=1&items_per_page=1")
                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                .header(ERIC_IDENTITY.key, requestingUserId)
                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isOk());

        final var previousStatesList = parseResponseTo(response, PreviousStatesList.class);
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();

        Assertions.assertEquals(1, previousStatesList.getItemsPerPage());
        Assertions.assertEquals(1, previousStatesList.getPageNumber());
        Assertions.assertEquals(4, previousStatesList.getTotalResults());
        Assertions.assertEquals(4, previousStatesList.getTotalPages());
        Assertions.assertEquals(ASSOCIATIONS_URL + "MKAssociation003/previous-states?page_index=1&items_per_page=1", links.getSelf());
        Assertions.assertEquals(ASSOCIATIONS_URL + "MKAssociation003/previous-states?page_index=2&items_per_page=1", links.getNext());
        Assertions.assertEquals(1, items.size());
        Assertions.assertEquals(AWAITING_APPROVAL, items.getFirst().getStatus());
        Assertions.assertEquals("MKUser003", items.getFirst().getChangedBy());
        Assertions.assertEquals(localDateTimeToNormalisedString(now.minusDays(7L)), reduceTimestampResolution(items.getFirst().getChangedAt()));
    }

    @Test
    void getPreviousStatesForAssociationSupportsUnauthorisedAssociation() throws Exception {
        final var now = LocalDateTime.now();

        final var requestingUserId = "MKUser002";

        final var associationId = "MKAssociation005";

        associationsRepository.insert(testDataManager.fetchAssociationDaos(associationId));

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());

        final var response = mockMvc.perform(get(ASSOCIATIONS_URL + associationId + "/previous-states?page_index=0&items_per_page=2")
                .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                .header(ERIC_IDENTITY.key, requestingUserId)
                .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isOk());

        final var previousStatesList = parseResponseTo(response, PreviousStatesList.class);
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();

        Assertions.assertEquals(2, previousStatesList.getItemsPerPage());
        Assertions.assertEquals(0, previousStatesList.getPageNumber());
        Assertions.assertEquals(5, previousStatesList.getTotalResults());
        Assertions.assertEquals(3, previousStatesList.getTotalPages());
        Assertions.assertEquals(ASSOCIATIONS_URL + "MKAssociation005/previous-states?page_index=0&items_per_page=2", links.getSelf());
        Assertions.assertEquals(ASSOCIATIONS_URL + "MKAssociation005/previous-states?page_index=1&items_per_page=2", links.getNext());
        Assertions.assertEquals(2, items.size());

        Assertions.assertEquals(UNAUTHORISED, items.getFirst().getStatus());
        Assertions.assertEquals("MKUser005", items.getFirst().getChangedBy());
        Assertions.assertEquals(localDateTimeToNormalisedString(now.minusDays(3L)), reduceTimestampResolution(items.getFirst().getChangedAt()));

        Assertions.assertEquals(CONFIRMED, items.getLast().getStatus());
        Assertions.assertEquals("Companies House", items.getLast().getChangedBy());
        Assertions.assertEquals(localDateTimeToNormalisedString(now.minusDays(6L)), reduceTimestampResolution(items.getLast().getChangedAt()));
    }

    @Test
    void getPreviousStatesForAssociationUsesDefaults() throws Exception {
        final var requestingUserId = "MKUser002";

        final var associationId = "MKAssociation001";

        associationsRepository.insert(testDataManager.fetchAssociationDaos(associationId));

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        // mockers.mockUsersServiceFetchUserDetails("MKUser002");

        final var response = mockMvc.perform(get(ASSOCIATIONS_URL + "MKAssociation001/previous-states")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isOk());

        final var previousStatesList = parseResponseTo(response, PreviousStatesList.class);
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();

        Assertions.assertEquals(15, previousStatesList.getItemsPerPage());
        Assertions.assertEquals(0, previousStatesList.getPageNumber());
        Assertions.assertEquals(0, previousStatesList.getTotalResults());
        Assertions.assertEquals(0, previousStatesList.getTotalPages());
        Assertions.assertEquals(ASSOCIATIONS_URL + "MKAssociation001/previous-states?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertTrue(items.isEmpty());
    }

    private static Stream<Arguments> getPreviousStatesForAssociationMalformedScenarios(){
        return Stream.of(
                Arguments.of("/associations/$$$/previous-states"),
                Arguments.of("/associations/MKAssociation003/previous-states?page_index=-1"),
                Arguments.of("/associations/MKAssociation003/previous-states?items_per_page=-1")
      );
    }

    @ParameterizedTest
    @MethodSource("getPreviousStatesForAssociationMalformedScenarios")
    void getPreviousStatesForAssociationWithMalformedAssociationIdReturnsBadRequest(final String uri) throws Exception {
        final var requestingUserId = "MKUser002";
        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        mockMvc.perform(get(uri)
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPreviousStatesForAssociationWithNonexistentAssociationReturnsNotFound() throws Exception {
        final var requestingUserId = "MKUser002";
        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        mockMvc.perform(get(ASSOCIATIONS_URL + "404MKAssociation/previous-states")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value))
                .andExpect(status().isNotFound());
    }

    @Test
    void getPreviousStatesForAssociationCanBeCalledByAdmin() throws Exception {
        final var requestingUserId = "111";
        final var associationId = "MKAssociation001";

        when(userClient.requestUserDetails(requestingUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(requestingUserId).getFirst());
        associationsRepository.insert(testDataManager.fetchAssociationDaos(associationId));

        mockMvc.perform(get(ASSOCIATIONS_URL + "MKAssociation001/previous-states")
                        .header(X_REQUEST_ID.key, X_REQUEST_ID.value)
                        .header(ERIC_IDENTITY.key, requestingUserId)
                        .header(ERIC_IDENTITY_TYPE_OAUTH.key, ERIC_IDENTITY_TYPE_OAUTH.value)
                        .header(ERIC_ADMIN_READ_PERMISSION.key, ERIC_ADMIN_READ_PERMISSION.value))
                .andExpect(status().isOk());
    }

    @AfterEach
    public void after() {
        associationsRepository.deleteAll();
    }
}
