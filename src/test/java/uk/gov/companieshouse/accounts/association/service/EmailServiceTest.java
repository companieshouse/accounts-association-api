package uk.gov.companieshouse.accounts.association.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.client.EmailClient;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.EmailSendException;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.factory.SendEmailFactory;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.models.context.RequestContext;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData.RequestContextDataBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationAcceptedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.data.EmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationAcceptedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InviteEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.YourAuthorisationRemovedEmailData;
import uk.gov.companieshouse.accounts.association.utils.MessageType;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.chskafka.SendEmail;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_UPDATE_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.models.Constants.DEFAULT_DISPLAY_NAME;
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
import static uk.gov.companieshouse.accounts.association.utils.MessageType.YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.REMOVED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.UNAUTHORISED;

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

    @Mock
    private AssociationsService associationsService;

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

    @AfterEach
    void tearDown(){
        RequestContext.clear();
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNameOrNullUsersThrowsNullPointerException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        final var withNullCompanyName = emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", "111111", null, "Harleen Quinzel" );
        final var withNullDisplayName = emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null ).apply( "333" );
        final var withValidArguments = emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel" );

        Assertions.assertThrows( NullPointerException.class, () -> withNullCompanyName.apply( "333" ) );
        Assertions.assertThrows( NullPointerException.class, withNullDisplayName::block );
        Assertions.assertThrows( NullPointerException.class, () -> withValidArguments.apply( null ) );
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
        final var operation = emailService.sendAuthCodeConfirmationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel" ).apply( "333" );
        Assertions.assertThrows( EmailSendException.class, operation::block );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );

        final var withNullCompanyName = emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", null, "Harleen Quinzel", "Batman" );
        final var withNullRemovedByDisplayName = emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null, "Batman" ).apply( "333" );
        final var withNullRemovedUserDisplayName = emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", null ).apply( "333" );
        final var withValidArguments = emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" );

        Assertions.assertThrows( NullPointerException.class, () -> withNullCompanyName.apply( "333" ) );
        Assertions.assertThrows( NullPointerException.class, withNullRemovedByDisplayName::block );
        Assertions.assertThrows( NullPointerException.class, withNullRemovedUserDisplayName::block );
        Assertions.assertThrows( NullPointerException.class, () -> withValidArguments.apply( null ) );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersSkipsNotFoundUserAndStillEmailsTheRest() {
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333", "444" );
        mockers.mockUsersServiceToFetchUserDetailsRequestNotFound( "404User" );
        sendEmailFactoryMock(AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue());

        Flux.just( "333", "404User", "444" )
                .flatMap( emailService.sendAuthorisationRemovedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" ) )
                .blockLast();

        Mockito.verify(sendEmailFactory, Mockito.times(2)).createSendEmail(Mockito.any(EmailData.class), eq(AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue()));
        Mockito.verify(emailClient, Mockito.times(2)).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
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
        final var operation = emailService.sendAuthorisationRemovedEmailToAssociatedUser("theId12345", "111111", Mono.just("Wayne Enterprises"), "Harleen Quinzel", "Batman").apply("333");
        Assertions.assertThrows( EmailSendException.class, operation::block );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );

        final var withNullCompanyName = emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", null, "Harleen Quinzel", "Batman" );
        final var withNullCancelledByDisplayName = emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null, "Batman" ).apply( "333" );
        final var withNullCancelledUserDisplayName = emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", null ).apply( "333" );
        final var withValidArguments = emailService.sendInvitationCancelledEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" );

        Assertions.assertThrows( NullPointerException.class, () -> withNullCompanyName.apply( "333" ) );
        Assertions.assertThrows( NullPointerException.class, withNullCancelledByDisplayName::block );
        Assertions.assertThrows( NullPointerException.class, withNullCancelledUserDisplayName::block );
        Assertions.assertThrows( NullPointerException.class, () -> withValidArguments.apply( null ) );
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
        final var operation = emailService.sendInvitationCancelledEmailToAssociatedUser("theId12345", "111111", Mono.just("Wayne Enterprises"), "Harleen Quinzel", "Batman").apply("333");
        Assertions.assertThrows( EmailSendException.class, operation::block );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );

        final var withNullCompanyName = emailService.sendInvitationEmailToAssociatedUser( "theId12345", "111111", null, "Harleen Quinzel", "Batman" );
        final var withNullInviterDisplayName = emailService.sendInvitationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null, "Batman" ).apply( "333" );
        final var withNullInviteeDisplayName = emailService.sendInvitationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", null ).apply( "333" );
        final var withValidArguments = emailService.sendInvitationEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Harleen Quinzel", "Batman" );

        Assertions.assertThrows( NullPointerException.class, () -> withNullCompanyName.apply( "333" ) );
        Assertions.assertThrows( NullPointerException.class, withNullInviterDisplayName::block );
        Assertions.assertThrows( NullPointerException.class, withNullInviteeDisplayName::block );
        Assertions.assertThrows( NullPointerException.class, () -> withValidArguments.apply( null ) );
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
        final var operation = emailService.sendInvitationEmailToAssociatedUser("theId12345", "111111", Mono.just("Wayne Enterprises"), "Harleen Quinzel", "Elon Musk").apply("333");
        Assertions.assertThrows( EmailSendException.class, operation::block );
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );

        final var withNullCompanyName = emailService.sendInvitationAcceptedEmailToAssociatedUser( "theId12345", "111111", null, Mono.just( "Harleen Quinzel" ), "Batman" ).apply( "333" );
        final var withNullInviterDisplayName = emailService.sendInvitationAcceptedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null, "Batman" );
        final var withNullInviteeDisplayName = emailService.sendInvitationAcceptedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), Mono.just( "Harleen Quinzel" ), null ).apply( "333" );
        final var withValidArguments = emailService.sendInvitationAcceptedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), Mono.just( "Harleen Quinzel" ), "Batman" );

        Assertions.assertThrows( NullPointerException.class, withNullCompanyName::block );
        Assertions.assertThrows( NullPointerException.class, () -> withNullInviterDisplayName.apply( "333" ) );
        Assertions.assertThrows( NullPointerException.class, withNullInviteeDisplayName::block );
        Assertions.assertThrows( NullPointerException.class, () -> withValidArguments.apply( null ) );
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
        final var operation = emailService.sendInvitationAcceptedEmailToAssociatedUser("theId12345", "111111", Mono.just("Wayne Enterprises"), Mono.just("Harleen Quinzel"), "Batman").apply("333");
        Assertions.assertThrows( EmailSendException.class, operation::block );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNameOrNullRequestsThrowsNullPointerException(){
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        final var withNullCompanyName = emailService.sendInvitationRejectedEmailToAssociatedUser( "theId12345", "111111", null, "Batman" );
        final var withNullInviteeDisplayName = emailService.sendInvitationRejectedEmailToAssociatedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null ).apply( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> withNullCompanyName.apply( "333" ) );
        Assertions.assertThrows( NullPointerException.class, withNullInviteeDisplayName::block );
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
        final var operation = emailService.sendInvitationRejectedEmailToAssociatedUser("theId12345", "111111", Mono.just("Wayne Enterprises"), "Batman").apply("333");
        Assertions.assertThrows( EmailSendException.class, operation::block );
    }

    @Test
    void sendInviteEmailWithNullCompanyDetailsOrNullCompanyNameOrNullInviterDisplayNameOrNullInvitationExpiryTimestampOrNullInvitationLinkOrNullInviteeEmailThrowsNullPointerException(){
        final var withNullInviterDisplayName = emailService.sendInviteEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), null, "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" );
        final var withNullInvitationExpiryTimestamp = emailService.sendInviteEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", null, "kpatel@companieshouse.gov.uk" );
        final var withNullInviteeEmail = emailService.sendInviteEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", "1992-05-01T10:30:00.000000", null );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", "111111", null, "Batman", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, withNullInviterDisplayName::block );
        Assertions.assertThrows( NullPointerException.class, withNullInvitationExpiryTimestamp::block );
        Assertions.assertThrows( NullPointerException.class, withNullInviteeEmail::block );
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
        final var operation = emailService.sendInviteEmail("theId12345", "111111", Mono.just("Wayne Enterprises"), "Krishna Patel", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk");
        Assertions.assertThrows( EmailSendException.class, operation::block );
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendInviteCancelledEmailWithoutCompanyDetailsOrCompanyNameOrCancelledByDisplayNameOrInviteeUserSupplierOrEmailThrowsNullPointerException(){
        final var association = testDataManager.fetchAssociationDaos( "34" ).getFirst();

        final var companyName = Mono.just( "Wayne Enterprises" );
        final var withNullCancelledByDisplayName = emailService.sendInviteCancelledEmail( "theId12345", "111111", companyName, null, association );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteCancelledEmail( "theId12345", "111111", null, "Batman", association ) );
        Assertions.assertThrows( NullPointerException.class, withNullCancelledByDisplayName::block );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteCancelledEmail( "theId12345", "111111", companyName, "Batman", null ) );
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

        final var withNullCompanyName = emailService.sendDelegatedRemovalOfMigratedBatchEmail( "theId12345", "111111", null, "Batman", "Ronald" );
        final var withNullRemovedBy = emailService.sendDelegatedRemovalOfMigratedBatchEmail( "theId12345", "111111", Mono.just( "McDonalds" ), null, "Ronald" ).apply( "111" );
        final var withNullRemovedUser = emailService.sendDelegatedRemovalOfMigratedBatchEmail( "theId12345", "111111", Mono.just( "McDonalds" ), "Batman", null ).apply( "111" );

        Assertions.assertThrows( NullPointerException.class, () -> withNullCompanyName.apply( "111" ) );
        Assertions.assertThrows( NullPointerException.class, withNullRemovedBy::block );
        Assertions.assertThrows( NullPointerException.class, withNullRemovedUser::block );
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
        final var operation = emailService.sendDelegatedRemovalOfMigratedBatchEmail("theId12345", "111111", Mono.just("McDonalds"), "Batman", "Ronald").apply("333");
        Assertions.assertThrows( EmailSendException.class, operation::block );
    }

    @Test
    void sendDelegatedRemovalOfMigratedWithNullCompanyNameOrNullRemovedByThrowsNullPointerException(){
        final var withNullRemovedBy = emailService.sendDelegatedRemovalOfMigratedEmail( "theId12345", "111111", Mono.just( "McDonalds" ), null, "bruce.wayne@gotham.city" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendDelegatedRemovalOfMigratedEmail( "theId12345", "111111", null, "Batman", "bruce.wayne@gotham.city" ) );
        Assertions.assertThrows( NullPointerException.class, withNullRemovedBy::block );
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
        final var operation = emailService.sendDelegatedRemovalOfMigratedEmail("theId12345", "111111", Mono.just("McDonalds"), "Batman", "harley.quinn@gotham.city");
        Assertions.assertThrows( EmailSendException.class, operation::block );
    }

    @Test
    void sendRemovalOfOwnMigratedWithNullCompanyNameThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendRemoveOfOwnMigratedEmail( "theId12345", "111111", null, "111" ) );
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
        final var operation = emailService.sendRemoveOfOwnMigratedEmail("theId12345", "111111", Mono.just("McDonalds"), "333");
        Assertions.assertThrows( EmailSendException.class, operation::block );
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
        Assertions.assertDoesNotThrow( (Executable) operation::block );
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
        Assertions.assertDoesNotThrow( (Executable) operation::block );
        Mockito.verify(sendEmailFactory).createSendEmail(argThat(comparisonUtils.reaDigitalAuthChangedEmailMatcher("rea@example.com", "Test Enterprises", "111111")), eq(REA_DIGITAL_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue())
        );
        Mockito.verify(emailClient).sendEmail(Mockito.any(SendEmail.class), eq("theId12345"));
    }

    @Test
    void sendReaDigitalAuthorisationEmails_doNothingWhenReaNotFound() {
        Mockito.doThrow( new NotFoundRuntimeException( "theId12345", "Not found", new Exception( "Not found" ) ) ).when( companyService ).fetchRegisteredEmailAddress( "111111" );
        emailService.sendReaDigitalAuthorisationAddedEmail( "theId12345", "111111", Mono.just( "Test Enterprises" ) ).block();
        Mockito.verifyNoInteractions( sendEmailFactory, emailClient );
    }

    @Test
    void sendReaDigitalAuthorisationEmails_doNothingWhenReaLookupFails() {
        Mockito.doThrow( new InternalServerErrorRuntimeException( "theId12345", "Failed", new Exception( "Failed" ) ) ).when( companyService ).fetchRegisteredEmailAddress( "111111" );
        emailService.sendReaDigitalAuthorisationRemovedEmail( "theId12345", "111111", Mono.just( "Test Enterprises" ) ).block();
        Mockito.verifyNoInteractions( sendEmailFactory, emailClient );
    }

    @Test
    void sendInviteCancelledEmailFetchesRecipientWhenAssociationHasUserId() {
        sendEmailFactoryMock( INVITE_CANCELLED_MESSAGE_TYPE.getValue() );
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        final var association = new AssociationDao().companyNumber( "111111" ).userId( "333" ).userEmail( "other@gotham.city" );
        emailService.sendInviteCancelledEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", association ).block();
        Assertions.assertEquals( List.of( "harley.quinn@gotham.city" ), captureEmailData().stream().map( EmailData::getTo ).toList() );
    }

    @Test
    void sendInviteCancelledEmailFallsBackToStoredEmailWhenRecipientNotFound() {
        sendEmailFactoryMock( INVITE_CANCELLED_MESSAGE_TYPE.getValue() );
        mockers.mockUsersServiceToFetchUserDetailsRequestNotFound( "333" );
        final var association = new AssociationDao().companyNumber( "111111" ).userId( "333" ).userEmail( "other@gotham.city" );
        emailService.sendInviteCancelledEmail( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", association ).block();
        Assertions.assertEquals( List.of( "other@gotham.city" ), captureEmailData().stream().map( EmailData::getTo ).toList() );
    }

    @Test
    void sendAuthorisationRemovedEmailToRemovedUserSendsEmail() {
        sendEmailFactoryMock( YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() );
        mockers.mockUsersServiceToFetchUserDetailsRequest( "333" );
        emailService.sendAuthorisationRemovedEmailToRemovedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", "333" ).block();
        final var emailData = (YourAuthorisationRemovedEmailData) captureEmailData().getFirst();
        Assertions.assertEquals( "harley.quinn@gotham.city", emailData.getTo() );
        Assertions.assertEquals( "Batman", emailData.getPersonWhoRemovedAuthorisation() );
        Assertions.assertEquals( "Wayne Enterprises", emailData.getCompanyName() );
    }

    @Test
    void sendAuthorisationRemovedEmailToRemovedUserSkipsEmailWhenRecipientNotFound() {
        mockers.mockUsersServiceToFetchUserDetailsRequestNotFound( "333" );
        emailService.sendAuthorisationRemovedEmailToRemovedUser( "theId12345", "111111", Mono.just( "Wayne Enterprises" ), "Batman", "333" ).block();
        Mockito.verifyNoInteractions( sendEmailFactory, emailClient );
    }

    @Test
    void sendStatusUpdateEmailsWhenUserRejectsOwnInvitationNotifiesAssociatedUsers() {
        mockStatusUpdateDependencies();
        final var association = association( "333", "harley.quinn@gotham.city", AWAITING_APPROVAL );
        emailService.sendStatusUpdateEmails( association, user( "333" ), REMOVED, oauth2Context( "333" ) );
        Assertions.assertEquals( List.of( INVITATION_REJECTED_MESSAGE_TYPE.getValue() ), captureMessageTypes() );
    }

    @Test
    void sendStatusUpdateEmailsWhenUserCancelsAnotherUsersInvitationNotifiesInviteeAndAssociatedUsers() {
        mockStatusUpdateDependencies();
        final var association = association( "333", "harley.quinn@gotham.city", AWAITING_APPROVAL );
        emailService.sendStatusUpdateEmails( association, user( "333" ), REMOVED, oauth2Context( "111" ) );
        Assertions.assertEquals( List.of( INVITE_CANCELLED_MESSAGE_TYPE.getValue(), INVITATION_CANCELLED_MESSAGE_TYPE.getValue() ), captureMessageTypes() );
    }

    @Test
    void sendStatusUpdateEmailsWhenConfirmedAssociationIsRemovedNotifiesRemovedUserAssociatedUsersAndRea() {
        mockStatusUpdateDependencies();
        final var association = association( "333", "harley.quinn@gotham.city", CONFIRMED );
        emailService.sendStatusUpdateEmails( association, user( "333" ), REMOVED, oauth2Context( "111" ) );
        Assertions.assertEquals( List.of( YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue(), AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue(), REA_DIGITAL_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() ), captureMessageTypes() );
    }

    @Test
    void sendStatusUpdateEmailsUsesCompaniesHouseAsDisplayNameForAdminUpdates() {
        mockStatusUpdateDependencies();
        final var association = association( "333", "harley.quinn@gotham.city", CONFIRMED );
        emailService.sendStatusUpdateEmails( association, user( "333" ), REMOVED, context( "oauth2", "111", ADMIN_UPDATE_PERMISSION ) );
        final var removedUserEmail = (YourAuthorisationRemovedEmailData) captureEmailData().getFirst();
        Assertions.assertEquals( COMPANIES_HOUSE, removedUserEmail.getPersonWhoRemovedAuthorisation() );
    }

    @Test
    void sendStatusUpdateEmailsWhenUserAcceptsInvitationNotifiesAssociatedUsersAndReaWithLatestInviterName() {
        mockStatusUpdateDependencies();
        Mockito.doReturn( user( "222" ) ).when( usersService ).fetchUserDetails( "222", "theId12345" );
        final var association = association( null, "harley.quinn@gotham.city", AWAITING_APPROVAL ).invitations( List.of(
                new InvitationDao().invitedBy( "111" ).invitedAt( LocalDateTime.now().minusDays( 2 ) ),
                new InvitationDao().invitedBy( "222" ).invitedAt( LocalDateTime.now().minusDays( 1 ) ),
                new InvitationDao().invitedBy( "111" ).invitedAt( LocalDateTime.now().minusDays( 3 ) ) ) );
        emailService.sendStatusUpdateEmails( association, user( "333" ), CONFIRMED, oauth2Context( "333" ) );

        final var emails = captureEmailData();
        Assertions.assertEquals( List.of( INVITATION_ACCEPTED_MESSAGE_TYPE.getValue(), REA_DIGITAL_AUTHORISATION_ADDED_MESSAGE_TYPE.getValue() ), captureMessageTypes() );
        Assertions.assertEquals( "the.joker@gotham.city", ( (InvitationAcceptedEmailData) emails.getFirst() ).getPersonWhoCreatedInvite() );
        Assertions.assertEquals( "Harleen Quinzel", ( (InvitationAcceptedEmailData) emails.getFirst() ).getAuthorisedPerson() );
    }

    @Test
    void sendStatusUpdateEmailsWhenUserAcceptsInvitationUsesDefaultInviterNameWhenInviterNotFound() {
        mockStatusUpdateDependencies();
        mockers.mockUsersServiceFetchUserDetailsNotFound( "111" );
        final var association = association( "333", "harley.quinn@gotham.city", AWAITING_APPROVAL )
                .invitations( List.of( new InvitationDao().invitedBy( "111" ).invitedAt( LocalDateTime.now() ) ) );
        emailService.sendStatusUpdateEmails( association, user( "333" ), CONFIRMED, oauth2Context( "333" ) );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, ( (InvitationAcceptedEmailData) captureEmailData().getFirst() ).getPersonWhoCreatedInvite() );
    }

    @Test
    void sendStatusUpdateEmailsWhenUserRemovesOwnMigratedAssociationNotifiesUserAndAssociatedUsers() {
        mockStatusUpdateDependencies();
        final var association = association( "333", "harley.quinn@gotham.city", MIGRATED );
        emailService.sendStatusUpdateEmails( association, user( "333" ), REMOVED, oauth2Context( "333" ) );
        Assertions.assertEquals( List.of( REMOVAL_OF_OWN_MIGRATED.getValue(), DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue() ), captureMessageTypes() );
    }

    @Test
    void sendStatusUpdateEmailsWhenUserRemovesAnotherUsersMigratedAssociationNotifiesUserAndAssociatedUsers() {
        mockStatusUpdateDependencies();
        final var association = association( "333", "harley.quinn@gotham.city", MIGRATED );
        emailService.sendStatusUpdateEmails( association, user( "333" ), REMOVED, oauth2Context( "111" ) );
        Assertions.assertEquals( List.of( DELEGATED_REMOVAL_OF_MIGRATED.getValue(), DELEGATED_REMOVAL_OF_MIGRATED_BATCH.getValue() ), captureMessageTypes() );
    }

    @Test
    void sendStatusUpdateEmailsWhenMigratedUserIsInvitedNotifiesInviteeAndAssociatedUsers() {
        mockStatusUpdateDependencies();
        final var association = association( "333", "harley.quinn@gotham.city", MIGRATED );
        emailService.sendStatusUpdateEmails( association, user( "333" ), CONFIRMED, oauth2Context( "111" ) );
        Assertions.assertEquals( List.of( INVITE_MESSAGE_TYPE.getValue(), INVITATION_MESSAGE_TYPE.getValue() ), captureMessageTypes() );
    }

    @Test
    void sendStatusUpdateEmailsWhenUnauthorisedUserWithoutAccountIsInvitedSendsInviteToStoredEmail() {
        mockStatusUpdateDependencies();
        final var association = association( null, "new.user@gotham.city", UNAUTHORISED );
        emailService.sendStatusUpdateEmails( association, null, CONFIRMED, oauth2Context( "111" ) );

        final var emails = captureEmailData();
        Assertions.assertEquals( List.of( INVITE_MESSAGE_TYPE.getValue(), INVITATION_MESSAGE_TYPE.getValue() ), captureMessageTypes() );
        Assertions.assertEquals( "new.user@gotham.city", emails.getFirst().getTo() );
        Assertions.assertEquals( "Batman", ( (InviteEmailData) emails.getFirst() ).getInviterDisplayName() );
    }

    @Test
    void sendStatusUpdateEmailsWhenUnauthorisedAssociationIsRemovedSendsNoEmails() {
        mockStatusUpdateDependencies();
        final var association = association( "333", "harley.quinn@gotham.city", UNAUTHORISED );
        emailService.sendStatusUpdateEmails( association, user( "333" ), REMOVED, oauth2Context( "111" ) );
        Mockito.verifyNoInteractions( sendEmailFactory, emailClient );
    }

    @ParameterizedTest
    @CsvSource( {
            "confirmed, unauthorised, 111",
            "awaiting-approval, unauthorised, 333",
            "awaiting-approval, confirmed, 111",
            "unauthorised, confirmed, 333"
    } )
    void sendStatusUpdateEmailsForStatusChangesWithoutNotificationsSendsNoEmails( final String oldStatus, final String newStatus, final String requestingUserId ) {
        mockStatusUpdateDependencies();
        final var association = association( "333", "harley.quinn@gotham.city", StatusEnum.fromValue( oldStatus ) );
        emailService.sendStatusUpdateEmails( association, user( "333" ), StatusEnum.fromValue( newStatus ), oauth2Context( requestingUserId ) );
        Mockito.verifyNoInteractions( sendEmailFactory, emailClient );
    }

    @Test
    void sendStatusUpdateEmailsWhenUserConfirmsWithAuthCodeNotifiesAssociatedUsersAndRea() {
        mockStatusUpdateDependencies();
        final var association = association( "333", "harley.quinn@gotham.city", MIGRATED );
        emailService.sendStatusUpdateEmails( association, user( "333" ), CONFIRMED, context( "key", "abc", null ) );
        Assertions.assertEquals( List.of( AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue(), REA_DIGITAL_AUTHORISATION_ADDED_MESSAGE_TYPE.getValue() ), captureMessageTypes() );
    }

    @Test
    void sendStatusUpdateEmailsWithApiKeyWhenAssociationIsNotBeingAuthorisedSendsNoEmails() {
        mockStatusUpdateDependencies();
        final var association = association( "333", "harley.quinn@gotham.city", CONFIRMED );
        emailService.sendStatusUpdateEmails( association, user( "333" ), REMOVED, context( "key", "abc", null ) );
        Mockito.verifyNoInteractions( sendEmailFactory, emailClient );
    }

    @Test
    void sendStatusUpdateEmailsWithUnsupportedIdentityTypeSendsNoEmails() {
        mockStatusUpdateDependencies();
        final var association = association( "333", "harley.quinn@gotham.city", CONFIRMED );
        emailService.sendStatusUpdateEmails( association, user( "333" ), REMOVED, context( "unknown", "111", null ) );
        Mockito.verifyNoInteractions( sendEmailFactory, emailClient );
    }

    @Test
    void sendStatusUpdateEmailsSkipsAssociatedUsersThatCannotBeFound() {
        mockStatusUpdateDependencies();
        Mockito.lenient().doReturn( Flux.just( "111", "999" ) ).when( associationsService ).fetchConfirmedUserIds( "111111" );
        mockers.mockUsersServiceToFetchUserDetailsRequestNotFound( "999" );
        final var association = association( "333", "harley.quinn@gotham.city", AWAITING_APPROVAL );
        emailService.sendStatusUpdateEmails( association, user( "333" ), REMOVED, oauth2Context( "333" ) );
        Assertions.assertEquals( List.of( "bruce.wayne@gotham.city" ), captureEmailData().stream().map( EmailData::getTo ).toList() );
    }

    private void mockStatusUpdateDependencies() {
        Mockito.lenient().doReturn( testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst() ).when( companyService ).fetchCompanyProfile( "111111" );
        Mockito.lenient().doReturn( "rea@example.com" ).when( companyService ).fetchRegisteredEmailAddress( "111111" );
        Mockito.lenient().doReturn( Flux.just( "111" ) ).when( associationsService ).fetchConfirmedUserIds( "111111" );
        Mockito.lenient().doAnswer( invocation -> Mono.just( user( invocation.getArgument( 0 ) ) ) ).when( usersService ).toFetchUserDetailsRequest( Mockito.anyString(), Mockito.any() );
        Mockito.lenient().doReturn( Mockito.mock( SendEmail.class ) ).when( sendEmailFactory ).createSendEmail( Mockito.any( EmailData.class ), Mockito.anyString() );
    }

    private static User user( final String userId ) {
        return testDataManager.fetchUserDtos( userId ).getFirst();
    }

    private static AssociationDao association( final String userId, final String userEmail, final StatusEnum status ) {
        return new AssociationDao().companyNumber( "111111" ).userId( userId ).userEmail( userEmail ).status( status.getValue() );
    }

    private static RequestContextData oauth2Context( final String ericIdentity ) {
        return context( "oauth2", ericIdentity, null );
    }

    private static RequestContextData context( final String identityType, final String ericIdentity, final String adminRoles ) {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId12345" );
        request.addHeader( "Eric-Identity", ericIdentity );
        request.addHeader( "Eric-Identity-Type", identityType );
        if ( adminRoles != null ) {
            request.addHeader( "Eric-Authorised-Roles", adminRoles );
        }
        final var requestingUser = "key".equals( identityType ) ? null : user( ericIdentity );
        return new RequestContextDataBuilder()
                .setXRequestId( request )
                .setEricIdentity( request )
                .setEricIdentityType( request )
                .setAdminPrivileges( request )
                .setUser( requestingUser )
                .build();
    }

    private List<String> captureMessageTypes() {
        final var messageTypes = ArgumentCaptor.forClass( String.class );
        Mockito.verify( sendEmailFactory, Mockito.atLeastOnce() ).createSendEmail( Mockito.any( EmailData.class ), messageTypes.capture() );
        return messageTypes.getAllValues();
    }

    private List<EmailData> captureEmailData() {
        final var emailData = ArgumentCaptor.forClass( EmailData.class );
        Mockito.verify( sendEmailFactory, Mockito.atLeastOnce() ).createSendEmail( emailData.capture(), Mockito.anyString() );
        return emailData.getAllValues();
    }

    private void sendEmailFactoryMock(String messageType) {
        Mockito.when(sendEmailFactory.createSendEmail(Mockito.any(EmailData.class), Mockito.eq(messageType)))
                .thenReturn(Mockito.mock(SendEmail.class));
    }
}
