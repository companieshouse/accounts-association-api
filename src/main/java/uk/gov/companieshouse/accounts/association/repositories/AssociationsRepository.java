package uk.gov.companieshouse.accounts.association.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.Associations;

@Repository
public interface AssociationsRepository extends MongoRepository<Associations, String> {

    Iterable<Associations> findAllByCompanyNumber( final String companyNumber );

    Iterable<Associations> findAllByUserId( final String userId );

}
