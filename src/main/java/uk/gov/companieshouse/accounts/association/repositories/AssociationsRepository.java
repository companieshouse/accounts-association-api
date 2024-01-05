package uk.gov.companieshouse.accounts.association.repositories;

import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.Associations;

@Repository
public interface AssociationsRepository extends MongoRepository<Associations, String> {

    Iterable<Associations> findAllByCompanyNumber( final String companyNumber );

    @Query( "{ 'userId': ?0 }" )
    List<Associations> fetchCompanyAssociations( final String userId );

    @Query( value = "{ 'userId': ?0, 'companyNumber': ?1 }", fields = "{ 'confirmationExpirationTime': 1, '_id': 0 }" )
    Optional<Associations> fetchConfirmationExpirationTime( final String userId, final String companyNumber );

    @Query( "{ 'userId': ?0, 'companyNumber': ?1 }" )
    void updateAssociation( final String userId, final String companyNumber, Update update );

}