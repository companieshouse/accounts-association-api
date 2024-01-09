package uk.gov.companieshouse.accounts.association.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.Associations;
import org.springframework.data.mongodb.core.query.Update;

@Repository
public interface AssociationsRepository extends MongoRepository<Associations, String> {

    Iterable<Associations> findAllByCompanyNumber( final String companyNumber );

    Iterable<Associations> findAllByUserId( final String userId );

    @Query( value = "{ 'userId': ?0, 'companyNumber': ?1 }", exists = true )
    boolean associationExists( String userId, String companyNumber );

    @Query( "{ 'userId': ?0, 'companyNumber': ?1 }" )
    void updateAssociation( final String userId, final String companyNumber, Update update );

}
