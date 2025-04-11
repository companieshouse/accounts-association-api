package uk.gov.companieshouse.accounts.association.controller;

import static java.time.LocalDateTime.now;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.companieshouse.accounts.association.models.Constants.PAGINATION_IS_MALFORMED;
import static uk.gov.companieshouse.accounts.association.models.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToConfirmedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToInvitationUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToRemovedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.isRequestingUser;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.mapToDisplayValue;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;

import java.util.Objects;
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












    private void updateAssociationWithUserEmail( final AssociationDao targetAssociation, final StatusEnum newStatus, final User targetUser ){
        final var update = Optional.of( mapToInvitationUpdate( targetAssociation, targetUser, getEricIdentity(), now() ) )
                .filter( any -> Objects.isNull( targetUser ) && StatusEnum.CONFIRMED.equals( newStatus ) )
                .orElse( StatusEnum.CONFIRMED.equals( newStatus ) ? mapToConfirmedUpdate( targetAssociation, targetUser, getEricIdentity() ) : mapToRemovedUpdate( targetAssociation, targetUser, getEricIdentity() ) );

        associationsService.updateAssociation( targetAssociation.getId(), update );
    }

    private void updateAssociationWithUserId( final AssociationDao targetAssociation, final StatusEnum newStatus, final boolean requestingAndTargetUserMatches, final User targetUser ){
        final var update = StatusEnum.CONFIRMED.equals( newStatus ) ? mapToConfirmedUpdate( targetAssociation, targetUser, getEricIdentity() ) : mapToRemovedUpdate( targetAssociation, targetUser, getEricIdentity() );
        associationsService.updateAssociation( targetAssociation.getId(), update );
    }

    @Override
    public ResponseEntity<Void> updateAssociationStatusForId( final String associationId, final RequestBodyPut requestBody ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with id=%s, user_id=%s, status=%s.", associationId, getEricIdentity(), requestBody.getStatus() ),null );

        final var targetAssociation = associationsService
                .fetchAssociationDao( associationId )
                .orElseThrow( () -> new NotFoundRuntimeException( String.format( "Association %s was not found.", associationId ), new Exception( String.format( "Could not find association %s.", associationId ) ) ) );

        final var targetUser = usersService.fetchUserDetails( targetAssociation );

        final var oldStatus = StatusEnum.fromValue( targetAssociation.getStatus() );
        final StatusEnum newStatus;
        if ( isRequestingUser( targetAssociation ) ){
            newStatus = switch( requestBody.getStatus() ){
                case CONFIRMED -> Optional.of( StatusEnum.CONFIRMED )
                        .filter( status -> !MIGRATED.equals( oldStatus ) )
                        .orElseThrow( () -> new BadRequestRuntimeException( "requesting user does not have access to perform the action", new Exception( "Requesting user cannot change their status from migrated to confirmed" ) ) );
                case REMOVED -> StatusEnum.REMOVED;
            };
        } else {
            newStatus = switch ( requestBody.getStatus() ){
                case CONFIRMED -> Optional.of( StatusEnum.CONFIRMED )
                        .filter( status -> MIGRATED.equals( oldStatus ) )
                        .filter( status -> associationsService.confirmedAssociationExists( targetAssociation.getCompanyNumber(), getEricIdentity() ) )
                        .orElseThrow( () -> new BadRequestRuntimeException( "requesting user does not have access to perform the action", new Exception( String.format( "Requesting %s user cannot change another user to confirmed or the requesting user is not associated with company %s", getEricIdentity(), targetAssociation.getCompanyNumber() ) ) ) );
                case REMOVED -> Optional.of( StatusEnum.REMOVED )
                        .filter( status -> associationsService.confirmedAssociationExists( targetAssociation.getCompanyNumber(), getEricIdentity() ) )
                        .orElseThrow( () -> new BadRequestRuntimeException( "requesting user does not have access to perform the action", new Exception( String.format( "Requesting %s user cannot change another user to confirmed or the requesting user is not associated with company %s", getEricIdentity(), targetAssociation.getCompanyNumber() ) ) ) );
            };
        }

        // TODO: refactor the above switches so that they return update, based on the code in the below if/else statement
        if ( Objects.nonNull( targetAssociation.getUserEmail() ) ) {
            updateAssociationWithUserEmail( targetAssociation, newStatus, targetUser );
        } else {
            updateAssociationWithUserId( targetAssociation, newStatus, isRequestingUser( targetAssociation ), targetUser );
        }

        sendEmailNotificationForStatusUpdate( getXRequestId(), mapToDisplayValue( getUser(), getUser().getEmail() ), mapToDisplayValue( targetUser, targetAssociation.getUserEmail() ), isRequestingUser( targetAssociation ), newStatus, targetAssociation.getStatus(), targetAssociation );

        return new ResponseEntity<>( OK );
    }








    private void sendEmailNotificationForStatusUpdate( final String xRequestId, final String requestingUserDisplayValue, final String targetUserDisplayValue, final boolean requestingAndTargetUserMatches, final StatusEnum newStatus, final String oldStatus, final AssociationDao associationDao ) {
        final var cachedCompanyName = Mono.just( associationDao.getCompanyNumber() )
                .map( companyService::fetchCompanyProfile )
                .map( CompanyDetails::getCompanyName )
                .cache();

        final var cachedAssociatedUsers =
                Mono.just( associationDao.getCompanyNumber() )
                        .flatMapMany( associationsService::fetchConfirmedUserIds)
                        .cache();

        final var cachedInvitedByDisplayName = Mono.just( associationDao )
                .map( AssociationDao::getInvitations )
                .flatMapMany( Flux::fromIterable )
                .reduce( (firstInvitation, secondInvitation) -> firstInvitation.getInvitedAt().isAfter( secondInvitation.getInvitedAt() ) ? firstInvitation : secondInvitation )
                .map( InvitationDao::getInvitedBy )
                .map( usersService::fetchUserDetails )
                .map( user -> Optional.ofNullable( user.getDisplayName() ).orElse( user.getEmail() ) )
                .cache();

        var emails = Flux.empty();
        if ( requestingAndTargetUserMatches && oldStatus.equals( StatusEnum.AWAITING_APPROVAL.getValue() ) && newStatus.equals( StatusEnum.REMOVED ) ) {
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendInvitationRejectedEmailToAssociatedUser( xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue ) ) );
        } else if ( oldStatus.equals( StatusEnum.CONFIRMED.getValue() ) && newStatus.equals( StatusEnum.REMOVED ) ) {
            emails = emails.concatWith( emailService.sendAuthorisationRemovedEmailToRemovedUser( xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, associationDao.getUserId() ) );
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendAuthorisationRemovedEmailToAssociatedUser( xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ) );
        } else if ( requestingAndTargetUserMatches && oldStatus.equals( StatusEnum.AWAITING_APPROVAL.getValue() ) && newStatus.equals( StatusEnum.CONFIRMED ) ) {
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendInvitationAcceptedEmailToAssociatedUser( xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, cachedInvitedByDisplayName, requestingUserDisplayValue ) ) );
        } else if ( !requestingAndTargetUserMatches && oldStatus.equals( StatusEnum.AWAITING_APPROVAL.getValue() ) && newStatus.equals( StatusEnum.REMOVED ) ) {
            emails = emails.concatWith( emailService.sendInviteCancelledEmail(xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, associationDao ) );
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendInvitationCancelledEmailToAssociatedUser( xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ) );
        }
        emails.subscribe();
    }

}
