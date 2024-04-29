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
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
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
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmail( "Krishna Patel", "Tesla" ).accept( null ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmail(  null, "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthCodeConfirmationEmail(  "Krishna Patel", null ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendAuthCodeConfirmationEmailWithUnexpectedIssueThrowsBadRequestRuntimeException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( BadRequestRuntimeException.class, () -> emailService.sendAuthCodeConfirmationEmail( "Krishna Patel", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendAuthCodeConfirmationEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new AuthCodeConfirmationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Krishna Patel is now authorised to file online for Tesla" );
        emailData.setAuthorisedPerson( "Krishna Patel" );
        emailData.setCompanyName( "Tesla" );

        emailService.sendAuthCodeConfirmationEmail( "Krishna Patel", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" );
        Mockito.verify( emailProducer ).sendEmail( emailData, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE );
    }

    @Test
    void sendAuthorisationRemovedEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( null) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmail( null, "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmail( "Krishna Patel", null, "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendAuthorisationRemovedEmail(  "Krishna Patel", "Elon Musk", null ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendAuthorisationRemovedEmailWithUnexpectedIssueThrowsBadRequestRuntimeException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( BadRequestRuntimeException.class, () -> emailService.sendAuthorisationRemovedEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendAuthorisationRemovedEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new AuthorisationRemovedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk's authorisation removed to file online for Tesla" );
        emailData.setPersonWhoRemovedAuthorisation( "Krishna Patel" );
        emailData.setPersonWhoWasRemoved( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        emailService.sendAuthorisationRemovedEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" );
        Mockito.verify( emailProducer ).sendEmail( emailData, AUTHORISATION_REMOVED_MESSAGE_TYPE );
    }

    @Test
    void sendInvitationCancelledEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( null ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmail(  null, "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmail(  "Krishna Patel", null, "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationCancelledEmail( "Krishna Patel", "Elon Musk", null ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendInvitationCancelledEmailWithUnexpectedIssueThrowsBadRequestRuntimeException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( BadRequestRuntimeException.class, () -> emailService.sendInvitationCancelledEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendInvitationCancelledEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new InvitationCancelledEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Invitation cancelled for Elon Musk to be authorised to file online for Tesla" );
        emailData.setPersonWhoCancelledInvite( "Krishna Patel" );
        emailData.setPersonWhoWasCancelled( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        emailService.sendInvitationCancelledEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" );
        Mockito.verify( emailProducer ).sendEmail( emailData, INVITATION_CANCELLED_MESSAGE_TYPE );
    }

    @Test
    void sendInvitationEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( null ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmail( null, "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmail( "Krishna Patel", null, "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationEmail( "Krishna Patel", "Elon Musk", null ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendInvitationEmailWithUnexpectedIssueThrowsBadRequestRuntimeException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( BadRequestRuntimeException.class, () -> emailService.sendInvitationEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendInvitationEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new InvitationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk invited to be authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setInvitee( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        emailService.sendInvitationEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" );
        Mockito.verify( emailProducer ).sendEmail( emailData, INVITATION_MESSAGE_TYPE );
    }

    @Test
    void sendInvitationAcceptedEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( null ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmail( null, "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmail( "Krishna Patel", null, "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationAcceptedEmail( "Krishna Patel", "Elon Musk", null ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendInvitationAcceptedEmailWithUnexpectedIssueThrowsBadRequestRuntimeException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( BadRequestRuntimeException.class, () -> emailService.sendInvitationAcceptedEmail(  "Krishna Patel", "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendInvitationAcceptedEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new InvitationAcceptedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk is now authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setAuthorisedPerson( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        emailService.sendInvitationAcceptedEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" );
        Mockito.verify( emailProducer ).sendEmail( emailData, INVITATION_ACCEPTED_MESSAGE_TYPE );
    }

    @Test
    void sendInvitationRejectedEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmail( "Elon Musk", "Tesla" ).accept( null ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmail( null, "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> emailService.sendInvitationRejectedEmail(  "Elon Musk", null ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendInvitationRejectedEmailWithUnexpectedIssueThrowsBadRequestRuntimeException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( BadRequestRuntimeException.class, () -> emailService.sendInvitationRejectedEmail( "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendInvitationRejectedEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new InvitationRejectedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk has declined to be digitally authorised to file online for Tesla" );
        emailData.setPersonWhoDeclined( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        emailService.sendInvitationRejectedEmail( "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" );
        Mockito.verify( emailProducer ).sendEmail( emailData, INVITATION_REJECTED_MESSAGE_TYPE );
    }

}
