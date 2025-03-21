package uk.gov.companieshouse.accounts.association.mapper;

import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.MIGRATED;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.REMOVED;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class PreviousStatesCollectionMappersTest {

    private PreviousStatesCollectionMappers previousStatesCollectionMappers;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup(){
        previousStatesCollectionMappers = new PreviousStatesCollectionMappers( new PreviousStatesMapperImpl() );
    }

    @Test
    void daoToDtoThrowsNullPointerExceptionWhenAssociationIsNull(){
        Assertions.assertThrows( NullPointerException.class, () -> previousStatesCollectionMappers.daoToDto( null, 0, 15 ) );
    }

    @Test
    void daoToDtoMapsDataCorrectly(){
        final var association = testDataManager.fetchAssociationDaos( "MKAssociation003" ).getFirst();

        final var now = LocalDateTime.now();

        final var previousStatesList = previousStatesCollectionMappers.daoToDto( association, 0, 15 );
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();

        Assertions.assertEquals( 15, previousStatesList.getItemsPerPage() );
        Assertions.assertEquals( 0, previousStatesList.getPageNumber() );
        Assertions.assertEquals( 4, previousStatesList.getTotalResults() );
        Assertions.assertEquals( 1, previousStatesList.getTotalPages() );
        Assertions.assertEquals( "/associations/MKAssociation003/previous-states?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 4, items.size() );
        Assertions.assertEquals( CONFIRMED, items.get( 0 ).getStatus() );
        Assertions.assertEquals( "MKUser003", items.get( 0 ).getChangedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.minusDays( 6L ) ), reduceTimestampResolution( items.get( 0 ).getChangedAt() ) );
        Assertions.assertEquals( AWAITING_APPROVAL, items.get( 1 ).getStatus() );
        Assertions.assertEquals( "MKUser003", items.get( 1 ).getChangedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.minusDays( 7L ) ), reduceTimestampResolution( items.get( 1 ).getChangedAt() ) );
        Assertions.assertEquals( REMOVED, items.get( 2 ).getStatus() );
        Assertions.assertEquals( "MKUser002", items.get( 2 ).getChangedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.minusDays( 8L ) ), reduceTimestampResolution( items.get( 2 ).getChangedAt() ) );
        Assertions.assertEquals( MIGRATED, items.get( 3 ).getStatus() );
        Assertions.assertEquals( "MKUser002", items.get( 3 ).getChangedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.minusDays( 9L ) ), reduceTimestampResolution( items.get( 3 ).getChangedAt() ) );
    }

    @Test
    void daoToDtoPaginatesCorrectly(){
        final var association = testDataManager.fetchAssociationDaos( "MKAssociation003" ).getFirst();

        final var now = LocalDateTime.now();

        final var previousStatesList = previousStatesCollectionMappers.daoToDto( association, 1, 1 );
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();

        Assertions.assertEquals( 1, previousStatesList.getItemsPerPage() );
        Assertions.assertEquals( 1, previousStatesList.getPageNumber() );
        Assertions.assertEquals( 4, previousStatesList.getTotalResults() );
        Assertions.assertEquals( 4, previousStatesList.getTotalPages() );
        Assertions.assertEquals( "/associations/MKAssociation003/previous-states?page_index=1&items_per_page=1", links.getSelf() );
        Assertions.assertEquals( "/associations/MKAssociation003/previous-states?page_index=2&items_per_page=1", links.getNext() );
        Assertions.assertEquals( 1, items.size() );
        Assertions.assertEquals( AWAITING_APPROVAL, items.getFirst().getStatus() );
        Assertions.assertEquals( "MKUser003", items.getFirst().getChangedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.minusDays( 7L ) ), reduceTimestampResolution( items.getFirst().getChangedAt() ) );
    }

    @Test
    void daoToDtoReturnsEmptyListWhenThereAreNoPreviousStates(){
        final var association = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst();

        final var previousStatesList = previousStatesCollectionMappers.daoToDto( association, 0, 15 );
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();

        Assertions.assertEquals( 15, previousStatesList.getItemsPerPage() );
        Assertions.assertEquals( 0, previousStatesList.getPageNumber() );
        Assertions.assertEquals( 0, previousStatesList.getTotalResults() );
        Assertions.assertEquals( 0, previousStatesList.getTotalPages() );
        Assertions.assertEquals( "/associations/MKAssociation001/previous-states?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertTrue( items.isEmpty() );
    }

}
