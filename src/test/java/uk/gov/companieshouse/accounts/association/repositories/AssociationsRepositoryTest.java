package uk.gov.companieshouse.accounts.association.repositories;

import org.springframework.dao.DuplicateKeyException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.companieshouse.accounts.association.models.Associations;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@DataMongoTest()
@ExtendWith(SpringExtension.class)
@Tag("integration-test")
public class AssociationsRepositoryTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    AssociationsRepository associationsRepository;

    @BeforeEach
    public void setup() {
        final var associationOne = new Associations();
        associationOne.setCompanyNumber("111111");
        associationOne.setUserId("111");

        final var associationTwo = new Associations();
        associationTwo.setCompanyNumber("222222");
        associationTwo.setUserId("111");

        final var associationThree = new Associations();
        associationThree.setCompanyNumber("111111");
        associationThree.setUserId("222");

        associationsRepository.insert(associationOne);
        associationsRepository.insert(associationTwo);
        associationsRepository.insert(associationThree);
    }


    @Test
    void findAllAssociationsByCompanyNumber() {

        assertThat(associationsRepository.findAllByCompanyNumber("111111")).hasSize(2);

    }


    @Test
    public void findAllUserId() {

        assertThat(associationsRepository.findAllByUserId("222")).hasSize(1);

        //duplicate user
        //duplicate company
        //duplicate user and company
        //invalid user
        //invalid company
        //null
    }

    @Test
    void shouldNotAllowDuplicateAssociations() {

        assertThrows(DuplicateKeyException.class, () -> {
            final var associationOneUser = new Associations();
            associationOneUser.setCompanyNumber("111111");
            associationOneUser.setUserId("111");
            associationsRepository.save(associationOneUser);
        });

    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Associations.class);
    }
}