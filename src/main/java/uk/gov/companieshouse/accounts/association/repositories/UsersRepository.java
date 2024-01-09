package uk.gov.companieshouse.accounts.association.repositories;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.Users;


@Repository
public interface UsersRepository extends MongoRepository<Users, String> {

    @Query( value = "{ 'email': ?0 }", fields = "{ 'id': 1 }" )
    Optional<Users> fetchUserId( String email );

}