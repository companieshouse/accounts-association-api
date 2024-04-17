package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.utils.Constants.AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_ACCEPTED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_CANCELLED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_REJECTED_MESSAGE_TYPE;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.models.email.AuthCodeConfirmationEmailData;
import uk.gov.companieshouse.accounts.association.models.email.AuthorisationRemovedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.InvitationAcceptedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.InvitationCancelledEmailData;
import uk.gov.companieshouse.accounts.association.models.email.InvitationEmailData;
import uk.gov.companieshouse.accounts.association.models.email.InvitationRejectedEmailData;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;
import uk.gov.companieshouse.email_producer.model.EmailData;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
@ComponentScan("uk.gov.companieshouse.email_producer")
public class EmailService {

    private final EmailProducer emailProducer;

    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    @Autowired
    public EmailService( final EmailProducer emailProducer ) {
        this.emailProducer = emailProducer;
    }

    private void sendEmail( final EmailData emailData, final String messageType ) throws EmailSendingException {
        try {
            emailProducer.sendEmail(emailData, messageType);
            LOG.debug(String.format("Submitted %s email to Kafka", messageType));
        } catch (EmailSendingException exception) {
            LOG.error("Error sending email", exception);
            throw exception;
        }
    }

    public void sendAuthCodeConfirmationEmail( final String recipientEmail, final String displayName, final String companyName ){
        if ( Objects.isNull( recipientEmail ) || Objects.isNull( displayName ) || Objects.isNull( companyName ) ){
            LOG.error( "Attempted to send email where recipientEmail, displayName, or companyName was null" );
            throw new NullPointerException( "recipientEmail, displayName, and companyName cannot be null" );
        }

        final var subject = String.format( "Companies House: %s is now authorised to file online for %s", displayName, companyName );

        final var emailData = new AuthCodeConfirmationEmailData();
        emailData.setTo( recipientEmail );
        emailData.setSubject( subject );
        emailData.setAuthorisedPerson( displayName );
        emailData.setCompanyName( companyName );

        LOG.debug( String.format( "Sending email entitled '%s' to '%s'", subject, recipientEmail ) );
        sendEmail( emailData, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE);
    }

    public void sendAuthorisationRemovedEmail( final String recipientEmail, final String removedByDisplayName, final String removedUserDisplayName, final String companyName ){
        if ( Objects.isNull( recipientEmail ) || Objects.isNull( removedByDisplayName ) || Objects.isNull( removedUserDisplayName ) || Objects.isNull( companyName ) ){
            LOG.error( "Attempted to send email where recipientEmail, removedByDisplayName, removedUserDisplayName, or companyName was null" );
            throw new NullPointerException( "recipientEmail, removedByDisplayName, removedUserDisplayName, and companyName cannot be null" );
        }

        final var subject = String.format( "Companies House: %s's authorisation removed to file online for %s", removedUserDisplayName, companyName );

        final var emailData = new AuthorisationRemovedEmailData();
        emailData.setTo( recipientEmail );
        emailData.setSubject( subject );
        emailData.setPersonWhoRemovedAuthorisation( removedByDisplayName );
        emailData.setAuthenticationRemovedPerson( removedUserDisplayName );
        emailData.setCompanyName( companyName );

        LOG.debug( String.format( "Sending email entitled '%s' to '%s'", subject, recipientEmail ) );
        sendEmail( emailData, AUTHORISATION_REMOVED_MESSAGE_TYPE );
    }

    public void sendInvitationCancelledEmail( final String recipientEmail, final String cancelledByDisplayName, final String cancelledUserDisplayName, final String companyName ) {
        if ( Objects.isNull( recipientEmail ) || Objects.isNull( cancelledByDisplayName ) || Objects.isNull( cancelledUserDisplayName ) || Objects.isNull( companyName ) ){
            LOG.error( "Attempted to send email where recipientEmail, cancelledByDisplayName, cancelledUserDisplayName, or companyName was null" );
            throw new NullPointerException( "recipientEmail, cancelledByDisplayName, cancelledUserDisplayName, and companyName cannot be null" );
        }

        final var subject = String.format( "Companies House: Invitation cancelled for %s to be authorised to file online for %s", cancelledUserDisplayName, companyName );

        final var emailData = new InvitationCancelledEmailData();
        emailData.setTo( recipientEmail );
        emailData.setSubject( subject );
        emailData.setPersonWhoCancelledInvite( cancelledByDisplayName );
        emailData.setInviteCancelledPerson( cancelledUserDisplayName );
        emailData.setCompanyName( companyName );

        LOG.debug( String.format( "Sending email entitled '%s' to '%s'", subject, recipientEmail ) );
        sendEmail( emailData, INVITATION_CANCELLED_MESSAGE_TYPE );
    }

