package uk.gov.companieshouse.accounts.association.mapper;

import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.utils.MapperUtil;

import java.time.LocalDateTime;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class InvitationsMapperTest {

    @Mock
    private MapperUtil mapperUtil;

    @InjectMocks
    private InvitationsMapper invitationsMapper = new InvitationsMapperImpl();

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final LocalDateTime now = LocalDateTime.now();

    @Test
    void daoToDtoMapsInvitationDaoToInvitation(){
        final var invitationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst().getInvitations().getFirst();
        final var invitationDto = invitationsMapper.daoToDto( invitationDao );
        Assertions.assertEquals( "666", invitationDto.getInvitedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( invitationDao.getInvitedAt() ), reduceTimestampResolution( invitationDto.getInvitedAt() ) );
    }

    @Test
    void daoToDtoWithActiveAndInactiveInvitationsMapsAssociationDaoToInvitations() {
        final var associationDao = testDataManager.fetchAssociationDaos( "37" ).getFirst();

        final var invitations = invitationsMapper.daoToDto(associationDao).toList();
        final var activeInvitation = invitations.stream().filter( invitation -> invitation.getInvitedBy().equals("444") ).toList().getFirst();
        final var inactiveInvitation = invitations.stream().filter( invitation -> invitation.getInvitedBy().equals("666") ).toList().getFirst();

        Assertions.assertEquals(3, invitations.size());
        Assertions.assertEquals("444", activeInvitation.getInvitedBy());
        Assertions.assertEquals(localDateTimeToNormalisedString(now.minusDays(4)), reduceTimestampResolution(activeInvitation.getInvitedAt()));
        Assertions.assertEquals("37", activeInvitation.getAssociationId());
        Assertions.assertTrue(activeInvitation.getIsActive());
        Assertions.assertEquals("666", inactiveInvitation.getInvitedBy());
        Assertions.assertEquals(localDateTimeToNormalisedString(now.minusDays(9)), reduceTimestampResolution(inactiveInvitation.getInvitedAt()));
        Assertions.assertEquals("37", inactiveInvitation.getAssociationId());
        Assertions.assertFalse(inactiveInvitation.getIsActive());
    }

}
