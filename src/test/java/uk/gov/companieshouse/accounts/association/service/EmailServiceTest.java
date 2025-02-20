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
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationAcceptedEmailBuilder;
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
    void sendAuthCodeConfirmationEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNameOrNullUsersThrowsNullPointerException(){
        final var emptyCompanyDetails = new CompanyDetails();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", null, "Harleen Quinzel" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", emptyCompanyDetails, "Harleen Quinzel" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", companyDetails, null ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", companyDetails, "Harleen Quinzel" ).apply( null ).block() );
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
    void sendAuthorisationRemovedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        mockers.mockUsersServiceFetchUserDetails( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", null,"Harleen Quinzel", "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null, "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", null ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" ).apply( null ).block() );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" ).apply( "333" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.authorisationRemovedEmailMatcher( "Harleen Quinzel", "Batman", "Wayne Enterprises", "harley.quinn@gotham.city" ) ), eq( AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman").apply( "333" ).block() );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        mockers.mockUsersServiceFetchUserDetails( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", null, "Harleen Quinzel", "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null, "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", null ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" ).apply( null ).block() );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" ).apply( "333" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationCancelledEmailMatcher( "harley.quinn@gotham.city", "Harleen Quinzel", "Batman", "Wayne Enterprises" ) ), eq( INVITATION_CANCELLED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( INVITATION_CANCELLED_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just(  "Wayne Enterprises"), "Harleen Quinzel", "Batman" ).apply( "333" ).block() );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        final var emptyCompanyDetails = new CompanyDetails();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser( "theId12345", null, "Harleen Quinzel", "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser( "theId12345", emptyCompanyDetails, "Harleen Quinzel", "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser( "theId12345", companyDetails, null, "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser( "theId12345", companyDetails, "Harleen Quinzel", null ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUser( "theId12345", companyDetails, "Harleen Quinzel", "Batman" ).apply( null ).block() );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );
        emailService.sendInvitationEmailToAssociatedUser( "theId12345", companyDetails, "Harleen Quinzel", "Batman" ).apply( "333" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationAndInviteEmailDataMatcher( "harley.quinn@gotham.city", "Harleen Quinzel", "bruce.wayne@gotham.city", "Batman", "Wayne Enterprises", COMPANY_INVITATIONS_URL ) ), eq( INVITATION_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( INVITATION_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationEmailToAssociatedUser( "theId12345", companyDetails, "Harleen Quinzel", "Elon Musk" ).apply( "333" ).block() );
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        mockers.mockUsersServiceFetchUserDetails( "333" );

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
        mockers.mockUsersServiceFetchUserDetails( "333" );

        emailService.sendInvitationAcceptedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), Mono.just( "Harleen Quinzel" ), "Batman" ).apply( "333" ).block();

        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationAcceptedEmailDataMatcher( List.of( "harley.quinn@gotham.city" ), expectedBaseEmail ) ), eq( INVITATION_ACCEPTED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( INVITATION_ACCEPTED_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), Mono.just( "Harleen Quinzel" ), "Batman" ).apply( "333" ).block() );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNameOrNullRequestsThrowsNullPointerException(){
        mockers.mockUsersServiceFetchUserDetails( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUser( "theId12345", "111111", null, "Batman" ).apply( "333" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null ).apply( "333" ).block() );
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
    void sendInviteEmailWithNullCompanyDetailsOrNullCompanyNameOrNullInviterDisplayNameOrNullInvitationExpiryTimestampOrNullInvitationLinkOrNullInviteeEmailThrowsNullPointerException(){
        final var emptyCompanyDetails = new CompanyDetails();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", null, "Batman", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", emptyCompanyDetails, "Batman", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", companyDetails, null, "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", companyDetails, "Batman", null, "kpatel@companieshouse.gov.uk" ).block() );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", companyDetails, "Batman", "1992-05-01T10:30:00.000000", null ).block() );
    }

    @Test
    void sendInviteEmailThrowsEmailOnKafkaQueue(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        emailService.sendInviteEmail( "theId12345", companyDetails, "Batman", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ).block();
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationAndInviteEmailDataMatcher( "bruce.wayne@gotham.city", "Batman", "kpatel@companieshouse.gov.uk", "Krishna Patel", "Wayne Enterprises", COMPANY_INVITATIONS_URL ) ), eq( INVITE_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInviteEmailWithUnexpectedIssueThrowsEmailSendingException(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockEmailSendingFailure( INVITE_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInviteEmail( "theId12345", companyDetails, "Krishna Patel", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ).block() );
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
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.inviteCancelledEmailMatcher( "light.yagami@death.note", "Wayne Enterprises", "Batman" ) ), eq( INVITE_CANCELLED_MESSAGE_TYPE.getValue() ) );
    }

}
