package uk.gov.companieshouse.accounts.association.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.Association;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Optional;

@Repository
public interface AssociationsRepository extends MongoRepository<Association, String> {

    Iterable<Association> findAllByCompanyNumber(final String companyNumber );

    Iterable<Association> findAllByUserId(final String userId );

    @Query( value = "{ 'userId': ?0, 'companyNumber': ?1 }")
    Optional<Association> findByUserIdAndCompanyNumber(final String userId, final String companyNumber );

    @Query( "{ 'userId': ?0, 'companyNumber': ?1 }" )
    void updateAssociation( final String userId, final String companyNumber, Update update );

}
