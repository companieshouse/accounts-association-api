package uk.gov.companieshouse.accounts.association.repositories;

import java.util.List;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;

public interface InvitationsRepository {

    List<AssociationDao> fetchActiveInvitations( final String userId );

}