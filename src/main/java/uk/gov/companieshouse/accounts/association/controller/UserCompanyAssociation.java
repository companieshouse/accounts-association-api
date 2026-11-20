package uk.gov.companieshouse.accounts.association.controller;

import static java.time.LocalDateTime.now;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_UPDATE_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.models.Constants.PAGINATION_IS_MALFORMED;
import static uk.gov.companieshouse.accounts.association.models.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.accounts.association.models.Constants.USER_IS_NOT_AUTHORISED_TO_MAKE_THIS_REQUEST;
import static uk.gov.companieshouse.accounts.association.models.context.RequestContext.getRequestContext;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToAuthCodeConfirmedUpdated;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToConfirmedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToInvitationUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToRemovedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToUnauthorisedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricTokenPermissions;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.hasAdminPrivilege;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.isAPIKeyRequest;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.isRequestingUser;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.REMOVED;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.UNAUTHORISED;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;
import java.util.regex.Pattern;

import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.UnauthorisedRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyAssociationInterface;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.associations.model.PreviousStatesList;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut;
import uk.gov.companieshouse.api.accounts.user.model.User;

@RestController
public class UserCompanyAssociation implements UserCompanyAssociationInterface {

    public static final String COMPANY_UPGRADED_AUTH_VALID_UNTIL = "company_upgraded_auth_valid_until";


    private final UsersService usersService;
    private final AssociationsService associationsService;
    private final EmailService emailService;

    public UserCompanyAssociation( final UsersService usersService, final AssociationsService associationsService, final EmailService emailService ) {
        this.usersService = usersService;
        this.associationsService = associationsService;
        this.emailService = emailService;
    }

