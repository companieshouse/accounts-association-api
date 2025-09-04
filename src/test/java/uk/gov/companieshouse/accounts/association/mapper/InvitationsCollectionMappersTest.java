package uk.gov.companieshouse.accounts.association.mapper;

import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.accounts.association.common.TestDataManager.block;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;

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

        final var invitations = block(invitationsCollectionMappers.daoToDto(association, 0, 15));
        Assertions.assertEquals( 0, invitations.getTotalResults() );
        Assertions.assertEquals( 0, invitations.getPageNumber() );
        Assertions.assertEquals( 15, invitations.getItemsPerPage() );
        Assertions.assertEquals( 0, invitations.getTotalPages() );
        Assertions.assertEquals( "/associations/38/invitations?page_index=0&items_per_page=15", invitations.getLinks().getSelf() );
        Assertions.assertEquals( "", invitations.getLinks().getNext() );
        Assertions.assertTrue( invitations.getItems().isEmpty() );
    }

    @Test
    void daoToDtoCorrectlyMapsAssociationToInvitationsList() {
        final var association = testDataManager.fetchAssociationDaos("38").getFirst();
        association.getInvitations().getLast().invitedAt(LocalDateTime.now().minusDays(30));
        mockers.mockUsersServiceFetchUserDetails("111", "222", "444");

        final var invitations = block(invitationsCollectionMappers.daoToDto(association, 0, 15));
        Assertions.assertNotNull(invitations);

        Assertions.assertEquals(3, invitations.getTotalResults());
        Assertions.assertEquals(0, invitations.getPageNumber());
        Assertions.assertEquals(15, invitations.getItemsPerPage());
        Assertions.assertEquals(1, invitations.getTotalPages());
        Assertions.assertEquals("/associations/38/invitations?page_index=0&items_per_page=15", invitations.getLinks().getSelf());
        Assertions.assertEquals("", invitations.getLinks().getNext());

        record ExpectedData(String associationId, int daysAgo, boolean active) { }
        final Map<String, ExpectedData> expected = new LinkedHashMap<>();
        expected.put("bruce.wayne@gotham.city", new ExpectedData("38", 3,  true));
        expected.put("the.joker@gotham.city",   new ExpectedData("38", 2,  true));
        expected.put("robin@gotham.city",       new ExpectedData("38", 30, false));

        final var now = LocalDateTime.now();
        final var actual = invitations.getItems().stream().collect(
                Collectors.toMap(Invitation::getInvitedBy, Function.identity(), (a, b) -> a, LinkedHashMap::new));

        Assertions.assertEquals(expected.keySet(), actual.keySet(), "Expected emails addresses do not match actual results");

        expected.forEach((email, exp) -> {
            final var invitation = actual.get(email);
            Assertions.assertEquals("38", invitation.getAssociationId());
            Assertions.assertEquals(localDateTimeToNormalisedString(now.minusDays(exp.daysAgo())),
                    reduceTimestampResolution(invitation.getInvitedAt()));
            Assertions.assertEquals(exp.active(), invitation.getIsActive());
        });

        Assertions.assertIterableEquals(expected.keySet(), actual.keySet(), "Result set is out of order");
    }


    @Test
    void daoToDtoCorrectlyMapsAssociationToInvitationsListAndPaginatesCorrectly(){
        final var association = testDataManager.fetchAssociationDaos( "38" ).getFirst();
        association.getInvitations().getLast().invitedAt( LocalDateTime.now().minusDays( 8 ) );

        mockers.mockUsersServiceFetchUserDetails( "222" );

        final var invitations = block(invitationsCollectionMappers.daoToDto(association, 1, 1));
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
        Assertions.assertThrows( NullPointerException.class, () -> invitationsCollectionMappers.daoToDto( (List<AssociationDao>) null, 0, 15) );
    }

    @Test
    void daoToDtoAppliedToEmptyListReturnsEmpty(){
        final var invitations = block(invitationsCollectionMappers.daoToDto( List.of(), 0, 15 ));
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

        mockers.mockUsersServiceFetchUserDetails( "9999", "444" );

        final var invitations = block(invitationsCollectionMappers.daoToDto( associations, 0, 15 ));
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
        associations.getFirst().approvalExpiryAt( LocalDateTime.now().plusDays( 7 ) );
        associations.getFirst().getInvitations().getLast().invitedAt( LocalDateTime.now().minusDays( 8 ) );

        mockers.mockUsersServiceFetchUserDetails( "444" );

        final var invitations = block(invitationsCollectionMappers.daoToDto( associations, 0, 1 ));
        final var invitation = invitations.getItems().getFirst();

        Assertions.assertEquals( 2, invitations.getTotalResults() );
        Assertions.assertEquals( 0, invitations.getPageNumber() );
        Assertions.assertEquals( 1, invitations.getItemsPerPage() );
        Assertions.assertEquals( 2, invitations.getTotalPages() );
        Assertions.assertEquals( "/associations/invitations?page_index=0&items_per_page=1", invitations.getLinks().getSelf() );
        Assertions.assertEquals( "/associations/invitations?page_index=1&items_per_page=1", invitations.getLinks().getNext() );

        Assertions.assertEquals( "38", invitation.getAssociationId() );
        Assertions.assertEquals( "robin@gotham.city", invitation.getInvitedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( LocalDateTime.now().plusDays( 8 ) ), reduceTimestampResolution( invitation.getInvitedAt() ) );
        Assertions.assertTrue( invitation.getIsActive() );
    }


}
