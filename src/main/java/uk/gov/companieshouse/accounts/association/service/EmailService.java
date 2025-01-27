package uk.gov.companieshouse.accounts.association.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.models.email.EmailNotification;
import uk.gov.companieshouse.accounts.association.models.email.builders.*;
import uk.gov.companieshouse.accounts.association.utils.MessageType;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;

@Service
public class EmailService {

    @Value( "${invitation.url}" )
    private String invitationLink;

    protected static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    private final UsersService usersService;
    private final EmailProducer emailProducer;


    @Autowired
    public EmailService( final UsersService usersService, final EmailProducer emailProducer ) {
        this.usersService = usersService;
        this.emailProducer = emailProducer;
    }

    @Async
    public void sendAuthCodeConfirmationEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String displayName, final List<String> userIds ) {
        userIds.forEach( userId -> {
            final var user = usersService.fetchUserDetails( userId );

            final var emailData = new AuthCodeConfirmationEmailBuilder()
                    .setRecipientEmail( user.getEmail() )
                    .setDisplayName( displayName )
                    .setCompanyName( companyDetails.getCompanyName() )
                    .build();

            final var logMessageSupplier = new EmailNotification(AUTH_CODE_CONFIRMATION_MESSAGE_TYPE, StaticPropertyUtil.APPLICATION_NAMESPACE, user.getEmail(), companyDetails.getCompanyNumber());
            try {
                emailProducer.sendEmail( emailData, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getValue() );
                LOG.infoContext( xRequestId, logMessageSupplier.toMessage(), null );
            } catch ( Exception exception ){
                LOG.errorContext( xRequestId, new Exception( logMessageSupplier.toMessageSendingFailureLoggingMessage() ), null );
                throw exception;
            }
        });
    }

    @Async
    public void sendAuthorisationRemovedEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String removedByDisplayName, final String removedUserDisplayName, final List<String> userIds ) {
        userIds.forEach( userId -> {
            final var user = usersService.fetchUserDetails( userId );

            final var emailData = new AuthorisationRemovedEmailBuilder()
                    .setCompanyName( companyDetails.getCompanyName() )
                    .setRemovedByDisplayName( removedByDisplayName )
                    .setRemovedUserDisplayName( removedUserDisplayName )
                    .setRecipientEmail( user.getEmail() )
                    .build();

            final var logMessageSupplier = new EmailNotification( MessageType.AUTHORISATION_REMOVED_MESSAGE_TYPE, StaticPropertyUtil.APPLICATION_NAMESPACE, user.getEmail(), companyDetails.getCompanyNumber() );
            try {
                emailProducer.sendEmail( emailData, MessageType.AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() );
                LOG.infoContext( xRequestId, logMessageSupplier.toMessage(), null );
            } catch ( Exception exception ){
                LOG.errorContext( xRequestId, new Exception( logMessageSupplier.toMessageSendingFailureLoggingMessage() ), null );
                throw exception;
            }
        });
    }

    @Async
    public void sendAuthorisationRemovedEmailToRemovedUser(final String xRequestId, final CompanyDetails companyDetails, final String removedByDisplayName, final String userId ) {
        final var user = usersService.fetchUserDetails( userId );

        final var emailData = new YourAuthorisationRemovedEmailBuilder()
                .setCompanyName( companyDetails.getCompanyName() )
                .setRemovedByDisplayName( removedByDisplayName )
                .setRecipientEmail( user.getEmail() )
                .build();


        final var logMessageSupplier = new EmailNotification( MessageType.YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE, removedByDisplayName, user.getEmail(), companyDetails.getCompanyNumber() );
        try {
            emailProducer.sendEmail( emailData, MessageType.YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE.getValue() );
            LOG.infoContext( xRequestId, logMessageSupplier.toMessage(), null );
        } catch ( Exception exception ){
            LOG.errorContext( xRequestId, new Exception( logMessageSupplier.toMessageSendingFailureLoggingMessage() ), null );
            throw exception;
        }
    }

    @Async
    public void sendInvitationCancelledEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String cancelledByDisplayName, final String cancelledUserDisplayName, final List<String> userIds ) {
        userIds.forEach( userId -> {
            final var user = usersService.fetchUserDetails( userId );

            final var emailData = new InvitationCancelledEmailBuilder()
                    .setCompanyName( companyDetails.getCompanyName() )
                    .setCancelledByDisplayName( cancelledByDisplayName )
                    .setCancelledUserDisplayName( cancelledUserDisplayName )
                    .setRecipientEmail( user.getEmail() )
                    .build();

            final var loggingMessageSupplier = new EmailNotification( MessageType.INVITATION_CANCELLED_MESSAGE_TYPE, StaticPropertyUtil.APPLICATION_NAMESPACE, user.getEmail(), companyDetails.getCompanyNumber() );
            try {
                emailProducer.sendEmail( emailData, MessageType.INVITATION_CANCELLED_MESSAGE_TYPE.getValue() );
                LOG.infoContext( xRequestId, loggingMessageSupplier.toMessage(), null );
            } catch ( Exception exception ){
                LOG.errorContext( xRequestId, new Exception( loggingMessageSupplier.toMessageSendingFailureLoggingMessage() ), null );
                throw exception;
            }
        });
    }

    @Async
    public void sendInvitationEmailToAssociatedUsers(final String xRequestId, final CompanyDetails companyDetails, final String inviterDisplayName, final String inviteeDisplayName, final List<String> userIds ) {
        userIds.forEach( userId -> {
            final var user = usersService.fetchUserDetails( userId );

            final var emailData = new InvitationEmailBuilder()
                    .setCompanyName( companyDetails.getCompanyName() )
                    .setInviteeDisplayName( inviteeDisplayName )
                    .setInviterDisplayName( inviterDisplayName )
                    .setRecipientEmail( user.getEmail() )
                    .build();

            final var loggingMessageSupplier = new EmailNotification( MessageType.INVITATION_MESSAGE_TYPE, StaticPropertyUtil.APPLICATION_NAMESPACE, user.getEmail(), companyDetails.getCompanyNumber() );
            try {
                emailProducer.sendEmail( emailData, MessageType.INVITATION_MESSAGE_TYPE.getValue() );
                LOG.infoContext( xRequestId, loggingMessageSupplier.toMessage(), null );
            } catch ( Exception exception ){
                LOG.errorContext( xRequestId, new Exception( loggingMessageSupplier.toMessageSendingFailureLoggingMessage() ), null );
                throw exception;
            }
        });
    }

    @Async
    public void sendInvitationAcceptedEmailToAssociatedUsers(final String xRequestId, final CompanyDetails companyDetails, final String inviterDisplayName, final String inviteeDisplayName, final List<String> userIds ) {
        userIds .forEach( userId -> {
            final var user = usersService.fetchUserDetails( userId );

            final var emailData = new InvitationAcceptedEmailBuilder()
                    .setCompanyName( companyDetails.getCompanyName() )
                    .setInviteeDisplayName( inviteeDisplayName )
                    .setInviterDisplayName( inviterDisplayName )
                    .setRecipientEmail( user.getEmail() )
                    .build();


            final var loggingMessageSupplier = new EmailNotification( MessageType.INVITATION_ACCEPTED_MESSAGE_TYPE, StaticPropertyUtil.APPLICATION_NAMESPACE, user.getEmail(), companyDetails.getCompanyNumber() );
            try {
                emailProducer.sendEmail( emailData, MessageType.INVITATION_ACCEPTED_MESSAGE_TYPE.getValue() );
                LOG.infoContext(xRequestId, loggingMessageSupplier.toMessage(), null);
            } catch ( Exception exception ){
                LOG.errorContext( xRequestId, new Exception( loggingMessageSupplier.toMessageSendingFailureLoggingMessage() ), null );
                throw exception;
            }
        });
    }

    @Async
    public void sendInvitationRejectedEmailToAssociatedUsers(final String xRequestId, final CompanyDetails companyDetails, final String inviteeDisplayName, final List<String> userIds ) {
        userIds.forEach( userId -> {
            final var user = usersService.fetchUserDetails( userId );

            final var emailData = new InvitationRejectedEmailBuilder()
                    .setCompanyName( companyDetails.getCompanyName() )
                    .setInviteeDisplayName( inviteeDisplayName )
                    .setRecipientEmail( user.getEmail() )
                    .build();


            final var loggingMessageSupplier = new EmailNotification( MessageType.INVITATION_REJECTED_MESSAGE_TYPE, StaticPropertyUtil.APPLICATION_NAMESPACE, user.getEmail(), companyDetails.getCompanyNumber() );
            try {
                emailProducer.sendEmail( emailData, MessageType.INVITATION_REJECTED_MESSAGE_TYPE.getValue() );
                LOG.infoContext( xRequestId, loggingMessageSupplier.toMessage(), null );
            } catch ( Exception exception ){
                LOG.errorContext( xRequestId, new Exception( loggingMessageSupplier.toMessageSendingFailureLoggingMessage() ), null );
                throw exception;
            }
        });
    }

    @Async
    public void sendInviteEmail( final String xRequestId, final CompanyDetails companyDetails, final String inviterDisplayName, final String invitationExpiryTimestamp, final String inviteeEmail ){
        final var emailData = new InviteEmailBuilder()
                .setRecipientEmail( inviteeEmail )
                .setInviterDisplayName( inviterDisplayName )
                .setCompanyName( companyDetails.getCompanyName() )
                .setInvitationExpiryTimestamp( invitationExpiryTimestamp )
                .setInvitationLink( invitationLink )
                .build();

        final var loggingMessageSupplier = new EmailNotification( MessageType.INVITE_MESSAGE_TYPE, StaticPropertyUtil.APPLICATION_NAMESPACE, inviteeEmail, companyDetails.getCompanyNumber() ).setInvitationExpiryTimestamp( invitationExpiryTimestamp );
        try {
            emailProducer.sendEmail( emailData, MessageType.INVITE_MESSAGE_TYPE.getValue() );
            LOG.infoContext( xRequestId, loggingMessageSupplier.toMessage(), null );
        } catch ( Exception exception ){
            LOG.errorContext( xRequestId, new Exception( loggingMessageSupplier.toMessageSendingFailureLoggingMessage() ), null );
            throw exception;
        }
    }

    @Async
    public void sendInviteCancelledEmail( final String xRequestId, final CompanyDetails companyDetails, final String cancelledByDisplayName, final Supplier<User> inviteeUserSupplier ){
        final var inviteeEmail = inviteeUserSupplier.get().getEmail();

        final var emailData = new InviteCancelledEmailBuilder()
                .setRecipientEmail( inviteeEmail )
                .setCompanyName( companyDetails.getCompanyName() )
                .setCancelledBy( cancelledByDisplayName )
                .build();

        final var loggingMessageSupplier = new EmailNotification( MessageType.INVITE_CANCELLED_MESSAGE_TYPE, StaticPropertyUtil.APPLICATION_NAMESPACE, inviteeEmail, companyDetails.getCompanyNumber() );
        try {
            emailProducer.sendEmail( emailData, MessageType.INVITE_CANCELLED_MESSAGE_TYPE.getValue() );
            LOG.infoContext( xRequestId, loggingMessageSupplier.toMessage(), null );
        } catch ( Exception exception ){
            LOG.errorContext( xRequestId, new Exception( loggingMessageSupplier.toMessageSendingFailureLoggingMessage() ), null );
            throw exception;
        }
    }

}
