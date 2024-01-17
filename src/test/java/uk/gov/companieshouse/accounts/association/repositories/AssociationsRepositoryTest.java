package uk.gov.companieshouse.accounts.association.repositories;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.accounts.association.models.Association;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@SpringBootTest
@Testcontainers(parallel = true)
@Tag("integration-test")
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
        final var associationOne = new Association();
        associationOne.setCompanyNumber("111111");
        associationOne.setUserId("111");
        associationOne.setStatus("Confirmed");

        final var associationTwo = new Association();
        associationTwo.setCompanyNumber("222222");
        associationTwo.setUserId("111");
        associationTwo.setStatus("Confirmed");

        final var associationThree = new Association();
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
    @DirtiesContext
    void testToNotAllowNotNullStatus() {
        assertThrows(ConstraintViolationException.class, () -> {
            final var associationUser = new Association();
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
    void findByUserIdAndCompanyNumberWithInvalidUserIdOrCompanyNumberReturnsFalse(){
        Assertions.assertFalse(Optional.ofNullable(associationsRepository.findByUserIdAndCompanyNumber( null, "111111")).isEmpty() );
        Assertions.assertFalse( Optional.ofNullable(associationsRepository.findByUserIdAndCompanyNumber( "", "111111")).isEmpty() );
        Assertions.assertFalse( Optional.ofNullable(associationsRepository.findByUserIdAndCompanyNumber( "abc", "111111")).isEmpty() );
        Assertions.assertFalse( Optional.ofNullable(associationsRepository.findByUserIdAndCompanyNumber( "111", null)).isEmpty() );
        Assertions.assertFalse( Optional.ofNullable(associationsRepository.findByUserIdAndCompanyNumber( "111", "")).isEmpty() );
        Assertions.assertFalse( Optional.ofNullable(associationsRepository.findByUserIdAndCompanyNumber( "111", "abc" )).isEmpty() );
    }

    @Test
    void findByUserIdAndCompanyNumberWithNonexistentUserIdOrCompanyNumberReturnsFalse(){
        Assertions.assertFalse( Optional.ofNullable(associationsRepository.findByUserIdAndCompanyNumber( "333", "111111")).isEmpty() );
        Assertions.assertFalse( Optional.ofNullable(associationsRepository.findByUserIdAndCompanyNumber( "111", "333333")).isEmpty() );
    }


    @Test
    void findByUserIdAndCompanyNumberReturnsTrueWhenfindByUserIdAndCompanyNumberOrOtherwiseFalse(){
        Assertions.assertTrue(Optional.ofNullable(associationsRepository.findByUserIdAndCompanyNumber( "111", "111111") ).isPresent());
        Assertions.assertFalse( Optional.ofNullable(associationsRepository.findByUserIdAndCompanyNumber( "222", "222222") ).isEmpty());
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

        for ( Association association: associationsRepository.findAll() )
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

        for ( Association association: associationsRepository.findAll() )
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

        for ( Association association: associationsRepository.findAll() )
            if ( association.getUserId().equals( "111" ) && association.getCompanyNumber().equals( "111111" ) )
                Assertions.assertEquals( "Removed", association.getStatus() );
            else
                Assertions.assertEquals( "Confirmed", association.getStatus() );
    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
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