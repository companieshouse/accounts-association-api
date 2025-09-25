package uk.gov.companieshouse.accounts.association.service;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData;
import uk.gov.companieshouse.accounts.association.models.email.EmailNotification;
import uk.gov.companieshouse.accounts.association.models.email.builders.*;
import uk.gov.companieshouse.accounts.association.utils.MessageType;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.model.EmailData;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_UPDATE_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.models.context.RequestContext.setRequestContext;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.*;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.hasAdminPrivilege;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.isAPIKeyRequest;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.APPLICATION_NAMESPACE;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.DAYS_SINCE_INVITE_TILL_EXPIRES;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.isRequestingUser;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.mapToDisplayValue;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.REMOVED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.UNAUTHORISED;

@Service
@ComponentScan( basePackages = "uk.gov.companieshouse.email_producer" )
public class EmailService {

    @Value( "${invitation.url}" )
    private String invitationLink;

    protected static final Logger LOG = LoggerFactory.getLogger( APPLICATION_NAMESPACE );

    private final UsersService usersService;
    private final CompanyService companyService;
    private final AssociationsService associationsService;
    private final EmailProducer emailProducer;

    @Autowired
    public EmailService( final UsersService usersService, final CompanyService companyService, final AssociationsService associationsService, final EmailProducer emailProducer ) {
        this.usersService = usersService;
        this.companyService = companyService;
        this.emailProducer = emailProducer;
        this.associationsService = associationsService;
    }

