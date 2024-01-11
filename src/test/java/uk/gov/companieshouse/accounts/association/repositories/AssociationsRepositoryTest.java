package uk.gov.companieshouse.accounts.association.repositories;

import jakarta.validation.ConstraintViolationException;
import org.junit.Ignore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
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
        associationOne.setStatus("Confirmed");

        final var associationTwo = new Associations();
        associationTwo.setCompanyNumber("222222");
        associationTwo.setUserId("111");
        associationTwo.setStatus("Confirmed");

        final var associationThree = new Associations();
        associationThree.setCompanyNumber("111111");
        associationThree.setUserId("222");
        associationThree.setStatus("Confirmed");

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
    void associationExistsWithInvalidUserIdOrCompanyNumberReturnsFalse(){
        Assertions.assertFalse( associationsRepository.associationExists( null, "111111") );
        Assertions.assertFalse( associationsRepository.associationExists( "", "111111") );
        Assertions.assertFalse( associationsRepository.associationExists( "abc", "111111") );
        Assertions.assertFalse( associationsRepository.associationExists( "111", null) );
        Assertions.assertFalse( associationsRepository.associationExists( "111", "") );
        Assertions.assertFalse( associationsRepository.associationExists( "111", "abc" ) );
    }

    @Test
    void associationExistsWithNonexistentUserIdOrCompanyNumberReturnsFalse(){
        Assertions.assertFalse( associationsRepository.associationExists( "333", "111111") );
        Assertions.assertFalse( associationsRepository.associationExists( "111", "333333") );
    }


    @Test
    void associationExistsReturnsTrueWhenAssociationExistsOrOtherwiseFalse(){
        Assertions.assertTrue( associationsRepository.associationExists( "111", "111111") );
        Assertions.assertFalse( associationsRepository.associationExists( "222", "222222") );
    }

    @Test
    void updateAssociationWithNonExistentUserIdOrCompanyNumberShouldDoNothing(){
        final var setStatusToRemoved = new Update()
                .set( "status", "Removed" );

        associationsRepository.updateAssociation( null, "111111", setStatusToRemoved );
        associationsRepository.updateAssociation( "", "111111", setStatusToRemoved );
        associationsRepository.updateAssociation( "333", "111111", setStatusToRemoved );
        associationsRepository.updateAssociation( "111", null, setStatusToRemoved );
        associationsRepository.updateAssociation( "111", "", setStatusToRemoved );
        associationsRepository.updateAssociation( "111", "333333", setStatusToRemoved );

        for ( Associations association: associationsRepository.findAll() )
            Assertions.assertEquals( "Confirmed", association.getStatus() );
    }

    @Test
    void updateAssociationWithNullUpdateShouldThrowIllegalStateException() {
        Assertions.assertThrows( IllegalStateException.class, () -> associationsRepository.updateAssociation( "111", "111111", null ) );
    }

    @Test
    void updateAssociationShouldCreateNewKeyValuePairIfKeyDoesNotExist(){
        final var setDeletionTime = new Update()
                .set( "deletionTime", "1992-05-01T10:30:00.000000Z" );

        associationsRepository.updateAssociation( "111", "111111", setDeletionTime );

        for ( Associations association: associationsRepository.findAll() )
            if ( association.getUserId().equals( "111" ) && association.getCompanyNumber().equals( "111111" ) )
                Assertions.assertEquals( "1992-05-01T10:30:00.000000Z", association.getDeletionTime() );
            else
                Assertions.assertNull( association.getDeletionTime() );
    }

    @Test
    void updateAssociationShouldOverwriteExistingValues(){
        final var setStatusToRemoved = new Update()
                .set( "status", "Removed" );

        associationsRepository.updateAssociation( "111", "111111", setStatusToRemoved );

        for ( Associations association: associationsRepository.findAll() )
            if ( association.getUserId().equals( "111" ) && association.getCompanyNumber().equals( "111111" ) )
                Assertions.assertEquals( "Removed", association.getStatus() );
            else
                Assertions.assertEquals( "Confirmed", association.getStatus() );
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