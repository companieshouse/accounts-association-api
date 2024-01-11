package uk.gov.companieshouse.accounts.association.repositories;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.User;


@Repository
public interface UsersRepository extends MongoRepository<User, String> {
}
