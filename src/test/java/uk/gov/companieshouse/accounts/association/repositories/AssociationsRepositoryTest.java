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
import uk.gov.companieshouse.accounts.association.models.Associations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Testcontainers
class AssociationsRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer container = new MongoDBContainer("mongo:4.4.22");
    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    AssociationsRepository associationsRepository;

    @BeforeEach
    public void setup() {
        final var associationOne = new Associations();
        associationOne.setCompanyNumber("111111");
        associationOne.setUserId("111");
        associationOne.setStatus("New");

        final var associationTwo = new Associations();
        associationTwo.setCompanyNumber("222222");
        associationTwo.setUserId("111");
        associationTwo.setStatus("New");

        final var associationThree = new Associations();
        associationThree.setCompanyNumber("111111");
        associationThree.setUserId("222");
        associationThree.setStatus("New");

        associationsRepository.insert(associationOne);
        associationsRepository.insert(associationTwo);
        associationsRepository.insert(associationThree);

    }


    @Test
    void findAllAssociationsByCompanyNumberShouldReturnRightTwoAssociations() {

        assertThat(associationsRepository.findAllByCompanyNumber("111111")).hasSize(2);

    }


    @Test
    void testToNotAllowNotNullStatus() {
        assertThrows(ConstraintViolationException.class, () -> {
            final var associationUser = new Associations();
            associationUser.setCompanyNumber("111111");
            associationUser.setUserId("111");
            associationsRepository.save(associationUser);
        });

    }

    @Test
    void findAllByUserIdShouldReturnOneAssociation() {

        assertThat(associationsRepository.findAllByUserId("222")).hasSize(1);

    }

    @Test
    @Ignore("working in isolation")
    void shouldNotAllowDuplicateAssociations() {

        assertThrows(DuplicateKeyException.class, () -> {
            final var associationUser = new Associations();
            associationUser.setCompanyNumber("111111");
            associationUser.setUserId("111");
            associationUser.setStatus("New");
            associationsRepository.save(associationUser);
        });

    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Associations.class);
    }
}