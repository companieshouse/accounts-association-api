package uk.gov.companieshouse.accounts.association.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;

import java.time.LocalDateTime;
import java.util.stream.Stream;

@Mapper( componentModel = "spring" )
public abstract class InvitationsMapper {

    @Autowired
    private UsersService usersService;

    @AfterMapping
    protected void replaceUserIdWithUserEmail( @MappingTarget final Invitation invitation ){
        invitation.setInvitedBy( usersService.fetchUserDetails( invitation.getInvitedBy() ).getEmail() );
    }

    public abstract Invitation daoToDto( final InvitationDao invitation );

    public Stream<Invitation> daoToDto( final AssociationDao associationDao ){
        return associationDao.getInvitations()
                .stream()
                .map( invitationDao -> {
                    final var isActive = LocalDateTime.now().isBefore( invitationDao.getExpiredAt() );
                    return daoToDto( invitationDao ).associationId( associationDao.getId() ).isActive( isActive );
                } );
    }

}
