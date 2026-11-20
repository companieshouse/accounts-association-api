package uk.gov.companieshouse.accounts.association.mapper;

import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.service.UsersService;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class InvitationsMapperTest {

    @Mock
    private UsersService usersService;

    private InvitationMapper invitationsMapper;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private Mockers mockers;

    @BeforeEach
    void setup(){
        invitationsMapper = new InvitationMapper( usersService, new InvitationDaoMapperImpl() );
        mockers = new Mockers( null, null, null, usersService );
    }

    @Test
    void daoToDtoMapsInvitationDaoToInvitation(){
        mockers.mockUsersServiceFetchUserDetails( "666" );
        final var invitationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst().getInvitations().getFirst();
        final var invitationDto = invitationsMapper.daoToDto( invitationDao, "1" );
        Assertions.assertEquals( "homer.simpson@springfield.com", invitationDto.getInvitedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( invitationDao.getInvitedAt() ), reduceTimestampResolution( invitationDto.getInvitedAt() ) );
        Assertions.assertEquals( "1", invitationDto.getAssociationId() );
        Assertions.assertTrue( invitationDto.getIsActive() );
    }

    @Test
    void daoToDtoLeavesInvitedByNullWhenInvitingUserIsNotFound(){
        final var invitationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst().getInvitations().getFirst();
        mockers.mockUsersServiceFetchUserDetailsNotFound( invitationDao.getInvitedBy() );
        final var invitationDto = invitationsMapper.daoToDto( invitationDao, "1" );
        Assertions.assertNull( invitationDto.getInvitedBy() );
        Assertions.assertEquals( "1", invitationDto.getAssociationId() );
    }

}
