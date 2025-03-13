package uk.gov.companieshouse.accounts.association.controller;

import static uk.gov.companieshouse.accounts.association.models.Constants.ITEMS_PER_PAGE_WAS_LESS_THAN_OR_EQUAL_TO_0;
import static uk.gov.companieshouse.accounts.association.models.Constants.PAGE_INDEX_WAS_LESS_THAN_0;
import static uk.gov.companieshouse.accounts.association.models.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.REMOVED;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.data.mongodb.core.query.Update;
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

    private void throwBadRequestWhenUserIsNotPermittedToPerformAction( final String xRequestId, final String requestingUserId, final AssociationDao associationDao, final RequestBodyPut.StatusEnum newStatus, final boolean requestingAndTargetUserMatches ){
        if ( !requestingAndTargetUserMatches && ( newStatus.equals( CONFIRMED ) || !associationsService.confirmedAssociationExists( associationDao.getCompanyNumber(), requestingUserId ) ) ) {
            LOGGER.errorContext( xRequestId, new Exception( String.format( "Requesting %s user cannot change another user to confirmed or the requesting user is not associated with company %s", requestingUserId, associationDao.getCompanyNumber() ) ), null );
            throw new BadRequestRuntimeException( "requesting user does not have access to perform the action" );
        }
    }

    private String updateAssociationWithUserEmail( final String xRequestId, final RequestBodyPut.StatusEnum newStatus, final AssociationDao associationDao ){
        final var targetUserEmail = associationDao.getUserEmail();
        final var timestampKey = newStatus.equals( CONFIRMED ) ? "approved_at" : "removed_at";
        final var update = new Update().set( "status", newStatus.getValue() ).set( timestampKey, LocalDateTime.now().toString() );

        final var targetUserDisplayValue =
                Optional.ofNullable( usersService.searchUserDetails( List.of( targetUserEmail ) ) )
                        .flatMap( list -> list.stream().findFirst() )
                        .map( user -> {
                            update.set( "user_email", null ).set( "user_id", user.getUserId() );
                            return Optional.ofNullable( user.getDisplayName() ).orElse( user.getEmail() );
                        } )
                        .orElseGet( () -> {
                            if ( newStatus.equals( CONFIRMED ) ) {
                                LOGGER.errorContext( xRequestId, new Exception( String.format( "Could not find user %s, so cannot change status to confirmed.", targetUserEmail ) ), null );
                                throw new BadRequestRuntimeException( String.format( "Could not find data for user %s", targetUserEmail ) );
                            }
                            return targetUserEmail;
                        } );

        LOGGER.debugContext( xRequestId, String.format( "Attempting to update association with id: %s", associationDao.getId() ), null );
        associationsService.updateAssociation( associationDao.getId(), update );

        return targetUserDisplayValue;
    }

    private String updateAssociationWithUserId( final RequestBodyPut.StatusEnum newStatus, final AssociationDao associationDao, final String requestingUserDisplayValue, final boolean requestingAndTargetUserMatches ){
        final var timestampKey = newStatus.equals( CONFIRMED ) ? "approved_at" : "removed_at";
        final var update = new Update().set( "status", newStatus.getValue() ).set( timestampKey, LocalDateTime.now().toString() );

        var targetUserDisplayValue = requestingUserDisplayValue;
        if( !requestingAndTargetUserMatches ){
            final var tempUser = usersService.fetchUserDetails( associationDao.getUserId() );
            targetUserDisplayValue = Optional.ofNullable( tempUser.getDisplayName() ).orElse( tempUser.getEmail() );
        }

        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to update association with id: %s", associationDao.getId() ), null );
        associationsService.updateAssociation(associationDao.getId(), update);

        return targetUserDisplayValue;
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

        throwBadRequestWhenUserIsNotPermittedToPerformAction( getXRequestId(), getEricIdentity(), associationDao, newStatus, requestingAndTargetUserMatches );

        var targetUserDisplayValue = requestingUserDisplayValue;
        if ( associationWithUserEmailExists ) {
            targetUserDisplayValue = updateAssociationWithUserEmail( getXRequestId(), newStatus, associationDao );
        } else {
            targetUserDisplayValue = updateAssociationWithUserId( newStatus, associationDao, requestingUserDisplayValue, requestingAndTargetUserMatches );
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
