package uk.gov.companieshouse.accounts.association.repositories;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
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

    @Test
    void findAllByUserIdWithMalformedOrNonexistentUserIdReturnsEmptyList(){
        Assertions.assertEquals( List.of(), associationsRepository.findAllByUserId( null ) );
        Assertions.assertEquals( List.of(), associationsRepository.findAllByUserId( "" ) );
        Assertions.assertEquals( List.of(), associationsRepository.findAllByUserId( "abc" ) );
        Assertions.assertEquals( List.of(), associationsRepository.findAllByUserId( "333" ) );
    }

    @Test
    void findAllByUserIdWithOneAssociationReturnsUsersAssociation(){
        final var associations = associationsRepository.findAllByUserId( "222" );
        Assertions.assertEquals( 1, associations.size() );
        Assertions.assertEquals( "111111", associations.get( 0 ).getCompanyNumber() );
    }

    @Test
    void findAllByUserIdWithMultipleAssociationsReturnsAllUsersAssociations(){
        final var associations = associationsRepository.findAllByUserId( "111" );

        Assertions.assertEquals( 2, associations.size() );

        final var companyNumbers =
        associations.stream()
                    .map( Association::getCompanyNumber )
                    .toList();

        Assertions.assertTrue( companyNumbers.containsAll( List.of( "111111", "222222" ) ) );
    }

    @Test
    void findAllConfirmedAndAwaitingAssociationsByUserIdWithMalformedOrNonexistentUserIdReturnsEmptyList(){
        Assertions.assertEquals( List.of(), associationsRepository.findAllConfirmedAndAwaitingAssociationsByUserId( null ) );
        Assertions.assertEquals( List.of(), associationsRepository.findAllConfirmedAndAwaitingAssociationsByUserId( "" ) );
        Assertions.assertEquals( List.of(), associationsRepository.findAllConfirmedAndAwaitingAssociationsByUserId( "abc" ) );
        Assertions.assertEquals( List.of(), associationsRepository.findAllConfirmedAndAwaitingAssociationsByUserId( "333" ) );
    }

    @Test
    void findAllConfirmedAndAwaitingAssociationsByUserIdWithUserThatOnlyHasRemovedCompaniesReturnsEmptyList(){
        final var removedAssociation = new Association();
        removedAssociation.setCompanyNumber("333333");
        removedAssociation.setUserId("333");
        removedAssociation.setStatus("Removed");

        associationsRepository.insert( List.of( removedAssociation ) );

        final var associations = associationsRepository.findAllConfirmedAndAwaitingAssociationsByUserId( "333" );

        Assertions.assertTrue(  associations.isEmpty() );
    }

    @Test
    void findAllConfirmedAndAwaitingAssociationsByUserIdRetrievesAllNonRemovedUsersAssociations(){
        final var awaitingAssociation = new Association();
        awaitingAssociation.setCompanyNumber("333333");
        awaitingAssociation.setUserId("111");
        awaitingAssociation.setStatus("Awaiting Confirmation");

        final var removedAssociation = new Association();
        removedAssociation.setCompanyNumber("444444");
        removedAssociation.setUserId("111");
        removedAssociation.setStatus("Removed");

        associationsRepository.insert( List.of( awaitingAssociation, removedAssociation ) );

        final var associations = associationsRepository.findAllConfirmedAndAwaitingAssociationsByUserId( "111" );

        Assertions.assertEquals( 3, associations.size() );

        final var companyNumbers =
                associations.stream()
                        .map( Association::getCompanyNumber )
                        .toList();

        Assertions.assertTrue( companyNumbers.containsAll( List.of( "111111", "222222", "333333" ) ) );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Association.class);
    }
}