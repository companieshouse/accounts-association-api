package uk.gov.companieshouse.accounts.association.service;

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
import uk.gov.companieshouse.accounts.association.client.EmailClient;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.EmailSendException;
import uk.gov.companieshouse.accounts.association.factory.SendEmailFactory;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationAcceptedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.data.EmailData;
import uk.gov.companieshouse.accounts.association.utils.MessageType;
import uk.gov.companieshouse.api.chskafka.SendEmail;

import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
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
import static uk.gov.companieshouse.accounts.association.utils.MessageType.REA_DIGITAL_AUTHORISATION_ADDED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.REA_DIGITAL_AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.REMOVAL_OF_OWN_MIGRATED;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class EmailServiceTest {

    @Mock
    private EmailClient emailClient;

    @Mock
    private UsersService usersService;

    @Mock
    private CompanyService companyService;

    @Mock
    private SendEmailFactory sendEmailFactory;

    @InjectMocks
    private EmailService emailService;

    private final String COMPANY_INVITATIONS_URL = "http://chs.local/your-companies/company-invitations?mtm_campaign=associations_invite";

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private Mockers mockers;

    private static final ComparisonUtils comparisonUtils = new ComparisonUtils();

    @BeforeEach
    void setup(){
        mockers = new Mockers(null, emailClient, null, usersService);
        ReflectionTestUtils.setField(emailService, "invitationLink", COMPANY_INVITATIONS_URL);
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNameOrNullUsersThrowsNullPointerException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", "111111",null, "Harleen Quinzel" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel" ).apply( null ).block() );
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsers() {
        sendEmailFactoryMock(MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue());
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel" ).apply( "333" ).block();
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.authCodeConfirmationEmailMatcher("harley.quinn@gotham.city", "Wayne Enterprises", "Harleen Quinzel")), eq(MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue()));
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendException() {
        mockers.mockUsersServiceToFetchUserDetailsRequest("333");
        sendEmailFactoryMock(AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue());
        mockers.mockEmailSendingFailure();
        Assertions.assertThrows(EmailSendException.class, () ->
                emailService.sendAuthCodeConfirmationEmailToAssociatedUser("theId12345", "111111", Mono.just("Wayne Enterprises"), "Harleen Quinzel")
                        .apply("333")
                        .block()
        );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", null,"Harleen Quinzel", "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null, "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", null ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" ).apply( null ).block() );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersSendEmail() {
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        sendEmailFactoryMock(AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue());
        emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" ).apply( "333" ).block();
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.authorisationRemovedAndYourAuthorisationRemovedEmailMatcher("Harleen Quinzel", "Batman", "Wayne Enterprises", "harley.quinn@gotham.city", "null")), eq(AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue()));
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendException() {
        sendEmailFactoryMock(AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue());
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        mockers.mockEmailSendingFailure();
        Assertions.assertThrows(EmailSendException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser("theId12345", "111111", Mono.just("Wayne Enterprises"), "Harleen Quinzel", "Batman").apply("333").block());
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", null, "Harleen Quinzel", "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null, "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", null ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" ).apply( null ).block() );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersSendsEmail() {
        sendEmailFactoryMock(INVITATION_CANCELLED_MESSAGE_TYPE.getValue());
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" ).apply( "333" ).block();
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher("harley.quinn@gotham.city", "Harleen Quinzel", "Batman", "Wayne Enterprises", "null")), eq(INVITATION_CANCELLED_MESSAGE_TYPE.getValue()));
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendException() {
        sendEmailFactoryMock(INVITATION_CANCELLED_MESSAGE_TYPE.getValue());
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        mockers.mockEmailSendingFailure();
        Assertions.assertThrows(EmailSendException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser("theId12345", "111111", Mono.just("Wayne Enterprises"), "Harleen Quinzel", "Batman").apply("333").block());
    }

    @Test
    void sendInvitationEmailToAssociatedUsersNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser( "theId12345", "111111", null,"Harleen Quinzel", "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ),null, "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ),"Harleen Quinzel", null ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ),"Harleen Quinzel", "Batman" ).apply( null ).block() );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersSendsEmail() {
        sendEmailFactoryMock(INVITATION_MESSAGE_TYPE.getValue());
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        emailService.sendInvitationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" ).apply( "333" ).block();
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("harley.quinn@gotham.city", "Harleen Quinzel", "bruce.wayne@gotham.city", "Batman", "Wayne Enterprises", COMPANY_INVITATIONS_URL)), eq(INVITATION_MESSAGE_TYPE.getValue()));
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendInvitationEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendException() {
        sendEmailFactoryMock(INVITATION_MESSAGE_TYPE.getValue());
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        mockers.mockEmailSendingFailure();
        Assertions.assertThrows(EmailSendException.class, () -> emailService.sendInvitationEmailToAssociatedUser("theId12345", "111111", Mono.just("Wayne Enterprises"), "Harleen Quinzel", "Elon Musk").apply("333").block());
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser( "theId12345", "111111", null, Mono.just( "Harleen Quinzel" ), "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null, "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), Mono.just("Harleen Quinzel" ), null ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), Mono.just("Harleen Quinzel" ), "Batman" ).apply( null ).block() );
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersSendsEmail() {
        final var expectedBaseEmail = new InvitationAcceptedEmailBuilder()
                .setInviterDisplayName( "Harleen Quinzel" )
                .setInviteeDisplayName( "Batman" )
                .setCompanyName( "Wayne Enterprises" );
        sendEmailFactoryMock(INVITATION_ACCEPTED_MESSAGE_TYPE.getValue());
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        emailService.sendInvitationAcceptedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), Mono.just( "Harleen Quinzel" ), "Batman" ).apply( "333" ).block();
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.invitationAcceptedEmailDataMatcher(List.of("harley.quinn@gotham.city"), expectedBaseEmail)), eq(INVITATION_ACCEPTED_MESSAGE_TYPE.getValue()));
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendException() {
        sendEmailFactoryMock(INVITATION_ACCEPTED_MESSAGE_TYPE.getValue());
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        mockers.mockEmailSendingFailure();
        Assertions.assertThrows(EmailSendException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser("theId12345", "111111", Mono.just("Wayne Enterprises"), Mono.just("Harleen Quinzel"), "Batman").apply("333").block());
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNameOrNullRequestsThrowsNullPointerException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUser( "theId12345", "111111", null, "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null ).apply( "333" ).block() );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersSendsEmail() {
        sendEmailFactoryMock(INVITATION_REJECTED_MESSAGE_TYPE.getValue());
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        emailService.sendInvitationRejectedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman" ).apply( "333" ).block();
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.invitationRejectedEmailMatcher("harley.quinn@gotham.city", "Batman", "Wayne Enterprises")), eq(INVITATION_REJECTED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendException() {
        sendEmailFactoryMock(INVITATION_REJECTED_MESSAGE_TYPE.getValue());
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        mockers.mockEmailSendingFailure();
        Assertions.assertThrows(EmailSendException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUser("theId12345", "111111", Mono.just("Wayne Enterprises"), "Batman").apply("333").block());
    }

    @Test
    void sendInviteEmailWithNullCompanyDetailsOrNullCompanyNameOrNullInviterDisplayNameOrNullInvitationExpiryTimestampOrNullInvitationLinkOrNullInviteeEmailThrowsNullPointerException(){

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", "111111", null, "Batman", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null, "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", null, "kpatel@companieshouse.gov.uk" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", "1992-05-01T10:30:00.000000", null ).block() );
    }

    @Test
    void sendInviteEmailSendsEmail() {
        sendEmailFactoryMock(INVITE_MESSAGE_TYPE.getValue());
        emailService.sendInviteEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ).block();
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.invitationAndInviteEmailDataMatcher("bruce.wayne@gotham.city", "Batman", "kpatel@companieshouse.gov.uk", "Krishna Patel", "Wayne Enterprises", COMPANY_INVITATIONS_URL)), eq(INVITE_MESSAGE_TYPE.getValue()));
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendInviteEmailWithUnexpectedIssueThrowsEmailSendException() {
        Mockito.when(sendEmailFactory.createSendEmail(Mockito.any(EmailData.class), Mockito.eq(INVITE_MESSAGE_TYPE.getValue())))
                .thenReturn(Mockito.mock(SendEmail.class));
        mockers.mockEmailSendingFailure();
        Assertions.assertThrows(EmailSendException.class, () -> emailService.sendInviteEmail("theId12345", "111111", Mono.just("Wayne Enterprises"), "Krishna Patel", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk").block());
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendInviteCancelledEmailWithoutCompanyDetailsOrCompanyNameOrCancelledByDisplayNameOrInviteeUserSupplierOrEmailThrowsNullPointerException(){
        final var association = testDataManager.fetchAssociationDaos( "34" ).getFirst();

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteCancelledEmail( "theId12345", "111111", null, "Batman", association ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteCancelledEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null, association ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteCancelledEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", null ).block() );
    }

    @Test
    void sendInviteCancelledEmailSendsEmail(){
        sendEmailFactoryMock(INVITE_CANCELLED_MESSAGE_TYPE.getValue());
        final var association = testDataManager.fetchAssociationDaos( "34" ).getFirst();
        emailService.sendInviteCancelledEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", association ).block();
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher("null", "Batman", "null", "Wayne Enterprises", "light.yagami@death.note")), eq(INVITE_CANCELLED_MESSAGE_TYPE.getValue()));
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendDelegatedRemovalOfMigratedBatchWithNullCompanyNameOrNullRemovedByOrNullRemovedUsersThrowsNullPointerException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "111" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail( "theId12345", "111111", null, "Batman", "Ronald" ).apply("111").block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail( "theId12345", "111111", Mono.just("McDonalds"), null, "Ronald" ).apply("111").block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail( "theId12345", "111111", Mono.just("McDonalds"), "Batman", null ).apply("111").block() );
    }

    @Test
    void sendDelegatedRemovalOfMigratedBatchSendsEmail() {
        sendEmailFactoryMock(DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue());
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        emailService.sendDelegatedRemovalOfMigratedBatchEmail( "theId12345", "111111", Mono.just("McDonalds"), "Batman", "Harleen Quinzel" ).apply("333").block();
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.delegatedRemovalOfMigratedBatchMatcher("harley.quinn@gotham.city", "McDonalds", "Batman", "Harleen Quinzel")), eq(DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue()));
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendDelegatedRemovalOfMigratedBatchWithUnexpectedIssueThrowsEmailSendException() {
        sendEmailFactoryMock(DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue());
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        mockers.mockEmailSendingFailure();
        Assertions.assertThrows(EmailSendException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail("theId12345", "111111", Mono.just("McDonalds"), "Batman", "Ronald").apply("333").block());
    }

    @Test
    void sendDelegatedRemovalOfMigratedWithNullCompanyNameOrNullRemovedByThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedEmail( "theId12345", "111111", null, "Batman", "bruce.wayne@gotham.city").block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedEmail( "theId12345", "111111", Mono.just("McDonalds"), null, "bruce.wayne@gotham.city" ).block() );
    }

    @Test
    void sendDelegatedRemovalOfMigratedSendsEmail() {
        sendEmailFactoryMock(DELEGATED_REMOVAL_OF_MIGRATED.getValue());
        emailService.sendDelegatedRemovalOfMigratedEmail( "theId12345", "111111", Mono.just("McDonalds"), "Batman", "harley.quinn@gotham.city" ).block();
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.delegatedRemovalOfMigratedMatcher("harley.quinn@gotham.city", "McDonalds", "Batman")), eq(DELEGATED_REMOVAL_OF_MIGRATED.getValue()));
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendDelegatedRemovalOfMigratedWithUnexpectedIssueThrowsEmailSendException() {
        sendEmailFactoryMock(DELEGATED_REMOVAL_OF_MIGRATED.getValue());
        mockers.mockEmailSendingFailure();
        Assertions.assertThrows(EmailSendException.class, () -> emailService.sendDelegatedRemovalOfMigratedEmail("theId12345", "111111", Mono.just("McDonalds"), "Batman", "harley.quinn@gotham.city").block());
    }

    @Test
    void sendRemovalOfOwnMigratedWithNullCompanyNameThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendRemoveOfOwnMigratedEmail( "theId12345", "111111", null, "111").block() );
    }

    @Test
    void sendRemovalOfOwnMigratedSendsEmail() {
        sendEmailFactoryMock(REMOVAL_OF_OWN_MIGRATED.getValue());
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        emailService.sendRemoveOfOwnMigratedEmail( "theId12345", "111111", Mono.just("McDonalds"), "333" ).block();
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.removalOfOwnMigratedMatcher("harley.quinn@gotham.city", "McDonalds")), eq(REMOVAL_OF_OWN_MIGRATED.getValue()));
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendRemovalOfOwnMigratedWithUnexpectedIssueThrowsEmailSendException() {
        sendEmailFactoryMock(REMOVAL_OF_OWN_MIGRATED.getValue());
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        mockers.mockEmailSendingFailure();
        Assertions.assertThrows(EmailSendException.class, () -> emailService.sendRemoveOfOwnMigratedEmail("theId12345", "111111", Mono.just("McDonalds"), "333").block());
    }

    @Test
    void sendReaDigitalAuthorisationAddedEmail_sendsWhenReaPresent() {
        sendEmailFactoryMock(REA_DIGITAL_AUTHORISATION_ADDED_MESSAGE_TYPE.getValue());
        Mockito.doReturn("rea@example.com").when(companyService).fetchRegisteredEmailAddress("111111");
        emailService.sendReaDigitalAuthorisationAddedEmail("theId12345", "111111", Mono.just("Test Enterprises")).block();
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.reaDigitalAuthChangedEmailMatcher("rea@example.com", "Test Enterprises", "111111")),
                eq(REA_DIGITAL_AUTHORISATION_ADDED_MESSAGE_TYPE.getValue()));
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendReaDigitalAuthorisationRemovedEmail_sendsWhenReaPresent() {
        sendEmailFactoryMock(REA_DIGITAL_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue());
        Mockito.doReturn("rea@example.com").when(companyService).fetchRegisteredEmailAddress("111111");
        emailService.sendReaDigitalAuthorisationRemovedEmail("theId12345", "111111", Mono.just("Test Enterprises")).block();
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.reaDigitalAuthChangedEmailMatcher("rea@example.com", "Test Enterprises", "111111")),
                eq(REA_DIGITAL_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue()));
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendReaDigitalAuthorisationEmails_doNothingWhenReaMissing() {
        Mockito.doReturn(null).when(companyService).fetchRegisteredEmailAddress("111111");
        emailService.sendReaDigitalAuthorisationAddedEmail("theId12345", "111111", Mono.just("Test Enterprises")).block();
        emailService.sendReaDigitalAuthorisationRemovedEmail("theId12345", "111111", Mono.just("Test Enterprises")).block();
        Mockito.verifyNoInteractions(usersService);
        Mockito.verifyNoMoreInteractions(sendEmailFactory);
        Mockito.verifyNoMoreInteractions(emailClient);

    }

    @Test
    void sendReaDigitalAuthorisationEmails_doNothingWhenReaBlank() {
        Mockito.doReturn("").when(companyService).fetchRegisteredEmailAddress("111111");
        emailService.sendReaDigitalAuthorisationAddedEmail("theId12345", "111111", Mono.just("Test Enterprises")).block();
        emailService.sendReaDigitalAuthorisationRemovedEmail("theId12345", "111111", Mono.just("Test Enterprises")).block();
        Mockito.verifyNoInteractions(usersService);
        Mockito.verifyNoMoreInteractions(sendEmailFactory);
        Mockito.verifyNoMoreInteractions(emailClient);
    }

    @Test
    void sendReaDigitalAuthorisationAddedEmail_throwsWhenEmailFails() {
        sendEmailFactoryMock(REA_DIGITAL_AUTHORISATION_ADDED_MESSAGE_TYPE.getValue());
        Mockito.doReturn("rea@example.com").when(companyService).fetchRegisteredEmailAddress("111111");
        mockers.mockEmailSendingFailure();
        final Mono<String> companyName = Mono.just("Test Enterprises");
        final Mono<Void> operation = emailService.sendReaDigitalAuthorisationAddedEmail("theId12345", "111111", companyName);
        Assertions.assertDoesNotThrow(() -> operation.block());
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.reaDigitalAuthChangedEmailMatcher("rea@example.com", "Test Enterprises", "111111")), eq(REA_DIGITAL_AUTHORISATION_ADDED_MESSAGE_TYPE.getValue())
        );
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendReaDigitalAuthorisationRemovedEmail_throwsWhenEmailServiceFails() {
        sendEmailFactoryMock(REA_DIGITAL_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue());
        Mockito.doReturn("rea@example.com").when(companyService).fetchRegisteredEmailAddress("111111");
        mockers.mockEmailSendingFailure();
        final Mono<String> companyName = Mono.just("Test Enterprises");
        final Mono<Void> operation = emailService.sendReaDigitalAuthorisationRemovedEmail("theId12345", "111111", companyName);
        Assertions.assertDoesNotThrow(() -> operation.block());
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.reaDigitalAuthChangedEmailMatcher("rea@example.com", "Test Enterprises", "111111")), eq(REA_DIGITAL_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue())
        );
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    private void sendEmailFactoryMock(String messageType) {
        Mockito.when(sendEmailFactory.createSendEmail(Mockito.any(EmailData.class), Mockito.eq(messageType)))
                .thenReturn(Mockito.mock(SendEmail.class));
    }
}
