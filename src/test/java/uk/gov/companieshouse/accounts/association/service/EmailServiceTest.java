package uk.gov.companieshouse.accounts.association.service;

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
import static uk.gov.companieshouse.accounts.association.utils.MessageType.REMOVAL_OF_OWN_MIGRATED;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.REA_DIGITAL_AUTHORISATION_ADDED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.REA_DIGITAL_AUTHORISATION_REMOVED_MESSAGE_TYPE;

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

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private EmailService emailService;

    private final String COMPANY_INVITATIONS_URL = "http://chs.local/your-companies/company-invitations?mtm_campaign=associations_invite";

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private Mockers mockers;

    private static final ComparisonUtils comparisonUtils = new ComparisonUtils();

    @BeforeEach
    void setup(){
        mockers = new Mockers( null, emailProducer, null, usersService );
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
    void sendAuthCodeConfirmationEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel" ).apply( "333" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.authCodeConfirmationEmailMatcher( "harley.quinn@gotham.city", "Wayne Enterprises", "Harleen Quinzel" ) ), eq( MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        mockers.mockEmailSendingFailure( AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel" ).apply( "333" ).block() );
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
    void sendAuthorisationRemovedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" ).apply( "333" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.authorisationRemovedAndYourAuthorisationRemovedEmailMatcher( "Harleen Quinzel", "Batman", "Wayne Enterprises", "harley.quinn@gotham.city", "null" ) ), eq( AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        mockers.mockEmailSendingFailure( AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman").apply( "333" ).block() );
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
    void sendInvitationCancelledEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" ).apply( "333" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher( "harley.quinn@gotham.city", "Harleen Quinzel", "Batman", "Wayne Enterprises", "null" ) ), eq( INVITATION_CANCELLED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        mockers.mockEmailSendingFailure( INVITATION_CANCELLED_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just(  "Wayne Enterprises"), "Harleen Quinzel", "Batman" ).apply( "333" ).block() );
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
    void sendInvitationEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        emailService.sendInvitationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" ).apply( "333" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationAndInviteEmailDataMatcher( "harley.quinn@gotham.city", "Harleen Quinzel", "bruce.wayne@gotham.city", "Batman", "Wayne Enterprises", COMPANY_INVITATIONS_URL ) ), eq( INVITATION_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        mockers.mockEmailSendingFailure( INVITATION_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Elon Musk" ).apply( "333" ).block() );
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
    void sendInvitationAcceptedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var expectedBaseEmail = new InvitationAcceptedEmailBuilder()
                .setInviterDisplayName( "Harleen Quinzel" )
                .setInviteeDisplayName( "Batman" )
                .setCompanyName( "Wayne Enterprises" );
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );

        emailService.sendInvitationAcceptedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), Mono.just( "Harleen Quinzel" ), "Batman" ).apply( "333" ).block();

        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationAcceptedEmailDataMatcher( List.of( "harley.quinn@gotham.city" ), expectedBaseEmail ) ), eq( INVITATION_ACCEPTED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        mockers.mockEmailSendingFailure( INVITATION_ACCEPTED_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), Mono.just( "Harleen Quinzel" ), "Batman" ).apply( "333" ).block() );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNameOrNullRequestsThrowsNullPointerException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUser( "theId12345", "111111", null, "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null ).apply( "333" ).block() );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        emailService.sendInvitationRejectedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman" ).apply( "333" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationRejectedEmailMatcher( "harley.quinn@gotham.city", "Batman", "Wayne Enterprises" ) ), eq( INVITATION_REJECTED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        mockers.mockEmailSendingFailure( INVITATION_REJECTED_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman" ).apply( "333" ).block() );
    }

    @Test
    void sendInviteEmailWithNullCompanyDetailsOrNullCompanyNameOrNullInviterDisplayNameOrNullInvitationExpiryTimestampOrNullInvitationLinkOrNullInviteeEmailThrowsNullPointerException(){

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", "111111", null, "Batman", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null, "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", null, "kpatel@companieshouse.gov.uk" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", "1992-05-01T10:30:00.000000", null ).block() );
    }

    @Test
    void sendInviteEmailThrowsEmailOnKafkaQueue(){
        emailService.sendInviteEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationAndInviteEmailDataMatcher( "bruce.wayne@gotham.city", "Batman", "kpatel@companieshouse.gov.uk", "Krishna Patel", "Wayne Enterprises", COMPANY_INVITATIONS_URL ) ), eq( INVITE_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInviteEmailWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockEmailSendingFailure( INVITE_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInviteEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Krishna Patel", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ).block() );
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
        final var association = testDataManager.fetchAssociationDaos( "34" ).getFirst();
        emailService.sendInviteCancelledEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", association ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher( "null", "Batman", "null", "Wayne Enterprises", "light.yagami@death.note"  ) ), eq( INVITE_CANCELLED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendDelegatedRemovalOfMigratedBatchWithNullCompanyNameOrNullRemovedByOrNullRemovedUsersThrowsNullPointerException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "111" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail( "theId12345", "111111", null, "Batman", "Ronald" ).apply("111").block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail( "theId12345", "111111", Mono.just("McDonalds"), null, "Ronald" ).apply("111").block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail( "theId12345", "111111", Mono.just("McDonalds"), "Batman", null ).apply("111").block() );
    }

    @Test
    void sendDelegatedRemovalOfMigratedBatchThrowsEmailOnKafkaQueue(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        emailService.sendDelegatedRemovalOfMigratedBatchEmail( "theId12345", "111111", Mono.just("McDonalds"), "Batman", "Harleen Quinzel" ).apply("333").block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.delegatedRemovalOfMigratedBatchMatcher( "harley.quinn@gotham.city", "McDonalds", "Batman" , "Harleen Quinzel") ), eq( DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue() ) );
    }

    @Test
    void sendDelegatedRemovalOfMigratedBatchWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        mockers.mockEmailSendingFailure( DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail( "theId12345", "111111", Mono.just("McDonalds"), "Batman", "Ronald" ).apply( "333" ).block() );
    }

    @Test
    void sendDelegatedRemovalOfMigratedWithNullCompanyNameOrNullRemovedByThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedEmail( "theId12345", "111111", null, "Batman", "bruce.wayne@gotham.city").block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedEmail( "theId12345", "111111", Mono.just("McDonalds"), null, "bruce.wayne@gotham.city" ).block() );
    }

    @Test
    void sendDelegatedRemovalOfMigratedThrowsEmailOnKafkaQueue(){
        emailService.sendDelegatedRemovalOfMigratedEmail( "theId12345", "111111", Mono.just("McDonalds"), "Batman", "harley.quinn@gotham.city" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.delegatedRemovalOfMigratedMatcher( "harley.quinn@gotham.city", "McDonalds", "Batman" ) ), eq( DELEGATED_REMOVAL_OF_MIGRATED.getValue() ) );
    }

    @Test
    void sendDelegatedRemovalOfMigratedWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockEmailSendingFailure( DELEGATED_REMOVAL_OF_MIGRATED.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendDelegatedRemovalOfMigratedEmail( "theId12345", "111111", Mono.just("McDonalds"), "Batman", "harley.quinn@gotham.city" ).block() );
    }

    @Test
    void sendRemovalOfOwnMigratedWithNullCompanyNameThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendRemoveOfOwnMigratedEmail( "theId12345", "111111", null, "111").block() );
    }

    @Test
    void sendRemovalOfOwnMigratedThrowsEmailOnKafkaQueue(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        emailService.sendRemoveOfOwnMigratedEmail( "theId12345", "111111", Mono.just("McDonalds"), "333" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.removalOfOwnMigratedMatcher( "harley.quinn@gotham.city", "McDonalds" ) ), eq( REMOVAL_OF_OWN_MIGRATED.getValue() ) );
    }

    @Test
    void sendRemovalOfOwnMigratedWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        mockers.mockEmailSendingFailure( REMOVAL_OF_OWN_MIGRATED.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendRemoveOfOwnMigratedEmail( "theId12345", "111111", Mono.just("McDonalds"),  "333" ).block() );
    }

    @Test
    void sendReaDigitalAuthorisationAddedEmail_sendsWhenReaPresent() {
        Mockito.doReturn("rea@example.com").when(companyService).fetchRegisteredEmailAddress("111111");
        emailService.sendReaDigitalAuthorisationAddedEmail("theId12345", "111111", Mono.just("Test Enterprises")).block();
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.reaDigitalAuthChangedEmailMatcher("rea@example.com", "Test Enterprises", "111111")),
                eq(REA_DIGITAL_AUTHORISATION_ADDED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendReaDigitalAuthorisationRemovedEmail_sendsWhenReaPresent() {
        Mockito.doReturn("rea@example.com").when(companyService).fetchRegisteredEmailAddress("111111");
        emailService.sendReaDigitalAuthorisationRemovedEmail("theId12345", "111111", Mono.just("Test Enterprises")).block();
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.reaDigitalAuthChangedEmailMatcher("rea@example.com", "Test Enterprises", "111111")),
                eq(REA_DIGITAL_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue()));
    }

    @Test
    void sendReaDigitalAuthorisationEmails_doNothingWhenReaMissing() {
        Mockito.doReturn(null).when(companyService).fetchRegisteredEmailAddress("111111");
        emailService.sendReaDigitalAuthorisationAddedEmail("theId12345", "111111", Mono.just("Test Enterprises")).block();
        emailService.sendReaDigitalAuthorisationRemovedEmail("theId12345", "111111", Mono.just("Test Enterprises")).block();
        Mockito.verifyNoInteractions(usersService);
        Mockito.verifyNoMoreInteractions(emailProducer);
    }

    @Test
    void sendReaDigitalAuthorisationEmails_doNothingWhenReaBlank() {
        Mockito.doReturn("").when(companyService).fetchRegisteredEmailAddress("111111");
        emailService.sendReaDigitalAuthorisationAddedEmail("theId12345", "111111", Mono.just("Test Enterprises")).block();
        emailService.sendReaDigitalAuthorisationRemovedEmail("theId12345", "111111", Mono.just("Test Enterprises")).block();
        Mockito.verifyNoInteractions(usersService);
        Mockito.verifyNoMoreInteractions(emailProducer);
    }

    @Test
    void sendReaDigitalAuthorisationAddedEmail_throwsWhenProducerFails() {
        Mockito.doReturn("rea@example.com").when(companyService).fetchRegisteredEmailAddress("111111");
        mockers.mockEmailSendingFailure(REA_DIGITAL_AUTHORISATION_ADDED_MESSAGE_TYPE.getValue());
        final Mono<String> companyName = Mono.just("Test Enterprises");
        final Mono<Void> operation = emailService.sendReaDigitalAuthorisationAddedEmail("theId12345", "111111", companyName);
        Assertions.assertDoesNotThrow(() -> operation.block());
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.reaDigitalAuthChangedEmailMatcher("rea@example.com", "Test Enterprises", "111111")), eq(REA_DIGITAL_AUTHORISATION_ADDED_MESSAGE_TYPE.getValue())
        );
    }

    @Test
    void sendReaDigitalAuthorisationRemovedEmail_throwsWhenProducerFails() {
        Mockito.doReturn("rea@example.com").when(companyService).fetchRegisteredEmailAddress("111111");
        mockers.mockEmailSendingFailure(REA_DIGITAL_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue());
        final Mono<String> companyName = Mono.just("Test Enterprises");
        final Mono<Void> operation = emailService.sendReaDigitalAuthorisationRemovedEmail("theId12345", "111111", companyName);
        Assertions.assertDoesNotThrow(() -> operation.block());
        Mockito.verify(emailProducer).sendEmail(argThat(comparisonUtils.reaDigitalAuthChangedEmailMatcher("rea@example.com", "Test Enterprises", "111111")), eq(REA_DIGITAL_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue())
        );
    }
}
