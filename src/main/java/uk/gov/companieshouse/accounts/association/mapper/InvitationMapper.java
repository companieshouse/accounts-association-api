package uk.gov.companieshouse.accounts.association.mapper;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.DAYS_SINCE_INVITE_TILL_EXPIRES;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;

@Mapper( componentModel = "spring" )
public abstract class InvitationMapper {

    @Autowired
    private UsersService usersService;

    @AfterMapping
    protected void replaceUserIdWithUserEmail( @MappingTarget final Invitation invitation ){
        final var invitedBy = invitation.getInvitedBy();
        try {
            invitation.setInvitedBy( usersService.fetchUserDetails( invitedBy, getXRequestId() ).getEmail() );
        } catch ( NotFoundRuntimeException exception ){
            LOGGER.errorContext( getXRequestId(), new Exception( "Inviting user not found in accounts-user-api; leaving invited_by null", exception ), new HashMap<>( Map.of( "user_id", String.valueOf( invitedBy ) ) ) );
            invitation.setInvitedBy( null );
        }
    }

    @AfterMapping
    protected void enrichWithIsActive( @MappingTarget final Invitation invitation ){
        final var invitedAt = LocalDateTime.parse( invitation.getInvitedAt() );
        final var expiredAt = invitedAt.plusDays( DAYS_SINCE_INVITE_TILL_EXPIRES );
        final var isActive = LocalDateTime.now().isBefore( expiredAt );
        invitation.setIsActive( isActive );
    }

    @AfterMapping
    protected void enrichWithAssociationId( @MappingTarget final Invitation invitation, @Context final String associationId ){
        invitation.setAssociationId( associationId );
    }

    public abstract Invitation daoToDto( final InvitationDao invitation, @Context final String associationId );

}
