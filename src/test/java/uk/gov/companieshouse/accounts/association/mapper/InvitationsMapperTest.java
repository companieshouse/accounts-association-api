package uk.gov.companieshouse.accounts.association.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.utils.MapperUtil;

import java.time.LocalDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class InvitationsMapperTest {

    @Mock
    MapperUtil mapperUtil;

    @InjectMocks
    InvitationsMapper invitationsMapper = new InvitationsMapperImpl();

    LocalDateTime now = LocalDateTime.now();


    private String reduceTimestampResolution( String timestamp ){
        return timestamp.substring( 0, timestamp.indexOf( ":" ) );
    }

    @Test
    void daoToDtoMapsInvitationDaoToInvitation(){
        final var invitationDao = new InvitationDao();
        invitationDao.setInvitedAt( now );
        invitationDao.setInvitedBy( "111" );

        final var actualInvitationDto = invitationsMapper.daoToDto( invitationDao );

        Assertions.assertEquals( "111", actualInvitationDto.getInvitedBy() );
        Assertions.assertEquals( reduceTimestampResolution( now.toString() ), reduceTimestampResolution( actualInvitationDto.getInvitedAt() ) );
    }

    @Test
    void daoToDtoWithActiveAndInactiveInvitationsMapsAssociationDaoToInvitations() {
        final var activeInvitationDao = new InvitationDao();
        activeInvitationDao.setInvitedAt(now);
        activeInvitationDao.setInvitedBy("111");

        final var inactiveInvitationDao = new InvitationDao();
        inactiveInvitationDao.setInvitedAt(now.minusDays(7));
        inactiveInvitationDao.setInvitedBy("222");

        final var associationDao = new AssociationDao();
        associationDao.setId("1");
        associationDao.setApprovalExpiryAt(now.plusDays(7));
        associationDao.setInvitations(List.of(activeInvitationDao, inactiveInvitationDao));

        final var invitations = invitationsMapper.daoToDto(associationDao).toList();

        Assertions.assertEquals(2, invitations.size());

        final var activeInvitation = invitations.stream()
                .filter(inv -> "111".equals(inv.getInvitedBy()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Active invitation not found"));

        final var inactiveInvitation = invitations.stream()
                .filter(inv -> "222".equals(inv.getInvitedBy()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Inactive invitation not found"));

        // Active invitation assertions
        Assertions.assertEquals("111", activeInvitation.getInvitedBy());
        Assertions.assertEquals(reduceTimestampResolution(now.toString()), reduceTimestampResolution(activeInvitation.getInvitedAt()));
        Assertions.assertEquals("1", activeInvitation.getAssociationId());
        Assertions.assertTrue(activeInvitation.getIsActive());

        // Inactive invitation assertions
        Assertions.assertEquals("222", inactiveInvitation.getInvitedBy());
        Assertions.assertEquals(reduceTimestampResolution(now.minusDays(7).toString()), reduceTimestampResolution(inactiveInvitation.getInvitedAt()));
        Assertions.assertEquals("1", inactiveInvitation.getAssociationId());
        Assertions.assertFalse(inactiveInvitation.getIsActive());
    }

    @Test
    void daoToDtoWithInactiveInvitationMapsAssociationDaoToInvitations(){
        final var sevenDaysAgo = now.minusDays(7);

        final var invitationDao = new InvitationDao();
        invitationDao.setInvitedAt( sevenDaysAgo );
        invitationDao.setInvitedBy( "111" );

        final var associationDao = new AssociationDao();
        associationDao.setId( "1" );
        associationDao.setApprovalExpiryAt( now );
        associationDao.setInvitations( List.of( invitationDao ) );

        final var invitations = invitationsMapper.daoToDto( associationDao ).toList();
        final var invitation = invitations.getFirst();

        Assertions.assertEquals( 1, invitations.size() );
        Assertions.assertEquals( "111", invitation.getInvitedBy() );
        Assertions.assertEquals( reduceTimestampResolution( sevenDaysAgo.toString() ), reduceTimestampResolution( invitation.getInvitedAt() ) );
        Assertions.assertEquals( "1", invitation.getAssociationId() );
        Assertions.assertFalse( invitation.getIsActive() );
    }

}
