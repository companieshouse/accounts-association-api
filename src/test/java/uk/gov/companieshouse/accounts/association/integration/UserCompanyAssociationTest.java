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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.accounts.association.client.EmailClient;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.factory.SendEmailFactory;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationAcceptedEmailBuilder;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.PreviousStatesList;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ASSOCIATIONS;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_AUTHORISED_KEY_ROLES;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_IDENTITY;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.ERIC_ID_VALUE;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.KEY_ROLES_VALUE;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.MK_USER_001;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.OAUTH_2;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.X_REQUEST_ID;
import static uk.gov.companieshouse.accounts.association.utils.TestConstant.X_REQUEST_ID_VALUE;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.UNAUTHORISED;

@AutoConfigureMockMvc
@SpringBootTest
@ExtendWith( MockitoExtension.class )
@Tag( "integration-test" )
class UserCompanyAssociationTest extends BaseMongoIntegration {
    @Value("${invitation.url}")
    private String COMPANY_INVITATIONS_URL;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private MockMvc mockMvc;
    @Mock
    private CompanyService companyService;
    @Mock
    private UsersService usersService;
    @Mock
    private EmailClient emailClient;
    @Mock
    private SendEmailFactory emailFactory;
    @Autowired
    private AssociationsRepository associationsRepository;
    private final TestDataManager testDataManager = TestDataManager.getInstance();
    private Mockers mockers;
    private final ComparisonUtils comparisonUtils = new ComparisonUtils();

    @BeforeEach
    public void setup() throws IOException, URIValidationException {
        mockers = new Mockers(null, emailClient, companyService, usersService);
    }

