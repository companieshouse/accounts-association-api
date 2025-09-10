//package uk.gov.companieshouse.accounts.association.service;
//
//import java.util.Objects;
//import java.util.function.Function;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.ComponentScan;
//import org.springframework.stereotype.Service;
//import reactor.core.publisher.Mono;
//import uk.gov.companieshouse.accounts.association.models.AssociationDao;
//import uk.gov.companieshouse.accounts.association.models.email.EmailNotification;
//import uk.gov.companieshouse.accounts.association.models.email.builders.*;
//import uk.gov.companieshouse.accounts.association.utils.MessageType;
//import uk.gov.companieshouse.api.accounts.user.model.User;
//import uk.gov.companieshouse.email_producer.EmailProducer;
//import uk.gov.companieshouse.email_producer.model.EmailData;
//import uk.gov.companieshouse.logging.Logger;
//import uk.gov.companieshouse.logging.LoggerFactory;
//
//import static uk.gov.companieshouse.accounts.association.utils.MessageType.*;
//import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.APPLICATION_NAMESPACE;
//
//@Service
//@ComponentScan( basePackages = "uk.gov.companieshouse.email_producer" )
//public class EmailService {
//
//    @Value( "${invitation.url}" )
//    private String invitationLink;
//
//    protected static final Logger LOG = LoggerFactory.getLogger( APPLICATION_NAMESPACE );
//
//    private final UsersService usersService;
//    private final EmailProducer emailProducer;
//
//    @Autowired
//    public EmailService( final UsersService usersService, final EmailProducer emailProducer ) {
//        this.usersService = usersService;
//        this.emailProducer = emailProducer;
//    }
//
//    private void sendEmail( final String xRequestId, final MessageType messageType, final EmailData emailData, final EmailNotification logMessageSupplier ){
//        try {
//            emailProducer.sendEmail( emailData, messageType.getValue() );
//            LOG.infoContext( xRequestId, logMessageSupplier.toMessage(), null );
//        } catch ( Exception exception ){
//            LOG.errorContext( xRequestId, new Exception( logMessageSupplier.toMessageSendingFailureLoggingMessage() ), null );
//            throw exception;
//        }
//    }
//
//    public Function<String, Mono<Void>> sendAuthCodeConfirmationEmailToAssociatedUser( final String xRequestId, final String companyNumber, Mono<String> companyName, final String displayName ) {
//        return userId -> Mono.just( userId )
//                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
//                .map( user -> new AuthCodeConfirmationEmailBuilder()
//                        .setRecipientEmail( user.getEmail() )
//                        .setDisplayName( displayName ) )
//                .zipWith( companyName, AuthCodeConfirmationEmailBuilder::setCompanyName )
//                .map( AuthCodeConfirmationEmailBuilder::build )
//                .doOnNext( emailData -> {
//                    final var logMessageSupplier = new EmailNotification( AUTH_CODE_CONFIRMATION_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
//                    sendEmail( xRequestId, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
//                .then();
//    }
//
//    public Function<String, Mono<Void>> sendAuthorisationRemovedEmailToAssociatedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String removedByDisplayName, final String removedUserDisplayName ) {
//        return userId -> Mono.just( userId )
//                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
//                .map( user -> new AuthorisationRemovedEmailBuilder()
//                        .setRemovedByDisplayName( removedByDisplayName )
//                        .setRemovedUserDisplayName( removedUserDisplayName )
//                        .setRecipientEmail( user.getEmail() ) )
//                .zipWith( companyName, AuthorisationRemovedEmailBuilder::setCompanyName )
//                .map( AuthorisationRemovedEmailBuilder::build )
//                .doOnNext( emailData -> {
//                        final var logMessageSupplier = new EmailNotification( AUTHORISATION_REMOVED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
//                        sendEmail( xRequestId, AUTHORISATION_REMOVED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
//                .then();
//    }
//
//    public Mono<Void> sendAuthorisationRemovedEmailToRemovedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String removedByDisplayName, final String userId ) {
//       return Mono.just( userId )
//               .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
//               .map( user -> new YourAuthorisationRemovedEmailBuilder()
//                       .setRemovedByDisplayName( removedByDisplayName )
//                       .setRecipientEmail( user.getEmail() ) )
//               .zipWith( companyName, YourAuthorisationRemovedEmailBuilder::setCompanyName )
//               .map( YourAuthorisationRemovedEmailBuilder::build )
//               .doOnNext( emailData -> {
//                    final var logMessageSupplier = new EmailNotification( YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE, removedByDisplayName, emailData.getTo(), companyNumber );
//                    sendEmail( xRequestId, YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
//               .then();
//    }
//
//    public Function<String, Mono<Void>> sendInvitationCancelledEmailToAssociatedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String cancelledByDisplayName, final String cancelledUserDisplayName ) {
//        return userId -> Mono.just( userId )
//                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
//                .map( user -> new InvitationCancelledEmailBuilder()
//                        .setCancelledByDisplayName( cancelledByDisplayName )
//                        .setCancelledUserDisplayName( cancelledUserDisplayName )
//                        .setRecipientEmail( user.getEmail() ) )
//                .zipWith( companyName, InvitationCancelledEmailBuilder::setCompanyName )
//                .map( InvitationCancelledEmailBuilder::build )
//                .doOnNext( emailData -> {
//                    final var logMessageSupplier = new EmailNotification( INVITATION_CANCELLED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
//                    sendEmail( xRequestId, INVITATION_CANCELLED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
//                .then();
//    }
//
//    public Function<String, Mono<Void>> sendInvitationEmailToAssociatedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String inviterDisplayName, final String inviteeDisplayName ) {
//        return userId -> Mono.just( userId )
//                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
//                .map( user -> new InvitationEmailBuilder()
//                        .setInviteeDisplayName( inviteeDisplayName )
//                        .setInviterDisplayName( inviterDisplayName )
//                        .setRecipientEmail( user.getEmail() ) )
//                .zipWith( companyName, InvitationEmailBuilder::setCompanyName )
//                .map( InvitationEmailBuilder::build )
//                .doOnNext( emailData -> {
//                    final var logMessageSupplier = new EmailNotification( INVITATION_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
//                    sendEmail( xRequestId, INVITATION_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
//                .then();
//    }
//
//    public Function<String, Mono<Void>> sendInvitationAcceptedEmailToAssociatedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final Mono<String> invitedByDisplayName, final String inviteeDisplayName ) {
//        return userId -> Mono.just( userId )
//                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
//                .map( user -> new InvitationAcceptedEmailBuilder()
//                                .setInviteeDisplayName( inviteeDisplayName )
//                                .setRecipientEmail( user.getEmail() ) )
//                .zipWith( invitedByDisplayName, InvitationAcceptedEmailBuilder::setInviterDisplayName )
//                .zipWith( companyName, InvitationAcceptedEmailBuilder::setCompanyName )
//                .map( InvitationAcceptedEmailBuilder::build )
//                .doOnNext( emailData -> {
//                    final var logMessageSupplier = new EmailNotification( INVITATION_ACCEPTED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
//                    sendEmail( xRequestId, INVITATION_ACCEPTED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
//                .then();
//    }
//
//    public Function<String, Mono<Void>> sendInvitationRejectedEmailToAssociatedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String inviteeDisplayName ) {
//        return userId -> Mono.just( userId )
//                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
//                .map( user -> new InvitationRejectedEmailBuilder()
//                                .setInviteeDisplayName( inviteeDisplayName )
//                                .setRecipientEmail( user.getEmail() ) )
//                .zipWith( companyName, InvitationRejectedEmailBuilder::setCompanyName )
//                .map( InvitationRejectedEmailBuilder::build )
//                .doOnNext( emailData -> {
//                    final var logMessageSupplier = new EmailNotification( INVITATION_REJECTED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
//                    sendEmail( xRequestId, INVITATION_REJECTED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
//                .then();
//    }
//
//    public Mono<Void> sendInviteEmail( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String inviterDisplayName, final String invitationExpiryTimestamp, final String inviteeEmail ){
//        return Mono.just( new InviteEmailBuilder()
//                        .setRecipientEmail( inviteeEmail )
//                        .setInviterDisplayName( inviterDisplayName )
//                        .setInvitationExpiryTimestamp( invitationExpiryTimestamp )
//                        .setInvitationLink( invitationLink ) )
//                .zipWith( companyName, InviteEmailBuilder::setCompanyName )
//                .map( InviteEmailBuilder::build )
//                .doOnNext( emailData -> {
//                    final var logMessageSupplier = new EmailNotification( INVITE_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber ).setInvitationExpiryTimestamp( invitationExpiryTimestamp );
//                    sendEmail( xRequestId, INVITE_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
//                .then();
//    }
//
//    public Mono<Void> sendInviteCancelledEmail( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String cancelledByDisplayName, final AssociationDao associationDao ){
//        return Mono.just( associationDao )
//                .filter( dao -> Objects.nonNull( dao.getUserId() ) )
//                .map( AssociationDao::getUserId )
//                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
//                .map( User::getEmail )
//                .switchIfEmpty( Mono.just( associationDao ).map( AssociationDao::getUserEmail ) )
//                .map( inviteeEmail -> new InviteCancelledEmailBuilder()
//                        .setRecipientEmail( inviteeEmail )
//                        .setCancelledBy( cancelledByDisplayName ) )
//                .zipWith( companyName, InviteCancelledEmailBuilder::setCompanyName )
//                .map( InviteCancelledEmailBuilder::build )
//                .doOnNext( emailData -> {
//                    final var logMessageSupplier = new EmailNotification( INVITE_CANCELLED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
//                    sendEmail( xRequestId, INVITE_CANCELLED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
//                .then();
//    }
//    public Mono<Void> sendDelegatedRemovalOfMigratedEmail( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String removedBy, final String recipientEmail ) {
//        return Mono.just( new DelegatedRemovalOfMigratedEmailBuilder()
//                        .setRemovedBy( removedBy )
//                        .setRecipientEmail( recipientEmail ) )
//                .zipWith( companyName, DelegatedRemovalOfMigratedEmailBuilder::setCompanyName )
//                .map( DelegatedRemovalOfMigratedEmailBuilder::build )
//                .doOnNext( emailData -> {
//                    final var logMessageSupplier = new EmailNotification( DELEGATED_REMOVAL_OF_MIGRATED, removedBy, emailData.getTo(), companyNumber );
//                    sendEmail( xRequestId, DELEGATED_REMOVAL_OF_MIGRATED, emailData, logMessageSupplier ); } )
//                .then();
//    }
//
//    public Mono<Void> sendRemoveOfOwnMigratedEmail( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String userId ) {
//        return Mono.just( userId )
//                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
//                .map( user -> new RemovalOfOwnMigratedEmailBuilder()
//                        .setRecipientEmail( user.getEmail() ) )
//                .zipWith( companyName, RemovalOfOwnMigratedEmailBuilder::setCompanyName )
//                .map( RemovalOfOwnMigratedEmailBuilder::build )
//                .doOnNext( emailData -> {
//                    final var logMessageSupplier = new EmailNotification( REMOVAL_OF_OWN_MIGRATED, emailData.getTo(), emailData.getTo(), companyNumber );
//                    sendEmail( xRequestId, REMOVAL_OF_OWN_MIGRATED, emailData, logMessageSupplier ); } )
//                .then();
//    }
//
//    public Function<String, Mono<Void>> sendDelegatedRemovalOfMigratedBatchEmail(final String xRequestId, final String companyNumber, final Mono<String> companyName, final String removedBy, final String removedUser ) {
//        return userId -> Mono.just( userId )
//                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
//                .map( user -> new DelegatedRemovalOfMigratedBatchEmailBuilder()
//                        .setRemovedBy( removedBy )
//                        .setRemovedUser( removedUser )
//                        .setRecipientEmail( user.getEmail() ) )
//                .zipWith( companyName, DelegatedRemovalOfMigratedBatchEmailBuilder::setCompanyName )
//                .map( DelegatedRemovalOfMigratedBatchEmailBuilder::build )
//                .doOnNext( emailData -> {
//                    final var logMessageSupplier = new EmailNotification( DELEGATED_REMOVAL_OF_MIGRATED_BATCH, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
//                    sendEmail( xRequestId, DELEGATED_REMOVAL_OF_MIGRATED_BATCH, emailData, logMessageSupplier ); } )
//                .then();
//    }
//
//}
