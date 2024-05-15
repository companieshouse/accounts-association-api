package uk.gov.companieshouse.accounts.association.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_ACCEPTED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_CANCELLED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_REJECTED_MESSAGE_TYPE;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;
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
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.models.email.data.AuthCodeConfirmationEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.AuthorisationRemovedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationAcceptedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationCancelledEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationRejectedEmailData;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.utils.MessageType;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class EmailServiceTest {

    @Mock
    EmailProducer emailProducer;

    @Mock
    AssociationsRepository associationsRepository;

    @Mock
    UsersService usersService;

    @InjectMocks
    EmailService emailService;

    private AssociationDao associationOne;

    @BeforeEach
    void setup(){
        final var now = LocalDateTime.now();

        final var invitationOne = new InvitationDao();
        invitationOne.setInvitedBy("666");
        invitationOne.setInvitedAt(now.plusDays(4));

        associationOne = new AssociationDao();
        associationOne.setCompanyNumber("111111");
        associationOne.setUserId("111");
        associationOne.setUserEmail("bruce.wayne@gotham.city");
        associationOne.setStatus(StatusEnum.CONFIRMED.getValue());
        associationOne.setId("1");
        associationOne.setApprovedAt(now.plusDays(1));
        associationOne.setRemovedAt(now.plusDays(2));
        associationOne.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationOne.setApprovalExpiryAt(now.plusDays(3));
        associationOne.setInvitations(List.of(invitationOne));
        associationOne.setEtag("a");
    }



    @Test
    void createRequestsToFetchAssociatedUsersCorrectlyFormsUpRequests(){
        final var content = List.of(associationOne);
        final var pageRequest = PageRequest.of(0, 20);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        Supplier<User> userSupplier = () -> new User().userId( "111" );

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociatedUsers( eq( "111111" ), any(), any() );
        Mockito.doReturn( userSupplier ).when( usersService ).createFetchUserDetailsRequest( anyString() );

        Assertions.assertEquals( "111", emailService.createRequestsToFetchAssociatedUsers( "111111", List.of() ).get( 0 ).get().getUserId() );
    }

    @Test
    void createRequestsToFetchAssociatedUsersAppliesFilterCorrectly(){
        final var associationTwo = new AssociationDao();
        associationTwo.setUserId("222");

        final var content = List.of( associationOne, associationTwo );
        final var pageRequest = PageRequest.of(0, 20);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        Supplier<User> userSupplier = () -> new User().userId( "111" );

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociatedUsers( eq( "111111" ), any(), any() );
        Mockito.doReturn( userSupplier ).when( usersService ).createFetchUserDetailsRequest( "111" );

        final var requests = emailService.createRequestsToFetchAssociatedUsers( "111111", List.of("222") );
        Assertions.assertEquals( 1, requests.size() );
        Assertions.assertEquals( "111", requests.get( 0 ).get().getUserId() );
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNameOrNullRequestsThrowsNullPointerException(){
        final var emailData = new AuthCodeConfirmationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Krishna Patel is now authorised to file online for Tesla" );
        emailData.setAuthorisedPerson( "Krishna Patel" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( "theId12345", null, "Krishna Patel", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( "theId12345", new CompanyDetails(), "Krishna Patel", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( "theId12345", companyDetails, null, requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", null ) );
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var emailData = new AuthCodeConfirmationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Krishna Patel is now authorised to file online for Tesla" );
        emailData.setAuthorisedPerson( "Krishna Patel" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );
        emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", requestsToFetchAssociatedUsers );

        Mockito.verify( emailProducer ).sendEmail( emailData, MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getMessageType() );
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersDoesNotThrowEmailOnKafkaQueue(){
        final var emailData = new AuthCodeConfirmationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Krishna Patel is now authorised to file online for Tesla" );
        emailData.setAuthorisedPerson( "Krishna Patel" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", List.of() );

        Mockito.verify( emailProducer, Mockito.never() ).sendEmail( emailData, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getMessageType() );
    }

    @Test
    void sendAuthCodeConfirmationEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var emailData = new AuthCodeConfirmationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Krishna Patel is now authorised to file online for Tesla" );
        emailData.setAuthorisedPerson( "Krishna Patel" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", requestsToFetchAssociatedUsers ) );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        final var emailData = new AuthorisationRemovedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk's authorisation removed to file online for Tesla" );
        emailData.setPersonWhoRemovedAuthorisation( "Krishna Patel" );
        emailData.setPersonWhoWasRemoved( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", null, "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", new CompanyDetails(), "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", companyDetails, null, "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", null, requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", null ) );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var emailData = new AuthorisationRemovedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk's authorisation removed to file online for Tesla" );
        emailData.setPersonWhoRemovedAuthorisation( "Krishna Patel" );
        emailData.setPersonWhoWasRemoved( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers );

        Mockito.verify( emailProducer ).sendEmail( emailData, AUTHORISATION_REMOVED_MESSAGE_TYPE.getMessageType() );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersDoesNotThrowEmailOnKafkaQueue(){
        final var emailData = new AuthorisationRemovedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk's authorisation removed to file online for Tesla" );
        emailData.setPersonWhoRemovedAuthorisation( "Krishna Patel" );
        emailData.setPersonWhoWasRemoved( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", List.of() );

        Mockito.verify( emailProducer, Mockito.never() ).sendEmail( emailData, AUTHORISATION_REMOVED_MESSAGE_TYPE.getMessageType() );
    }

    @Test
    void sendAuthorisationRemovedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var emailData = new AuthorisationRemovedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk's authorisation removed to file online for Tesla" );
        emailData.setPersonWhoRemovedAuthorisation( "Krishna Patel" );
        emailData.setPersonWhoWasRemoved( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendAuthorisationRemovedEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers ) );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        final var emailData = new InvitationCancelledEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Invitation cancelled for Elon Musk to be authorised to file online for Tesla" );
        emailData.setPersonWhoCancelledInvite( "Krishna Patel" );
        emailData.setPersonWhoWasCancelled( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", null, "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", new CompanyDetails(), "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", companyDetails, null, "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", null, requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", null ) );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var emailData = new InvitationCancelledEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Invitation cancelled for Elon Musk to be authorised to file online for Tesla" );
        emailData.setPersonWhoCancelledInvite( "Krishna Patel" );
        emailData.setPersonWhoWasCancelled( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers );

        Mockito.verify( emailProducer ).sendEmail( emailData, INVITATION_CANCELLED_MESSAGE_TYPE.getMessageType() );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersDoesNotThrowEmailOnKafkaQueue(){
        final var emailData = new InvitationCancelledEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Invitation cancelled for Elon Musk to be authorised to file online for Tesla" );
        emailData.setPersonWhoCancelledInvite( "Krishna Patel" );
        emailData.setPersonWhoWasCancelled( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", List.of() );

        Mockito.verify( emailProducer, Mockito.never() ).sendEmail( emailData, INVITATION_CANCELLED_MESSAGE_TYPE.getMessageType() );
    }

    @Test
    void sendInvitationCancelledEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var emailData = new InvitationCancelledEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Invitation cancelled for Elon Musk to be authorised to file online for Tesla" );
        emailData.setPersonWhoCancelledInvite( "Krishna Patel" );
        emailData.setPersonWhoWasCancelled( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationCancelledEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers ) );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        final var emailData = new InvitationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk invited to be authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setInvitee( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUsers( "theId12345", null, "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUsers( "theId12345", new CompanyDetails(), "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUsers( "theId12345", companyDetails, null, "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", null, requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", null ) );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var emailData = new InvitationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk invited to be authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setInvitee( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        emailService.sendInvitationEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers );

        Mockito.verify( emailProducer ).sendEmail( emailData, INVITATION_MESSAGE_TYPE.getMessageType() );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersDoesNotThrowEmailOnKafkaQueue(){
        final var emailData = new InvitationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk invited to be authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setInvitee( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        emailService.sendInvitationEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", List.of() );

        Mockito.verify( emailProducer, Mockito.never() ).sendEmail( emailData, INVITATION_MESSAGE_TYPE.getMessageType() );
    }

    @Test
    void sendInvitationEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var emailData = new InvitationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk invited to be authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setInvitee( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers ) );
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNamesOrNullRequestsThrowsNullPointerException(){
        final var emailData = new InvitationAcceptedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk is now authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setAuthorisedPerson( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", null, "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", new CompanyDetails(), "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", companyDetails, null, "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", null, requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", null ) );
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var emailData = new InvitationAcceptedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk is now authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setAuthorisedPerson( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers );

        Mockito.verify( emailProducer ).sendEmail( emailData, INVITATION_ACCEPTED_MESSAGE_TYPE.getMessageType() );
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersDoesNotThrowEmailOnKafkaQueue(){
        final var emailData = new InvitationAcceptedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk is now authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setAuthorisedPerson( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", List.of() );

        Mockito.verify( emailProducer, Mockito.never() ).sendEmail( emailData, INVITATION_ACCEPTED_MESSAGE_TYPE.getMessageType() );
    }

    @Test
    void sendInvitationAcceptedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var emailData = new InvitationAcceptedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk is now authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setAuthorisedPerson( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationAcceptedEmailToAssociatedUsers( "theId12345", companyDetails, "Krishna Patel", "Elon Musk", requestsToFetchAssociatedUsers ) );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersWithNullCompanyDetailsOrNullCompanyNameOrNullDisplayNameOrNullRequestsThrowsNullPointerException(){
        final var emailData = new InvitationRejectedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk has declined to be digitally authorised to file online for Tesla" );
        emailData.setPersonWhoDeclined( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUsers( "theId12345", null, "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUsers( "theId12345", new CompanyDetails(), "Elon Musk", requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUsers( "theId12345", companyDetails, null, requestsToFetchAssociatedUsers ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUsers( "theId12345", companyDetails, "Elon Musk", null ) );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersThrowsEmailOnKafkaQueue(){
        final var emailData = new InvitationRejectedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk has declined to be digitally authorised to file online for Tesla" );
        emailData.setPersonWhoDeclined( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );
        emailService.sendInvitationRejectedEmailToAssociatedUsers( "theId12345", companyDetails, "Elon Musk", requestsToFetchAssociatedUsers );

        Mockito.verify( emailProducer ).sendEmail( emailData, INVITATION_REJECTED_MESSAGE_TYPE.getMessageType() );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersDoesNotThrowEmailOnKafkaQueue(){
        final var emailData = new InvitationRejectedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk has declined to be digitally authorised to file online for Tesla" );
        emailData.setPersonWhoDeclined( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        emailService.sendInvitationRejectedEmailToAssociatedUsers( "theId12345", companyDetails, "Elon Musk", List.of() );

        Mockito.verify( emailProducer, Mockito.never() ).sendEmail( emailData, INVITATION_REJECTED_MESSAGE_TYPE.getMessageType() );
    }

    @Test
    void sendInvitationRejectedEmailToAssociatedUsersWithUnexpectedIssueThrowsEmailSendingException(){
        final var emailData = new InvitationRejectedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk has declined to be digitally authorised to file online for Tesla" );
        emailData.setPersonWhoDeclined( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        final var companyDetails = new CompanyDetails().companyName( "Tesla" );
        List<Supplier<User>> requestsToFetchAssociatedUsers = List.of( () -> new User().email( "kpatel@companieshouse.gov.uk" ) );

        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationRejectedEmailToAssociatedUsers( "theId12345", companyDetails, "Elon Musk", requestsToFetchAssociatedUsers ) );
    }

}
