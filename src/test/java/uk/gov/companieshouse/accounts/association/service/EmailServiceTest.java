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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationAcceptedEmailBuilder;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.utils.MessageType;
import uk.gov.companieshouse.api.accounts.user.model.User;
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
    private AssociationsRepository associationsRepository;

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

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( "theId12345", null, "Harleen Quinzel", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( "theId12345", emptyCompanyDetails, "Harleen Quinzel", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( "theId12345", companyDetails, null, List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", null ) );
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );
        emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", List.of("333") );
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.authCodeConfirmationEmailMatcher( "harley.quinn@gotham.city", "Wayne Enterprises", "Harleen Quinzel" ) ), eq( MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersDoesNotThrowEmailOnKafkaQueue(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", List.of() );
        Mockito.verify( emailProducer, Mockito.never() ).sendEmail( argThat( comparisonUtils.authCodeConfirmationEmailMatcher( "kpatel@companieshouse.gov.uk", "Wayne Enterprises", "Krishna Patel" ) ), eq( AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", List.of("333") ) );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        final var emptyCompanyDetails = new CompanyDetails();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", null, "Harleen Quinzel", "Batman", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", emptyCompanyDetails, "Harleen Quinzel", "Batman", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", companyDetails, null, "Batman", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", null, List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", "Batman", null ) );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );
        emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", "Batman", List.of("333") );
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.authorisationRemovedEmailMatcher( "Harleen Quinzel", "Batman", "Wayne Enterprises", "harley.quinn@gotham.city" ) ), eq( AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersDoesNotThrowEmailOnKafkaQueue(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Batman", List.of() );
        Mockito.verify( emailProducer, Mockito.never() ).sendEmail( argThat( comparisonUtils.authorisationRemovedEmailMatcher( "Krishna Patel", "Batman", "Wayne Enterprises", "kpatel@companieshouse.gov.uk" ) ), eq( AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", "Batman", List.of("333") ) );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        final var emptyCompanyDetails = new CompanyDetails();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", null, "Harleen Quinzel", "Batman", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", emptyCompanyDetails, "Harleen Quinzel", "Batman", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", companyDetails, null, "Batman", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", null, List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", "Batman", null ) );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );
        emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", "Batman", List.of("333") );
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationCancelledEmailMatcher( "harley.quinn@gotham.city", "Harleen Quinzel", "Batman", "Wayne Enterprises" ) ), eq( INVITATION_CANCELLED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersDoesNotThrowEmailOnKafkaQueue(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Batman", List.of() );
        Mockito.verify( emailProducer, Mockito.never() ).sendEmail( argThat( comparisonUtils.invitationCancelledEmailMatcher( "kpatel@companieshouse.gov.uk", "Krishna Patel", "Batman", "Wayne Enterprises" ) ), eq( INVITATION_CANCELLED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( INVITATION_CANCELLED_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", "Batman", List.of("333") ) );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        final var emptyCompanyDetails = new CompanyDetails();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUsers( "theId12345", null, "Harleen Quinzel", "Batman", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUsers( "theId12345", emptyCompanyDetails, "Harleen Quinzel", "Batman", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUsers( "theId12345", companyDetails, null, "Batman", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", null, List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", "Batman", null ) );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );
        emailService.sendInvitationEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", "Batman", List.of("333") );
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationAndInviteEmailDataMatcher( "harley.quinn@gotham.city", "Harleen Quinzel", "bruce.wayne@gotham.city", "Batman", "Wayne Enterprises", COMPANY_INVITATIONS_URL ) ), eq( INVITATION_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersDoesNotThrowEmailOnKafkaQueue(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        emailService.sendInvitationEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Batman", List.of() );
        Mockito.verify( emailProducer, Mockito.never() ).sendEmail( argThat( comparisonUtils.invitationAndInviteEmailDataMatcher( "kpatel@companieshouse.gov.uk", "Krishna Patel", "bruce.wayne@gotham.city", "Batman", "Wayne Enterprises", COMPANY_INVITATIONS_URL ) ), eq( INVITATION_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( INVITATION_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", "Elon Musk", List.of("333") ) );
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        final var emptyCompanyDetails = new CompanyDetails();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", null, "Harleen Quinzel", "Batman", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", emptyCompanyDetails, "Harleen Quinzel", "Batman", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", companyDetails, null, "Batman", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", null, List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", "Batman", null ) );
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var expectedBaseEmail = new InvitationAcceptedEmailBuilder()
                .setInviterDisplayName( "Harleen Quinzel" )
                .setInviteeDisplayName( "Batman" )
                .setCompanyName( "Wayne Enterprises" );
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );

        emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", "Batman", List.of("333") );

        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationAcceptedEmailDataMatcher( List.of( "harley.quinn@gotham.city" ), expectedBaseEmail ) ), eq( INVITATION_ACCEPTED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersDoesNotThrowEmailOnKafkaQueue(){
        final var expectedBaseEmail = new InvitationAcceptedEmailBuilder()
                .setInviterDisplayName( "Harleen Quinzel" )
                .setInviteeDisplayName( "Batman" )
                .setCompanyName( "Wayne Enterprises" );
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();

        emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", "Batman", List.of() );

        Mockito.verify( emailProducer, Mockito.never() ).sendEmail( argThat( comparisonUtils.invitationAcceptedEmailDataMatcher( List.of( "harley.quinn@gotham.city" ), expectedBaseEmail ) ), eq( INVITATION_ACCEPTED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( INVITATION_ACCEPTED_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", companyDetails, "Harleen Quinzel", "Batman", List.of("333") ) );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNameOrNullRequestsThrowsNullPointerException(){
        final var emptyCompanyDetails = new CompanyDetails();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUsers( "theId12345", null, "Batman", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUsers( "theId12345", emptyCompanyDetails, "Batman", List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUsers( "theId12345", companyDetails, null, List.of("333") ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUsers( "theId12345", companyDetails, "Batman", null ) );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );
        emailService.sendInvitationRejectedEmailToAssociatedUsers( "theId12345", companyDetails, "Batman", List.of("333") );
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationRejectedEmailMatcher( "harley.quinn@gotham.city", "Batman", "Wayne Enterprises" ) ), eq( INVITATION_REJECTED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersDoesNotThrowEmailOnKafkaQueue(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        emailService.sendInvitationRejectedEmailToAssociatedUsers( "theId12345", companyDetails, "Batman", List.of() );
        Mockito.verify( emailProducer, Mockito.never() ).sendEmail( argThat( comparisonUtils.invitationRejectedEmailMatcher( "kpatel@companieshouse.gov.uk", "Batman", "Wayne Enterprises" ) ), eq( INVITATION_REJECTED_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockUsersServiceFetchUserDetails( "333" );
        mockers.mockEmailSendingFailure( INVITATION_REJECTED_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUsers( "theId12345", companyDetails, "Batman", List.of("333") ) );
    }

    @Test
    void sendInviteEmailWithNullCompanyDetailsOrNullCompanyNameOrNullInviterDisplayNameOrNullInvitationExpiryTimestampOrNullInvitationLinkOrNullInviteeEmailThrowsNullPointerException(){
        final var emptyCompanyDetails = new CompanyDetails();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", null, "Batman", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", emptyCompanyDetails, "Batman", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", companyDetails, null, "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", companyDetails, "Batman", null, "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteEmail( "theId12345", companyDetails, "Batman", "1992-05-01T10:30:00.000000", null ) );
    }

    @Test
    void sendInviteEmailThrowsEmailOnKafkaQueue(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        emailService.sendInviteEmail( "theId12345", companyDetails, "Batman", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" );
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.invitationAndInviteEmailDataMatcher( "bruce.wayne@gotham.city", "Batman", "kpatel@companieshouse.gov.uk", "Krishna Patel", "Wayne Enterprises", COMPANY_INVITATIONS_URL ) ), eq( INVITE_MESSAGE_TYPE.getValue() ) );
    }

    @Test
    void sendInviteEmailWithUnexpectedIssueThrowsEmailSendingException(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        mockers.mockEmailSendingFailure( INVITE_MESSAGE_TYPE.getValue() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInviteEmail( "theId12345", companyDetails, "Krishna Patel", "1992-05-01T10:30:00.000000", "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendInviteCancelledEmailWithoutCompanyDetailsOrCompanyNameOrCancelledByDisplayNameOrInviteeUserSupplierOrEmailThrowsNullPointerException(){
        final var emptyCompanyDetails = new CompanyDetails();
        final var userDetails = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteCancelledEmail( "theId12345", null, "Batman", () -> userDetails ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteCancelledEmail( "theId12345", emptyCompanyDetails, "Batman", () -> userDetails ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteCancelledEmail( "theId12345", companyDetails, null, () -> userDetails ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteCancelledEmail( "theId12345", companyDetails, "Batman", null ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInviteCancelledEmail( "theId12345", companyDetails, "Batman", () -> new User() ) );
    }

    @Test
    void sendInviteCancelledEmailSendsEmail(){
        final var userDetails = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        emailService.sendInviteCancelledEmail( "theId12345", companyDetails, "Batman", () -> userDetails );
        Mockito.verify( emailProducer ).sendEmail( argThat( comparisonUtils.inviteCancelledEmailMatcher( "bruce.wayne@gotham.city", "Wayne Enterprises", "Batman" ) ), eq( INVITE_CANCELLED_MESSAGE_TYPE.getValue() ) );
    }

}
