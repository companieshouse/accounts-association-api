package uk.gov.companieshouse.accounts.association.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.client.EmailClient;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.factory.SendEmailFactory;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData;
import uk.gov.companieshouse.accounts.association.models.email.EmailNotification;
import uk.gov.companieshouse.accounts.association.models.email.builders.AuthCodeConfirmationEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.AuthorisationRemovedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.DelegatedRemovalOfMigratedBatchEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.DelegatedRemovalOfMigratedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationAcceptedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationCancelledEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InvitationRejectedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InviteCancelledEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.InviteEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.ReaDigitalAuthChangedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.RemovalOfOwnMigratedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.builders.YourAuthorisationRemovedEmailBuilder;
import uk.gov.companieshouse.accounts.association.models.email.data.EmailData;
import uk.gov.companieshouse.accounts.association.utils.MessageType;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.chskafka.SendEmail;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_UPDATE_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.models.Constants.DEFAULT_DISPLAY_NAME;
import static uk.gov.companieshouse.accounts.association.models.context.RequestContext.setRequestContext;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.DELEGATED_REMOVAL_OF_MIGRATED;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.DELEGATED_REMOVAL_OF_MIGRATED_BATCH;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_ACCEPTED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_CANCELLED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITATION_REJECTED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITE_CANCELLED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.INVITE_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.REA_DIGITAL_AUTHORISATION_ADDED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.REA_DIGITAL_AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.REMOVAL_OF_OWN_MIGRATED;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.YOUR_AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.hasAdminPrivilege;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.isAPIKeyRequest;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.accounts.association.AccountsAssociationServiceApplication.APPLICATION_NAMESPACE;
import static uk.gov.companieshouse.accounts.association.models.Constants.DAYS_SINCE_INVITE_TILL_EXPIRES;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.isRequestingUser;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.mapToDisplayValue;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.REMOVED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.UNAUTHORISED;

@Service
public class EmailService {

    @Value( "${invitation.url}" )
    private String invitationLink;

    protected static final Logger LOG = LoggerFactory.getLogger( APPLICATION_NAMESPACE );

    private final UsersService usersService;
    private final CompanyService companyService;
    private final AssociationsService associationsService;
    private final EmailClient emailClient;
    private final SendEmailFactory sendEmailFactory;
    @Autowired
    public EmailService(final UsersService usersService, final CompanyService companyService, final AssociationsService associationsService, EmailClient emailClient, SendEmailFactory sendEmailFactory) {
        this.usersService = usersService;
        this.companyService = companyService;
        this.associationsService = associationsService;
        this.emailClient = emailClient;
        this.sendEmailFactory = sendEmailFactory;
    }

    private enum StatusUpdateEmailScenario {
        REJECTING_INVITATION,
        REMOVING_AUTHORISATION,
        ACCEPTING_INVITATION,
        CANCELLING_ANOTHER_USERS_INVITATION,
        REMOVING_ANOTHER_USERS_MIGRATED_ASSOCIATION,
        REMOVING_OWN_MIGRATED_ASSOCIATION,
        INVITING_USER,
        CONFIRMING_WITH_AUTH_CODE,
        NONE
    }

