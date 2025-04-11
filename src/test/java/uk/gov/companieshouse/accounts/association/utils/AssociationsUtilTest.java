package uk.gov.companieshouse.accounts.association.utils;

import static java.time.LocalDateTime.now;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.fetchAllStatusesWithout;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToAuthCodeConfirmedUpdated;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToConfirmedUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToInvitationUpdate;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToRemovedUpdate;

import java.util.Set;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.models.PreviousStatesDao;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class AssociationsUtilTest {

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void mapToInvitationUpdateWithNullTargetAssociationThrowsNullPointerException(){
        final var targetUser = testDataManager.fetchUserDtos( "MKUser001" ).getFirst();
        Assertions.assertThrows( NullPointerException.class, () -> mapToInvitationUpdate( null, targetUser, "MKUser002", now() ) );
    }

    @Test
    void mapToInvitationUpdateWithTargetUserSwapsUserIdAndUserEmail(){
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos( "MKUser001" ).getFirst();

        final var update = mapToInvitationUpdate( targetAssociation, targetUser, "MKUser002", now() );
        final var documentSet = update.getUpdateObject().get( "$set", Document.class );
        final var documentPush = update.getUpdateObject().get( "$push", Document.class );
        final var invitations = (InvitationDao) documentPush.get( "invitations" );
        final var previousStates = (PreviousStatesDao) documentPush.get( "previous_states" );

        Assertions.assertEquals( "awaiting-approval", documentSet.get( "status" ) );
        Assertions.assertEquals( "MKUser002", invitations.getInvitedBy() );
        Assertions.assertNotNull( invitations.getInvitedAt() );
        Assertions.assertNotNull( documentSet.get( "approval_expiry_at" ) );
        Assertions.assertNotNull( documentSet.get( "etag" ) );
        Assertions.assertEquals( "migrated", previousStates.getStatus() );
        Assertions.assertEquals( "MKUser002", previousStates.getChangedBy() );
        Assertions.assertNotNull(  previousStates.getChangedAt() );
        Assertions.assertNull( documentSet.get( "user_email" ) );
        Assertions.assertEquals( "MKUser001", documentSet.get( "user_id" ) );
    }

    @Test
    void mapToInvitationUpdateWithoutTargetUserSwapsUserIdAndUserEmail(){
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst();

        final var update = mapToInvitationUpdate( targetAssociation, null, "MKUser002", now() );
        final var documentSet = update.getUpdateObject().get( "$set", Document.class );
        final var documentPush = update.getUpdateObject().get( "$push", Document.class );
        final var invitations = (InvitationDao) documentPush.get( "invitations" );
        final var previousStates = (PreviousStatesDao) documentPush.get( "previous_states" );

        Assertions.assertEquals( "awaiting-approval", documentSet.get( "status" ) );
        Assertions.assertEquals( "MKUser002", invitations.getInvitedBy() );
        Assertions.assertNotNull( invitations.getInvitedAt() );
        Assertions.assertNotNull( documentSet.get( "approval_expiry_at" ) );
        Assertions.assertNotNull( documentSet.get( "etag" ) );
        Assertions.assertEquals( "migrated", previousStates.getStatus() );
        Assertions.assertEquals( "MKUser002", previousStates.getChangedBy() );
        Assertions.assertNotNull(  previousStates.getChangedAt() );
    }

    @Test
    void mapToConfirmedUpdateCarriesOutUpdateCorrectly(){
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst();
        final var update = mapToConfirmedUpdate( targetAssociation, null, "MKUser002" );
        final var documentSet = update.getUpdateObject().get( "$set", Document.class );
        Assertions.assertEquals( "confirmed", documentSet.get( "status" ) );
        Assertions.assertNotNull( documentSet.get( "approved_at" ) );
    }

    @Test
    void mapToRemovedUpdateCarriesOutUpdateCorrectly(){
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst();
        final var update = mapToRemovedUpdate( targetAssociation, null, "MKUser002" );
        final var documentSet = update.getUpdateObject().get( "$set", Document.class );
        Assertions.assertEquals( "removed", documentSet.get( "status" ) );
        Assertions.assertNotNull( documentSet.get( "removed_at" ) );
    }

    @Test
    void mapToAuthCodeConfirmedUpdatedCarriesOutUpdateCorrectly(){
        final var targetAssociation = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst();
        final var targetUser = testDataManager.fetchUserDtos( "MKUser001" ).getFirst();
        final var update = mapToAuthCodeConfirmedUpdated( targetAssociation, targetUser, "MKUser002" );
        final var documentSet = update.getUpdateObject().get( "$set", Document.class );
        Assertions.assertEquals( "confirmed", documentSet.get( "status" ) );
        Assertions.assertEquals( "auth_code", documentSet.get( "approval_route" ) );
    }

    @Test
    void fetchAllStatusesWithoutWithNullThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> fetchAllStatusesWithout( null ) );
    }

    @Test
    void fetchAllStatusesWithoutWithEmptyListRetrievesAllStatuses(){
        Assertions.assertEquals( 4, fetchAllStatusesWithout( Set.of() ).size() );
    }

    @Test
    void fetchAllStatusesWithoutRetrievesSpecifiedStatuses(){
        final var statuses = fetchAllStatusesWithout( Set.of( StatusEnum.CONFIRMED, StatusEnum.REMOVED, StatusEnum.AWAITING_APPROVAL ) );
        Assertions.assertEquals( 1, statuses.size() );
        Assertions.assertTrue(  statuses.contains( StatusEnum.MIGRATED ) );
    }

}