    @Test
    void getAssociationDetailsWithoutPathVariableReturnsNotFound() throws Exception {
        mockers.mockUsersServiceFetchUserDetails(ERIC_ID_VALUE);
        mockMvc.perform(get(ASSOCIATIONS + "/")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_ID_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect( status().isNotFound() );
    }

    @Test
    void getAssociationUserDetailsWithNonexistentIdReturnsNotFound() throws Exception {
        mockers.mockUsersServiceFetchUserDetails(ERIC_ID_VALUE);
        mockMvc.perform(get(ASSOCIATIONS + "/1")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_ID_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect( status().isNotFound() );
    }
    @Test
    void getAssociationDetailsFetchesAssociationDetails() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18" ) );
        mockers.mockUsersServiceFetchUserDetails(ERIC_ID_VALUE);
        mockers.mockCompanyServiceFetchCompanyProfile("333333");
        final var response =
                mockMvc.perform(get(ASSOCIATIONS + "/18")
                                .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                                .header(ERIC_IDENTITY, ERIC_ID_VALUE)
                                .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                                .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                        .andExpect(status().isOk());
        final var association = parseResponseTo(response, Association.class);
        Assertions.assertEquals(ERIC_ID_VALUE, association.getUserId());
    }

    @Test
    void getAssociationForIdCanFetchMigratedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockUsersServiceFetchUserDetails(MK_USER_001);
        mockers.mockCompanyServiceFetchCompanyProfile("MKCOMP001");
        final var response =
                mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation001")
                                .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                                .header(ERIC_IDENTITY, MK_USER_001)
                                .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                        .andExpect(status().isOk());
        final var association = parseResponseTo(response, Association.class);
        Assertions.assertEquals("MKAssociation001", association.getId());
        Assertions.assertEquals( "migrated", association.getStatus().getValue() );
        Assertions.assertEquals( "migration", association.getApprovalRoute().getValue() );
    }

    @Test
    void getAssociationForIdWithAPIKeyRequest() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockUsersServiceFetchUserDetails(MK_USER_001);
        mockers.mockCompanyServiceFetchCompanyProfile("MKCOMP001");
        final var response = mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE))
                .andExpect(status().isOk());
        final var association = parseResponseTo(response, Association.class);
        Assertions.assertEquals("MKAssociation001", association.getId());
    }

    @Test
    void getAssociationForIdCanBeCalledByAdmin() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockUsersServiceFetchUserDetails(MK_USER_001, "111");
        mockers.mockCompanyServiceFetchCompanyProfile("MKCOMP001");
        mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header( "Eric-Authorised-Roles", ADMIN_READ_PERMISSION ) )
                .andExpect( status().isOk() );
    }

    @Test
    void getAssociationForIdCanRetrieveUnauthorisedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation004" ) );
        mockers.mockUsersServiceFetchUserDetails( "MKUser004", "111" );
        mockers.mockCompanyServiceFetchCompanyProfile("MKCOMP001");
        final var response =
                mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation004")
                                .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                                .header(ERIC_IDENTITY, "111")
                                .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header( "Eric-Authorised-Roles", ADMIN_READ_PERMISSION ) )
                        .andExpect(status().isOk());
        final var association = parseResponseTo(response, Association.class);
        Assertions.assertEquals( "MKAssociation004", association.getId() );
        Assertions.assertEquals( StatusEnum.UNAUTHORISED, association.getStatus() );
        Assertions.assertNotNull( association.getUnauthorisedAt() );
    }

    @Test
    void updateAssociationStatusForIdWithMalformedAssociationIdReturnsBadRequest() throws Exception {
        mockers.mockUsersServiceFetchUserDetails(ERIC_ID_VALUE);
        mockMvc.perform(patch(ASSOCIATIONS + "/$$$")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_ID_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithoutRequestBodyReturnsBadRequest() throws Exception {
        mockers.mockUsersServiceFetchUserDetails(ERIC_ID_VALUE);
        mockMvc.perform(patch(ASSOCIATIONS + "/18")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_ID_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithoutStatusReturnsBadRequest() throws Exception {
        mockers.mockUsersServiceFetchUserDetails(ERIC_ID_VALUE);
        mockMvc.perform(patch(ASSOCIATIONS + "/18")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, ERIC_ID_VALUE)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithMalformedStatusReturnsBadRequest() throws Exception {
        mockers.mockUsersServiceFetchUserDetails(ERIC_ID_VALUE);
        mockMvc.perform(patch(ASSOCIATIONS + "/18")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"complicated\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWithNonexistentAssociationIdReturnsNotFound() throws Exception {
        mockers.mockUsersServiceFetchUserDetails("9999");
        mockMvc.perform(patch(ASSOCIATIONS + "/9191")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isNotFound() );
    }

    @Test
    void updateAssociationStatusForIdWithRemovedUpdatesAssociationStatus() throws Exception {
        final var oldAssociationData = testDataManager.fetchAssociationDaos("18").getFirst();
        associationsRepository.insert(oldAssociationData);
        mockers.mockUsersServiceFetchUserDetails(  "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );
        Mockito.doReturn(testDataManager.fetchUserDtos("9999").getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/18")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect(status().isOk());
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
        final var oldAssociationData = testDataManager.fetchAssociationDaos("18").getFirst();
        associationsRepository.insert(oldAssociationData);
        mockers.mockUsersServiceFetchUserDetails(  "9999" );
        mockers.mockCompanyServiceFetchCompanyProfile( "333333" );
        Mockito.doReturn(testDataManager.fetchUserDtos("9999").getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/18")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect(status().isOk());
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
        mockMvc.perform(patch(ASSOCIATIONS + "/17")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "222")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
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
        Mockito.doReturn(testDataManager.fetchUserDtos("000").getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/34")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect(status().isOk());
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
        Mockito.doReturn(null).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/34")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect(status().isOk());
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
        Mockito.doReturn(testDataManager.fetchUserDtos("000").getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/35")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereTargetUserIdDoesNotExistSendsNotification() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "34" ) );
        mockers.mockUsersServiceFetchUserDetails( "111" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(testDataManager.fetchUserDtos("000").getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/34")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect(status().isOk());
        Mockito.verify(emailFactory).createSendEmail(argThat(comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher("null", "Batman", "null", "Wayne Enterprises", "light.yagami@death.note")), eq(INVITE_CANCELLED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void updateAssociationStatusForIdNotificationsWhereUserCannotBeFoundSendsNotification() throws Exception {
        final var associationDaos = testDataManager.fetchAssociationDaos("1", "34");
        associationsRepository.insert(associationDaos);
        mockers.mockUsersServiceFetchUserDetails( "111" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(testDataManager.fetchUserDtos("000").getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/34")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );
        Mockito.verify(emailFactory).createSendEmail(argThat(comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher("null", "Batman", "null", "Wayne Enterprises", "light.yagami@death.note")), eq(INVITE_CANCELLED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void updateAssociationStatusForIdNotificationsUsesDisplayNamesWhenAvailable() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "3", "4" ) );
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(testDataManager.fetchUserDtos("444").getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/4")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "333")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );
    }

    @Test
    void updateAssociationToConfirmedStatusForOtherUserShouldThrow400Error() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "3", "4" ) );
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(testDataManager.fetchUserDtos("444").getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/4")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "333")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdUserAcceptedInvitationNotificationsSendsNotification() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "2", "4", "6" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        mockers.mockUsersServiceFetchUserDetails( "222", "666" );
        mockers.mockUsersServiceToFetchUserDetailsRequest( "222", "444", "666" );
        Mockito.doReturn(testDataManager.fetchUserDtos("666").getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/6")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "666")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect(status().isOk());
        final var expectedBaseEmail = new InvitationAcceptedEmailBuilder()
                .setInviterDisplayName( "the.joker@gotham.city" )
                .setInviteeDisplayName( "homer.simpson@springfield.com" )
                .setCompanyName("Wayne Enterprises");
        Mockito.verify(emailFactory, times(3)).createSendEmail(argThat(comparisonUtils.invitationAcceptedEmailDataMatcher(List.of("the.joker@gotham.city", "robin@gotham.city", "homer.simpson@springfield.com"), expectedBaseEmail)), eq(INVITATION_ACCEPTED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void updateAssociationStatusForIdThrowsBadRequestWhenUserTriesToConfirmOwnMigratedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        mockers.mockUsersServiceFetchUserDetails(MK_USER_001);
        Mockito.doReturn(testDataManager.fetchUserDtos(MK_USER_001).getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, MK_USER_001)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdUpdatesAssociationWhenUserTriesToRemoveOwnMigratedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        mockers.mockUsersServiceFetchUserDetails(MK_USER_001);
        Mockito.doReturn(testDataManager.fetchUserDtos(MK_USER_001).getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, MK_USER_001)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect(status().isOk());
        final var updatedAssociation = associationsRepository.findById("MKAssociation001").get();
        Assertions.assertEquals("removed", updatedAssociation.getStatus());
        Assertions.assertNotNull( updatedAssociation.getRemovedAt() );
        Assertions.assertNotNull( updatedAssociation.getEtag() );
        Assertions.assertEquals( 1, updatedAssociation.getPreviousStates().size() );
        Assertions.assertEquals( "migrated", updatedAssociation.getPreviousStates().getFirst().getStatus() );
        Assertions.assertEquals(MK_USER_001, updatedAssociation.getPreviousStates().getFirst().getChangedBy());
        Assertions.assertNotNull(  updatedAssociation.getPreviousStates().getFirst().getChangedAt() );
        Assertions.assertNull( updatedAssociation.getUserEmail() );
        Assertions.assertEquals(MK_USER_001, updatedAssociation.getUserId());
    }

    @Test
    void updateAssociationStatusForIdUpdatesAssociationWhenUserTriesToConfirmedAnotherUsersMigratedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        Mockito.doReturn(testDataManager.fetchUserDtos(MK_USER_001).getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect(status().isOk());
        final var updatedAssociation = associationsRepository.findById("MKAssociation001").get();
        final var invitation = updatedAssociation.getInvitations().getFirst();
        Assertions.assertNotNull(updatedAssociation.getEtag());
        Assertions.assertEquals( 1, updatedAssociation.getPreviousStates().size() );
        Assertions.assertEquals( "migrated", updatedAssociation.getPreviousStates().getFirst().getStatus() );
        Assertions.assertEquals( "MKUser002", updatedAssociation.getPreviousStates().getFirst().getChangedBy() );
        Assertions.assertNotNull(  updatedAssociation.getPreviousStates().getFirst().getChangedAt() );
        Assertions.assertEquals(MK_USER_001, updatedAssociation.getUserId());
        Assertions.assertNull( updatedAssociation.getUserEmail() );
        Assertions.assertEquals( "MKUser002", invitation.getInvitedBy() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertNotNull( invitation.getExpiredAt() );
        Assertions.assertEquals( "awaiting-approval", updatedAssociation.getStatus() );
        Assertions.assertNotNull( updatedAssociation.getApprovalExpiryAt() );
    }

    @Test
    void updateAssociationStatusForIdUpdatesAssociationWhenUserTriesToRemoveAnotherUsersMigratedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        Mockito.doReturn(testDataManager.fetchUserDtos(MK_USER_001).getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect(status().isOk());
        final var updatedAssociation = associationsRepository.findById("MKAssociation001").get();
        Assertions.assertEquals("removed", updatedAssociation.getStatus());
        Assertions.assertNotNull( updatedAssociation.getRemovedAt() );
        Assertions.assertNotNull( updatedAssociation.getEtag() );
        Assertions.assertEquals( 1, updatedAssociation.getPreviousStates().size() );
        Assertions.assertEquals( "migrated", updatedAssociation.getPreviousStates().getFirst().getStatus() );
        Assertions.assertEquals( "MKUser002", updatedAssociation.getPreviousStates().getFirst().getChangedBy() );
        Assertions.assertNotNull(  updatedAssociation.getPreviousStates().getFirst().getChangedAt() );
        Assertions.assertNull( updatedAssociation.getUserEmail() );
        Assertions.assertEquals(MK_USER_001, updatedAssociation.getUserId());
    }

    @Test
    void updateAssociationStatusForIdSendInvitationWhenUserTriesToConfirmNonexistentUsersMigratedAssociation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002" ) );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        Mockito.doReturn(null).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect(status().isOk());
        final var updatedAssociation = associationsRepository.findById("MKAssociation001").get();
        Assertions.assertEquals("awaiting-approval", updatedAssociation.getStatus());
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
        Mockito.doReturn(null).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect(status().isOk());
        final var updatedAssociation = associationsRepository.findById("MKAssociation001").get();
        Assertions.assertEquals("removed", updatedAssociation.getStatus());
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
        mockers.mockUsersServiceFetchUserDetails( "9999" );
        mockers.mockUsersServiceToFetchUserDetailsRequest( "9999", "111", "222" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(testDataManager.fetchUserDtos("111").getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/1")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header( "Eric-Authorised-Roles", ADMIN_UPDATE_PERMISSION )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect(status().isOk());
        final var updatedAssociation = associationsRepository.findById("1").get();
        Assertions.assertEquals("removed", updatedAssociation.getStatus());
        Mockito.verify(emailFactory, times(2)).createSendEmail(argThat(comparisonUtils.authorisationRemovedAndYourAuthorisationRemovedEmailMatcher(COMPANIES_HOUSE, "Batman", "Wayne Enterprises", "the.joker@gotham.city", "bruce.wayne@gotham.city")), argThat(messageType -> List.of(AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue(), YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue()).contains(messageType)));
    }

    @Test
    void updateAssociationStatusForIdAllowsAdminUserToCancelInvitation() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "6", "2" ) );
        mockers.mockUsersServiceFetchUserDetails( "9999", "666" );
        mockers.mockUsersServiceToFetchUserDetailsRequest( "9999", "666", "222" );
        mockers.mockCompanyServiceFetchCompanyProfile( "111111" );
        Mockito.doReturn(testDataManager.fetchUserDtos("666").getFirst()).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/6")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header( "Eric-Authorised-Roles", ADMIN_UPDATE_PERMISSION )
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );
        final var updatedAssociation = associationsRepository.findById("6").get();
        Assertions.assertEquals("removed", updatedAssociation.getStatus());
        Mockito.verify(emailFactory, times(2)).createSendEmail(argThat(comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher("the.joker@gotham.city", "Companies House", "homer.simpson@springfield.com", "Wayne Enterprises", "homer.simpson@springfield.com")), argThat(messageType -> List.of(INVITATION_CANCELLED_MESSAGE_TYPE.getValue(), INVITE_CANCELLED_MESSAGE_TYPE.getValue()).contains(messageType)));
    }

    @Test
    void updateAssociationStatusForIdWithAPIKeyCanSetStatusToUnauthorised() throws Exception {
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation002" ).getFirst();
        final var tagetUser = testDataManager.fetchUserDtos("MKUser002").getFirst();
        associationsRepository.insert(targetAssociation);
        Mockito.doReturn(tagetUser).when(usersService).fetchUserDetails(targetAssociation);
        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation002")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"unauthorised\"}" ) )
                .andExpect(status().isOk());
        final var updatedAssociation = associationsRepository.findById("MKAssociation002").get();
        Assertions.assertEquals(UNAUTHORISED.getValue(), updatedAssociation.getStatus());
        Assertions.assertNotNull( updatedAssociation.getUnauthorisedAt() );
        Assertions.assertEquals( COMPANIES_HOUSE, updatedAssociation.getUnauthorisedBy() );
    }

    private static Stream<Arguments> updateAssociationStatusForIdWithAPIKeyBadRequestScenarios() {
        return Stream.of(
                Arguments.of( "confirmed" ),
                Arguments.of( "removed" )
        );
    }

    @ParameterizedTest
    @MethodSource( "updateAssociationStatusForIdWithAPIKeyBadRequestScenarios" )
    void updateAssociationStatusForIdWithAPIKeyReturnsBadRequestWhenBodyContainsConfirmedOrRemoved( final String status ) throws Exception {
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation002" ).getFirst();
        final var tagetUser = testDataManager.fetchUserDtos("MKUser002").getFirst();
        associationsRepository.insert(targetAssociation);
        Mockito.doReturn(tagetUser).when(usersService).fetchUserDetails(targetAssociation);
        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation002")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( String.format( "{\"status\":\"%s\"}", status ) ) )
                .andExpect( status().isBadRequest() );
    }

    private static Stream<Arguments> updateAssociationStatusForIdAPIRequestsThatChangeUnauthorisedOrMigratedAssociationsToConfirmedScenarios() {
        return Stream.of(
                Arguments.of( "MKAssociation004", "MKUser004" ),
                Arguments.of("MKAssociation001", MK_USER_001)
        );
    }

    @ParameterizedTest
    @MethodSource( "updateAssociationStatusForIdAPIRequestsThatChangeUnauthorisedOrMigratedAssociationsToConfirmedScenarios" )
    void updateAssociationStatusForIdSupportsAPIRequestsThatChangeUnauthorisedOrMigratedAssociationsToConfirmed( final String targetAssociationId, final String targetUserId ) throws Exception {
        final var associations = testDataManager.fetchAssociationDaos( targetAssociationId ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos(targetUserId).getFirst();
        associationsRepository.insert(associations);
        Mockito.doReturn( targetUser ).when( usersService ).fetchUserDetails( any( AssociationDao.class ) );
        mockers.mockCompanyServiceFetchCompanyProfile( associations.getCompanyNumber() );
        mockers.mockUsersServiceFetchUserDetails( targetUserId );
        mockers.mockUsersServiceToFetchUserDetailsRequest(targetUserId);
        mockMvc.perform(patch(String.format(ASSOCIATIONS + "/%s", targetAssociationId))
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "9999")
                        .header(ERIC_IDENTITY_TYPE, "key")
                        .header(ERIC_AUTHORISED_KEY_ROLES, KEY_ROLES_VALUE)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect(status().isOk());
        Assertions.assertEquals(CONFIRMED.getValue(), associationsRepository.findById(targetAssociationId).get().getStatus());
        Mockito.verify(emailFactory).createSendEmail(argThat(comparisonUtils.authCodeConfirmationEmailMatcher(targetUser.getEmail(), "Mushroom Kingdom", targetUser.getDisplayName())), argThat(messageType -> List.of(AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue(), AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue()).contains(messageType)));
    }

    private static Stream<Arguments> updateAssociationStatusForIdSameUserUnauthorisedBadRequestScenarios() {
        return Stream.of(
                Arguments.of( "confirmed" ),
                Arguments.of( "unauthorised" )
        );
    }

    @ParameterizedTest
    @MethodSource( "updateAssociationStatusForIdSameUserUnauthorisedBadRequestScenarios" )
    void updateAssociationStatusForIdWhereUserTriesToConfirmOwnUnauthorisedAssociationOrSetStatusToUnauthorisedReturnsBadRequest( final String status ) throws Exception {
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation004" ).getFirst();
        final var tagetUser = testDataManager.fetchUserDtos("MKUser004").getFirst();
        associationsRepository.insert(targetAssociation);
        mockers.mockUsersServiceFetchUserDetails( "MKUser004" );
        Mockito.doReturn(tagetUser).when(usersService).fetchUserDetails(targetAssociation);
        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation004")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser004")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( String.format( "{\"status\":\"%s\"}", status ) ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdWhereDifferentUserAttemptsToConfirmAnotherUsersUnauthorisedAssociationSendsInvitation() throws Exception {
        final var associations = testDataManager.fetchAssociationDaos( "MKAssociation004", "MKAssociation002" );
        final var tagetUser = testDataManager.fetchUserDtos("MKUser004").getFirst();
        associationsRepository.insert(associations);
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        Mockito.doReturn(tagetUser).when(usersService).fetchUserDetails(associations.getFirst());
        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation004")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect(status().isOk());
        Assertions.assertEquals(AWAITING_APPROVAL.getValue(), associationsRepository.findById("MKAssociation004").get().getStatus());
    }

    @Test
    void updateAssociationStatusForIdWhereDifferentUserAttemptsToSetAnotherUsersAssociationToUnauthorisedReturnsBadRequest() throws Exception {
        final var associations = testDataManager.fetchAssociationDaos( "MKAssociation004", "MKAssociation002" );
        final var tagetUser = testDataManager.fetchUserDtos("MKUser004").getFirst();
        associationsRepository.insert(associations);
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        Mockito.doReturn(tagetUser).when(usersService).fetchUserDetails(associations.getFirst());
        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation004")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"unauthorised\"}" ) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    void updateAssociationStatusForIdSendsEmailWhenOneUserRemovesAnotherUsersMigratedAssociation() throws Exception {
        final var associations = testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002" );
        final var targetUser = testDataManager.fetchUserDtos(MK_USER_001).getFirst();
        associationsRepository.insert(associations);
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        mockers.mockUsersServiceToFetchUserDetailsRequest( "MKUser002" );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doReturn(targetUser).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect( status().isOk() );
        Mockito.verify(emailFactory, times(2)).createSendEmail(argThat(comparisonUtils.delegatedRemovalOfMigratedAndBatchEmailMatcher("mario@mushroom.kingdom", "Mario", "luigi@mushroom.kingdom", "Luigi", "Mushroom Kingdom")), argThat(messageType -> List.of(DELEGATED_REMOVAL_OF_MIGRATED.getValue(), DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue()).contains(messageType)));
    }

    @Test
    void updateAssociationStatusForIdSendsEmailWhenOneUserRemovesTheirOwnMigratedAssociation() throws Exception {
        final var associations = testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002" );
        final var targetUser = testDataManager.fetchUserDtos(MK_USER_001).getFirst();
        associationsRepository.insert(associations);
        mockers.mockUsersServiceFetchUserDetails(MK_USER_001, "MKUser002");
        mockers.mockUsersServiceToFetchUserDetailsRequest(MK_USER_001, "MKUser002");
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doReturn(targetUser).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, MK_USER_001)
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"removed\"}" ) )
                .andExpect(status().isOk());
        Mockito.verify(emailFactory, times(2)).createSendEmail(argThat(comparisonUtils.removalOfOwnMigratedEmailAndBatchMatcher("mario@mushroom.kingdom", "Mario", "luigi@mushroom.kingdom", "Mario", "Mushroom Kingdom")), argThat(messageType -> List.of(REMOVAL_OF_OWN_MIGRATED.getValue(), DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue()).contains(messageType)));
    }

    @Test
    void updateAssociationStatusForIdSendsEmailWhenOneUserConfirmsAnotherUsersMigratedAssociation() throws Exception {
        final var associations = testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002" );
        final var targetUser = testDataManager.fetchUserDtos(MK_USER_001).getFirst();
        associationsRepository.insert(associations);
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        mockers.mockUsersServiceToFetchUserDetailsRequest( "MKUser002" );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doReturn(targetUser).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation001")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect(status().isOk());
        Mockito.verify(emailFactory, times(2)).createSendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("luigi@mushroom.kingdom", "Luigi", "mario@mushroom.kingdom", "Mario", "Mushroom Kingdom", COMPANY_INVITATIONS_URL)), argThat(messageType -> List.of(INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue()).contains(messageType)));
    }

    @Test
    void updateAssociationStatusForIdSendsEmailWhenAPIKeyConfirmsAnUnauthorisedAssociation() throws Exception {
        final var associations = testDataManager.fetchAssociationDaos( "MKAssociation004", "MKAssociation002" );
        final var targetUser = testDataManager.fetchUserDtos("MKUser004").getFirst();
        associationsRepository.insert(associations);
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        mockers.mockUsersServiceToFetchUserDetailsRequest( "MKUser002" );
        mockers.mockCompanyServiceFetchCompanyProfile( "MKCOMP001" );
        Mockito.doReturn(targetUser).when(usersService).fetchUserDetails(any(AssociationDao.class));
        mockMvc.perform(patch(ASSOCIATIONS + "/MKAssociation004")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .contentType( MediaType.APPLICATION_JSON )
                        .content( "{\"status\":\"confirmed\"}" ) )
                .andExpect( status().isOk() );
        Mockito.verify(emailFactory, times(2)).createSendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("luigi@mushroom.kingdom", "Luigi", "bowser@mushroom.kingdom", "Bowser", "Mushroom Kingdom", COMPANY_INVITATIONS_URL)), argThat(messageType -> List.of(INVITATION_MESSAGE_TYPE.getValue(), INVITE_MESSAGE_TYPE.getValue()).contains(messageType)));
    }

    @Test
    void getPreviousStatesForAssociationRetrievesData() throws Exception {
        final var now = LocalDateTime.now();
        associationsRepository.insert(testDataManager.fetchAssociationDaos("MKAssociation003"));
        mockers.mockUsersServiceFetchUserDetails("MKUser002");
        final var response = mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation003/previous-states?page_index=1&items_per_page=1")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isOk());
        final var previousStatesList = parseResponseTo(response, PreviousStatesList.class);
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();
        Assertions.assertEquals(1, previousStatesList.getItemsPerPage());
        Assertions.assertEquals( 1, previousStatesList.getPageNumber() );
        Assertions.assertEquals( 4, previousStatesList.getTotalResults() );
        Assertions.assertEquals( 4, previousStatesList.getTotalPages() );
        Assertions.assertEquals(ASSOCIATIONS + "/MKAssociation003/previous-states?page_index=1&items_per_page=1", links.getSelf());
        Assertions.assertEquals(ASSOCIATIONS + "/MKAssociation003/previous-states?page_index=2&items_per_page=1", links.getNext());
        Assertions.assertEquals( 1, items.size() );
        Assertions.assertEquals( AWAITING_APPROVAL, items.getFirst().getStatus() );
        Assertions.assertEquals( "MKUser003", items.getFirst().getChangedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.minusDays( 7L ) ), reduceTimestampResolution( items.getFirst().getChangedAt() ) );
    }

    @Test
    void getPreviousStatesForAssociationSupportsUnauthorisedAssociation() throws Exception {
        final var now = LocalDateTime.now();
        associationsRepository.insert(testDataManager.fetchAssociationDaos("MKAssociation005"));
        mockers.mockUsersServiceFetchUserDetails("MKUser002");
        final var response = mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation005/previous-states?page_index=0&items_per_page=2")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isOk());
        final var previousStatesList = parseResponseTo(response, PreviousStatesList.class);
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();
        Assertions.assertEquals(2, previousStatesList.getItemsPerPage());
        Assertions.assertEquals( 0, previousStatesList.getPageNumber() );
        Assertions.assertEquals( 5, previousStatesList.getTotalResults() );
        Assertions.assertEquals( 3, previousStatesList.getTotalPages() );
        Assertions.assertEquals(ASSOCIATIONS + "/MKAssociation005/previous-states?page_index=0&items_per_page=2", links.getSelf());
        Assertions.assertEquals(ASSOCIATIONS + "/MKAssociation005/previous-states?page_index=1&items_per_page=2", links.getNext());
        Assertions.assertEquals(2, items.size());
        Assertions.assertEquals(UNAUTHORISED, items.getFirst().getStatus());
        Assertions.assertEquals( "MKUser005", items.getFirst().getChangedBy() );
        Assertions.assertEquals(localDateTimeToNormalisedString(now.minusDays(3L)), reduceTimestampResolution(items.getFirst().getChangedAt()));
        Assertions.assertEquals(CONFIRMED, items.getLast().getStatus());
        Assertions.assertEquals( "Companies House", items.getLast().getChangedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.minusDays( 6L ) ), reduceTimestampResolution( items.getLast().getChangedAt() ) );
    }

    @Test
    void getPreviousStatesForAssociationUsesDefaults() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockUsersServiceFetchUserDetails("MKUser002");
        final var response = mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation001/previous-states")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect(status().isOk());
        final var previousStatesList = parseResponseTo(response, PreviousStatesList.class);
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();
        Assertions.assertEquals(15, previousStatesList.getItemsPerPage());
        Assertions.assertEquals( 0, previousStatesList.getPageNumber() );
        Assertions.assertEquals( 0, previousStatesList.getTotalResults() );
        Assertions.assertEquals( 0, previousStatesList.getTotalPages() );
        Assertions.assertEquals(ASSOCIATIONS + "/MKAssociation001/previous-states?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertTrue( items.isEmpty() );
    }

    private static Stream<Arguments> getPreviousStatesForAssociationMalformedScenarios() {
        return Stream.of(
                Arguments.of(ASSOCIATIONS + "/$$$/previous-states"),
                Arguments.of(ASSOCIATIONS + "/MKAssociation003/previous-states?page_index=-1"),
                Arguments.of(ASSOCIATIONS + "/MKAssociation003/previous-states?items_per_page=-1")
        );
    }

    @ParameterizedTest
    @MethodSource( "getPreviousStatesForAssociationMalformedScenarios" )
    void getPreviousStatesForAssociationWithMalformedAssociationIdReturnsBadRequest( final String uri ) throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        mockMvc.perform( get( uri )
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect( status().isBadRequest() );
    }

    @Test
    void getPreviousStatesForAssociationWithNonexistentAssociationReturnsNotFound() throws Exception {
        mockers.mockUsersServiceFetchUserDetails( "MKUser002" );
        mockMvc.perform(get(ASSOCIATIONS + "/404MKAssociation/previous-states")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "MKUser002")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2))
                .andExpect( status().isNotFound() );
    }

    @Test
    void getPreviousStatesForAssociationCanBeCalledByAdmin() throws Exception {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MKAssociation001" ) );
        mockers.mockUsersServiceFetchUserDetails("111");
        mockMvc.perform(get(ASSOCIATIONS + "/MKAssociation001/previous-states")
                        .header(X_REQUEST_ID, X_REQUEST_ID_VALUE)
                        .header(ERIC_IDENTITY, "111")
                        .header(ERIC_IDENTITY_TYPE, OAUTH_2)
                        .header( "Eric-Authorised-Roles", ADMIN_READ_PERMISSION ) )
                .andExpect( status().isOk() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }
}