    @Async
    public void sendStatusUpdateEmails( final AssociationDao targetAssociation, final User targetUser, final StatusEnum newStatus, final
            RequestContextData requestContextData) {
        setRequestContext(requestContextData);

        final var xRequestId = getXRequestId();
        final var companyNumber = targetAssociation.getCompanyNumber();
        final var requestingUserDisplayValue = isAPIKeyRequest() || hasAdminPrivilege( ADMIN_UPDATE_PERMISSION ) ? COMPANIES_HOUSE : mapToDisplayValue( getUser(), getUser().getEmail() );
        final var targetUserDisplayValue = mapToDisplayValue( targetUser, targetAssociation.getUserEmail() );
        final var targetUserEmail = Optional.ofNullable( targetUser ).map( User::getEmail ).orElse( targetAssociation.getUserEmail() );

        final var cachedCompanyName = Mono
                .just( companyNumber )
                .map( companyService::fetchCompanyProfile )
                .map( CompanyDetails::getCompanyName )
                .cache();

        final var cachedAssociatedUsers = Mono
                .just( companyNumber )
                .flatMapMany( associationsService::fetchConfirmedUserIds )
                .cache();

        final Flux<Void> emails = switch ( determineStatusUpdateEmailScenario( targetAssociation, newStatus ) ) {
            case REJECTING_INVITATION -> cachedAssociatedUsers.flatMap( sendInvitationRejectedEmailToAssociatedUser( xRequestId, companyNumber, cachedCompanyName, requestingUserDisplayValue ) );
            case REMOVING_AUTHORISATION -> Flux.concat(
                    sendAuthorisationRemovedEmailToRemovedUser( xRequestId, companyNumber, cachedCompanyName, requestingUserDisplayValue, targetAssociation.getUserId() ),
                    cachedAssociatedUsers.flatMap( sendAuthorisationRemovedEmailToAssociatedUser( xRequestId, companyNumber, cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ),
                    sendReaDigitalAuthorisationRemovedEmail( xRequestId, companyNumber, cachedCompanyName ) );
            case ACCEPTING_INVITATION -> Flux.concat(
                    cachedAssociatedUsers.flatMap( sendInvitationAcceptedEmailToAssociatedUser( xRequestId, companyNumber, cachedCompanyName, fetchInvitedByDisplayName( targetAssociation, xRequestId ), requestingUserDisplayValue ) ),
                    sendReaDigitalAuthorisationAddedEmail( xRequestId, companyNumber, cachedCompanyName ) );
            case CANCELLING_ANOTHER_USERS_INVITATION -> Flux.concat(
                    sendInviteCancelledEmail( xRequestId, companyNumber, cachedCompanyName, requestingUserDisplayValue, targetAssociation ),
                    cachedAssociatedUsers.flatMap( sendInvitationCancelledEmailToAssociatedUser( xRequestId, companyNumber, cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ) );
            case REMOVING_ANOTHER_USERS_MIGRATED_ASSOCIATION -> Flux.concat(
                    sendDelegatedRemovalOfMigratedEmail( xRequestId, companyNumber, cachedCompanyName, requestingUserDisplayValue, targetUserEmail ),
                    cachedAssociatedUsers.flatMap( sendDelegatedRemovalOfMigratedBatchEmail( xRequestId, companyNumber, cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ) );
            case REMOVING_OWN_MIGRATED_ASSOCIATION -> Flux.concat(
                    sendRemoveOfOwnMigratedEmail( xRequestId, companyNumber, cachedCompanyName, getEricIdentity() ),
                    cachedAssociatedUsers.flatMap( sendDelegatedRemovalOfMigratedBatchEmail( xRequestId, companyNumber, cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ) );
            case INVITING_USER -> Flux.concat(
                    sendInviteEmail( xRequestId, companyNumber, cachedCompanyName, requestingUserDisplayValue, LocalDateTime.now().plusDays( DAYS_SINCE_INVITE_TILL_EXPIRES ).toString(), targetUserEmail ),
                    cachedAssociatedUsers.flatMap( sendInvitationEmailToAssociatedUser( xRequestId, companyNumber, cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ) );
            case CONFIRMING_WITH_AUTH_CODE -> Flux.concat(
                    cachedAssociatedUsers.flatMap( sendAuthCodeConfirmationEmailToAssociatedUser( xRequestId, companyNumber, cachedCompanyName, targetUserDisplayValue ) ),
                    sendReaDigitalAuthorisationAddedEmail( xRequestId, companyNumber, cachedCompanyName ) );
            case NONE -> Flux.empty();
        };
        emails.subscribe();
    }

    private static StatusUpdateEmailScenario determineStatusUpdateEmailScenario( final AssociationDao targetAssociation, final StatusEnum newStatus ) {
        if ( isAPIKeyRequest() ) {
            return isBeingAuthorised( targetAssociation.getStatus(), newStatus ) ? StatusUpdateEmailScenario.CONFIRMING_WITH_AUTH_CODE : StatusUpdateEmailScenario.NONE;
        }
        return isOAuth2Request() ? determineOAuth2StatusUpdateEmailScenario( targetAssociation, newStatus ) : StatusUpdateEmailScenario.NONE;
    }

    private static StatusUpdateEmailScenario determineOAuth2StatusUpdateEmailScenario( final AssociationDao targetAssociation, final StatusEnum newStatus ) {
        final var oldStatus = targetAssociation.getStatus();
        final var isRequestingUser = isRequestingUser( targetAssociation );
        final var wasAwaitingApproval = oldStatus.equals( AWAITING_APPROVAL.getValue() );
        final var wasMigrated = oldStatus.equals( MIGRATED.getValue() );
        final var isBeingRemoved = newStatus.equals( REMOVED );

        if ( wasAwaitingApproval && isBeingRemoved ) {
            return isRequestingUser ? StatusUpdateEmailScenario.REJECTING_INVITATION : StatusUpdateEmailScenario.CANCELLING_ANOTHER_USERS_INVITATION;
        }
        if ( oldStatus.equals( CONFIRMED.getValue() ) && isBeingRemoved ) {
            return StatusUpdateEmailScenario.REMOVING_AUTHORISATION;
        }
        if ( wasAwaitingApproval && isRequestingUser && newStatus.equals( CONFIRMED ) ) {
            return StatusUpdateEmailScenario.ACCEPTING_INVITATION;
        }
        if ( wasMigrated && isBeingRemoved ) {
            return isRequestingUser ? StatusUpdateEmailScenario.REMOVING_OWN_MIGRATED_ASSOCIATION : StatusUpdateEmailScenario.REMOVING_ANOTHER_USERS_MIGRATED_ASSOCIATION;
        }
        if ( !isRequestingUser && isBeingAuthorised( oldStatus, newStatus ) ) {
            return StatusUpdateEmailScenario.INVITING_USER;
        }
        return StatusUpdateEmailScenario.NONE;
    }

    private static boolean isBeingAuthorised( final String oldStatus, final StatusEnum newStatus ) {
        return oldStatus.equals( MIGRATED.getValue() ) || oldStatus.equals( UNAUTHORISED.getValue() ) && newStatus.equals( CONFIRMED );
    }

    private Mono<String> fetchInvitedByDisplayName( final AssociationDao targetAssociation, final String xRequestId ) {
        return Mono
                .just( targetAssociation )
                .map( AssociationDao::getInvitations )
                .flatMapMany( Flux::fromIterable )
                .reduce( (firstInvitation, secondInvitation) -> firstInvitation.getInvitedAt().isAfter( secondInvitation.getInvitedAt() ) ? firstInvitation : secondInvitation )
                .map( InvitationDao::getInvitedBy )
                .flatMap( invitedBy -> Mono.fromCallable( () -> usersService.fetchUserDetails( invitedBy, xRequestId ) )
                        .map( user -> Optional.ofNullable( user.getDisplayName() ).orElse( user.getEmail() ) )
                        .onErrorResume( NotFoundRuntimeException.class, exception -> {
                            LOG.errorContext( xRequestId, new Exception( "Inviting user not found in accounts-user-api; using placeholder display name in emails", exception ), new HashMap<>( Map.of( "user_id", invitedBy ) ) );
                            return Mono.just( DEFAULT_DISPLAY_NAME );
                        } ) )
                .cache();
    }

    private Mono<User> fetchRecipient( final String userId, final String xRequestId ){
        return usersService.toFetchUserDetailsRequest( userId, xRequestId )
                .onErrorResume( NotFoundRuntimeException.class, exception -> {
                    LOG.errorContext( xRequestId, new Exception( "Recipient not found in accounts-user-api; skipping email to this user", exception ), new HashMap<>( Map.of( "user_id", userId ) ) );
                    return Mono.empty();
                } );
    }

    private void sendEmail(final String xRequestId, final MessageType messageType, final EmailData emailData, final EmailNotification logMessageSupplier) {
        try {
            SendEmail sendEmail = sendEmailFactory.createSendEmail(emailData, messageType.getValue());
            emailClient.sendEmail(sendEmail, xRequestId);
            LOG.infoContext( xRequestId, logMessageSupplier.toMessage(), null );
        } catch ( Exception exception ){
            LOG.errorContext( xRequestId, new Exception( logMessageSupplier.toMessageSendingFailureLoggingMessage() ), null );
            throw exception;
        }
    }

    private Mono<Void> sendReaDigitalAuthorisationChangedEmail(final String xRequestId, final String companyNumber, final Mono<String> companyName, final MessageType messageType) {
        final String rea;
        try {
            rea = companyService.fetchRegisteredEmailAddress(companyNumber);
        } catch (NotFoundRuntimeException e) {
            LOG.infoContext(xRequestId, String.format("Registered email address not found for company: %s; skipping REA email.", companyNumber), null);
            return Mono.empty();
        } catch (InternalServerErrorRuntimeException e) {
            LOG.errorContext(xRequestId, new Exception(String.format("Failed to retrieve REA for company: %s; skipping REA email.", companyNumber), e), null);
            return Mono.empty();
        }
        if (rea == null || rea.isBlank()) {
            return Mono.empty();
        }
        return Mono.just(new ReaDigitalAuthChangedEmailBuilder()
                        .setRecipientEmail(rea)
                        .setCompanyNumber(companyNumber))
                .zipWith(companyName, ReaDigitalAuthChangedEmailBuilder::setCompanyName)
                .map(ReaDigitalAuthChangedEmailBuilder::build)
                .flatMap(emailData -> Mono.fromRunnable(() -> {
                    final var logMessageSupplier = new EmailNotification(messageType, APPLICATION_NAMESPACE, emailData.getTo(), companyNumber);
                    try {
                        sendEmail(xRequestId, messageType, emailData, logMessageSupplier);
                    } catch (Exception ex) {
                        LOG.errorContext(xRequestId, new Exception(String.format("Failed sending REA email for company: %s; continuing without failing request.", companyNumber), ex), null);
                    }
                }))
                .then();
    }

    public Mono<Void> sendReaDigitalAuthorisationAddedEmail(final String xRequestId, final String companyNumber, final Mono<String> companyName) {
        return sendReaDigitalAuthorisationChangedEmail(xRequestId, companyNumber, companyName, REA_DIGITAL_AUTHORISATION_ADDED_MESSAGE_TYPE);
    }

    public Mono<Void> sendReaDigitalAuthorisationRemovedEmail(final String xRequestId, final String companyNumber, final Mono<String> companyName) {
        return sendReaDigitalAuthorisationChangedEmail(xRequestId, companyNumber, companyName, REA_DIGITAL_AUTHORISATION_REMOVED_MESSAGE_TYPE);
    }

    public Function<String, Mono<Void>> sendAuthCodeConfirmationEmailToAssociatedUser( final String xRequestId, final String companyNumber, Mono<String> companyName, final String displayName ) {
        return userId -> Mono.just( userId )
                .flatMap( user -> fetchRecipient( user, xRequestId ) )
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
                .flatMap( user -> fetchRecipient( user, xRequestId ) )
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
               .flatMap( user -> fetchRecipient( user, xRequestId ) )
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
                .flatMap( user -> fetchRecipient( user, xRequestId ) )
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
                .flatMap( user -> fetchRecipient( user, xRequestId ) )
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
                .flatMap( user -> fetchRecipient( user, xRequestId ) )
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
                .flatMap( user -> fetchRecipient( user, xRequestId ) )
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
                .flatMap( user -> fetchRecipient( user, xRequestId ) )
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
                .flatMap( user -> fetchRecipient( user, xRequestId ) )
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
                .flatMap( user -> fetchRecipient( user, xRequestId ) )
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
