package uk.gov.companieshouse.accounts.association.controller;

import static java.time.LocalDateTime.now;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_UPDATE_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.models.Constants.PAGINATION_IS_MALFORMED;
import static uk.gov.companieshouse.accounts.association.models.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToAuthCodeConfirmedUpdated;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToConfirmedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToInvitationUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToRemovedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToUnauthorisedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.hasAdminPrivilege;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.isAPIKeyRequest;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.isOAuth2Request;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.DAYS_SINCE_INVITE_TILL_EXPIRES;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.isRequestingUser;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.mapToDisplayValue;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.REMOVED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.UNAUTHORISED;

import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyAssociationInterface;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.associations.model.PreviousStatesList;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;

@RestController
public class UserCompanyAssociation implements UserCompanyAssociationInterface {

    private final UsersService usersService;
    private final CompanyService companyService;
    private final AssociationsService associationsService;
    private final EmailService emailService;

    public UserCompanyAssociation( final UsersService usersService, final CompanyService companyService, final AssociationsService associationsService, final EmailService emailService ) {
        this.usersService = usersService;
        this.companyService = companyService;
        this.associationsService = associationsService;
        this.emailService = emailService;
    }

    @Override
    public ResponseEntity<Association> getAssociationForId( final String associationId ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with id=%s.", associationId ),null );
        return associationsService.fetchAssociationDto( associationId )
                .map( association -> new ResponseEntity<>( association, OK ) )
                .orElseThrow( () -> new NotFoundRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "Cannot find Association for the id: %s" ) ) );
    }

    @Override
    public ResponseEntity<InvitationsList> getInvitationsForAssociation( final String associationId, final Integer pageIndex, final Integer itemsPerPage ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with id=%s, page_index=%d, items_per_page=%d.", associationId, pageIndex, itemsPerPage ),null );

        if ( pageIndex < 0 || itemsPerPage <= 0 ){
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( PAGINATION_IS_MALFORMED ) );
        }

        return associationsService.fetchInvitations( associationId, pageIndex, itemsPerPage )
                .map( invitations -> new ResponseEntity<>( invitations, OK ) )
                .orElseThrow( () -> new NotFoundRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "Could not find association %s.", associationId ) ) ) );
    }

    @Override
    public ResponseEntity<PreviousStatesList> getPreviousStatesForAssociation( final String associationId, final Integer pageIndex, final Integer itemsPerPage ){
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with id=%s, page_index=%d, items_per_page=%d.", associationId, pageIndex, itemsPerPage ),null );

        if ( pageIndex < 0 || itemsPerPage <= 0 ) {
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( PAGINATION_IS_MALFORMED ) );
        }

        return associationsService.fetchPreviousStates( associationId, pageIndex, itemsPerPage )
                .map( previousStates -> new ResponseEntity<>( previousStates, OK ) )
                .orElseThrow( () -> new NotFoundRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "Association %s was not found", associationId ) ) ) );
    }

    @Override
    public ResponseEntity<Void> updateAssociationStatusForId( final String associationId, final RequestBodyPut requestBody ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with id=%s, user_id=%s, status=%s.", associationId, getEricIdentity(), requestBody.getStatus() ),null );

        final var targetAssociation = associationsService
                .fetchAssociationDao( associationId )
                .orElseThrow( () -> new NotFoundRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "Could not find association %s.", associationId ) ) ) );

        final var targetUser = usersService.fetchUserDetails( targetAssociation );




        final var oldStatus = StatusEnum.fromValue( targetAssociation.getStatus() );
        final var proposedStatus = requestBody.getStatus();

        final var xRequestId = getXRequestId();
        final var requestingUserDisplayValue = isAPIKeyRequest() || hasAdminPrivilege( ADMIN_UPDATE_PERMISSION ) ? COMPANIES_HOUSE : mapToDisplayValue( getUser(), getUser().getEmail() );
        final var targetUserDisplayValue = mapToDisplayValue( targetUser, targetAssociation.getUserEmail() );
        final var targetUserEmail = Optional.ofNullable( targetUser ).map( User::getEmail ).orElse( targetAssociation.getUserEmail() );


        final var cachedAssociatedUsers = getCachedAssociatedUsers(targetAssociation);
        final var cachedCompanyName = getCachedCompanyName(targetAssociation);
        final var cachedInvitedByDisplayName = getCachedInvitedByDisplayName(targetAssociation, xRequestId);

        // TODO: add maps to every block, return status.

        // END STATE: UNAUTHORISED
        final var apiKeyUnauthorisesAssociation = Mono.just( proposedStatus )
                .filter( newStatus -> isAPIKeyRequest() )
                .filter( RequestBodyPut.StatusEnum.UNAUTHORISED::equals )
                .doOnNext( newStatus -> {
                    final var update = mapToUnauthorisedUpdate( targetAssociation, targetUser );
                    associationsService.updateAssociation( targetAssociation.getId(), update );
                    //TODO: Update email, no business information on it yet.
                } );

        // END STATE: CONFIRMED, API KEY
        final var apiKeyConfirmsAssociation = Mono.just( proposedStatus )
                .filter( newStatus -> isAPIKeyRequest() )
                .filter( RequestBodyPut.StatusEnum.CONFIRMED::equals )
                .filter( newStatus -> MIGRATED.equals( oldStatus ) || UNAUTHORISED.equals( oldStatus ) )
                .doOnNext( newStatus -> {
                    final var update = mapToAuthCodeConfirmedUpdated( targetAssociation, targetUser, COMPANIES_HOUSE );
                    associationsService.updateAssociation( targetAssociation.getId(), update );
                } )
                .map( status -> {
                    cachedAssociatedUsers.flatMap(emailService.sendAuthCodeConfirmationEmailToAssociatedUser(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, targetUserDisplayValue)).subscribe();
                    return status;
                });

        // END STATE: REMOVED
        final var canUpdateOwnAssociation = isRequestingUser( targetAssociation );
        final var canUpdateAnotherUsersAssociation = !isRequestingUser( targetAssociation ) && ( associationsService.confirmedAssociationExists( targetAssociation.getCompanyNumber(), getEricIdentity() ) || hasAdminPrivilege( ADMIN_UPDATE_PERMISSION ) );

        final var userRemovesAssociation = Mono.just( proposedStatus )
                .map( x -> x)
                .filter( newStatus -> isOAuth2Request() )
                .filter( RequestBodyPut.StatusEnum.REMOVED::equals )
                .filter( newStatus -> canUpdateOwnAssociation || canUpdateAnotherUsersAssociation )
                .doOnNext( newStatus -> {
                    final var update = mapToRemovedUpdate( targetAssociation, targetUser, getEricIdentity() );
                    associationsService.updateAssociation( targetAssociation.getId(), update );
                    // send emails
                } )
                .map(newStatus -> {
                    final var isRejectingInvitation = isRequestingUser( targetAssociation ) && targetAssociation.getStatus().equals( AWAITING_APPROVAL.getValue() );
                    final var authorisationIsBeingRemoved = targetAssociation.getStatus().equals( CONFIRMED.getValue() );
                    final var isCancellingAnotherUsersInvitation = !isRequestingUser( targetAssociation ) && targetAssociation.getStatus().equals( AWAITING_APPROVAL.getValue() );
                    final var isRemovingAnotherUsersMigratedAssociation = !isRequestingUser( targetAssociation ) && targetAssociation.getStatus().equals( MIGRATED.getValue() );
                    final var isRemovingOwnMigratedAssociation = isRequestingUser( targetAssociation ) && targetAssociation.getStatus().equals( MIGRATED.getValue() );
                    var emails = Flux.empty();

                    if (isRejectingInvitation) {
                        emails = emails.concatWith(cachedAssociatedUsers.flatMap(emailService.sendInvitationRejectedEmailToAssociatedUser(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue)));
                    } else if (authorisationIsBeingRemoved) {
                        emails = emails.concatWith(emailService.sendAuthorisationRemovedEmailToRemovedUser(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetAssociation.getUserId()));
                        emails = emails.concatWith(cachedAssociatedUsers.flatMap(emailService.sendAuthorisationRemovedEmailToAssociatedUser(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue))) ;
                    } else if (isCancellingAnotherUsersInvitation) {
                        emails = emails.concatWith(emailService.sendInviteCancelledEmail(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetAssociation));
                        emails = emails.concatWith(cachedAssociatedUsers.flatMap(emailService.sendInvitationCancelledEmailToAssociatedUser(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue)));
                    } else if (isRemovingAnotherUsersMigratedAssociation) {
                        emails = emails.concatWith(emailService.sendDelegatedRemovalOfMigratedEmail(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserEmail));
                        emails = emails.concatWith(cachedAssociatedUsers.flatMap(emailService.sendDelegatedRemovalOfMigratedBatchEmail(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue)));
                    } else if (isRemovingOwnMigratedAssociation) {
                        emails = emails.concatWith(emailService.sendRemoveOfOwnMigratedEmail(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, getEricIdentity()));
                        emails = emails.concatWith(cachedAssociatedUsers.flatMap(emailService.sendDelegatedRemovalOfMigratedBatchEmail(xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue)));
                    }
                    return emails;
                } );

        // END STATE: CONFIRMED, OAuth
        final var userAcceptsInvitation = Mono.just( proposedStatus )
                .filter( newStatus -> isOAuth2Request() )
                .filter( newStatus -> isRequestingUser( targetAssociation ) )
                .filter( RequestBodyPut.StatusEnum.CONFIRMED::equals )
                .filter( newStatus -> AWAITING_APPROVAL.equals( oldStatus ) )
                .doOnNext( newStatus -> {
                    final var update = mapToConfirmedUpdate( targetAssociation, targetUser, getEricIdentity() );
                    associationsService.updateAssociation( targetAssociation.getId(), update );
                } )
                .flatMapMany( newStatus -> cachedAssociatedUsers.flatMap( emailService.sendInvitationAcceptedEmailToAssociatedUser( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, cachedInvitedByDisplayName, requestingUserDisplayValue ) ) );

        // END STATE: CONFIRMED, OAuth, FROM: MIGRATED/UNAUTHORISED.
        final var userSendsInvitation = Mono.just( proposedStatus )
                .filter( newStatus -> isOAuth2Request() )
                .filter( newStatus -> !isRequestingUser( targetAssociation ) )
                .filter( RequestBodyPut.StatusEnum.CONFIRMED::equals )
                .filter( newStatus -> MIGRATED.equals( oldStatus ) || UNAUTHORISED.equals( oldStatus ) )
                .filter( newStatus -> associationsService.confirmedAssociationExists( targetAssociation.getCompanyNumber(), getEricIdentity() ) )
                .doOnNext( newStatus -> {
                    final var update = mapToInvitationUpdate( targetAssociation, targetUser, getEricIdentity(), now() );
                    associationsService.updateAssociation( targetAssociation.getId(), update );
                } )
                .map( newStatus -> {
                    final var invitationExpiryTimestamp = LocalDateTime.now().plusDays( DAYS_SINCE_INVITE_TILL_EXPIRES ).toString();
                    emailService.sendInviteEmail( xRequestId, targetAssociation.getCompanyNumber(), getCachedCompanyName(targetAssociation), requestingUserDisplayValue, invitationExpiryTimestamp, targetUserEmail ).subscribe();
                    cachedAssociatedUsers.flatMap( emailService.sendInvitationEmailToAssociatedUser( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ).subscribe();
                    return newStatus;
                });

        Flux.concat( apiKeyUnauthorisesAssociation, apiKeyConfirmsAssociation, userRemovesAssociation, userAcceptsInvitation, userSendsInvitation )
                .switchIfEmpty( Mono.error( new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "Caller not permitted to perform action", getEricIdentity(), targetAssociation.getCompanyNumber() ) ) ) ) )
                .blockFirst();

        return new ResponseEntity<>( OK );
    }

    private Mono<String> getCachedCompanyName( final AssociationDao targetAssociation ) {
        return Mono
                .just( targetAssociation.getCompanyNumber() )
                .map( companyService::fetchCompanyProfile )
                .map( CompanyDetails::getCompanyName )
                .cache();
    }

    private Flux<String> getCachedAssociatedUsers( final AssociationDao targetAssociation ) {
        return Mono
                .just( targetAssociation.getCompanyNumber() )
                .flatMapMany( associationsService::fetchConfirmedUserIds )
                .cache();
    }

    private Mono<String> getCachedInvitedByDisplayName( final AssociationDao targetAssociation, final String xRequestId ) {
        return Mono.just( targetAssociation )
                .map( AssociationDao::getInvitations )
                .flatMapMany( Flux::fromIterable )
                .reduce( (firstInvitation, secondInvitation) -> firstInvitation.getInvitedAt().isAfter( secondInvitation.getInvitedAt() ) ? firstInvitation : secondInvitation )
                .map( InvitationDao::getInvitedBy )
                .map( user -> usersService.fetchUserDetails( user, xRequestId ) )
                .map( user -> Optional.ofNullable( user.getDisplayName() ).orElse( user.getEmail() ) )
                .cache();

    }
}
