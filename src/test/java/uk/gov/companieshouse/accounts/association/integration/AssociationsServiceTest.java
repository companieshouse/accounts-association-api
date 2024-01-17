package uk.gov.companieshouse.accounts.association.integration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;

@SpringBootTest
@Testcontainers
@Tag("integration-test")
class AssociationsServiceTest {

    @Container
    @ServiceConnection
    static MongoDBContainer container = new MongoDBContainer("mongo:4.4.22");

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    AssociationsRepository associationsRepository;

    @Autowired
    private AssociationsService associationsService;

    @BeforeEach
    public void setup() {
        final var associationOne = new Association();
        associationOne.setCompanyNumber("111111");
        associationOne.setUserId("111");
        associationOne.setStatus( "Confirmed" );

        final var associationTwo = new Association();
        associationTwo.setCompanyNumber("222222");
        associationTwo.setUserId("111");
        associationTwo.setStatus( "Confirmed" );

        final var associationThree = new Association();
        associationThree.setCompanyNumber("111111");
        associationThree.setUserId("222");
        associationThree.setStatus( "Confirmed" );

        associationsRepository.insert(associationOne);
        associationsRepository.insert(associationTwo);
        associationsRepository.insert(associationThree);
    }

    @Test
    void softDeleteAssociationWithNonExistentUserIdOrCompanyNumberDoesNothing(){

        associationsService.softDeleteAssociation( null, "111111", true );
        associationsService.softDeleteAssociation( "", "111111", true );
        associationsService.softDeleteAssociation( "333", "111111", true );
        associationsService.softDeleteAssociation( "111", null, true );
        associationsService.softDeleteAssociation( "111", "", true );
        associationsService.softDeleteAssociation( "111", "333333", true );

        for ( Association association: associationsRepository.findAll() ) {
            Assertions.assertEquals("Confirmed", association.getStatus());
            Assertions.assertNull( association.getDeletionTime() );
        }
    }

    @Test
    void softDeleteAssociationWithUserInfoExistsShouldUpdateStatusAndDeletionTimeAndTemporaryForSpecifiedAssociation() {
        associationsService.softDeleteAssociation( "111", "111111", true );

        for ( Association association: associationsRepository.findAll() ) {
            if ( association.getUserId().equals( "111" ) && association.getCompanyNumber().equals( "111111" ) ) {
                Assertions.assertEquals("Removed", association.getStatus());
                Assertions.assertNotNull(association.getDeletionTime());
                Assertions.assertFalse( association.isTemporary() );
            } else {
                Assertions.assertEquals("Confirmed", association.getStatus());
                Assertions.assertNull(association.getDeletionTime());
                Assertions.assertFalse( association.isTemporary() );
            }
        }
    }

    @Test
    void softDeleteAssociationWithUserInfoDoesNotExistShouldUpdateStatusAndDeletionTimeForSpecifiedAssociation() {
        associationsService.softDeleteAssociation( "111", "111111", false );

        for ( Association association: associationsRepository.findAll() ) {
            if ( association.getUserId().equals( "111" ) && association.getCompanyNumber().equals( "111111" ) ) {
                Assertions.assertEquals("Removed", association.getStatus());
                Assertions.assertNotNull(association.getDeletionTime());
                Assertions.assertFalse( association.isTemporary() );
            } else {
                Assertions.assertEquals("Confirmed", association.getStatus());
                Assertions.assertNull(association.getDeletionTime());
                Assertions.assertFalse( association.isTemporary() );

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

        for ( Association association: associationsRepository.findAll() ) {
            Assertions.assertEquals("Confirmed", association.getStatus());
            Assertions.assertNull( association.getConfirmationApprovalTime() );
        }
    }

    @Test
    void confirmAssociationShouldUpdateStatusAndConfirmationApprovalTimeForSpecifiedAssociation() {
        associationsService.confirmAssociation( "111", "111111" );

        for ( Association association: associationsRepository.findAll() ) {
            Assertions.assertEquals("Confirmed", association.getStatus());
            if ( association.getUserId().equals( "111" ) && association.getCompanyNumber().equals( "111111" ) ) {
                Assertions.assertNotNull(association.getConfirmationApprovalTime());
                Assertions.assertFalse(association.isTemporary());
            } else {
                Assertions.assertNull(association.getConfirmationApprovalTime());
                Assertions.assertFalse(association.isTemporary());
            }
        }
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Association.class);
    }

}
