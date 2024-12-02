package uk.gov.companieshouse.accounts.association.tasks;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class DataMigrationPostProcessingTaskTest {

    @Mock
    private UsersService usersService;

    @Mock
    private AssociationsService associationsService;

    @InjectMocks
    private DataMigrationPostProcessingTask dataMigrationPostProcessingTask;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static Mockers mockers;

    @BeforeEach
    void setup(){
        mockers = new Mockers( null, null, null, null, usersService );
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

        final var associationDaos = testDataManager.fetchAssociationDaos( "FutAssociation001", "FutAssociation003", "FutAssociation004" );
        final var fryUser = testDataManager.fetchUserDtos( "FutUser001" ).getFirst();

        Mockito.doReturn( 3L ).when( associationsService ).fetchNumberOfUnprocessedMigratedAssociations();

        if ( batchSize == 1 ){
            Mockito.doReturn( new PageImpl<>( List.of( associationDaos.getFirst() ), PageRequest.of(0, 1 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 0, batchSize );
            Mockito.doReturn( new PageImpl<>( List.of( associationDaos.get( 1 ) ), PageRequest.of(1, 1 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 1, batchSize );
            Mockito.doReturn( new PageImpl<>( List.of( associationDaos.getLast() ), PageRequest.of(2, 1 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 2, batchSize );
        } else if ( batchSize == 2 ){
            Mockito.doReturn( new PageImpl<>( List.of( associationDaos.getFirst(), associationDaos.get( 1 ) ), PageRequest.of(0, 2 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 0, batchSize );
            Mockito.doReturn( new PageImpl<>( List.of( associationDaos.getLast() ), PageRequest.of(1, 2 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 1, batchSize );
        } else if ( batchSize == 3 ){
            Mockito.doReturn( new PageImpl<>( associationDaos, PageRequest.of(0, 3 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 0, batchSize );
        } if ( batchSize == 4 ){
            Mockito.doReturn( new PageImpl<>( associationDaos, PageRequest.of(0, 4 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 0, batchSize );
        }

        Mockito.doReturn( Map.of("fry@futurama.com", fryUser ) ).when( usersService ).searchUserDetails( any( Stream.class ) );

        dataMigrationPostProcessingTask.processMigratedAssociations();

        Mockito.verify( associationsService ).updateAssociation( eq( "FutAssociation001" ), any() );
    }

    @ParameterizedTest
    @MethodSource( "batchSizeData" )
    void processMigratedAssociationsWithEvenNumberOfAssociationsPerformsUpdatesAssociationsForExistingUsers( final int batchSize ) throws ApiErrorResponseException, URIValidationException {
        ReflectionTestUtils.setField( dataMigrationPostProcessingTask, "ITEMS_PER_PAGE", batchSize );

        final var associationDaos = testDataManager.fetchAssociationDaos( "FutAssociation001", "FutAssociation003" );
        final var fryUser = testDataManager.fetchUserDtos( "FutUser001" ).getFirst();

        Mockito.doReturn( 2L ).when( associationsService ).fetchNumberOfUnprocessedMigratedAssociations();

        if ( batchSize == 1 ){
            Mockito.doReturn( new PageImpl<>( List.of( associationDaos.getFirst() ), PageRequest.of(0, 1 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 0, batchSize );
            Mockito.doReturn( new PageImpl<>( List.of( associationDaos.get( 1 ) ), PageRequest.of(1, 1 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 1, batchSize );
        } else if ( batchSize == 2 ){
            Mockito.doReturn( new PageImpl<>( List.of( associationDaos.getFirst(), associationDaos.get( 1 ) ), PageRequest.of(0, 2 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 0, batchSize );
        } else if ( batchSize == 3 ){
            Mockito.doReturn( new PageImpl<>( associationDaos, PageRequest.of(0, 3 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 0, batchSize );
        } if ( batchSize == 4 ){
            Mockito.doReturn( new PageImpl<>( associationDaos, PageRequest.of(0, 4 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 0, batchSize );
        }

        Mockito.doReturn( Map.of("fry@futurama.com", fryUser ) ).when( usersService ).searchUserDetails( any( Stream.class ) );

        dataMigrationPostProcessingTask.processMigratedAssociations();

        Mockito.verify( associationsService ).updateAssociation( eq( "FutAssociation001" ), any() );
    }

    @ParameterizedTest
    @MethodSource( "batchSizeData" )
    void processMigratedAssociationsWithNoMigratedAssociationsDoesNothing( final int batchSize ) {
        ReflectionTestUtils.setField( dataMigrationPostProcessingTask, "ITEMS_PER_PAGE", batchSize );

        Mockito.doReturn( 0L ).when( associationsService ).fetchNumberOfUnprocessedMigratedAssociations();

        dataMigrationPostProcessingTask.processMigratedAssociations();

        Mockito.verify( associationsService, Mockito.times( 0 ) ).updateAssociation( any(), any() );
    }

    @ParameterizedTest
    @MethodSource( "batchSizeData" )
    void processMigratedAssociations( final int batchSize ) throws ApiErrorResponseException, URIValidationException {
        ReflectionTestUtils.setField( dataMigrationPostProcessingTask, "ITEMS_PER_PAGE", batchSize );

        final var associationDaos = testDataManager.fetchAssociationDaos( "FutAssociation001", "FutAssociation003", "FutAssociation004" );
        final var fryUser = testDataManager.fetchUserDtos( "FutUser001" ).getFirst();

        Mockito.doReturn( 3L ).when( associationsService ).fetchNumberOfUnprocessedMigratedAssociations();

        if ( batchSize == 1 ){
            Mockito.doReturn( new PageImpl<>( List.of( associationDaos.getFirst() ), PageRequest.of(0, 1 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 0, batchSize );
            Mockito.doReturn( new PageImpl<>( List.of( associationDaos.get( 1 ) ), PageRequest.of(1, 1 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 1, batchSize );
            Mockito.doReturn( new PageImpl<>( List.of( associationDaos.getLast() ), PageRequest.of(2, 1 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 2, batchSize );
        } else if ( batchSize == 2 ){
            Mockito.doReturn( new PageImpl<>( List.of( associationDaos.getFirst(), associationDaos.get( 1 ) ), PageRequest.of(0, 2 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 0, batchSize );
            Mockito.doReturn( new PageImpl<>( List.of( associationDaos.getLast() ), PageRequest.of(1, 2 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 1, batchSize );
        } else if ( batchSize == 3 ){
            Mockito.doReturn( new PageImpl<>( associationDaos, PageRequest.of(0, 3 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 0, batchSize );
        } if ( batchSize == 4 ){
            Mockito.doReturn( new PageImpl<>( associationDaos, PageRequest.of(0, 4 ), 3 ) ).when( associationsService ).fetchUnprocessedMigratedAssociations( 0, batchSize );
        }

        Mockito.doReturn( Map.of("fry@futurama.com", fryUser ) ).when( usersService ).searchUserDetails( any( Stream.class ) );

        dataMigrationPostProcessingTask.processMigratedAssociations();

        Mockito.verify( associationsService ).updateAssociation( eq( "FutAssociation001" ), any() );
    }

}

