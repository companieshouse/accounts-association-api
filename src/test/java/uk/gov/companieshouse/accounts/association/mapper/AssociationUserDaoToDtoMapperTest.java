package uk.gov.companieshouse.accounts.association.mapper;

import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;
import uk.gov.companieshouse.api.accounts.user.model.User;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class AssociationUserDaoToDtoMapperTest {

    @Mock
    private UsersService usersService;

    @InjectMocks
    private AssociationUserDaoToDtoMapper associationUserDaoToDtoMapper = new AssociationUserDaoToDtoMapperImpl();

    private static final String DEFAULT_DISPLAY_NAME = "Not provided";
    private static final String DEFAULT_KIND = "association";

    @Test
    void enrichAssociationWithUserDetailsWithNullInputThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> associationUserDaoToDtoMapper.enrichAssociationWithUserDetails( null ) );
    }

    @Test
    void enrichAssociationWithUserDetailsWithoutDisplayNameSetsDefaultDisplayName(){
        final var association = new Association().userId( "111" );

        final var user = new User().email( "jason.manford@comedy.com" );
        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any() );
        associationUserDaoToDtoMapper.enrichAssociationWithUserDetails( association );

        Assertions.assertEquals( "jason.manford@comedy.com", association.getUserEmail() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, association.getDisplayName() );
    }

    @Test
    void enrichAssociationWithUserDetailsSetsDisplayName(){
        final var association = new Association().userId( "111" );

        final var user = new User().email( "anne@the.chase.com" ).displayName( "The Governess" );
        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any() );
        associationUserDaoToDtoMapper.enrichAssociationWithUserDetails( association );

        Assertions.assertEquals( "anne@the.chase.com", association.getUserEmail() );
        Assertions.assertEquals( "The Governess", association.getDisplayName() );
    }

    @Test
    void daoToDtoWithNullInputReturnsNull(){
        Assertions.assertNull( associationUserDaoToDtoMapper.daoToDto( null ) );
    }

    @Test
    void daoToDtoWithOnlyMandatoryFieldsSuccessfullyPerformsMapping(){
        final var dao = new uk.gov.companieshouse.accounts.association.models.Association();
        dao.setId( "1" );
        dao.setUserId( "111" );
        dao.setCompanyNumber( "111111" );

        final var user =
        new User().email( "bruce.wayne@gotham.city" )
                  .displayName( "Batman" );
        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any() );

        final var dto = associationUserDaoToDtoMapper.daoToDto( dao );
        final var links = dto.getLinks();

        Assertions.assertEquals( "1", dto.getId() );
        Assertions.assertEquals( "111", dto.getUserId() );
        Assertions.assertEquals( "111111", dto.getCompanyNumber() );
        Assertions.assertEquals( "bruce.wayne@gotham.city", dto.getUserEmail() );
        Assertions.assertEquals( "Batman", dto.getDisplayName() );
        Assertions.assertEquals( "/1", links.getSelf() );
        Assertions.assertEquals( DEFAULT_KIND, dto.getKind() );
    }

    private String reduceTimestampResolution( String timestamp ){
        return timestamp.substring( 0, timestamp.indexOf( ":" ) );
    }

    private String localDateTimeToNormalisedString( LocalDateTime localDateTime ){
        final var timestamp = localDateTime.toString();
        return reduceTimestampResolution( timestamp );
    }

    @Test
    void daoToDtoWithAllFieldsSuccessfullyPerformsMapping(){
        final var now = LocalDateTime.now();
        final var threeDaysAgo = now.minusDays( 3 );
        final var lastWeek = now.minusWeeks( 1 );
        final var twoWeeksAgo = now.minusWeeks( 2 );

        final var invitationDto = new Invitation();
        invitationDto.setInvitedBy( "222" );
        invitationDto.setInvitedAt( twoWeeksAgo.toString() );

        final var invitationDao = new uk.gov.companieshouse.accounts.association.models.Invitation();
        invitationDao.setInvitedBy( "222" );
        invitationDao.setInvitedAt( twoWeeksAgo );

        final var dao = new uk.gov.companieshouse.accounts.association.models.Association();
        dao.setId( "1" );
        dao.setUserId( "111" );
        dao.setUserEmail( "bruce.wayne@gotham.city" );
        dao.setCompanyNumber( "111111" );
        dao.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE );
        dao.setStatus( StatusEnum.REMOVED );
        dao.setApprovedAt( lastWeek );
        dao.setRemovedAt( now );
        dao.setApprovalExpiryAt( threeDaysAgo );
        dao.setInvitations( List.of( invitationDao ) );
        dao.setEtag( "theTag" );

        final var user = new User().email( "bruce.wayne@gotham.city" );
        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any() );

        final var dto = associationUserDaoToDtoMapper.daoToDto( dao );
        final var links = dto.getLinks();

        Assertions.assertEquals( "1", dto.getId() );
        Assertions.assertEquals( "111", dto.getUserId() );
        Assertions.assertEquals( "bruce.wayne@gotham.city", dto.getUserEmail() );
        Assertions.assertEquals( "111111", dto.getCompanyNumber() );
        Assertions.assertEquals( ApprovalRouteEnum.AUTH_CODE, dto.getApprovalRoute() );
        Assertions.assertEquals( StatusEnum.REMOVED, dto.getStatus() );
        Assertions.assertEquals( localDateTimeToNormalisedString( lastWeek ), localDateTimeToNormalisedString( dto.getApprovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( localDateTimeToNormalisedString( now ), localDateTimeToNormalisedString( dto.getRemovedAt().toLocalDateTime() ) );
        Assertions.assertEquals( reduceTimestampResolution( threeDaysAgo.toString() ), reduceTimestampResolution( dto.getApprovalExpiryAt() ) );
        Assertions.assertEquals(List.of( invitationDto ), dto.getInvitations());
        Assertions.assertEquals( "theTag", dto.getEtag() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, dto.getDisplayName() );
        Assertions.assertEquals( "/1", links.getSelf() );
        Assertions.assertEquals( DEFAULT_KIND, dto.getKind() );
    }


}
