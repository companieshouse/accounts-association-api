package uk.gov.companieshouse.accounts.association.repositories;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.springframework.dao.DuplicateKeyException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
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
        associationOne.setStatus( "Confirmed" );

        final var associationTwo = new Associations();
        associationTwo.setCompanyNumber("222222");
        associationTwo.setUserId("111");
        associationTwo.setStatus( "Confirmed" );

        final var associationThree = new Associations();
        associationThree.setCompanyNumber("111111");
        associationThree.setUserId("222");
        associationThree.setStatus( "Confirmed" );

        associationsRepository.insert(associationOne);
        associationsRepository.insert(associationTwo);
        associationsRepository.insert(associationThree);
    }


    @Test
    void findAllAssociationsByCompanyNumber() {

        assertThat(associationsRepository.findAllByCompanyNumber("111111")).hasSize(2);

    }

    @Test
    public void fetchCompanyAssociationsReturnsEmptyListWhenSpecifiedUserIdDoesNotExistOrInputIsMalformed() {
        Assertions.assertEquals( 0, associationsRepository.fetchCompanyAssociations( null ).size() );
        Assertions.assertEquals( 0, associationsRepository.fetchCompanyAssociations( "" ).size() );
        Assertions.assertEquals( 0, associationsRepository.fetchCompanyAssociations( "abc" ).size() );
        Assertions.assertEquals( 0, associationsRepository.fetchCompanyAssociations( "333" ).size() );
    }

    @Test
    public void fetchCompanyAssociationsAppliedToUserAssociatedWithOneCompanyReturnsTheAssociatedCompany() {
        final var companiesAssociatedWithUser222 =
        associationsRepository.fetchCompanyAssociations( "222" )
                              .stream()
                              .map( Associations::getCompanyNumber )
                              .toList();

        Assertions.assertEquals( 1, companiesAssociatedWithUser222.size() );
        Assertions.assertTrue( companiesAssociatedWithUser222.contains( "111111" ) );
    }

    @Test
    public void fetchCompanyAssociationsAppliedToUserAssociatedWithMultipleCompaniesReturnsTheAssociatedCompanies() {
        final var companiesAssociatedWithUser111 =
        associationsRepository.fetchCompanyAssociations( "111" )
                              .stream()
                              .map( Associations::getCompanyNumber )
                              .toList();

        Assertions.assertEquals( 2, companiesAssociatedWithUser111.size() );
        Assertions.assertTrue( companiesAssociatedWithUser111.containsAll( List.of( "111111", "222222" ) ) );
    }

    @Test
    public void fetchCompanyAssociationsAppliedToUserAssociatedWithZeroCompaniesReturnsEmptyList(){
        // TODO: Need to find out if it possible for this case to even exist.
    }

    @Test
    void fetchConfirmationExpirationTimeWithNonExistentUserOrCompanyReturnsNull(){
        Assertions.assertNull( associationsRepository.fetchConfirmationExpirationTime( null, "111111" ) );
        Assertions.assertNull( associationsRepository.fetchConfirmationExpirationTime( "", "111111" ) );
        Assertions.assertNull( associationsRepository.fetchConfirmationExpirationTime( "333", "111111" ) );
        Assertions.assertNull( associationsRepository.fetchConfirmationExpirationTime( "111", null ) );
        Assertions.assertNull( associationsRepository.fetchConfirmationExpirationTime( "111", "" ) );
        Assertions.assertNull( associationsRepository.fetchConfirmationExpirationTime( "111", "333333" ) );
    }

    @Test
    void fetchConfirmationExpirationTimeShouldReturnTimestampIfItExists(){
        final var association = new Associations();
        association.setUserId( "333" );
        association.setCompanyNumber( "333333" );
        association.setConfirmationExpirationTime( "1992-05-01T10:30:00.000000Z" );
        associationsRepository.insert( association );

        Assertions.assertEquals( "1992-05-01T10:30:00.000000Z", associationsRepository.fetchConfirmationExpirationTime( "333", "333333" ).getConfirmationExpirationTime() );
    }

    @Test
    void fetchConfirmationExpirationTimeShouldReturnNullIfTimestampDoesNotExist(){
        Assertions.assertNull( associationsRepository.fetchConfirmationExpirationTime( "111", "111111" ).getConfirmationExpirationTime() );
    }

    @Test
    void updateAssociationWithNonExistentUserIdOrCompanyNumberShouldDoNothing(){
        final var setStatusToDeleted = new Update()
                .set( "status", "Deleted" );

        associationsRepository.updateAssociation( null, "111111", setStatusToDeleted );
        associationsRepository.updateAssociation( "", "111111", setStatusToDeleted );
        associationsRepository.updateAssociation( "333", "111111", setStatusToDeleted );
        associationsRepository.updateAssociation( "111", null, setStatusToDeleted );
        associationsRepository.updateAssociation( "111", "", setStatusToDeleted );
        associationsRepository.updateAssociation( "111", "333333", setStatusToDeleted );

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
        final var setStatusToDeleted = new Update()
                .set( "status", "Deleted" );

        associationsRepository.updateAssociation( "111", "111111", setStatusToDeleted );

        for ( Associations association: associationsRepository.findAll() )
            if ( association.getUserId().equals( "111" ) && association.getCompanyNumber().equals( "111111" ) )
                Assertions.assertEquals( "Deleted", association.getStatus() );
            else
                Assertions.assertEquals( "Confirmed", association.getStatus() );
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