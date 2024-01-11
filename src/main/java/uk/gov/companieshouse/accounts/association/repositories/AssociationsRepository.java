package uk.gov.companieshouse.accounts.association.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.Association;

import java.util.List;

@Repository
public interface AssociationsRepository extends MongoRepository<Association, String> {

    List<Association> findAllByCompanyNumber(final String companyNumber );

    Iterable<Association> findAllByUserId( final String userId );

}
