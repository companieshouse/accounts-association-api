package uk.gov.companieshouse.accounts.association.integration;

import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.rest.AccountsUserEndpoint;
import uk.gov.companieshouse.accounts.association.tasks.DataMigrationPostProcessingTask;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

@SpringBootTest
@ExtendWith( MockitoExtension.class )
@Tag( "integration-test" )
class DataMigrationPostProcessingTaskTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AssociationsRepository associationsRepository;

    @MockBean
    private EmailProducer emailProducer;

    @MockBean
    private KafkaProducerFactory kafkaProducerFactory;

    @MockBean
    private AccountsUserEndpoint accountsUserEndpoint;

    @Autowired
    private DataMigrationPostProcessingTask dataMigrationPostProcessingTask;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static Mockers mockers;

    @BeforeEach
    void setup(){
        mockers = new Mockers( accountsUserEndpoint, null, null, null, null );
    }

    private static Stream<Arguments> batchSizeData(){
        return Stream.of(
                Arguments.of( 1 ),
                Arguments.of( 2 ),
                Arguments.of( 3 ),
                Arguments.of( 4 )
        );
    }

    @ParameterizedTest
    @MethodSource( "batchSizeData" )
    void processMigratedAssociationsWithOddNumberOfAssociationsPerformsUpdatesAssociationsForExistingUsers( final int batchSize ) throws ApiErrorResponseException, URIValidationException {
        ReflectionTestUtils.setField( dataMigrationPostProcessingTask, "ITEMS_PER_PAGE", batchSize );

        final var associationDaos = testDataManager.fetchAssociationDaos( "FutAssociation001", "FutAssociation002", "FutAssociation003", "FutAssociation004", "1" );
        associationsRepository.insert( associationDaos );

        mockers.mockSearchUserDetails( "FutUser001" );
        mockers.mockSearchUserDetailsNotFound( "leela@futurama.com" );
        mockers.mockSearchUserDetailsNotFoundNull( "amy@futurama.com" );

        dataMigrationPostProcessingTask.processMigratedAssociations();
        final var updatedAssociation = associationsRepository.findById( "FutAssociation001" ).get();

        Assertions.assertNotEquals( associationDaos.getFirst().getEtag(), updatedAssociation.getEtag() );
        Assertions.assertEquals( "FutUser001", updatedAssociation.getUserId() );
        Assertions.assertNull( updatedAssociation.getUserEmail() );
        Assertions.assertEquals( associationDaos.get( 1 ).getEtag(), associationsRepository.findById( "FutAssociation002" ).get().getEtag() );
        Assertions.assertEquals( associationDaos.get( 2 ).getEtag(), associationsRepository.findById( "FutAssociation003" ).get().getEtag() );
        Assertions.assertEquals( associationDaos.get( 3 ).getEtag(), associationsRepository.findById( "FutAssociation004" ).get().getEtag() );
        Assertions.assertEquals( associationDaos.get( 4 ).getEtag(), associationsRepository.findById( "1" ).get().getEtag() );
    }

    @ParameterizedTest
    @MethodSource( "batchSizeData" )
    void processMigratedAssociationsWithEvenNumberOfAssociationsPerformsUpdatesAssociationsForExistingUsers( final int batchSize ) throws ApiErrorResponseException, URIValidationException {
        ReflectionTestUtils.setField( dataMigrationPostProcessingTask, "ITEMS_PER_PAGE", batchSize );

        final var associationDaos = testDataManager.fetchAssociationDaos( "FutAssociation001", "FutAssociation002", "FutAssociation003", "1" );
        associationsRepository.insert( associationDaos );

        mockers.mockSearchUserDetails( "FutUser001" );
        mockers.mockSearchUserDetailsNotFound( "leela@futurama.com" );

        dataMigrationPostProcessingTask.processMigratedAssociations();
        final var updatedAssociation = associationsRepository.findById( "FutAssociation001" ).get();

        Assertions.assertNotEquals( associationDaos.getFirst().getEtag(), updatedAssociation.getEtag() );
        Assertions.assertEquals( "FutUser001", updatedAssociation.getUserId() );
        Assertions.assertNull( updatedAssociation.getUserEmail() );
        Assertions.assertEquals( associationDaos.get( 1 ).getEtag(), associationsRepository.findById( "FutAssociation002" ).get().getEtag() );
        Assertions.assertEquals( associationDaos.get( 2 ).getEtag(), associationsRepository.findById( "FutAssociation003" ).get().getEtag() );
        Assertions.assertEquals( associationDaos.get( 3 ).getEtag(), associationsRepository.findById( "1" ).get().getEtag() );
    }


    @Test
    void processMigratedAssociationsWithNoMigratedAssociationsDoesNothing(){
        final var associationDaos = testDataManager.fetchAssociationDaos( "1", "FutAssociation002" );
        associationsRepository.insert( associationDaos );

        dataMigrationPostProcessingTask.processMigratedAssociations();

        Assertions.assertEquals( associationDaos.getFirst().getEtag(), associationsRepository.findById( "1" ).get().getEtag() );
        Assertions.assertEquals( associationDaos.get( 1 ).getEtag(), associationsRepository.findById( "FutAssociation002" ).get().getEtag() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }

}
