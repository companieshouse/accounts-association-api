package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.utils.Constants.AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_ACCEPTED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_CANCELLED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_REJECTED_MESSAGE_TYPE;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.email.AuthCodeConfirmationEmailData;
import uk.gov.companieshouse.accounts.association.models.email.AuthorisationRemovedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.InvitationAcceptedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.InvitationCancelledEmailData;
import uk.gov.companieshouse.accounts.association.models.email.InvitationEmailData;
import uk.gov.companieshouse.accounts.association.models.email.InvitationRejectedEmailData;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;
import uk.gov.companieshouse.email_producer.model.EmailData;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
@ComponentScan("uk.gov.companieshouse.email_producer")
public class EmailService {

    private final EmailProducer emailProducer;
    private final AssociationsRepository associationsRepository;
    private final UsersService usersService;

    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    @Autowired
    public EmailService( final EmailProducer emailProducer, AssociationsRepository associationsRepository, UsersService usersService) {
        this.emailProducer = emailProducer;
        this.associationsRepository = associationsRepository;
        this.usersService = usersService;
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

    @Transactional( readOnly = true )
    public Page<String> fetchAllAssociatedUsersEmails( final CompanyDetails companyDetails, final Set<String> statuses ){
        final var companyNumber = companyDetails.getCompanyNumber();
        return associationsRepository.fetchAssociatedUsers( companyNumber, statuses, Pageable.unpaged() )
                .map( AssociationDao::getUserId )
                .map( usersService::fetchUserDetails )
                .map( User::getEmail );
    }

    private void sendEmailToUsersAssociatedWithCompany( final String xRequestId, final CompanyDetails companyDetails, final Consumer<String> sendEmailForEmailAddress ){
        final var companyNumber = companyDetails.getCompanyNumber();
        LOG.debugContext( xRequestId, String.format( "Attempting to send notifications to users from company %s", companyNumber ), null );
        fetchAllAssociatedUsersEmails( companyDetails, Set.of( StatusEnum.CONFIRMED.getValue() ) )
                .forEach( sendEmailForEmailAddress );
        LOG.debugContext( xRequestId, String.format( "Successfully sent notifications to users from company %s", companyNumber ), null );
    }

    private Consumer<String> sendAuthCodeConfirmationEmail( final String displayName, final String companyName ){
        return recipientEmail -> {
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
        };
    }

    @Async
    public void sendAuthCodeConfirmationEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String displayName ){
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendAuthCodeConfirmationEmail( displayName, companyDetails.getCompanyName() ) );
    }

    private Consumer<String> sendAuthorisationRemovedEmail( final String removedByDisplayName, final String removedUserDisplayName, final String companyName ){
       return recipientEmail -> {
            if ( Objects.isNull( recipientEmail ) || Objects.isNull( removedByDisplayName ) || Objects.isNull( removedUserDisplayName ) || Objects.isNull( companyName ) ){
                LOG.error( "Attempted to send email where recipientEmail, removedByDisplayName, removedUserDisplayName, or companyName was null" );
                throw new NullPointerException( "recipientEmail, removedByDisplayName, removedUserDisplayName, and companyName cannot be null" );
            }

            final var subject = String.format( "Companies House: %s's authorisation removed to file online for %s", removedUserDisplayName, companyName );

            final var emailData = new AuthorisationRemovedEmailData();
            emailData.setTo( recipientEmail );
            emailData.setSubject( subject );
            emailData.setPersonWhoRemovedAuthorisation( removedByDisplayName );
            emailData.setPersonWhoWasRemoved( removedUserDisplayName );
            emailData.setCompanyName( companyName );

            LOG.debug( String.format( "Sending email entitled '%s' to '%s'", subject, recipientEmail ) );
            sendEmail( emailData, AUTHORISATION_REMOVED_MESSAGE_TYPE );
       };
    }

    @Async
    public void sendAuthorisationRemovedEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String removedByDisplayName, final String removedUserDisplayName ) {
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendAuthorisationRemovedEmail( removedByDisplayName, removedUserDisplayName, companyDetails.getCompanyName() ) );
    }

    private Consumer<String> sendInvitationCancelledEmail( final String cancelledByDisplayName, final String cancelledUserDisplayName, final String companyName ) {
        return recipientEmail -> {
            if ( Objects.isNull( recipientEmail ) || Objects.isNull( cancelledByDisplayName ) || Objects.isNull( cancelledUserDisplayName ) || Objects.isNull( companyName ) ){
                LOG.error( "Attempted to send email where recipientEmail, cancelledByDisplayName, cancelledUserDisplayName, or companyName was null" );
                throw new NullPointerException( "recipientEmail, cancelledByDisplayName, cancelledUserDisplayName, and companyName cannot be null" );
            }

            final var subject = String.format( "Companies House: Invitation cancelled for %s to be authorised to file online for %s", cancelledUserDisplayName, companyName );

            final var emailData = new InvitationCancelledEmailData();
            emailData.setTo( recipientEmail );
            emailData.setSubject( subject );
            emailData.setPersonWhoCancelledInvite( cancelledByDisplayName );
            emailData.setPersonWhoWasCancelled( cancelledUserDisplayName );
            emailData.setCompanyName( companyName );

            LOG.debug( String.format( "Sending email entitled '%s' to '%s'", subject, recipientEmail ) );
            sendEmail( emailData, INVITATION_CANCELLED_MESSAGE_TYPE );
        };
    }

    @Async
    public void sendInvitationCancelledEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String cancelledByDisplayName, final String cancelledUserDisplayName ){
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendInvitationCancelledEmail( cancelledByDisplayName, cancelledUserDisplayName, companyDetails.getCompanyName() ) );
    }

    private Consumer<String> sendInvitationEmail( final String inviterDisplayName, final String inviteeDisplayName, final String companyName ){
        return recipientEmail -> {
            if ( Objects.isNull( recipientEmail ) || Objects.isNull( inviterDisplayName ) || Objects.isNull( inviteeDisplayName ) || Objects.isNull( companyName ) ){
                LOG.error( "Attempted to send email where recipientEmail, inviterDisplayName, inviteeDisplayName, or companyName was null" );
                throw new NullPointerException( "recipientEmail, inviterDisplayName, inviteeDisplayName, and companyName cannot be null" );
            }

            final var subject = String.format( "Companies House: %s invited to be authorised to file online for %s", inviteeDisplayName, companyName );

            final var emailData = new InvitationEmailData();
            emailData.setTo( recipientEmail );
            emailData.setSubject( subject );
            emailData.setPersonWhoCreatedInvite( inviterDisplayName );
            emailData.setInvitee( inviteeDisplayName );
            emailData.setCompanyName( companyName );

            LOG.debug( String.format( "Sending email entitled '%s' to '%s'", subject, recipientEmail ) );
            sendEmail( emailData, INVITATION_MESSAGE_TYPE );
        };
    }

    @Async
    public void sendInvitationEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String inviterDisplayName, final String inviteeDisplayName ){
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendInvitationEmail( inviterDisplayName, inviteeDisplayName, companyDetails.getCompanyName() ) );
    }

    private Consumer<String> sendInvitationAcceptedEmail( final String inviterDisplayName, final String inviteeDisplayName, final String companyName ){
        return recipientEmail -> {
            if ( Objects.isNull( recipientEmail ) || Objects.isNull( inviterDisplayName ) || Objects.isNull( inviteeDisplayName ) || Objects.isNull( companyName ) ){
                LOG.error( "Attempted to send email where recipientEmail, inviterDisplayName, inviteeDisplayName, or companyName was null" );
                throw new NullPointerException( "recipientEmail, inviterDisplayName, inviteeDisplayName, and companyName cannot be null" );
            }

            final var subject = String.format( "Companies House: %s is now authorised to file online for %s", inviteeDisplayName, companyName );

            final var emailData = new InvitationAcceptedEmailData();
            emailData.setTo( recipientEmail );
            emailData.setSubject( subject );
            emailData.setPersonWhoCreatedInvite( inviterDisplayName );
            emailData.setAuthorisedPerson( inviteeDisplayName );
            emailData.setCompanyName( companyName );

            LOG.debug( String.format( "Sending email entitled '%s' to '%s'", subject, recipientEmail ) );
            sendEmail( emailData, INVITATION_ACCEPTED_MESSAGE_TYPE );
        };
    }

    @Async
    public void sendInvitationAcceptedEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String inviterDisplayName, final String inviteeDisplayName ){
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendInvitationAcceptedEmail( inviterDisplayName, inviteeDisplayName, companyDetails.getCompanyName() ) );
    }

    private Consumer<String> sendInvitationRejectedEmail( final String inviteeDisplayName, final String companyName ){
        return recipientEmail -> {
            if ( Objects.isNull( recipientEmail ) || Objects.isNull( inviteeDisplayName ) || Objects.isNull( companyName ) ){
                LOG.error( "Attempted to send email where recipientEmail, inviteeDisplayName, or companyName was null" );
                throw new NullPointerException( "recipientEmail, inviteeDisplayName, and companyName cannot be null" );
            }

            final var subject = String.format( "Companies House: %s has declined to be digitally authorised to file online for %s", inviteeDisplayName, companyName );

            final var emailData = new InvitationRejectedEmailData();
            emailData.setTo( recipientEmail );
            emailData.setSubject( subject );
            emailData.setPersonWhoDeclined( inviteeDisplayName );
            emailData.setCompanyName( companyName );

            LOG.debug( String.format( "Sending email entitled '%s' to '%s'", subject, recipientEmail ) );
            sendEmail( emailData, INVITATION_REJECTED_MESSAGE_TYPE );
        };
    }

    @Async
    public void sendInvitationRejectedEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String inviteeDisplayName ){
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendInvitationRejectedEmail( inviteeDisplayName, companyDetails.getCompanyName() ) );
    }

}
