package uk.gov.companieshouse.accounts.association.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.REQUEST_HEADERS.X_REQUEST_ID;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.DELEGATED_REMOVAL_OF_MIGRATED;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.DELEGATED_REMOVAL_OF_MIGRATED_BATCH;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_ACCEPTED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_CANCELLED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_REJECTED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITE_CANCELLED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITE_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.REMOVAL_OF_OWN_MIGRATED;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationAcceptedEmailBuilder;
import uk.gov.companieshouse.accounts.association.utils.MessageType;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class EmailServiceTest {

    @Mock
    private EmailProducer emailProducer;

    @Mock
    private UsersService usersService;

    @InjectMocks
    @Autowired
    private EmailService emailService;

    private final String COMPANY_INVITATIONS_URL = "http://chs.local/your-companies/company-invitations?mtm_campaign=associations_invite";

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final ComparisonUtils comparisonUtils = new ComparisonUtils();

    @BeforeEach
    void setup() {
    }
    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNameOrNullUsersThrowsNullPointerException() {
        final var userId = "333";
        when(usersService.fetchUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(usersService.fetchUserDetails(null, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("User not found"));

        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises",null, userId));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser(X_REQUEST_ID.value, "111111", null,  "Harleen Quinzel", userId));
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Harleen Quinzel", null));
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersThrowsEmailOnKafkaQueue() {
        final var userId = "333";
        when(usersService.fetchUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());

        emailService.sendAuthCodeConfirmationEmailToAssociatedUser(X_REQUEST_ID.value, "111111",  "Wayne Enterprises", "Harleen Quinzel", "333");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.authCodeConfirmationEmailMatcher("harley.quinn@gotham.city", "Wayne Enterprises", "Harleen Quinzel")), eq(MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException() {
        final var userId = "333";
        when(usersService.fetchUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), eq(AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue()));

        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Harleen Quinzel", "333"));
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException() {
        final var userId = "111";
        when(usersService.fetchUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(usersService.fetchUserDetails(null, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("User not found"));

        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", null,"Harleen Quinzel", "Batman", userId));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", null, "Batman", userId));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser(X_REQUEST_ID.value, "111111",  "Wayne Enterprises", "Harleen Quinzel", null, userId));
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", null));
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersThrowsEmailOnKafkaQueue() {
        final var userId = "333";
        when(usersService.fetchUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());

        emailService.sendAuthorisationRemovedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", userId);
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.authorisationRemovedAndYourAuthorisationRemovedEmailMatcher("Harleen Quinzel", "Batman", "Wayne Enterprises", "harley.quinn@gotham.city", "null")), eq(AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException() {
        final var userId = "333";
        when(usersService.fetchUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        doThrow(new EmailSendingException("Email failed", new Exception())).when(emailProducer).sendEmail(any(), any());
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", userId));
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException() {
        final var userId = "111";
        when(usersService.fetchUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());
        when(usersService.fetchUserDetails(null, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("User not found"));

        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser(X_REQUEST_ID.value, "111111", null, "Harleen Quinzel", "Batman", userId));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", null, "Batman", userId));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Harleen Quinzel", null, userId));
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser(X_REQUEST_ID.value, null, "Wayne Enterprises", "Harleen Quinzel", "Batman", null));
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersThrowsEmailOnKafkaQueue() {
        final var userId = "333";
        when(usersService.fetchUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());

        emailService.sendInvitationCancelledEmailToAssociatedUser(X_REQUEST_ID.value, "111111",  "Wayne Enterprises", "Harleen Quinzel", "Batman", userId);
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher("harley.quinn@gotham.city", "Harleen Quinzel", "Batman", "Wayne Enterprises", "null")), eq(INVITATION_CANCELLED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException() {
        final var userId = "333";
        when(usersService.fetchUserDetails(userId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());

        doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), any());

        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser(X_REQUEST_ID.value, "111111",  "Wayne Enterprises", "Harleen Quinzel", "Batman", userId));
    }

    @Test
    void sendInvitationEmailToAssociatedUsersNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException() {
        final var firstUserId = "111";
        final var secondUserId = "222";
        final var thirdUserId = "333";

        when(usersService.fetchUserDetails(firstUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(firstUserId).getFirst());
        when(usersService.fetchUserDetails(secondUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(secondUserId).getFirst());
        when(usersService.fetchUserDetails(thirdUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(thirdUserId).getFirst());
        when(usersService.fetchUserDetails(null, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("User not found"));

        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser(X_REQUEST_ID.value, "111111", null,"Harleen Quinzel", "Batman", thirdUserId));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises",null, "Batman", secondUserId));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises","Harleen Quinzel", null, firstUserId));
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> emailService.sendInvitationEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises","Harleen Quinzel", "Batman", null));
    }

    @Test
    void sendInvitationEmailToAssociatedUsersThrowsEmailOnKafkaQueue() {
        final var firstUserId = "111";
        final var secondUserId = "222";
        final var thirdUserId = "333";

        when(usersService.fetchUserDetails(firstUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(firstUserId).getFirst());
        when(usersService.fetchUserDetails(secondUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(secondUserId).getFirst());
        when(usersService.fetchUserDetails(thirdUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(thirdUserId).getFirst());

        emailService.sendInvitationEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", firstUserId);
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("bruce.wayne@gotham.city", "Harleen Quinzel", "bruce.wayne@gotham.city", "Batman", "Wayne Enterprises", COMPANY_INVITATIONS_URL)), eq(INVITATION_MESSAGE_TYPE.getValue()));
        emailService.sendInvitationEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", secondUserId);
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("the.joker@gotham.city", "Harleen Quinzel", "bruce.wayne@gotham.city", "Batman", "Wayne Enterprises", COMPANY_INVITATIONS_URL)), eq(INVITATION_MESSAGE_TYPE.getValue()));
        emailService.sendInvitationEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", thirdUserId);
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("harley.quinn@gotham.city", "Harleen Quinzel", "bruce.wayne@gotham.city", "Batman", "Wayne Enterprises", COMPANY_INVITATIONS_URL)), eq(INVITATION_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendInvitationEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException() {
        final var firstUserId = "111";
        final var secondUserId = "222";
        final var thirdUserId = "333";

        when(usersService.fetchUserDetails(firstUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(firstUserId).getFirst());
        when(usersService.fetchUserDetails(secondUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(secondUserId).getFirst());
        when(usersService.fetchUserDetails(thirdUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(thirdUserId).getFirst());

        doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), any());

        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationEmailToAssociatedUser(X_REQUEST_ID.value, "111111",  "Wayne Enterprises", "Harleen Quinzel", "Elon Musk", firstUserId));
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationEmailToAssociatedUser(X_REQUEST_ID.value, "111111",  "Wayne Enterprises", "Harleen Quinzel", "Elon Musk", secondUserId));
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationEmailToAssociatedUser(X_REQUEST_ID.value, "111111",  "Wayne Enterprises", "Harleen Quinzel", "Elon Musk", thirdUserId));
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException() {
        final var firstUserId = "111";
        final var secondUserId = "222";
        final var thirdUserId = "333";

        when(usersService.fetchUserDetails(firstUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(firstUserId).getFirst());
        when(usersService.fetchUserDetails(secondUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(secondUserId).getFirst());
        when(usersService.fetchUserDetails(thirdUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(thirdUserId).getFirst());
        when(usersService.fetchUserDetails(null, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("User not found"));

        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", null, Optional.of("Harleen Quinzel"), "Batman", firstUserId));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", Optional.empty(), "Batman", secondUserId));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", Optional.of("Harleen Quinzel"), null, thirdUserId));
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", Optional.of("Harleen Quinzel"), "Batman", null));
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersThrowsEmailOnKafkaQueue() {
        final var expectedBaseEmail = new InvitationAcceptedEmailBuilder()
                .setInviterDisplayName("Harleen Quinzel")
                .setInviteeDisplayName("Batman")
                .setCompanyName("Wayne Enterprises");

        final var firstUserId = "111";
        final var secondUserId = "222";
        final var thirdUserId = "333";

        when(usersService.fetchUserDetails(firstUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(firstUserId).getFirst());
        when(usersService.fetchUserDetails(secondUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(secondUserId).getFirst());
        when(usersService.fetchUserDetails(thirdUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(thirdUserId).getFirst());

        emailService.sendInvitationAcceptedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", Optional.of("Harleen Quinzel"), "Batman", thirdUserId);
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationAcceptedEmailDataMatcher(List.of("harley.quinn@gotham.city"), expectedBaseEmail)), eq(INVITATION_ACCEPTED_MESSAGE_TYPE.getValue()));
        emailService.sendInvitationAcceptedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", Optional.of("Harleen Quinzel"), "Batman", secondUserId);
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationAcceptedEmailDataMatcher(List.of("the.joker@gotham.city"), expectedBaseEmail)), eq(INVITATION_ACCEPTED_MESSAGE_TYPE.getValue()));
        emailService.sendInvitationAcceptedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", Optional.of("Harleen Quinzel"), "Batman", firstUserId);
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationAcceptedEmailDataMatcher(List.of("bruce.wayne@gotham.city"), expectedBaseEmail)), eq(INVITATION_ACCEPTED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException() {
        final var firstUserId = "111";
        final var secondUserId = "222";
        final var thirdUserId = "333";

        when(usersService.fetchUserDetails(firstUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(firstUserId).getFirst());
        when(usersService.fetchUserDetails(secondUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(secondUserId).getFirst());
        when(usersService.fetchUserDetails(thirdUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(thirdUserId).getFirst());

        doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), eq(INVITATION_ACCEPTED_MESSAGE_TYPE.getValue()));

        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", Optional.of("Harleen Quinzel"), "Batman", firstUserId));
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", Optional.of("Harleen Quinzel"), "Batman", secondUserId));
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", Optional.of("Harleen Quinzel"), "Batman", thirdUserId));
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNameOrNullRequestsThrowsNullPointerException() {
        final var thirdUserId = "333";
        when(usersService.fetchUserDetails(thirdUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(thirdUserId).getFirst());

        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", null, "Batman", thirdUserId));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", null, thirdUserId));
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersThrowsEmailOnKafkaQueue() {
        final var thirdUserId = "333";
        when(usersService.fetchUserDetails(thirdUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(thirdUserId).getFirst());

        emailService.sendInvitationRejectedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Batman", thirdUserId);
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationRejectedEmailMatcher("harley.quinn@gotham.city", "Batman", "Wayne Enterprises")), eq(INVITATION_REJECTED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException() {
        final var thirdUserId = "333";
        when(usersService.fetchUserDetails(thirdUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(thirdUserId).getFirst());

        doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), any());

        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUser(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Batman", thirdUserId));
    }

    @Test
    void sendInviteEmailWithNullCompanyDetailsOrNullCompanyNameOrNullInviterDisplayNameOrNullInvitationExpiryTimestampOrNullInvitationLinkOrNullInviteeEmailThrowsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInviteEmail(X_REQUEST_ID.value, "111111", null, "Batman", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInviteEmail(X_REQUEST_ID.value, "111111", "Wayne Enterprises", null, "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInviteEmail(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Batman", null, "kpatel@companieshouse.gov.uk"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInviteEmail(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Batman", "1992-05-01T10:30:00.000000", null));
    }

    @Test
    void sendInviteEmailThrowsEmailOnKafkaQueue() {
        ReflectionTestUtils.setField(emailService, "invitationLink", COMPANY_INVITATIONS_URL);

        emailService.sendInviteEmail(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Batman", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("bruce.wayne@gotham.city", "Batman", "kpatel@companieshouse.gov.uk", "Krishna Patel", "Wayne Enterprises", COMPANY_INVITATIONS_URL)), eq(INVITE_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendInviteEmailWithUnexpectedIssueThrowsEmailSendingException() {
        ReflectionTestUtils.setField(emailService, "invitationLink", COMPANY_INVITATIONS_URL);
        doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), any());

        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInviteEmail(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Krishna Patel", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk"));
    }

    @Test
    void sendInviteCancelledEmailWithoutCompanyDetailsOrCompanyNameOrCancelledByDisplayNameOrInviteeUserSupplierOrEmailThrowsNullPointerException() {
        final var associationUserId = "000";
        final var association = testDataManager.fetchAssociationDaos("34").getFirst();

        when(usersService.fetchUserDetailsByEmail(association.getUserEmail(), X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(associationUserId).stream().collect(Collectors.toCollection(UsersList::new)));

        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInviteCancelledEmail(X_REQUEST_ID.value, "111111", null, "Batman", association));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInviteCancelledEmail(X_REQUEST_ID.value, "111111", "Wayne Enterprises", null, association));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInviteCancelledEmail(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Batman", null));
    }

    @Test
    void sendInviteCancelledEmailSendsEmail() {
        final var associationUserId = "000";
        final var association = testDataManager.fetchAssociationDaos("34").getFirst();

        when(usersService.fetchUserDetailsByEmail(association.getUserEmail(), X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(associationUserId).stream().collect(Collectors.toCollection(UsersList::new)));

        emailService.sendInviteCancelledEmail(X_REQUEST_ID.value, "111111", "Wayne Enterprises", "Batman", association);
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.inviteCancelledEmailMatcher("Batman", "Wayne Enterprises", "light.yagami@death.note")), eq(INVITE_CANCELLED_MESSAGE_TYPE.getValue()));
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationCancelledEmailMatcher ("light.yagami@death.note","Batman", "null", "Wayne Enterprises")), eq(INVITE_CANCELLED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendDelegatedRemovalOfMigratedBatchWithNullCompanyNameOrNullRemovedByOrNullRemovedUsersThrowsNullPointerException() {
        final var firstUserId = "111";

        when(usersService.fetchUserDetails(firstUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(firstUserId).getFirst());
        when(usersService.fetchUserDetails(null, X_REQUEST_ID.value)).thenThrow(new NotFoundRuntimeException("User not found"));

        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail(X_REQUEST_ID.value, "111111", null, "Batman", "Ronald", firstUserId));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail(X_REQUEST_ID.value, "111111", "McDonalds", null, "Ronald", firstUserId));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail(X_REQUEST_ID.value, "111111", "McDonalds", "Batman", null, firstUserId));
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail(X_REQUEST_ID.value, "111111", "McDonalds", "Batman", "Ronald", null));
    }

    @Test
    void sendDelegatedRemovalOfMigratedBatchThrowsEmailOnKafkaQueue() {
        final var thirdUserId = "333";

        when(usersService.fetchUserDetails(thirdUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(thirdUserId).getFirst());

        emailService.sendDelegatedRemovalOfMigratedBatchEmail(X_REQUEST_ID.value, "111111", "McDonalds", "Batman", "Harleen Quinzel", thirdUserId);
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.delegatedRemovalOfMigratedBatchMatcher("harley.quinn@gotham.city", "McDonalds", "Batman" , "Harleen Quinzel")), eq(DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue()));
    }

    @Test
    void sendDelegatedRemovalOfMigratedBatchWithUnexpectedIssueThrowsEmailSendingException() {
        final var thirdUserId = "333";

        when(usersService.fetchUserDetails(thirdUserId, X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos(thirdUserId).getFirst());
        doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), any());

        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail(X_REQUEST_ID.value, "111111", "McDonalds", "Batman", "Harleen Quinzel", thirdUserId));
    }

    @Test
    void sendDelegatedRemovalOfMigratedWithNullCompanyNameOrNullRemovedByThrowsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedEmail(X_REQUEST_ID.value, "111111", null, "Batman", "bruce.wayne@gotham.city"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedEmail(X_REQUEST_ID.value, "111111", "McDonalds", null, "bruce.wayne@gotham.city"));
    }

    @Test
    void sendDelegatedRemovalOfMigratedThrowsEmailOnKafkaQueue() {
        emailService.sendDelegatedRemovalOfMigratedEmail(X_REQUEST_ID.value, "111111", "McDonalds", "Batman", "harley.quinn@gotham.city");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.delegatedRemovalOfMigratedMatcher("harley.quinn@gotham.city", "McDonalds", "Batman")), eq(DELEGATED_REMOVAL_OF_MIGRATED.getValue()));
    }

    @Test
    void sendDelegatedRemovalOfMigratedWithUnexpectedIssueThrowsEmailSendingException() {
        doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), any());

        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendDelegatedRemovalOfMigratedEmail(X_REQUEST_ID.value, "111111", "McDonalds", "Batman", "harley.quinn@gotham.city"));
    }

    @Test
    void sendRemovalOfOwnMigratedWithNullCompanyNameThrowsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendRemoveOfOwnMigratedEmail(X_REQUEST_ID.value, "111111", null, "111"));
    }

    @Test
    void sendRemovalOfOwnMigratedThrowsEmailOnKafkaQueue() {
        when(usersService.fetchUserDetails("333", X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos("333").getFirst());

        emailService.sendRemoveOfOwnMigratedEmail(X_REQUEST_ID.value, "111111", "McDonalds", "333");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.removalOfOwnMigratedMatcher("harley.quinn@gotham.city", "McDonalds")), eq(REMOVAL_OF_OWN_MIGRATED.getValue()));
    }

    @Test
    void sendRemovalOfOwnMigratedWithUnexpectedIssueThrowsEmailSendingException() {
        when(usersService.fetchUserDetails("333", X_REQUEST_ID.value)).thenReturn(testDataManager.fetchUserDtos("333").getFirst());

        doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), any());

        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendRemoveOfOwnMigratedEmail(X_REQUEST_ID.value, "111111", "McDonalds",  "333"));
    }
}