    @Async
    public void sendStatusUpdateEmails( final AssociationDao targetAssociation, final User targetUser, final StatusEnum newStatus, final
            RequestContextData requestContextData) {
        setRequestContext(requestContextData);

        final var xRequestId = getXRequestId();
        final var requestingUserDisplayValue = isAPIKeyRequest() || hasAdminPrivilege( ADMIN_UPDATE_PERMISSION ) ? COMPANIES_HOUSE : mapToDisplayValue( getUser(), getUser().getEmail() );
        final var targetUserDisplayValue = mapToDisplayValue( targetUser, targetAssociation.getUserEmail() );
        final var targetUserEmail = Optional.ofNullable( targetUser ).map( User::getEmail ).orElse( targetAssociation.getUserEmail() );
        final var oldStatus = targetAssociation.getStatus();

        final var cachedCompanyName = Mono
                .just( targetAssociation.getCompanyNumber() )
                .map( companyService::fetchCompanyProfile )
                .map( CompanyDetails::getCompanyName )
                .cache();

        final var cachedAssociatedUsers = Mono
                .just( targetAssociation.getCompanyNumber() )
                .flatMapMany( associationsService::fetchConfirmedUserIds )
                .cache();

        final var cachedInvitedByDisplayName = Mono
                .just( targetAssociation )
                .map( AssociationDao::getInvitations )
                .flatMapMany( Flux::fromIterable )
                .reduce( (firstInvitation, secondInvitation) -> firstInvitation.getInvitedAt().isAfter( secondInvitation.getInvitedAt() ) ? firstInvitation : secondInvitation )
                .map( InvitationDao::getInvitedBy )
                .map( user -> usersService.fetchUserDetails( user, xRequestId ) )
                .map( user -> Optional.ofNullable( user.getDisplayName() ).orElse( user.getEmail() ) )
                .cache();

        final var isRejectingInvitation = isOAuth2Request() && isRequestingUser( targetAssociation ) && oldStatus.equals( AWAITING_APPROVAL.getValue() ) && newStatus.equals( REMOVED );
        final var authorisationIsBeingRemoved = isOAuth2Request() && oldStatus.equals( CONFIRMED.getValue() ) && newStatus.equals( REMOVED );
        final var isAcceptingInvitation = isOAuth2Request() && isRequestingUser( targetAssociation ) && oldStatus.equals( AWAITING_APPROVAL.getValue() ) && newStatus.equals( CONFIRMED );
        final var isCancellingAnotherUsersInvitation = isOAuth2Request() && !isRequestingUser( targetAssociation ) && oldStatus.equals( AWAITING_APPROVAL.getValue() ) && newStatus.equals( REMOVED );
        final var isRemovingAnotherUsersMigratedAssociation = isOAuth2Request() && !isRequestingUser( targetAssociation ) && oldStatus.equals( MIGRATED.getValue() ) && newStatus.equals( REMOVED );
        final var isRemovingOwnMigratedAssociation = isOAuth2Request() && isRequestingUser( targetAssociation ) && oldStatus.equals( MIGRATED.getValue() ) && newStatus.equals( REMOVED );
        final var isInvitingUser = isOAuth2Request() && !isRequestingUser( targetAssociation ) && ( oldStatus.equals( MIGRATED.getValue() ) || oldStatus.equals( UNAUTHORISED.getValue() ) && newStatus.equals( CONFIRMED ) );
        final var isConfirmingWithAuthCode = isAPIKeyRequest() && ( oldStatus.equals( MIGRATED.getValue() ) || oldStatus.equals( UNAUTHORISED.getValue() ) && newStatus.equals( CONFIRMED ) );

        var emails = Flux.empty();
        if ( isRejectingInvitation ) {
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( sendInvitationRejectedEmailToAssociatedUser( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue ) ) );
        } else if ( authorisationIsBeingRemoved ) {
            emails = emails.concatWith( sendAuthorisationRemovedEmailToRemovedUser( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetAssociation.getUserId() ) );
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( sendAuthorisationRemovedEmailToAssociatedUser( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ) );
        } else if ( isAcceptingInvitation ) {
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( sendInvitationAcceptedEmailToAssociatedUser( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, cachedInvitedByDisplayName, requestingUserDisplayValue ) ) );
        } else if ( isCancellingAnotherUsersInvitation ) {
            emails = emails.concatWith( sendInviteCancelledEmail( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetAssociation ) );
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( sendInvitationCancelledEmailToAssociatedUser( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ) );
        } else if ( isRemovingAnotherUsersMigratedAssociation ) {
            emails = emails.concatWith( sendDelegatedRemovalOfMigratedEmail( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserEmail ) );
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( sendDelegatedRemovalOfMigratedBatchEmail( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ) );
        } else if ( isRemovingOwnMigratedAssociation ) {
            emails = emails.concatWith( sendRemoveOfOwnMigratedEmail( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, getEricIdentity() ) );
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( sendDelegatedRemovalOfMigratedBatchEmail( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ) );
        } else if ( isInvitingUser ) {
            final var invitationExpiryTimestamp = LocalDateTime.now().plusDays( DAYS_SINCE_INVITE_TILL_EXPIRES ).toString();
            emails = emails.concatWith( sendInviteEmail( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, invitationExpiryTimestamp, targetUserEmail ) );
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( sendInvitationEmailToAssociatedUser( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ) );
        } else if ( isConfirmingWithAuthCode ){
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( sendAuthCodeConfirmationEmailToAssociatedUser( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, targetUserDisplayValue ) ) );
        }
        emails.subscribe();
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

    public Function<String, Mono<Void>> sendAuthCodeConfirmationEmailToAssociatedUser( final String xRequestId, final String companyNumber, Mono<String> companyName, final String displayName ) {
        return userId -> Mono.just( userId )
                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
                .map( user -> new AuthCodeConfirmationEmailBuilder()
                        .setRecipientEmail( user.getEmail() )
                        .setDisplayName( displayName ) )
                .zipWith( companyName, AuthCodeConfirmationEmailBuilder::setCompanyName )
                .map( AuthCodeConfirmationEmailBuilder::build )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( AUTH_CODE_CONFIRMATION_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }

    public Function<String, Mono<Void>> sendAuthorisationRemovedEmailToAssociatedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String removedByDisplayName, final String removedUserDisplayName ) {
        return userId -> Mono.just( userId )
                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
                .map( user -> new AuthorisationRemovedEmailBuilder()
                        .setRemovedByDisplayName( removedByDisplayName )
                        .setRemovedUserDisplayName( removedUserDisplayName )
                        .setRecipientEmail( user.getEmail() ) )
                .zipWith( companyName, AuthorisationRemovedEmailBuilder::setCompanyName )
                .map( AuthorisationRemovedEmailBuilder::build )
                .doOnNext( emailData -> {
                        final var logMessageSupplier = new EmailNotification( AUTHORISATION_REMOVED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
                        sendEmail( xRequestId, AUTHORISATION_REMOVED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }

    public Mono<Void> sendAuthorisationRemovedEmailToRemovedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String removedByDisplayName, final String userId ) {
       return Mono.just( userId )
               .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
               .map( user -> new YourAuthorisationRemovedEmailBuilder()
                       .setRemovedByDisplayName( removedByDisplayName )
                       .setRecipientEmail( user.getEmail() ) )
               .zipWith( companyName, YourAuthorisationRemovedEmailBuilder::setCompanyName )
               .map( YourAuthorisationRemovedEmailBuilder::build )
               .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE, removedByDisplayName, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
               .then();
    }

    public Function<String, Mono<Void>> sendInvitationCancelledEmailToAssociatedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String cancelledByDisplayName, final String cancelledUserDisplayName ) {
        return userId -> Mono.just( userId )
                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
                .map( user -> new InvitationCancelledEmailBuilder()
                        .setCancelledByDisplayName( cancelledByDisplayName )
                        .setCancelledUserDisplayName( cancelledUserDisplayName )
                        .setRecipientEmail( user.getEmail() ) )
                .zipWith( companyName, InvitationCancelledEmailBuilder::setCompanyName )
                .map( InvitationCancelledEmailBuilder::build )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( INVITATION_CANCELLED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, INVITATION_CANCELLED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }

    public Function<String, Mono<Void>> sendInvitationEmailToAssociatedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String inviterDisplayName, final String inviteeDisplayName ) {
        return userId -> Mono.just( userId )
                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
                .map( user -> new InvitationEmailBuilder()
                        .setInviteeDisplayName( inviteeDisplayName )
                        .setInviterDisplayName( inviterDisplayName )
                        .setRecipientEmail( user.getEmail() ) )
                .zipWith( companyName, InvitationEmailBuilder::setCompanyName )
                .map( InvitationEmailBuilder::build )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( INVITATION_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, INVITATION_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }

    public Function<String, Mono<Void>> sendInvitationAcceptedEmailToAssociatedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final Mono<String> invitedByDisplayName, final String inviteeDisplayName ) {
        return userId -> Mono.just( userId )
                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
                .map( user -> new InvitationAcceptedEmailBuilder()
                                .setInviteeDisplayName( inviteeDisplayName )
                                .setRecipientEmail( user.getEmail() ) )
                .zipWith( invitedByDisplayName, InvitationAcceptedEmailBuilder::setInviterDisplayName )
                .zipWith( companyName, InvitationAcceptedEmailBuilder::setCompanyName )
                .map( InvitationAcceptedEmailBuilder::build )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( INVITATION_ACCEPTED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, INVITATION_ACCEPTED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }

    public Function<String, Mono<Void>> sendInvitationRejectedEmailToAssociatedUser( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String inviteeDisplayName ) {
        return userId -> Mono.just( userId )
                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
                .map( user -> new InvitationRejectedEmailBuilder()
                                .setInviteeDisplayName( inviteeDisplayName )
                                .setRecipientEmail( user.getEmail() ) )
                .zipWith( companyName, InvitationRejectedEmailBuilder::setCompanyName )
                .map( InvitationRejectedEmailBuilder::build )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( INVITATION_REJECTED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, INVITATION_REJECTED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }

    public Mono<Void> sendInviteEmail( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String inviterDisplayName, final String invitationExpiryTimestamp, final String inviteeEmail ){
        return Mono.just( new InviteEmailBuilder()
                        .setRecipientEmail( inviteeEmail )
                        .setInviterDisplayName( inviterDisplayName )
                        .setInvitationExpiryTimestamp( invitationExpiryTimestamp )
                        .setInvitationLink( invitationLink ) )
                .zipWith( companyName, InviteEmailBuilder::setCompanyName )
                .map( InviteEmailBuilder::build )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( INVITE_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber ).setInvitationExpiryTimestamp( invitationExpiryTimestamp );
                    sendEmail( xRequestId, INVITE_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }

    public Mono<Void> sendInviteCancelledEmail( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String cancelledByDisplayName, final AssociationDao associationDao ){
        return Mono.just( associationDao )
                .filter( dao -> Objects.nonNull( dao.getUserId() ) )
                .map( AssociationDao::getUserId )
                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
                .map( User::getEmail )
                .switchIfEmpty( Mono.just( associationDao ).map( AssociationDao::getUserEmail ) )
                .map( inviteeEmail -> new InviteCancelledEmailBuilder()
                        .setRecipientEmail( inviteeEmail )
                        .setCancelledBy( cancelledByDisplayName ) )
                .zipWith( companyName, InviteCancelledEmailBuilder::setCompanyName )
                .map( InviteCancelledEmailBuilder::build )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( INVITE_CANCELLED_MESSAGE_TYPE, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, INVITE_CANCELLED_MESSAGE_TYPE, emailData, logMessageSupplier ); } )
                .then();
    }
    public Mono<Void> sendDelegatedRemovalOfMigratedEmail( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String removedBy, final String recipientEmail ) {
        return Mono.just( new DelegatedRemovalOfMigratedEmailBuilder()
                        .setRemovedBy( removedBy )
                        .setRecipientEmail( recipientEmail ) )
                .zipWith( companyName, DelegatedRemovalOfMigratedEmailBuilder::setCompanyName )
                .map( DelegatedRemovalOfMigratedEmailBuilder::build )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( DELEGATED_REMOVAL_OF_MIGRATED, removedBy, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, DELEGATED_REMOVAL_OF_MIGRATED, emailData, logMessageSupplier ); } )
                .then();
    }

    public Mono<Void> sendRemoveOfOwnMigratedEmail( final String xRequestId, final String companyNumber, final Mono<String> companyName, final String userId ) {
        return Mono.just( userId )
                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
                .map( user -> new RemovalOfOwnMigratedEmailBuilder()
                        .setRecipientEmail( user.getEmail() ) )
                .zipWith( companyName, RemovalOfOwnMigratedEmailBuilder::setCompanyName )
                .map( RemovalOfOwnMigratedEmailBuilder::build )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( REMOVAL_OF_OWN_MIGRATED, emailData.getTo(), emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, REMOVAL_OF_OWN_MIGRATED, emailData, logMessageSupplier ); } )
                .then();
    }

    public Function<String, Mono<Void>> sendDelegatedRemovalOfMigratedBatchEmail(final String xRequestId, final String companyNumber, final Mono<String> companyName, final String removedBy, final String removedUser ) {
        return userId -> Mono.just( userId )
                .flatMap( user -> usersService.toFetchUserDetailsRequest( user, xRequestId ) )
                .map( user -> new DelegatedRemovalOfMigratedBatchEmailBuilder()
                        .setRemovedBy( removedBy )
                        .setRemovedUser( removedUser )
                        .setRecipientEmail( user.getEmail() ) )
                .zipWith( companyName, DelegatedRemovalOfMigratedBatchEmailBuilder::setCompanyName )
                .map( DelegatedRemovalOfMigratedBatchEmailBuilder::build )
                .doOnNext( emailData -> {
                    final var logMessageSupplier = new EmailNotification( DELEGATED_REMOVAL_OF_MIGRATED_BATCH, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber );
                    sendEmail( xRequestId, DELEGATED_REMOVAL_OF_MIGRATED_BATCH, emailData, logMessageSupplier ); } )
                .then();
    }

}
