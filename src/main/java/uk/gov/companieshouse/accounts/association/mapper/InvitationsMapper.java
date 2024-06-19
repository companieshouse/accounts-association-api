package uk.gov.companieshouse.accounts.association.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.utils.MapperUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;

import java.time.LocalDateTime;
import java.util.stream.Stream;

@Mapper( componentModel = "spring" )
public abstract class InvitationsMapper {

    @Autowired
    private MapperUtil mapperUtil;

    @AfterMapping
    protected void replaceUserIdWithUserEmail( @MappingTarget Invitation invitation ){
        mapperUtil.enrichInvitation( invitation );
    }

    public abstract Invitation daoToDto( InvitationDao invitation );

    public Stream<Invitation> daoToDto( AssociationDao association ){
        return association.getInvitations()
                .stream()
                .map(invitationDao -> {
                    boolean isActive = LocalDateTime.now().isBefore(invitationDao.getExpiredAt());
                    Invitation invitation = daoToDto(invitationDao);
                    invitation.setAssociationId(association.getId());
                    invitation.setIsActive(isActive);
                    return invitation;
                });
    }

}
