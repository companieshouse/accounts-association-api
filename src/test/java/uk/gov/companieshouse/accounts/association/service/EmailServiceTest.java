package uk.gov.companieshouse.accounts.association.service;

import static org.mockito.ArgumentMatchers.any;
import static uk.gov.companieshouse.accounts.association.utils.Constants.AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_ACCEPTED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_CANCELLED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_REJECTED_MESSAGE_TYPE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.AuthCodeConfirmationEmailData;
import uk.gov.companieshouse.accounts.association.models.email.AuthorisationRemovedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.InvitationAcceptedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.InvitationCancelledEmailData;
import uk.gov.companieshouse.accounts.association.models.email.InvitationEmailData;
import uk.gov.companieshouse.accounts.association.models.email.InvitationRejectedEmailData;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class EmailServiceTest {

    @Mock
    EmailProducer emailProducer;

    @InjectMocks
    EmailService emailService;

    @Test
    void sendAuthCodeConfirmationEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmail( null, "Krishna Patel", "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmail( "kpatel@companieshouse.gov.uk", null, "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", null ) );
    }

    @Test
    void sendAuthCodeConfirmationEmailWithUnexpectedIssueThrowsEmailSendingException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendAuthCodeConfirmationEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", "Tesla" ) );
    }

    @Test
    void sendAuthCodeConfirmationEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new AuthCodeConfirmationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Krishna Patel is now authorised to file online for Tesla" );
        emailData.setAuthorisedPerson( "Krishna Patel" );
        emailData.setCompanyName( "Tesla" );

        emailService.sendAuthCodeConfirmationEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", "Tesla" );
        Mockito.verify( emailProducer ).sendEmail( emailData, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE );
    }

    @Test
    void sendAuthorisationRemovedEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmail( null, "Krishna Patel", "Elon Musk", "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmail( "kpatel@companieshouse.gov.uk", null, "Elon Musk", "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", null, "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", "Elon Musk", null ) );
    }

    @Test
    void sendAuthorisationRemovedEmailWithUnexpectedIssueThrowsEmailSendingException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendAuthorisationRemovedEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", "Elon Musk", "Tesla" ) );
    }

    @Test
    void sendAuthorisationRemovedEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new AuthorisationRemovedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk's authorisation removed to file online for Tesla" );
        emailData.setPersonWhoRemovedAuthorisation( "Krishna Patel" );
        emailData.setPersonWhoWasRemoved( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        emailService.sendAuthorisationRemovedEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", "Elon Musk", "Tesla" );
        Mockito.verify( emailProducer ).sendEmail( emailData, AUTHORISATION_REMOVED_MESSAGE_TYPE );
    }

    @Test
    void sendInvitationCancelledEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmail( null, "Krishna Patel", "Elon Musk", "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmail( "kpatel@companieshouse.gov.uk", null, "Elon Musk", "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", null, "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", "Elon Musk", null ) );
    }

    @Test
    void sendInvitationCancelledEmailWithUnexpectedIssueThrowsEmailSendingException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationCancelledEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", "Elon Musk", "Tesla" ) );
    }

    @Test
    void sendInvitationCancelledEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new InvitationCancelledEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Invitation cancelled for Elon Musk to be authorised to file online for Tesla" );
        emailData.setPersonWhoCancelledInvite( "Krishna Patel" );
        emailData.setPersonWhoWasCancelled( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        emailService.sendInvitationCancelledEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", "Elon Musk", "Tesla" );
        Mockito.verify( emailProducer ).sendEmail( emailData, INVITATION_CANCELLED_MESSAGE_TYPE );
    }

    @Test
    void sendInvitationEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmail( null, "Krishna Patel", "Elon Musk", "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmail( "kpatel@companieshouse.gov.uk", null, "Elon Musk", "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", null, "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", "Elon Musk", null ) );
    }

    @Test
    void sendInvitationEmailWithUnexpectedIssueThrowsEmailSendingException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", "Elon Musk", "Tesla" ) );
    }

    @Test
    void sendInvitationEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new InvitationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk invited to be authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setInvitee( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        emailService.sendInvitationEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", "Elon Musk", "Tesla" );
        Mockito.verify( emailProducer ).sendEmail( emailData, INVITATION_MESSAGE_TYPE );
    }

    @Test
    void sendInvitationAcceptedEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmail( null, "Krishna Patel", "Elon Musk", "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmail( "kpatel@companieshouse.gov.uk", null, "Elon Musk", "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", null, "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", "Elon Musk", null ) );
    }

    @Test
    void sendInvitationAcceptedEmailWithUnexpectedIssueThrowsEmailSendingException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationAcceptedEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", "Elon Musk", "Tesla" ) );
    }

    @Test
    void sendInvitationAcceptedEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new InvitationAcceptedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk is now authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setAuthorisedPerson( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        emailService.sendInvitationAcceptedEmail( "kpatel@companieshouse.gov.uk", "Krishna Patel", "Elon Musk", "Tesla" );
        Mockito.verify( emailProducer ).sendEmail( emailData, INVITATION_ACCEPTED_MESSAGE_TYPE );
    }

    @Test
    void sendInvitationRejectedEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmail( null,  "Elon Musk", "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmail( "kpatel@companieshouse.gov.uk", null, "Tesla" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmail( "kpatel@companieshouse.gov.uk", "Elon Musk", null ) );
    }

    @Test
    void sendInvitationRejectedEmailWithUnexpectedIssueThrowsEmailSendingException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendInvitationRejectedEmail( "kpatel@companieshouse.gov.uk", "Elon Musk", "Tesla" ) );
    }

    @Test
    void sendInvitationRejectedEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new InvitationRejectedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk has declined to be digitally authorised to file online for Tesla" );
        emailData.setPersonWhoDeclined( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        emailService.sendInvitationRejectedEmail( "kpatel@companieshouse.gov.uk", "Elon Musk", "Tesla" );
        Mockito.verify( emailProducer ).sendEmail( emailData, INVITATION_REJECTED_MESSAGE_TYPE );
    }

}
