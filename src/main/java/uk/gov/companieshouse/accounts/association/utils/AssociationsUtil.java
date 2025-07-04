package uk.gov.companieshouse.accounts.association.utils;

import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;
import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.DAYS_SINCE_INVITE_TILL_EXPIRES;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum.AUTH_CODE;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.REMOVED;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.UNAUTHORISED;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.mongodb.core.query.Update;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.models.PreviousStatesDao;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;

public final class AssociationsUtil {

    private AssociationsUtil(){}

    private static Update mapToBaseUpdate( final AssociationDao targetAssociation, final User targetUser, final String changedByUserId ){
        final var previousState = new PreviousStatesDao().status( targetAssociation.getStatus() ).changedBy( changedByUserId ).changedAt( LocalDateTime.now() );

        final var baseUpdate = new Update()
                .set( "etag", generateEtag() )
                .push( "previous_states", previousState );

        return Optional.ofNullable( targetUser )
                .map( user -> baseUpdate.set( "user_email", null ).set( "user_id", user.getUserId() ) )
                .orElse( baseUpdate );
    }

    public static Update mapToInvitationUpdate( final AssociationDao targetAssociation, final User targetUser, final String invitedByUserId, final LocalDateTime now ){
        final var invitationDao = new InvitationDao().invitedBy( invitedByUserId ).invitedAt( now );
        return mapToBaseUpdate( targetAssociation, targetUser, invitedByUserId )
                .push( "invitations", invitationDao )
                .set( "status", AWAITING_APPROVAL.getValue() )
                .set( "approval_expiry_at", now.plusDays( DAYS_SINCE_INVITE_TILL_EXPIRES ) );
    }

    public static Update mapToConfirmedUpdate( final AssociationDao targetAssociation, final User targetUser, final String changedByUserId ){
        return mapToBaseUpdate( targetAssociation, targetUser, changedByUserId )
                .set( "status", CONFIRMED.getValue() )
                .set( "approved_at", LocalDateTime.now() );
    }

    public static Update mapToRemovedUpdate( final AssociationDao targetAssociation, final User targetUser, final String changedByUserId ){
        return mapToBaseUpdate( targetAssociation, targetUser, changedByUserId )
                .set( "status", REMOVED.getValue() )
                .set( "removed_at", LocalDateTime.now() );
    }

    public static Update mapToAuthCodeConfirmedUpdated( final AssociationDao targetAssociation, final User targetUser, final String changedByUserId ) {
        return mapToBaseUpdate( targetAssociation, targetUser, changedByUserId )
                .set( "status", CONFIRMED.getValue() )
                .set( "approval_route", AUTH_CODE.getValue() );
    }

    public static Update mapToUnauthorisedUpdate( final AssociationDao targetAssociation, final User targetUser ){
        return mapToBaseUpdate( targetAssociation, targetUser, COMPANIES_HOUSE )
                .set( "status", UNAUTHORISED.getValue() )
                .set( "unauthorised_at", LocalDateTime.now() )
                .set( "unauthorised_by", COMPANIES_HOUSE );
    }

    public static Set<StatusEnum> fetchAllStatusesWithout( final Set<StatusEnum> without ){
        return Arrays.stream( StatusEnum.values() ).filter( status -> !without.contains( status ) ).collect( Collectors.toSet() );
    }

}