    @Override
    public ResponseEntity<Association> getAssociationForId( final String associationId ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with id=%s.", associationId ),null );
        return associationsService.fetchAssociationDto( associationId )
                .map( association -> new ResponseEntity<>( association, OK ) )
                .orElseThrow( () -> new NotFoundRuntimeException( getXRequestId(), PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "Cannot find Association for the id: %s" ) ) );
    }

    @Override
    public ResponseEntity<InvitationsList> getInvitationsForAssociation( final String associationId, final Integer pageIndex, final Integer itemsPerPage ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with id=%s, page_index=%d, items_per_page=%d.", associationId, pageIndex, itemsPerPage ),null );

        if ( pageIndex < 0 || itemsPerPage <= 0 ){
            throw new BadRequestRuntimeException( getXRequestId(), PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( PAGINATION_IS_MALFORMED ) );
        }

        return associationsService.fetchInvitations( associationId, pageIndex, itemsPerPage )
                .map( invitations -> new ResponseEntity<>( invitations, OK ) )
                .orElseThrow( () -> new NotFoundRuntimeException( getXRequestId(),  PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "Could not find association %s.", associationId ) ) ) );
    }

    @Override
    public ResponseEntity<PreviousStatesList> getPreviousStatesForAssociation( final String associationId, final Integer pageIndex, final Integer itemsPerPage ){
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with id=%s, page_index=%d, items_per_page=%d.", associationId, pageIndex, itemsPerPage ),null );

        if ( pageIndex < 0 || itemsPerPage <= 0 ) {
            throw new BadRequestRuntimeException( getXRequestId(), PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( PAGINATION_IS_MALFORMED ) );
        }

        return associationsService.fetchPreviousStates( associationId, pageIndex, itemsPerPage )
                .map( previousStates -> new ResponseEntity<>( previousStates, OK ) )
                .orElseThrow( () -> new NotFoundRuntimeException( getXRequestId(), PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "Association %s was not found", associationId ) ) ) );
    }


    private Update mapToAPIKeyUpdate( final RequestBodyPut.StatusEnum proposedStatus, final AssociationDao targetAssociation, final User targetUser ){
        final var oldStatus = StatusEnum.fromValue( targetAssociation.getStatus() );
        return switch ( proposedStatus ){
            case CONFIRMED -> Optional.of( CONFIRMED )
                    .filter( status -> MIGRATED.equals( oldStatus ) || UNAUTHORISED.equals( oldStatus ) )
                    .map( status -> mapToAuthCodeConfirmedUpdated( targetAssociation, targetUser, COMPANIES_HOUSE ) )
                    .orElseThrow( () -> new BadRequestRuntimeException( getXRequestId(), PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "API Key cannot change a %s association to confirmed", oldStatus.getValue() ) ) ) );
            case REMOVED -> throw new BadRequestRuntimeException( getXRequestId(), PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "Unable to change the association status to removed with API Key" ) );
            case UNAUTHORISED -> mapToUnauthorisedUpdate( targetAssociation, targetUser );
        };
    }

    private Update mapToOAuth2Update( final RequestBodyPut.StatusEnum proposedStatus, final AssociationDao targetAssociation, final User targetUser ){
        final var oldStatus = StatusEnum.fromValue( targetAssociation.getStatus() );

        if ( isRequestingUser( targetAssociation ) ){
            return switch( proposedStatus ){
                case CONFIRMED -> Optional.of( CONFIRMED )
                        .filter( status -> !( MIGRATED.equals( oldStatus ) || UNAUTHORISED.equals( oldStatus ) ) )
                        .map( status -> mapToConfirmedUpdate( targetAssociation, targetUser, getEricIdentity() ) )
                        .orElseThrow( () -> new BadRequestRuntimeException( getXRequestId(), PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "Requesting user cannot change their status from migrated to confirmed" ) ) );
                case REMOVED -> mapToRemovedUpdate( targetAssociation, targetUser, getEricIdentity() );
                case UNAUTHORISED -> throw new BadRequestRuntimeException( getXRequestId(), PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "Requesting user cannot change their status to unauthorised" ) );
            };
        }

        if(hasAdminPrivilege(ADMIN_UPDATE_PERMISSION) || hasCompanyUpgradedAuthTokenPermission()) {
            return switch (proposedStatus) {
                case CONFIRMED -> Optional.of(CONFIRMED)
                        .filter(status -> MIGRATED.equals(oldStatus) || UNAUTHORISED.equals(oldStatus))
                        .filter(status -> associationsService.confirmedAssociationExists(targetAssociation.getCompanyNumber(), getEricIdentity()))
                        .map(status -> mapToInvitationUpdate(targetAssociation, targetUser, getEricIdentity(), now()))
                        .orElseThrow(() -> new BadRequestRuntimeException(getXRequestId(), PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception(String.format("Requesting %s user cannot change another user to confirmed or the requesting user is not associated with company %s", getEricIdentity(), targetAssociation.getCompanyNumber()))));
                case REMOVED -> Optional.of(REMOVED)
                        .filter(status -> associationsService.confirmedAssociationExists(targetAssociation.getCompanyNumber(), getEricIdentity()) || hasAdminPrivilege(ADMIN_UPDATE_PERMISSION))
                        .map(status -> mapToRemovedUpdate(targetAssociation, targetUser, getEricIdentity()))
                        .orElseThrow(() -> new BadRequestRuntimeException(getXRequestId(), PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception(String.format("Requesting %s user cannot change another user to confirmed or the requesting user is not associated with company %s", getEricIdentity(), targetAssociation.getCompanyNumber()))));
                case UNAUTHORISED ->
                        throw new BadRequestRuntimeException(getXRequestId(), PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception(String.format("Requesting %s user cannot change another user to unauthorised", getEricIdentity())));
            };

        } else {
            throw new UnauthorisedRuntimeException(getXRequestId(), USER_IS_NOT_AUTHORISED_TO_MAKE_THIS_REQUEST, new Exception(String.format("Requesting %s user cannot change another user's status", getEricIdentity())));
        }
    }

    @Override
    public ResponseEntity<Void> updateAssociationStatusForId( final String associationId, final RequestBodyPut requestBody ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with id=%s, user_id=%s, status=%s.", associationId, getEricIdentity(), requestBody.getStatus() ),null );

        final var targetAssociation = associationsService
                .fetchAssociationDao( associationId )
                .orElseThrow( () -> new NotFoundRuntimeException( getXRequestId(), PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "Could not find association %s.", associationId ) ) ) );

        final var targetUser = usersService.fetchUserDetails( targetAssociation );


        final var update = isAPIKeyRequest() ? mapToAPIKeyUpdate( requestBody.getStatus(), targetAssociation, targetUser ) : mapToOAuth2Update( requestBody.getStatus(), targetAssociation, targetUser );


        associationsService.updateAssociation( targetAssociation.getId(), update );

        final var newStatus = StatusEnum.fromValue( requestBody.getStatus().getValue() );
        emailService.sendStatusUpdateEmails( targetAssociation, targetUser, newStatus, getRequestContext() );

        return new ResponseEntity<>( OK );
    }

    private boolean hasCompanyUpgradedAuthTokenPermission( ) {
        final var xRequestId = getXRequestId();
        return   parseCompanyUpgradedAuthValidUntil(getEricTokenPermissions())
                .map( permissionExpiry -> {
                    if ( permissionExpiry.isBefore( Instant.now() ) ) {
                        LOGGER.infoContext( xRequestId,
                                "company_upgraded_auth_valid_until is present but expired.",
                                null );
                        return false;
                    }
                    LOGGER.infoContext(xRequestId, "company_upgraded_auth_valid_until is present and valid." , null);
                    return true;
                } )
                .orElseGet( () -> {
                    LOGGER.infoContext( xRequestId,
                            "company_upgraded_auth_valid_until is present but malformed or missing a value.",
                            null );
                    return false;
                } );
    }

    private Optional<Instant> parseCompanyUpgradedAuthValidUntil(final String tokenPermissions ) {
        final var pattern = Pattern.compile( COMPANY_UPGRADED_AUTH_VALID_UNTIL + "\\s*=\\s*([^\\s]+)" );
        final var matcher = pattern.matcher( tokenPermissions );
        if ( !matcher.find() ) {
            return Optional.empty();
        }

        final var expiryTime = matcher.group( 1 );
        if ( expiryTime == null || expiryTime.isBlank() ) {
            return Optional.empty();
        }

        try {
            // Try ISO-8601 format first
            return Optional.of( Instant.parse( expiryTime ) );
        } catch ( DateTimeParseException ignored ) {
            // Fall back to Unix timestamp (seconds since epoch)
            try {
                final var unixSeconds = Long.parseLong( expiryTime );
                return Optional.of( Instant.ofEpochSecond( unixSeconds ) );
            } catch ( NumberFormatException numberIgnored ) {
                return Optional.empty();
            }
        }
    }

}
