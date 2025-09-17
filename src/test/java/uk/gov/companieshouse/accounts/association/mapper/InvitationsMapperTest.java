package uk.gov.companieshouse.accounts.association.mapper;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.service.UsersService;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class InvitationsMapperTest {

    @Mock
    private UsersService usersService;

    @InjectMocks
    private InvitationMapper invitationsMapper = new InvitationMapperImpl();

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup() {
    }

    @Test
    void daoToDtoMapsInvitationDaoToInvitation() {
        final var userId = "666";

        when(usersService.fetchUserDetails(eq(userId), anyString())).thenReturn(testDataManager.fetchUserDtos(userId).getFirst());

        final var invitationDao = testDataManager.fetchAssociationDaos("1").getFirst().getInvitations().getFirst();
        final var invitationDto = invitationsMapper.daoToDto(invitationDao, "1");
        Assertions.assertEquals("homer.simpson@springfield.com", invitationDto.getInvitedBy());
        Assertions.assertEquals(localDateTimeToNormalisedString(invitationDao.getInvitedAt()), reduceTimestampResolution(invitationDto.getInvitedAt()));
        Assertions.assertEquals("1", invitationDto.getAssociationId());
        Assertions.assertTrue(invitationDto.getIsActive());
    }

}
