package uk.gov.companieshouse.accounts.association.repositories;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.companieshouse.accounts.association.models.Association;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@DataMongoTest()
@ExtendWith(SpringExtension.class)
@Tag("integration-test")
class AssociationsRepositoryTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    AssociationsRepository associationsRepository;

    @BeforeEach
    public void setup() {
        final var associationOne = new Association();
        associationOne.setCompanyNumber("111111");
        associationOne.setUserId("111");
        associationOne.setStatus("New");

        final var associationTwo = new Association();
        associationTwo.setCompanyNumber("222222");
        associationTwo.setUserId("111");
        associationTwo.setStatus("New");

        final var associationThree = new Association();
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
    void findAllByUserIdShouldReturnOneAssociation() {

        assertThat(associationsRepository.findAllByUserId("222")).hasSize(1);

    }


    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Association.class);
    }
}