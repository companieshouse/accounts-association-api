package uk.gov.companieshouse.accounts.association.unit.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import uk.gov.companieshouse.accounts.association.models.Associations;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;

@DataMongoTest()
@ExtendWith(SpringExtension.class)
@Tag("integration-test")
@ComponentScan("uk.gov.companieshouse.accounts.association")
public class AssociationsServiceTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    AssociationsRepository associationsRepository;

    @Autowired
    AssociationsService associationsService;

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
    void softDeleteAssociationWithNonExistentUserIdOrCompanyNumberDoesNothing(){

        associationsService.softDeleteAssociation( null, "111111" );
        associationsService.softDeleteAssociation( "", "111111" );
        associationsService.softDeleteAssociation( "333", "111111" );
        associationsService.softDeleteAssociation( "111", null );
        associationsService.softDeleteAssociation( "111", "" );
        associationsService.softDeleteAssociation( "111", "333333" );

        for ( Associations association: associationsRepository.findAll() ) {
            Assertions.assertEquals("Confirmed", association.getStatus());
            Assertions.assertNull( association.getDeletionTime() );
        }
    }

    @Test
    void softDeleteAssociationShouldUpdateStatusAndDeletionTimeForSpecifiedAssociation() {
        associationsService.softDeleteAssociation( "111", "111111" );

        for ( Associations association: associationsRepository.findAll() ) {
            if ( association.getUserId().equals( "111" ) && association.getCompanyNumber().equals( "111111" ) ) {
                Assertions.assertEquals("Deleted", association.getStatus());
                Assertions.assertNotNull(association.getDeletionTime());
            } else {
                Assertions.assertEquals("Confirmed", association.getStatus());
                Assertions.assertNull(association.getDeletionTime());
            }
        }
    }

    @Test
    void confirmAssociationWithNonExistentUserIdOrCompanyNumberDoesNothing(){

        associationsService.confirmAssociation( null, "111111" );
        associationsService.confirmAssociation( "", "111111" );
        associationsService.confirmAssociation( "333", "111111" );
        associationsService.confirmAssociation( "111", null );
        associationsService.confirmAssociation( "111", "" );
        associationsService.confirmAssociation( "111", "333333" );

        for ( Associations association: associationsRepository.findAll() ) {
            Assertions.assertEquals("Confirmed", association.getStatus());
            Assertions.assertNull( association.getConfirmationApprovalTime() );
        }
    }

    @Test
    void confirmAssociationShouldUpdateStatusAndConfirmationApprovalTimeForSpecifiedAssociation() {
        associationsService.confirmAssociation( "111", "111111" );

        for ( Associations association: associationsRepository.findAll() ) {
            Assertions.assertEquals("Confirmed", association.getStatus());
            if ( association.getUserId().equals( "111" ) && association.getCompanyNumber().equals( "111111" ) ) {
                Assertions.assertNotNull(association.getConfirmationApprovalTime());
            } else {
                Assertions.assertNull(association.getConfirmationApprovalTime());
            }
        }
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Associations.class);
    }

}
