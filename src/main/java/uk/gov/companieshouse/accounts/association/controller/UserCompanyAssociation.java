package uk.gov.companieshouse.accounts.association.controller;

import static java.time.LocalDateTime.now;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_UPDATE_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.models.Constants.PAGINATION_IS_MALFORMED;
import static uk.gov.companieshouse.accounts.association.models.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToConfirmedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToInvitationUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToRemovedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.hasAdminPrivilege;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.isRequestingUser;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.mapToDisplayValue;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.REMOVED;

import java.util.Optional;
import org.springframework.data.mongodb.core.query.Update;
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
                .orElseThrow( () -> new NotFoundRuntimeException( String.format( "Association %s was not found.", associationId ), new Exception( String.format( "Could not find association %s.", associationId ) ) ) );

        final var targetUser = usersService.fetchUserDetails( targetAssociation );

        final var oldStatus = StatusEnum.fromValue( targetAssociation.getStatus() );
        final var newStatus = StatusEnum.fromValue( requestBody.getStatus().getValue() );

        final Update update;
        if ( isRequestingUser( targetAssociation ) ){
            update = switch( requestBody.getStatus() ){
                case CONFIRMED -> Optional.of( CONFIRMED )
                        .filter( status -> !MIGRATED.equals( oldStatus ) )
                        .map( status -> mapToConfirmedUpdate( targetAssociation, targetUser, getEricIdentity() ) )
                        .orElseThrow( () -> new BadRequestRuntimeException( "requesting user does not have access to perform the action", new Exception( "Requesting user cannot change their status from migrated to confirmed" ) ) );
                case REMOVED -> mapToRemovedUpdate( targetAssociation, targetUser, getEricIdentity() );
                case UNAUTHORISED -> null; // TODO
            };
        } else {
            update = switch ( requestBody.getStatus() ){
                case CONFIRMED -> Optional.of( CONFIRMED )
                        .filter( status -> MIGRATED.equals( oldStatus ) )
                        .filter( status -> associationsService.confirmedAssociationExists( targetAssociation.getCompanyNumber(), getEricIdentity() ) )
                        .map( status -> mapToInvitationUpdate( targetAssociation, targetUser, getEricIdentity(), now() ) )
                        .orElseThrow( () -> new BadRequestRuntimeException( "requesting user does not have access to perform the action", new Exception( String.format( "Requesting %s user cannot change another user to confirmed or the requesting user is not associated with company %s", getEricIdentity(), targetAssociation.getCompanyNumber() ) ) ) );
                case REMOVED -> Optional.of( REMOVED )
                        .filter( status -> associationsService.confirmedAssociationExists( targetAssociation.getCompanyNumber(), getEricIdentity() ) || hasAdminPrivilege( ADMIN_UPDATE_PERMISSION ) )
                        .map( status -> mapToRemovedUpdate( targetAssociation, targetUser, getEricIdentity() ) )
                        .orElseThrow( () -> new BadRequestRuntimeException( "requesting user does not have access to perform the action", new Exception( String.format( "Requesting %s user cannot change another user to confirmed or the requesting user is not associated with company %s", getEricIdentity(), targetAssociation.getCompanyNumber() ) ) ) );
                case UNAUTHORISED -> null; // TODO
            };
        }
        associationsService.updateAssociation( targetAssociation.getId(), update );
        sendStatusUpdateEmails( targetAssociation, targetUser, newStatus );

        return new ResponseEntity<>( OK );
    }

    private void sendStatusUpdateEmails( final AssociationDao targetAssociation, final User targetUser, final StatusEnum newStatus ) {
        final var xRequestId = getXRequestId();
        final var requestingUserDisplayValue = mapToDisplayValue( getUser(), getUser().getEmail() );
        final var targetUserDisplayValue = mapToDisplayValue( targetUser, targetAssociation.getUserEmail() );
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

        var emails = Flux.empty();
        if ( isRequestingUser( targetAssociation ) && oldStatus.equals( AWAITING_APPROVAL.getValue() ) && newStatus.equals( REMOVED ) ) {
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendInvitationRejectedEmailToAssociatedUser( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue ) ) );
        } else if ( oldStatus.equals( CONFIRMED.getValue() ) && newStatus.equals( REMOVED ) ) {
            emails = emails.concatWith( emailService.sendAuthorisationRemovedEmailToRemovedUser( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, hasAdminPrivilege( ADMIN_UPDATE_PERMISSION ) ? COMPANIES_HOUSE : requestingUserDisplayValue, targetAssociation.getUserId() ) );
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendAuthorisationRemovedEmailToAssociatedUser( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, hasAdminPrivilege( ADMIN_UPDATE_PERMISSION ) ? COMPANIES_HOUSE : requestingUserDisplayValue, targetUserDisplayValue ) ) );
        } else if ( isRequestingUser( targetAssociation ) && oldStatus.equals( AWAITING_APPROVAL.getValue() ) && newStatus.equals( CONFIRMED ) ) {
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendInvitationAcceptedEmailToAssociatedUser( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, cachedInvitedByDisplayName, requestingUserDisplayValue ) ) );
        } else if ( !isRequestingUser( targetAssociation ) && oldStatus.equals( AWAITING_APPROVAL.getValue() ) && newStatus.equals( REMOVED ) ) {
            emails = emails.concatWith( emailService.sendInviteCancelledEmail( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, hasAdminPrivilege( ADMIN_UPDATE_PERMISSION ) ? COMPANIES_HOUSE : requestingUserDisplayValue, targetAssociation ) );
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendInvitationCancelledEmailToAssociatedUser( xRequestId, targetAssociation.getCompanyNumber(), cachedCompanyName, hasAdminPrivilege( ADMIN_UPDATE_PERMISSION ) ? COMPANIES_HOUSE : requestingUserDisplayValue, targetUserDisplayValue ) ) );
        }
        emails.subscribe();
    }

}
