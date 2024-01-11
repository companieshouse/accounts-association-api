package uk.gov.companieshouse.accounts.association.repositories;

import jakarta.validation.ConstraintViolationException;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.accounts.association.models.Association;

import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Testcontainers
class AssociationsRepositoryValidationTest {

    @Container
    @ServiceConnection
    static MongoDBContainer container = new MongoDBContainer("mongo:4.4.22").withExposedPorts(27017);
    @Autowired
    AssociationsRepository associationsRepository;

    @Autowired
    MongoTemplate mongoTemplate;

    @BeforeEach
    public void setup() {
        final var associationOne = new Association();
        associationOne.setCompanyNumber("111111");
        associationOne.setUserId("111");
        associationOne.setStatus("New");

        associationsRepository.insert(associationOne);

    }

    @Test
    void shouldNotAllowDuplicateAssociations() {
        final var associationUser = new Association();
        associationUser.setCompanyNumber("111111");
        associationUser.setUserId("111");
        associationUser.setStatus("New");

        assertThrows(DuplicateKeyException.class, () -> {
            associationsRepository.save(associationUser);
        });

    }


    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Association.class);
    }
}