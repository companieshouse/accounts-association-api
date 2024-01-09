package uk.gov.companieshouse.accounts.association.repositories;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.accounts.association.models.Users;

@SpringBootTest
@Testcontainers
public class UsersRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer container = new MongoDBContainer("mongo:4.4.22");

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    UsersRepository usersRepository;

    @BeforeEach
    public void setup() {
        final var userBatman = new Users();
        userBatman.setId( "1" );
        userBatman.setForename( "Bruce" );
        userBatman.setSurname( "Wayne" );
        userBatman.setEmail( "batman@gotham.city" );
        userBatman.setDisplayName( "Batman" );

        usersRepository.insert( userBatman );
    }

    @Test
    void fetchUserIdWithNonexistentEmailReturnsEmptyOptional(){
        Assertions.assertFalse( usersRepository.fetchUserId( null ).isPresent() );
        Assertions.assertFalse( usersRepository.fetchUserId( "" ).isPresent() );
        Assertions.assertFalse( usersRepository.fetchUserId( "123" ).isPresent() );
        Assertions.assertFalse( usersRepository.fetchUserId( "robin@gotham.city" ).isPresent() );
    }

    @Test
    void fetchUserIdReturnsId(){
        Assertions.assertEquals( "1", usersRepository.fetchUserId( "batman@gotham.city" ).get().getId() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Users.class);
    }

}
