package uk.gov.companieshouse.accounts.association.controller;

import static uk.gov.companieshouse.accounts.association.models.Constants.ITEMS_PER_PAGE_WAS_LESS_THAN_OR_EQUAL_TO_0;
import static uk.gov.companieshouse.accounts.association.models.Constants.PAGE_INDEX_WAS_LESS_THAN_0;
import static uk.gov.companieshouse.accounts.association.models.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToConfirmedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToInvitationUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToRemovedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.mapToDisplayValue;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.REMOVED;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
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
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyAssociationInterface;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.associations.model.PreviousStatesList;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut;
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
    public ResponseEntity<Association> getAssociationForId(final String id) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with id=%s.", id ),null );

        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to retrieve association with id: %s", id ), null );
        final var association = associationsService.findAssociationById(id);
        if (association.isEmpty()) {
            var errorMessage = String.format("Cannot find Association for the id: %s", id);
            LOGGER.errorContext( getXRequestId(), new Exception( errorMessage ), null );
            throw new NotFoundRuntimeException(StaticPropertyUtil.APPLICATION_NAMESPACE, errorMessage);
        }

        LOGGER.infoContext( getXRequestId(), String.format( "Successfully fetched association %s", id ), null );

        return new ResponseEntity<>(association.get(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<InvitationsList> getInvitationsForAssociation( final String associationId, final Integer pageIndex, final Integer itemsPerPage ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with id=%s, page_index=%d, items_per_page=%d.", associationId, pageIndex, itemsPerPage ),null );

        if ( pageIndex < 0 ) {
            LOGGER.errorContext( getXRequestId(), new Exception( PAGE_INDEX_WAS_LESS_THAN_0 ), null );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        if ( itemsPerPage <= 0 ) {
            LOGGER.errorContext( getXRequestId(), new Exception( ITEMS_PER_PAGE_WAS_LESS_THAN_OR_EQUAL_TO_0 ), null );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch association %s", associationId ), null );
        final var associationDaoOptional = associationsService.findAssociationDaoById( associationId );
        if ( associationDaoOptional.isEmpty() ) {
            LOGGER.errorContext( getXRequestId(), new Exception( String.format( "Could not find association %s.", associationId ) ), null );
            throw new NotFoundRuntimeException( "accounts-association-api", String.format( "Association %s was not found.", associationId ) );
        }

        final var associationDao = associationDaoOptional.get();
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch invitations for association %s", associationId ), null );
        final var invitations = associationsService.fetchInvitations( associationDao, pageIndex, itemsPerPage );

        LOGGER.infoContext( getXRequestId(), String.format( "Successfully fetched invitations for association %s", associationId ), null );

        return new ResponseEntity<>( invitations, HttpStatus.OK );
    }

    @Override
    public ResponseEntity<PreviousStatesList> getPreviousStatesForAssociation(@Pattern(regexp = "^[a-zA-Z0-9]*$") String s, @Valid Integer integer, @Valid Integer integer1) {
        // TODO
        return null;
    }

    private void throwBadRequestWhenUserIsNotPermittedToPerformAction( final AssociationDao targetAssociation, final RequestBodyPut.StatusEnum newStatus, final boolean requestingAndTargetUserMatches ){
        final var oldStatus = StatusEnum.fromValue( targetAssociation.getStatus() );
        if ( MIGRATED.equals( oldStatus ) && requestingAndTargetUserMatches ){
            if ( CONFIRMED.equals( newStatus ) ) {
                LOGGER.errorContext( getXRequestId(), new Exception( "Requesting user cannot change their status from migrated to confirmed" ), null );
                throw new BadRequestRuntimeException( "requesting user does not have access to perform the action" );
            }
        } else if ( !requestingAndTargetUserMatches && ( ( CONFIRMED.equals( newStatus ) && !MIGRATED.equals( oldStatus ) ) || !associationsService.confirmedAssociationExists( targetAssociation.getCompanyNumber(), getEricIdentity() ) ) ) {
            LOGGER.errorContext( getXRequestId(), new Exception( String.format( "Requesting %s user cannot change another user to confirmed or the requesting user is not associated with company %s", getEricIdentity(), targetAssociation.getCompanyNumber() ) ), null );
            throw new BadRequestRuntimeException( "requesting user does not have access to perform the action" );
        }
    }

    private String updateAssociationWithUserEmail( final AssociationDao targetAssociation, final RequestBodyPut.StatusEnum newStatus ){
        final var targetUser = Optional.ofNullable( usersService.searchUserDetails( List.of( targetAssociation.getUserEmail() ) ) )
                .flatMap( list -> list.stream().findFirst() )
                .orElse( null );

        final var update = Optional.of( mapToInvitationUpdate( targetAssociation, targetUser, getEricIdentity() ) )
                .filter( any -> Objects.isNull( targetUser ) && CONFIRMED.equals( newStatus ) )
                .map( any -> {
                    if ( !MIGRATED.equals( StatusEnum.fromValue( targetAssociation.getStatus() ) ) ){
                        LOGGER.errorContext( getXRequestId(), new Exception( String.format( "Could not find user %s, so cannot change status to confirmed.", targetAssociation.getUserEmail() ) ), null );
                        throw new BadRequestRuntimeException( String.format( "Could not find data for user %s", targetAssociation.getUserEmail() ) );
                    }
                    return any;
                } )
                .orElse( CONFIRMED.equals( newStatus ) ? mapToConfirmedUpdate( targetAssociation, targetUser, getEricIdentity() ) : mapToRemovedUpdate( targetAssociation, targetUser, getEricIdentity() ) );

        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to update association with id: %s", targetAssociation.getId() ), null );
        associationsService.updateAssociation( targetAssociation.getId(), update );

        return mapToDisplayValue( targetUser, targetAssociation.getUserEmail() );
    }

    private String updateAssociationWithUserId( final AssociationDao targetAssociation, final RequestBodyPut.StatusEnum newStatus, final boolean requestingAndTargetUserMatches ){
        final var targetUser = requestingAndTargetUserMatches ? getUser() : usersService.fetchUserDetails( targetAssociation.getUserId() );
        final var update = CONFIRMED.equals( newStatus ) ? mapToConfirmedUpdate( targetAssociation, targetUser, getEricIdentity() ) : mapToRemovedUpdate( targetAssociation, targetUser, getEricIdentity() );
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to update association with id: %s", targetAssociation.getId() ), null );
        associationsService.updateAssociation( targetAssociation.getId(), update );
        return mapToDisplayValue( targetUser, null );
    }

    @Override
    public ResponseEntity<Void> updateAssociationStatusForId( final String associationId, final RequestBodyPut requestBody ) {
        final var newStatus = requestBody.getStatus();

        LOGGER.infoContext( getXRequestId(), String.format( "Received request with id=%s, user_id=%s, status=%s.", associationId, getEricIdentity(), newStatus.getValue() ),null );

        final var requestingUserDetails = Objects.requireNonNull( getUser() );
        final var requestingUserDisplayValue = Optional.ofNullable( requestingUserDetails.getDisplayName() ).orElse( requestingUserDetails.getEmail() );

        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch association with id %s", associationId ), null );
        final var associationDao =
                associationsService.findAssociationDaoById( associationId )
                        .orElseThrow( () -> {
                            LOGGER.errorContext( getXRequestId(), new Exception( String.format( "Could not find association %s.", associationId ) ), null );
                            return new NotFoundRuntimeException( "accounts-association-api", String.format( "Association %s was not found.", associationId ) );
                        } );

        final boolean requestingAndTargetUserEmailMatches = Objects.nonNull( associationDao.getUserEmail() ) && associationDao.getUserEmail().equals( requestingUserDetails.getEmail() );
        final boolean requestingAndTargetUserIdMatches = Objects.nonNull( associationDao.getUserId() ) && associationDao.getUserId().equals( getEricIdentity() );
        final boolean requestingAndTargetUserMatches = requestingAndTargetUserIdMatches || requestingAndTargetUserEmailMatches;
        final boolean associationWithUserEmailExists = Objects.nonNull( associationDao.getUserEmail() );

        throwBadRequestWhenUserIsNotPermittedToPerformAction( associationDao, newStatus, requestingAndTargetUserMatches );

        var targetUserDisplayValue = requestingUserDisplayValue;
        if ( associationWithUserEmailExists ) {
            targetUserDisplayValue = updateAssociationWithUserEmail( associationDao, newStatus );
        } else {
            targetUserDisplayValue = updateAssociationWithUserId( associationDao, newStatus, requestingAndTargetUserMatches );
        }
        LOGGER.infoContext( getXRequestId(), String.format( "Updated association %s", associationDao.getId() ), null );

        sendEmailNotificationForStatusUpdate( getXRequestId(), requestingUserDisplayValue, targetUserDisplayValue, requestingAndTargetUserMatches, newStatus, associationDao.getStatus(), associationDao );

        return new ResponseEntity<>( HttpStatus.OK );
    }

    private void sendEmailNotificationForStatusUpdate( final String xRequestId, final String requestingUserDisplayValue, final String targetUserDisplayValue, final boolean requestingAndTargetUserMatches, final RequestBodyPut.StatusEnum newStatus, final String oldStatus, final AssociationDao associationDao ) {
        final var cachedCompanyName = Mono.just( associationDao.getCompanyNumber() )
                .map( companyService::fetchCompanyProfile )
                .map( CompanyDetails::getCompanyName )
                .cache();

        final var cachedAssociatedUsers =
                Mono.just( associationDao.getCompanyNumber() )
                        .map( associationsService::fetchAssociatedUsers )
                        .flatMapMany( Flux::fromIterable )
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
        if ( requestingAndTargetUserMatches && oldStatus.equals( AWAITING_APPROVAL.getValue() ) && newStatus.equals( REMOVED ) ) {
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendInvitationRejectedEmailToAssociatedUser( xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue ) ) );
        } else if ( oldStatus.equals( CONFIRMED.getValue() ) && newStatus.equals( REMOVED ) ) {
            emails = emails.concatWith( emailService.sendAuthorisationRemovedEmailToRemovedUser( xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, associationDao.getUserId() ) );
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendAuthorisationRemovedEmailToAssociatedUser( xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ) );
        } else if ( requestingAndTargetUserMatches && oldStatus.equals( AWAITING_APPROVAL.getValue() ) && newStatus.equals( CONFIRMED ) ) {
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendInvitationAcceptedEmailToAssociatedUser( xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, cachedInvitedByDisplayName, requestingUserDisplayValue ) ) );
        } else if ( !requestingAndTargetUserMatches && oldStatus.equals( AWAITING_APPROVAL.getValue() ) && newStatus.equals( REMOVED ) ) {
            emails = emails.concatWith( emailService.sendInviteCancelledEmail(xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, associationDao ) );
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendInvitationCancelledEmailToAssociatedUser( xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ) );
        }
        emails.subscribe();
    }

}
