package uk.gov.companieshouse.accounts.association.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
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
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationAcceptedEmailBuilder;
import uk.gov.companieshouse.accounts.association.utils.MessageType;
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
    private EmailService emailService;

    private final String COMPANY_INVITATIONS_URL = "http://chs.local/your-companies/company-invitations?mtm_campaign=associations_invite";

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private Mockers mockers;

    private static final ComparisonUtils comparisonUtils = new ComparisonUtils();

    @BeforeEach
    void setup() {
        mockers = new Mockers(null, emailProducer, null, usersService);
        ReflectionTestUtils.setField(emailService, "invitationLink", COMPANY_INVITATIONS_URL);
    }
    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNameOrNullUsersThrowsNullPointerException(){
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises",null));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser("theId12345","111111", null,  "Harleen Quinzel"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser("theId12345","111111", "Wayne Enterprises", "Harleen Quinzel"));
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        when(usersService.fetchUserDetails("Harleen Quinzel", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());

        emailService.sendAuthCodeConfirmationEmailToAssociatedUser("theId12345", "111111",  "Wayne Enterprises", "Harleen Quinzel");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.authCodeConfirmationEmailMatcher("harley.quinn@gotham.city", "Wayne Enterprises", "Harleen Quinzel")), eq(MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        when(usersService.fetchUserDetails("Harleen Quinzel", getXRequestId())).thenReturn(testDataManager.fetchUserDtos("333").getFirst());
        doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), eq(AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue()));

        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser(getXRequestId(), "111111", "Wayne Enterprises", "Harleen Quinzel"));
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser("theId12345", "111111", null,"Harleen Quinzel", "Batman"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", null, "Batman"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser("theId12345", "111111",  "Wayne Enterprises", "Harleen Quinzel", null));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman"));
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        when(usersService.fetchUserDetails("Harleen Quinzel", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());
        emailService.sendAuthorisationRemovedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.authorisationRemovedAndYourAuthorisationRemovedEmailMatcher("Harleen Quinzel", "Batman", "Wayne Enterprises", "harley.quinn@gotham.city", "null")), eq(AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        when(usersService.fetchUserDetails("Harleen Quinzel", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());
        mockers.mockEmailSendingFailure(AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue());
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman"));
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        when(usersService.fetchUserDetails("Batman", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser("theId12345", "111111", null, "Harleen Quinzel", "Batman"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", null, "Batman"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Harleen Quinzel", null));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser("theId12345", null, "Wayne Enterprises", "Harleen Quinzel", "Batman"));
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        when(usersService.fetchUserDetails("Batman", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());
        emailService.sendInvitationCancelledEmailToAssociatedUser("theId12345", "111111",  "Wayne Enterprises", "Harleen Quinzel", "Batman");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher("harley.quinn@gotham.city", "Harleen Quinzel", "Batman", "Wayne Enterprises", "null")), eq(INVITATION_CANCELLED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        when(usersService.fetchUserDetails("Batman", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());
        mockers.mockEmailSendingFailure(INVITATION_CANCELLED_MESSAGE_TYPE.getValue());
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser("theId12345", "111111",  "Wayne Enterprises", "Harleen Quinzel", "Batman"));
    }

    @Test
    void sendInvitationEmailToAssociatedUsersNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        when(usersService.fetchUserDetails("Batman", "theId12345")).thenReturn(testDataManager.fetchUserDtos("111").getFirst());
        when(usersService.fetchUserDetails("Joker", "theId12345")).thenReturn(testDataManager.fetchUserDtos("222").getFirst());
        when(usersService.fetchUserDetails("Harleen Quinzel", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());

        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser("theId12345", "111111", null,"Harleen Quinzel", "Batman", "Harleen Quinzel"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises",null, "Batman", "Joker"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises","Harleen Quinzel", null, "Batman"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises","Harleen Quinzel", "Batman", null));
    }

    @Test
    void sendInvitationEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        when(usersService.fetchUserDetails("Batman", "theId12345")).thenReturn(testDataManager.fetchUserDtos("111").getFirst());
        when(usersService.fetchUserDetails("Joker", "theId12345")).thenReturn(testDataManager.fetchUserDtos("222").getFirst());
        when(usersService.fetchUserDetails("Harleen Quinzel", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());

        //TODO: this has been restructured and needs thorough testing
        emailService.sendInvitationEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", "Batman");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("bruce.wayne@gotham.city", "Harleen Quinzel", "bruce.wayne@gotham.city", "Batman", "Wayne Enterprises", COMPANY_INVITATIONS_URL)), eq(INVITATION_MESSAGE_TYPE.getValue()));
        emailService.sendInvitationEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", "Joker");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("the.joker@gotham.city", "Harleen Quinzel", "bruce.wayne@gotham.city", "Batman", "Wayne Enterprises", COMPANY_INVITATIONS_URL)), eq(INVITATION_MESSAGE_TYPE.getValue()));
        emailService.sendInvitationEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", "Harleen Quinzel");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("harley.quinn@gotham.city", "Harleen Quinzel", "bruce.wayne@gotham.city", "Batman", "Wayne Enterprises", COMPANY_INVITATIONS_URL)), eq(INVITATION_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendInvitationEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        when(usersService.fetchUserDetails("Batman", "theId12345")).thenReturn(testDataManager.fetchUserDtos("111").getFirst());
        when(usersService.fetchUserDetails("Joker", "theId12345")).thenReturn(testDataManager.fetchUserDtos("222").getFirst());
        when(usersService.fetchUserDetails("Harleen Quinzel", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());

        mockers.mockEmailSendingFailure(INVITATION_MESSAGE_TYPE.getValue());
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationEmailToAssociatedUser("theId12345", "111111",  "Wayne Enterprises", "Harleen Quinzel", "Elon Musk", "Batman"));
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationEmailToAssociatedUser("theId12345", "111111",  "Wayne Enterprises", "Harleen Quinzel", "Elon Musk", "Joker"));
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationEmailToAssociatedUser("theId12345", "111111",  "Wayne Enterprises", "Harleen Quinzel", "Elon Musk", "Harleen Quinzel"));
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        when(usersService.fetchUserDetails("Batman", "theId12345")).thenReturn(testDataManager.fetchUserDtos("111").getFirst());
        when(usersService.fetchUserDetails("Joker", "theId12345")).thenReturn(testDataManager.fetchUserDtos("222").getFirst());
        when(usersService.fetchUserDetails("Harleen Quinzel", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());

        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser("theId12345", "111111", null, "Harleen Quinzel", "Batman", "Batman"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", null, "Batman", "Joker"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Harleen Quinzel", null, "Harleen Quinzel"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", null));
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var expectedBaseEmail = new InvitationAcceptedEmailBuilder()
                .setInviterDisplayName("Harleen Quinzel")
                .setInviteeDisplayName("Batman")
                .setCompanyName("Wayne Enterprises");
        mockers.mockUsersServiceToFetchUserDetailsRequest("333");

        when(usersService.fetchUserDetails("Batman", "theId12345")).thenReturn(testDataManager.fetchUserDtos("111").getFirst());
        when(usersService.fetchUserDetails("Joker", "theId12345")).thenReturn(testDataManager.fetchUserDtos("222").getFirst());
        when(usersService.fetchUserDetails("Harleen Quinzel", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());

        emailService.sendInvitationAcceptedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", "Harleen Quinzel");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationAcceptedEmailDataMatcher(List.of("harley.quinn@gotham.city"), expectedBaseEmail)), eq(INVITATION_ACCEPTED_MESSAGE_TYPE.getValue()));
        emailService.sendInvitationAcceptedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", "Joker");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationAcceptedEmailDataMatcher(List.of("the.joker@gotham.city"), expectedBaseEmail)), eq(INVITATION_ACCEPTED_MESSAGE_TYPE.getValue()));
        emailService.sendInvitationAcceptedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", "Batman");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationAcceptedEmailDataMatcher(List.of("bruce.wayne@gotham.city"), expectedBaseEmail)), eq(INVITATION_ACCEPTED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        when(usersService.fetchUserDetails("Batman", "theId12345")).thenReturn(testDataManager.fetchUserDtos("111").getFirst());
        when(usersService.fetchUserDetails("Joker", "theId12345")).thenReturn(testDataManager.fetchUserDtos("222").getFirst());
        when(usersService.fetchUserDetails("Harleen Quinzel", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());

        doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), eq(INVITATION_ACCEPTED_MESSAGE_TYPE.getValue()));

        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", "Batman"));
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", "Joker"));
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Harleen Quinzel", "Batman", "Harleen Quinzel"));
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNameOrNullRequestsThrowsNullPointerException(){
        when(usersService.fetchUserDetails("Harleen Quinzel", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());

        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUser("theId12345", "111111", null, "Batman", "Harleen Quinzel"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", null, "Harleen Quinzel"));
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        when(usersService.fetchUserDetails("Harleen Quinzel", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());

        emailService.sendInvitationRejectedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Batman", "Harleen Quinzel");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationRejectedEmailMatcher("harley.quinn@gotham.city", "Batman", "Wayne Enterprises")), eq(INVITATION_REJECTED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        when(usersService.fetchUserDetails("Harleen Quinzel", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());

        mockers.mockEmailSendingFailure(INVITATION_REJECTED_MESSAGE_TYPE.getValue());
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUser("theId12345", "111111", "Wayne Enterprises", "Batman", "Harleen Quinzel"));
    }

    @Test
    void sendInviteEmailWithNullCompanyDetailsOrNullCompanyNameOrNullInviterDisplayNameOrNullInvitationExpiryTimestampOrNullInvitationLinkOrNullInviteeEmailThrowsNullPointerException(){
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInviteEmail("theId12345", "111111", null, "Batman", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInviteEmail("theId12345", "111111", "Wayne Enterprises", null, "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInviteEmail("theId12345", "111111", "Wayne Enterprises", "Batman", null, "kpatel@companieshouse.gov.uk"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInviteEmail("theId12345", "111111", "Wayne Enterprises", "Batman", "1992-05-01T10:30:00.000000", null));
    }

    @Test
    void sendInviteEmailThrowsEmailOnKafkaQueue(){
        emailService.sendInviteEmail("theId12345", "111111", "Wayne Enterprises", "Batman", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("bruce.wayne@gotham.city", "Batman", "kpatel@companieshouse.gov.uk", "Krishna Patel", "Wayne Enterprises", COMPANY_INVITATIONS_URL)), eq(INVITE_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendInviteEmailWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockEmailSendingFailure(INVITE_MESSAGE_TYPE.getValue());
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendInviteEmail("theId12345", "111111", "Wayne Enterprises", "Krishna Patel", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk"));
    }

    @Test
    void sendInviteCancelledEmailWithoutCompanyDetailsOrCompanyNameOrCancelledByDisplayNameOrInviteeUserSupplierOrEmailThrowsNullPointerException(){
        final var association = testDataManager.fetchAssociationDaos("34").getFirst();

        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInviteCancelledEmail("theId12345", "111111", null, "Batman", association));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInviteCancelledEmail("theId12345", "111111", "Wayne Enterprises", null, association));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendInviteCancelledEmail("theId12345", "111111", "Wayne Enterprises", "Batman", null));
    }

    @Test
    void sendInviteCancelledEmailSendsEmail(){
        final var association = testDataManager.fetchAssociationDaos("34").getFirst();
        when(usersService.fetchUserDetails(association.getUserId(), "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());
        when(usersService.fetchUserDetails("Batman", "theId12345")).thenReturn(testDataManager.fetchUserDtos("111").getFirst());

        emailService.sendInviteCancelledEmail("theId12345", "111111", "Wayne Enterprises", "Batman", association);
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.inviteCancelledEmailMatcher("Batman", "Wayne Enterprises", "light.yagami@death.note")), eq(INVITE_CANCELLED_MESSAGE_TYPE.getValue()));
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.invitationCancelledEmailMatcher ("light.yagami@death.note","Batman", "null", "Wayne Enterprises")), eq(INVITE_CANCELLED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendDelegatedRemovalOfMigratedBatchWithNullCompanyNameOrNullRemovedByOrNullRemovedUsersThrowsNullPointerException(){
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail("theId12345", "111111", null, "Batman", "Ronald"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail("theId12345", "111111", "McDonalds", null, "Ronald"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail("theId12345", "111111", "McDonalds", "Batman", null));
    }

    @Test
    void sendDelegatedRemovalOfMigratedBatchThrowsEmailOnKafkaQueue(){
        when(usersService.fetchUserDetails("Harleen Quinzel", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());

        emailService.sendDelegatedRemovalOfMigratedBatchEmail("theId12345", "111111", "McDonalds", "Batman", "Harleen Quinzel");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.delegatedRemovalOfMigratedBatchMatcher("harley.quinn@gotham.city", "McDonalds", "Batman" , "Harleen Quinzel")), eq(DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue()));
    }

    @Test
    void sendDelegatedRemovalOfMigratedBatchWithUnexpectedIssueThrowsEmailSendingException(){
        when(usersService.fetchUserDetails("Harleen Quinzel", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());
        mockers.mockEmailSendingFailure(DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue());

        //TODO: Confirm this is correct, should the removed user be fetchable from the user service?
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail("theId12345", "111111", "McDonalds", "Batman", "Harleen Quinzel"));
    }

    @Test
    void sendDelegatedRemovalOfMigratedWithNullCompanyNameOrNullRemovedByThrowsNullPointerException(){
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedEmail("theId12345", "111111", null, "Batman", "bruce.wayne@gotham.city"));
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedEmail("theId12345", "111111", "McDonalds", null, "bruce.wayne@gotham.city"));
    }

    @Test
    void sendDelegatedRemovalOfMigratedThrowsEmailOnKafkaQueue(){
        emailService.sendDelegatedRemovalOfMigratedEmail("theId12345", "111111", "McDonalds", "Batman", "harley.quinn@gotham.city");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.delegatedRemovalOfMigratedMatcher("harley.quinn@gotham.city", "McDonalds", "Batman")), eq(DELEGATED_REMOVAL_OF_MIGRATED.getValue()));
    }

    @Test
    void sendDelegatedRemovalOfMigratedWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockEmailSendingFailure(DELEGATED_REMOVAL_OF_MIGRATED.getValue());
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendDelegatedRemovalOfMigratedEmail("theId12345", "111111", "McDonalds", "Batman", "harley.quinn@gotham.city"));
    }

    @Test
    void sendRemovalOfOwnMigratedWithNullCompanyNameThrowsNullPointerException(){
        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendRemoveOfOwnMigratedEmail("theId12345", "111111", null, "111"));
    }

    @Test
    void sendRemovalOfOwnMigratedThrowsEmailOnKafkaQueue(){
        when(usersService.fetchUserDetails("333", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());

        emailService.sendRemoveOfOwnMigratedEmail("theId12345", "111111", "McDonalds", "333");
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.removalOfOwnMigratedMatcher("harley.quinn@gotham.city", "McDonalds")), eq(REMOVAL_OF_OWN_MIGRATED.getValue()));
    }

    @Test
    void sendRemovalOfOwnMigratedWithUnexpectedIssueThrowsEmailSendingException(){
        when(usersService.fetchUserDetails("333", "theId12345")).thenReturn(testDataManager.fetchUserDtos("333").getFirst());

        mockers.mockEmailSendingFailure(REMOVAL_OF_OWN_MIGRATED.getValue());
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendRemoveOfOwnMigratedEmail("theId12345", "111111", "McDonalds",  "333"));
    }
}
