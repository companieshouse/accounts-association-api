package uk.gov.companieshouse.accounts.association.repositories;

import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.Association;
import org.springframework.data.mongodb.core.query.Update;

import java.util.Optional;

@Repository
public interface AssociationsRepository extends MongoRepository<Association, String> {

    Iterable<Association> findAllByCompanyNumber(final String companyNumber );

    List<Association> findAllByUserId( final String userId );

    @Query( "{ 'userId': ?0, 'status': { $ne: 'Removed' } }" )
    List<Association> findAllConfirmedAndAwaitingAssociationsByUserId( final String userId );

    @Query( value = "{ 'userId': ?0, 'companyNumber': ?1 }")
    Optional<Association> findByUserIdAndCompanyNumber(final String userId, final String companyNumber );

    @Query( "{ 'userId': ?0, 'companyNumber': ?1 }" )
    void updateAssociation( final String userId, final String companyNumber, Update update );

}
