package uk.gov.companieshouse.accounts.association.mapper;

import org.mapstruct.Mapper;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationWithInvitations;


@Mapper(componentModel = "spring")
public abstract class RemoveInvitationsFromAssociationMapper {

    public abstract Association removeInvitationsFromAssociation(final AssociationWithInvitations associationWithInvitations);

}
