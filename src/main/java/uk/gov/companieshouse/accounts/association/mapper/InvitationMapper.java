package uk.gov.companieshouse.accounts.association.mapper;

import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.DAYS_SINCE_INVITE_TILL_EXPIRES;

import java.time.LocalDateTime;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.accounts.association.models.Invitation;
import uk.gov.companieshouse.accounts.association.service.UsersService;

@Mapper( componentModel = "spring" )
public abstract class InvitationMapper {

    @Autowired
    private UsersService usersService;

    @AfterMapping
    protected void replaceUserIdWithUserEmail( @MappingTarget final uk.gov.companieshouse.api.accounts.associations.model.Invitation invitation ){
        invitation.setInvitedBy( usersService.fetchUserDetails( invitation.getInvitedBy(), getXRequestId() ).getEmail() );
    }

    @AfterMapping
    protected void enrichWithIsActive( @MappingTarget final uk.gov.companieshouse.api.accounts.associations.model.Invitation invitation ){
        final var invitedAt = LocalDateTime.parse( invitation.getInvitedAt() );
        final var expiredAt = invitedAt.plusDays( DAYS_SINCE_INVITE_TILL_EXPIRES );
        final var isActive = LocalDateTime.now().isBefore( expiredAt );
        invitation.setIsActive( isActive );
    }

    @AfterMapping
    protected void enrichWithAssociationId(@MappingTarget final uk.gov.companieshouse.api.accounts.associations.model.Invitation invitation, @Context final String associationId ){
        invitation.setAssociationId( associationId );
    }

    public abstract uk.gov.companieshouse.api.accounts.associations.model.Invitation daoToDto(final Invitation invitation, @Context final String associationId );

}
