package uk.gov.companieshouse.accounts.association.service;

import java.util.Objects;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.email.EmailNotification;
import uk.gov.companieshouse.accounts.association.models.email.data.*;
import uk.gov.companieshouse.accounts.association.utils.MessageType;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.model.EmailData;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static uk.gov.companieshouse.accounts.association.utils.MessageType.*;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.APPLICATION_NAMESPACE;

@Service
@ComponentScan( basePackages = "uk.gov.companieshouse.email_producer" )
public class EmailService {

    @Value( "${invitation.url}" )
    private String invitationLink;

    protected static final Logger LOG = LoggerFactory.getLogger( APPLICATION_NAMESPACE );

    private final UsersService usersService;
    private final EmailProducer emailProducer;

    @Autowired
    public EmailService( final UsersService usersService, final EmailProducer emailProducer ) {
        this.usersService = usersService;
        this.emailProducer = emailProducer;
    }

    private void sendEmail( final String xRequestId, final MessageType messageType, final EmailData emailData, final EmailNotification logMessageSupplier ){
        try {
            emailProducer.sendEmail( emailData, messageType.getValue() );
            LOG.infoContext( xRequestId, logMessageSupplier.toMessage(), null );
        } catch ( Exception exception ){
            LOG.errorContext( xRequestId, new Exception( logMessageSupplier.toMessageSendingFailureLoggingMessage() ), null );
            throw exception;
        }
    }

