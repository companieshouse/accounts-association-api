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
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationAcceptedEmailData;
import uk.gov.companieshouse.accounts.association.utils.MessageType;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.*;

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
    void setup(){
        mockers = new Mockers( null, emailProducer, null, usersService );
        ReflectionTestUtils.setField(emailService, "invitationLink", COMPANY_INVITATIONS_URL);
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );
        emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", companyDetails, "Harleen Quinzel" ).apply( "333" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.authCodeConfirmationEmailMatcher( "harley.quinn@gotham.city", "Wayne Enterprises", "Harleen Quinzel" ) ), eq( MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", companyDetails, "Harleen Quinzel" ).apply( "333" ).block() );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" ).apply( "333" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.authorisationRemovedAndYourAuthorisationRemovedEmailMatcher( "Harleen Quinzel", "Batman", "Wayne Enterprises", "harley.quinn@gotham.city", "null" ) ), eq( AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman").apply( "333" ).block() );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" ).apply( "333" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher( "harley.quinn@gotham.city", "Harleen Quinzel", "Batman", "Wayne Enterprises", "null" ) ), eq( INVITATION_CANCELLED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( INVITATION_CANCELLED_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just(  "Wayne Enterprises"), "Harleen Quinzel", "Batman" ).apply( "333" ).block() );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        emailService.sendInvitationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman").apply( "333" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationAndInviteEmailDataMatcher( "harley.quinn@gotham.city", "Harleen Quinzel", "bruce.wayne@gotham.city", "Batman", "Wayne Enterprises", COMPANY_INVITATIONS_URL ) ), eq( INVITATION_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( INVITATION_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Elon Musk" ).apply( "333" ).block() );
    }

//    //TODO: test failing as authorisedPerson isnt getting mapped in sendInvitationAcceptedEmailToAssociatedUser
//    @Test
//    void sendInvitationAcceptedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
//        final var expectedBaseEmail = new InvitationAcceptedEmailData()
//                .to( "harley.quinn@gotham.city" )
//                .authorisedPerson( "Harleen Quinzel" )
//                .personWhoCreatedInvite( "Batman" )
//                .companyName( "Wayne Enterprises" );
//        mockers.mockUsersServiceFetchUserDetails( "333" );
//
//        emailService.sendInvitationAcceptedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), Mono.just( "Harleen Quinzel" ), "Batman" ).apply( "333" ).block();
//
//        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationAcceptedEmailDataMatcher( List.of( "Harleen Quinzel" ), expectedBaseEmail ) ), eq( INVITATION_ACCEPTED_MESSAGE_TYPE.getValue() ) );
//    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( INVITATION_ACCEPTED_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), Mono.just( "Harleen Quinzel" ), "Batman" ).apply( "333" ).block() );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        emailService.sendInvitationRejectedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman" ).apply( "333" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationRejectedEmailMatcher( "harley.quinn@gotham.city", "Batman", "Wayne Enterprises" ) ), eq( INVITATION_REJECTED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( INVITATION_REJECTED_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman" ).apply( "333" ).block() );
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
    void sendInviteCancelledEmailSendsEmail(){
        final var association = testDataManager.fetchAssociationDaos( "34" ).getFirst();
        emailService.sendInviteCancelledEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", association ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationCancelledAndInviteCancelledEmailMatcher( "light.yagami@death.note", "null", "null", "Wayne Enterprises", "Batman" )  ), eq( INVITE_CANCELLED_MESSAGE_TYPE.getValue() ) );
    }

    // TODO: is this test needed
//    @Test
//    void sendDelegatedRemovalOfMigratedBatchWithNullCompanyNameThrowsNullPointerException() {
//        mockers.mockUsersServiceFetchUserDetails("111");
//        Assertions.assertThrows(NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail("theId12345", "111111", null, "Batman", "Ronald").apply("111").block());
//    }

    @Test
    void sendDelegatedRemovalOfMigratedBatchThrowsEmailOnKafkaQueue(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        emailService.sendDelegatedRemovalOfMigratedBatchEmail( "theId12345", "111111", Mono.just("McDonalds"), "Batman", "Harleen Quinzel" ).apply("333").block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.delegatedRemovalOfMigratedBatchMatcher( "harley.quinn@gotham.city", "McDonalds", "Batman" , "Harleen Quinzel") ), eq( DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue() ) );
    }

    @Test
    void sendDelegatedRemovalOfMigratedBatchWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendDelegatedRemovalOfMigratedBatchEmail( "theId12345", "111111", Mono.just("McDonalds"), "Batman", "Ronald" ).apply( "333" ).block() );
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
        mockers.mockUsersServiceFetchUserDetails( "333" );
        emailService.sendRemoveOfOwnMigratedEmail( "theId12345", "111111", Mono.just("McDonalds"), "333" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.removalOfOwnMigratedMatcher( "harley.quinn@gotham.city", "McDonalds" ) ), eq( REMOVAL_OF_OWN_MIGRATED.getValue() ) );
    }

    @Test
    void sendRemovalOfOwnMigratedWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( REMOVAL_OF_OWN_MIGRATED.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendRemoveOfOwnMigratedEmail( "theId12345", "111111", Mono.just("McDonalds"),  "333" ).block() );
    }
}
