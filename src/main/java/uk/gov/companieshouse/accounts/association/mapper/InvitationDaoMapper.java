package uk.gov.companieshouse.accounts.association.mapper;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;

@Mapper( componentModel = "spring" )
public interface InvitationDaoMapper {

    Invitation daoToDto( final InvitationDao invitation, @Context final String associationId,
                         @Context final InvitationMapper invitationMapper );

}