    public Function<String, Mono<Void>> sendAuthCodeConfirmationEmailToAssociatedUser( final String xRequestId, final CompanyDetails companyDetails, final String displayName ) {
        return userId -> Mono.just( userId )
                .map( user -> usersService.fetchUserDetails( user, xRequestId ) )
                .map( user -> new AuthCodeConfirmationEmailData()
                        .to( user.getEmail() )
                        .authorisedPerson( displayName )
                        .companyName( companyDetails.getCompanyName() ) )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( AUTH_CODE_CONFIRMATION_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyDetails.getCompanyNumber() );
                    sendEmail( xRequestId, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }

    public Function<String, Mono<Void>> sendAuthorisationRemovedEmailToAssociatedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String removedByDisplayName, final String removedUserDisplayName ) {
        return userId -> Mono.just( userId )
                .map( user -> usersService.fetchUserDetails( user, xRequestId ) )
                .map( user -> new AuthorisationRemovedEmailData()
                        .personWhoRemovedAuthorisation( removedByDisplayName )
                        .personWhoWasRemoved( removedUserDisplayName )
                        .to( user.getEmail() ) )
                .zipWith( companyName, AuthorisationRemovedEmailData::companyName )
                .map( AuthorisationRemovedEmailData :: subject )
                .doOnNext( emailData -> {
                        final var logMessageSupplier = new EmailNotification( AUTHORISATION_REMOVED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
                        sendEmail( xRequestId, AUTHORISATION_REMOVED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }

    public Mono<Void> sendAuthorisationRemovedEmailToRemovedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String removedByDisplayName, final String userId ) {
       return Mono.just( userId )
               .map( user -> usersService.fetchUserDetails( user, xRequestId ) )
               .map( user -> new YourAuthorisationRemovedEmailData()
                       .personWhoRemovedAuthorisation( removedByDisplayName )
                       .to( user.getEmail() ) )
               .zipWith( companyName, YourAuthorisationRemovedEmailData::companyName )
               .map( YourAuthorisationRemovedEmailData::subject )
               .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE, removedByDisplayName, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
               .then();
    }

    public Function<String, Mono<Void>> sendInvitationCancelledEmailToAssociatedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String cancelledByDisplayName, final String cancelledUserDisplayName ) {
        return userId -> Mono.just( userId )
                .map( user -> usersService.fetchUserDetails( user, xRequestId ) )
                .map( user -> new InvitationCancelledEmailData()
                        .personWhoCancelledInvite( cancelledByDisplayName )
                        .personWhoWasCancelled( cancelledUserDisplayName )
                        .to( user.getEmail() ) )
                .zipWith( companyName, InvitationCancelledEmailData::companyName )
                .map( InvitationCancelledEmailData::subject )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( INVITATION_CANCELLED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, INVITATION_CANCELLED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }

    public Function<String, Mono<Void>> sendInvitationEmailToAssociatedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String inviterDisplayName, final String inviteeDisplayName ) {
        return userId -> Mono.just( userId )
                .map( user -> usersService.fetchUserDetails( user, xRequestId ) )
                .map( user -> new InvitationEmailData( )
                        .invitee( inviteeDisplayName )
                        .personWhoCreatedInvite( inviterDisplayName )
                        .to( user.getEmail() ) )
                .zipWith( companyName, InvitationEmailData::companyName )
                .map( InvitationEmailData :: subject )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( INVITATION_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, INVITATION_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }

    public Function<String, Mono<Void>> sendInvitationAcceptedEmailToAssociatedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final Mono<String> personWhoCreatedInvite, final String inviteeDisplayName ) {
        return userId -> Mono.just( userId )
                .map( user -> usersService.fetchUserDetails( user, xRequestId ) )
                .map( user -> new InvitationAcceptedEmailData()
                        .personWhoCreatedInvite( inviteeDisplayName )
                        .to( user.getEmail() ) )
                .zipWith( personWhoCreatedInvite, InvitationAcceptedEmailData::personWhoCreatedInvite )
                .zipWith( companyName, InvitationAcceptedEmailData::companyName )
                .map( InvitationAcceptedEmailData :: subject )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( INVITATION_ACCEPTED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, INVITATION_ACCEPTED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }

    public Function<String, Mono<Void>> sendInvitationRejectedEmailToAssociatedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String inviteeDisplayName ) {
        return userId -> Mono.just( userId )
                .map( user -> usersService.fetchUserDetails( user, xRequestId ) )
                .map( user -> new InvitationRejectedEmailData()
                        .personWhoDeclined( inviteeDisplayName )
                        .to( user.getEmail() ) )
                .zipWith( companyName, InvitationRejectedEmailData::companyName )
                .map( InvitationRejectedEmailData :: subject )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( INVITATION_REJECTED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, INVITATION_REJECTED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }

    public Mono<Void> sendInviteEmail( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String inviterDisplayName, final String invitationExpiryTimestamp, final String inviteeEmail ){
        return Mono.just( new InviteEmailData()
                        .to( inviteeEmail )
                        .inviterDisplayName( inviterDisplayName )
                        .invitationExpiryTimestamp( invitationExpiryTimestamp )
                        .invitationLink( invitationLink ) )
                .zipWith( companyName, InviteEmailData::companyName )
                .map( InviteEmailData :: subject )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( INVITE_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber ).setInvitationExpiryTimestamp( invitationExpiryTimestamp );
                    sendEmail( xRequestId, INVITE_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }

    public Mono<Void> sendInviteCancelledEmail( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String cancelledByDisplayName, final AssociationDao associationDao ){
        return Mono.just( associationDao )
                .filter( dao -> Objects.nonNull( dao.getUserId() ) )
                .map( AssociationDao::getUserId )
                .map( user -> usersService.fetchUserDetails( user, xRequestId ) )
                .map( User::getEmail )
                .switchIfEmpty( Mono.just( associationDao ).map( AssociationDao::getUserEmail ) )
                .map( inviteeEmail -> new InviteCancelledEmailData()
                        .to( inviteeEmail )
                        .cancelledBy( cancelledByDisplayName ) )
                .zipWith( companyName, InviteCancelledEmailData::companyName )
                .map( InviteCancelledEmailData :: subject )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( INVITE_CANCELLED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, INVITE_CANCELLED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }
    public Mono<Void> sendDelegatedRemovalOfMigratedEmail( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String removedBy, final String recipientEmail ) {
        return Mono.just( new DelegatedRemovalOfMigratedEmailData()
                        .removedBy( removedBy )
                        .to( recipientEmail ) )
                .zipWith( companyName, DelegatedRemovalOfMigratedEmailData::companyName )
                .map( DelegatedRemovalOfMigratedEmailData::subject)
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( DELEGATED_REMOVAL_OF_MIGRATED, removedBy, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, DELEGATED_REMOVAL_OF_MIGRATED, emailData, logMessageSupplier ); } )
                .then();
    }

    public Mono<Void> sendRemoveOfOwnMigratedEmail( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String userId ) {
        return Mono.just( userId )
                .map( user -> usersService.fetchUserDetails( user, xRequestId ) )
                .map( user -> new RemovalOfOwnMigratedEmailData()
                        .to( user.getEmail() ) )
                .zipWith( companyName, RemovalOfOwnMigratedEmailData::companyName )
                .map( RemovalOfOwnMigratedEmailData :: subject )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( REMOVAL_OF_OWN_MIGRATED, emailData.getTo(), emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, REMOVAL_OF_OWN_MIGRATED, emailData, logMessageSupplier ); } )
                .then();
    }

    public Function<String, Mono<Void>> sendDelegatedRemovalOfMigratedBatchEmail(final String xRequestId, final String companyNumber, final Mono<String> companyName, final String removedBy, final String removedUser ) {
        return userId -> Mono.just( userId )
                .map( user -> usersService.fetchUserDetails( user, xRequestId ) )
                .map( user -> new DelegatedRemovalOfMigratedBatchEmailData()
                        .removedBy( removedBy)
                        .removedUser( removedUser )
                        .to( user.getEmail() ) )
                .zipWith( companyName, DelegatedRemovalOfMigratedBatchEmailData::companyName )
                .map( DelegatedRemovalOfMigratedBatchEmailData :: subject )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( DELEGATED_REMOVAL_OF_MIGRATED_BATCH, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, DELEGATED_REMOVAL_OF_MIGRATED_BATCH, emailData, logMessageSupplier ); } )
                .then();
    }

}
