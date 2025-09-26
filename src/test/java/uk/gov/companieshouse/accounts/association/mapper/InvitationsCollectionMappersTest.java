package uk.gov.companieshouse.accounts.association.mapper;

import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.UsersService;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class InvitationsCollectionMappersTest {

    @Mock
    private UsersService usersService;

    private InvitationsCollectionMappers invitationsCollectionMappers;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static Mockers mockers;

    @BeforeEach
    void setup(){
        final var invitationMapper = new InvitationMapperImpl();
        ReflectionTestUtils.setField( invitationMapper, "usersService", usersService );
        invitationsCollectionMappers = new InvitationsCollectionMappers( invitationMapper );
        mockers = new Mockers( null, null, null, usersService );
    }

    @Test
    void daoToDtoWithNullAssociationThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class , () -> invitationsCollectionMappers.daoToDto( (AssociationDao) null, 0, 15 ) );
    }

    @Test
    void daoToDtoAppliedToAssociationWithoutInvitationsReturnsEmpty(){
        final var association = testDataManager.fetchAssociationDaos( "38" ).getFirst();
        association.getInvitations().clear();

        final var invitations = invitationsCollectionMappers.daoToDto( association, 0, 15 );
        Assertions.assertEquals( 0, invitations.getTotalResults() );
        Assertions.assertEquals( 0, invitations.getPageNumber() );
        Assertions.assertEquals( 15, invitations.getItemsPerPage() );
        Assertions.assertEquals( 0, invitations.getTotalPages() );
        Assertions.assertEquals( "/associations/38/invitations?page_index=0&items_per_page=15", invitations.getLinks().getSelf() );
        Assertions.assertEquals( "", invitations.getLinks().getNext() );
        Assertions.assertTrue( invitations.getItems().isEmpty() );
    }

    @Test
    void daoToDtoCorrectlyMapsAssociationToInvitationsList(){
        final var association = testDataManager.fetchAssociationDaos( "38" ).getFirst();
        association.getInvitations().getLast().invitedAt( LocalDateTime.now().minusDays( 30 ) );

        mockers.mockUsersServiceFetchUserDetails( "111", "222", "444" );

        final var invitations = invitationsCollectionMappers.daoToDto( association, 0, 15 );
        final var invitation0 = invitations.getItems().getFirst();
        final var invitation1 = invitations.getItems().get( 1 );
        final var invitation2 = invitations.getItems().getLast();

        Assertions.assertEquals( 3, invitations.getTotalResults() );
        Assertions.assertEquals( 0, invitations.getPageNumber() );
        Assertions.assertEquals( 15, invitations.getItemsPerPage() );
        Assertions.assertEquals( 1, invitations.getTotalPages() );
        Assertions.assertEquals( "/associations/38/invitations?page_index=0&items_per_page=15", invitations.getLinks().getSelf() );
        Assertions.assertEquals( "", invitations.getLinks().getNext() );
        Assertions.assertEquals( "38", invitation0.getAssociationId() );
        Assertions.assertEquals( "bruce.wayne@gotham.city", invitation0.getInvitedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( LocalDateTime.now().minusDays( 3 ) ), reduceTimestampResolution( invitation0.getInvitedAt() ) );
        Assertions.assertTrue( invitation0.getIsActive() );
        Assertions.assertEquals( "38", invitation1.getAssociationId() );
        Assertions.assertEquals( "the.joker@gotham.city", invitation1.getInvitedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( LocalDateTime.now().minusDays( 2 ) ), reduceTimestampResolution( invitation1.getInvitedAt() ) );
        Assertions.assertTrue( invitation1.getIsActive() );
        Assertions.assertEquals( "38", invitation2.getAssociationId() );
        Assertions.assertEquals( "robin@gotham.city", invitation2.getInvitedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( LocalDateTime.now().minusDays( 30 ) ), reduceTimestampResolution( invitation2.getInvitedAt() ) );
        Assertions.assertFalse( invitation2.getIsActive() );
    }

    @Test
    void daoToDtoCorrectlyMapsAssociationToInvitationsListAndPaginatesCorrectly(){
        final var association = testDataManager.fetchAssociationDaos( "38" ).getFirst();
        association.getInvitations().getLast().invitedAt( LocalDateTime.now().minusDays( 8 ) );

        mockers.mockUsersServiceFetchUserDetails( "222" );

        final var invitations = invitationsCollectionMappers.daoToDto( association, 1, 1 );
        final var invitation = invitations.getItems().getFirst();

        Assertions.assertEquals( 3, invitations.getTotalResults() );
        Assertions.assertEquals( 1, invitations.getPageNumber() );
        Assertions.assertEquals( 1, invitations.getItemsPerPage() );
        Assertions.assertEquals( 3, invitations.getTotalPages() );
        Assertions.assertEquals( "/associations/38/invitations?page_index=1&items_per_page=1", invitations.getLinks().getSelf() );
        Assertions.assertEquals( "/associations/38/invitations?page_index=2&items_per_page=1", invitations.getLinks().getNext() );
        Assertions.assertEquals( "38", invitation.getAssociationId() );
        Assertions.assertEquals( "the.joker@gotham.city", invitation.getInvitedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( LocalDateTime.now().minusDays( 2 ) ), reduceTimestampResolution( invitation.getInvitedAt() ) );
        Assertions.assertTrue( invitation.getIsActive() );
    }

    @Test
    void daoToDtoWithNullListThrowsNullPointerException(){
        final var pageRequest = PageRequest.of( 0, 15 );
        Assertions.assertThrows(
                NullPointerException.class, () -> invitationsCollectionMappers.daoToDto( null, pageRequest ) );
    }

    @Test
    void daoToDtoAppliedToEmptyListReturnsEmpty(){
        final var pageRequest = PageRequest.of( 0, 15 );
        final var invitations = invitationsCollectionMappers.daoToDto( Page.empty(), pageRequest );
        Assertions.assertEquals( 0, invitations.getTotalResults() );
        Assertions.assertEquals( 0, invitations.getPageNumber() );
        Assertions.assertEquals( 15, invitations.getItemsPerPage() );
        Assertions.assertEquals( 0, invitations.getTotalPages() );
        Assertions.assertEquals( "/associations/invitations?page_index=0&items_per_page=15", invitations.getLinks().getSelf() );
        Assertions.assertEquals( "", invitations.getLinks().getNext() );
        Assertions.assertTrue( invitations.getItems().isEmpty() );
    }

    @Test
    void daoToDtoCorrectlyMapsAssociationsToInvitationsList(){
        final var associations = testDataManager.fetchAssociationDaos( "36", "38" );
        associations.getFirst().approvalExpiryAt( LocalDateTime.now().plusDays( 30 ) );
        associations.getFirst().getInvitations().getLast().invitedAt( LocalDateTime.now().minusDays( 31 ) );
        final var pageRequest = PageRequest.of( 0, 15);

        mockers.mockUsersServiceFetchUserDetails( "9999", "444" );

        final Page<AssociationDao> associationsWithInvitations = new PageImpl<>( associations, pageRequest, associations.size() );

        final var invitations = invitationsCollectionMappers.daoToDto( associationsWithInvitations, pageRequest );
        final var invitation0 = invitations.getItems().getFirst();
        final var invitation1 = invitations.getItems().getLast();

        Assertions.assertEquals( 2, invitations.getTotalResults() );
        Assertions.assertEquals( 0, invitations.getPageNumber() );
        Assertions.assertEquals( 15, invitations.getItemsPerPage() );
        Assertions.assertEquals( 1, invitations.getTotalPages() );
        Assertions.assertEquals( "/associations/invitations?page_index=0&items_per_page=15", invitations.getLinks().getSelf() );
        Assertions.assertEquals( "", invitations.getLinks().getNext() );

        Assertions.assertEquals( "36", invitation0.getAssociationId() );
        Assertions.assertEquals( "scrooge.mcduck@disney.land", invitation0.getInvitedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( LocalDateTime.now().minusDays( 31 ) ), reduceTimestampResolution( invitation0.getInvitedAt() ) );
        Assertions.assertFalse( invitation0.getIsActive() );

        Assertions.assertEquals( "38", invitation1.getAssociationId() );
        Assertions.assertEquals( "robin@gotham.city", invitation1.getInvitedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( LocalDateTime.now().plusDays( 8 ) ), reduceTimestampResolution( invitation1.getInvitedAt() ) );
        Assertions.assertTrue( invitation1.getIsActive() );
    }

    @Test
    void daoToDtoCorrectlyMapsAssociationsToInvitationsListAndPaginatesCorrectly(){
        final var associations = testDataManager.fetchAssociationDaos( "36", "38" );
        final var pageRequest = PageRequest.of( 0, 1);
        List<AssociationDao> pageContent = associations.subList(0, 1);
        final Page<AssociationDao> associationsWithInvitations = new PageImpl<>( pageContent, pageRequest, associations.size() );
        associations.getFirst().approvalExpiryAt( LocalDateTime.now().plusDays( 7 ) );
        associations.getFirst().getInvitations().getLast().invitedAt( LocalDateTime.now().minusDays( 8 ) );

        mockers.mockUsersServiceFetchUserDetails( "9999" );

        final var invitations = invitationsCollectionMappers.daoToDto( associationsWithInvitations, pageRequest );
        final var invitation = invitations.getItems().getFirst();

        Assertions.assertEquals( 2, invitations.getTotalResults() );
        Assertions.assertEquals( 0, invitations.getPageNumber() );
        Assertions.assertEquals( 1, invitations.getItemsPerPage() );
        Assertions.assertEquals( 2, invitations.getTotalPages() );
        Assertions.assertEquals( "/associations/invitations?page_index=0&items_per_page=1", invitations.getLinks().getSelf() );
        Assertions.assertEquals( "/associations/invitations?page_index=1&items_per_page=1", invitations.getLinks().getNext() );

        Assertions.assertEquals( "36", invitation.getAssociationId() );
        Assertions.assertEquals( "scrooge.mcduck@disney.land", invitation.getInvitedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( LocalDateTime.now().minusDays( 8 ) ), reduceTimestampResolution( invitation.getInvitedAt() ) );
        Assertions.assertTrue( invitation.getIsActive() );
    }

    @Test
    void mapToMostRecentInvitationThrowsWhenInvitationsIsNull() {
        final AssociationDao association = new AssociationDao() {
            @Override
            public List<uk.gov.companieshouse.accounts.association.models.InvitationDao> getInvitations() {
                return null;
            }
        };
        Assertions.assertThrows( IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod( invitationsCollectionMappers, "mapToMostRecentInvitation",
                        association ) );
    }

    @Test
    void mapToMostRecentInvitationThrowsWhenInvitationsIsEmpty() {
        final var association = new AssociationDao();
        association.setInvitations( Collections.emptyList() );

        Assertions.assertThrows( IllegalStateException.class,
                () -> ReflectionTestUtils.invokeMethod( invitationsCollectionMappers, "mapToMostRecentInvitation",
                        association ) );
    }

}