    public void sendInvitationEmail( final String recipientEmail, final String inviterDisplayName, final String inviteeDisplayName, final String companyName ){
        if ( Objects.isNull( recipientEmail ) || Objects.isNull( inviterDisplayName ) || Objects.isNull( inviteeDisplayName ) || Objects.isNull( companyName ) ){
            LOG.error( "Attempted to send email where recipientEmail, inviterDisplayName, inviteeDisplayName, or companyName was null" );
            throw new NullPointerException( "recipientEmail, inviterDisplayName, inviteeDisplayName, and companyName cannot be null" );
        }

        final var subject = String.format( "Companies House: %s invited to be authorised to file online for %s", inviteeDisplayName, companyName );

        final var emailData = new InvitationEmailData();
        emailData.setTo( recipientEmail );
        emailData.setSubject( subject );
        emailData.setPersonInvitationFrom( inviterDisplayName );
        emailData.setInvitee( inviteeDisplayName );
        emailData.setCompanyName( companyName );

        LOG.debug( String.format( "Sending email entitled '%s' to '%s'", subject, recipientEmail ) );
        sendEmail( emailData, INVITATION_MESSAGE_TYPE );
    }

    public void sendInvitationAcceptedEmail( final String recipientEmail, final String inviterDisplayName, final String inviteeDisplayName, final String companyName ){
        if ( Objects.isNull( recipientEmail ) || Objects.isNull( inviterDisplayName ) || Objects.isNull( inviteeDisplayName ) || Objects.isNull( companyName ) ){
            LOG.error( "Attempted to send email where recipientEmail, inviterDisplayName, inviteeDisplayName, or companyName was null" );
            throw new NullPointerException( "recipientEmail, inviterDisplayName, inviteeDisplayName, and companyName cannot be null" );
        }

        final var subject = String.format( "Companies House: %s is now authorised to file online for %s", inviteeDisplayName, companyName );

        final var emailData = new InvitationAcceptedEmailData();
        emailData.setTo( recipientEmail );
        emailData.setSubject( subject );
        emailData.setPersonInvitationFrom( inviterDisplayName );
        emailData.setAuthorisedPerson( inviteeDisplayName );
        emailData.setCompanyName( companyName );

        LOG.debug( String.format( "Sending email entitled '%s' to '%s'", subject, recipientEmail ) );
        sendEmail( emailData, INVITATION_ACCEPTED_MESSAGE_TYPE );
    }

    public void sendInvitationRejectedEmail( final String recipientEmail, final String inviteeDisplayName, final String companyName ){
        if ( Objects.isNull( recipientEmail ) || Objects.isNull( inviteeDisplayName ) || Objects.isNull( companyName ) ){
            LOG.error( "Attempted to send email where recipientEmail, inviteeDisplayName, or companyName was null" );
            throw new NullPointerException( "recipientEmail, inviteeDisplayName, and companyName cannot be null" );
        }

        final var subject = String.format( "Companies House: %s has declined to be digitally authorised to file online for %s", inviteeDisplayName, companyName );

        final var emailData = new InvitationRejectedEmailData();
        emailData.setTo( recipientEmail );
        emailData.setSubject( subject );
        emailData.setDeclinedAuthorisationPerson( inviteeDisplayName );
        emailData.setCompanyName( companyName );

        LOG.debug( String.format( "Sending email entitled '%s' to '%s'", subject, recipientEmail ) );
        sendEmail( emailData, INVITATION_REJECTED_MESSAGE_TYPE );
    }

}
