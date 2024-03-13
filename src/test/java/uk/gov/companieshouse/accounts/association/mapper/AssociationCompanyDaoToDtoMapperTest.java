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
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;
import uk.gov.companieshouse.api.model.company.CompanyProfileApi;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class AssociationCompanyDaoToDtoMapperTest {

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private AssociationCompanyDaoToDtoMapper associationCompanyDaoToDtoMapper = new AssociationCompanyDaoToDtoMapperImpl();

    private static final String DEFAULT_KIND = "association";

    @Test
    void enrichAssociationWithCompanyProfileWithNullInputThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> associationCompanyDaoToDtoMapper.enrichAssociationWithCompanyProfile( null ) );
    }

    @Test
    void enrichAssociationWithCompanyProfileSetsCompanyName(){
        final var association = new Association().companyNumber( "111111" );

        final var company = new CompanyProfileApi();
        company.setCompanyNumber( "111111" );
        company.setCompanyName( "Hogwarts" );
        Mockito.doReturn( company ).when( companyService ).fetchCompanyProfile( any() );

        associationCompanyDaoToDtoMapper.enrichAssociationWithCompanyProfile( association );

        Assertions.assertEquals( "Hogwarts", association.getCompanyName() );
    }

    @Test
    void daoToDtoWithNullInputReturnsNull(){
        Assertions.assertNull( associationCompanyDaoToDtoMapper.daoToDto( null ) );
    }

    @Test
    void daoToDtoWithOnlyMandatoryFieldsSuccessfullyPerformsMapping(){
        final var dao = new uk.gov.companieshouse.accounts.association.models.Association();
        dao.setId( "1" );
        dao.setUserId( "111" );
        dao.setCompanyNumber( "111111" );

        final var company = new CompanyProfileApi();
        company.setCompanyName( "Wayne Enterprises" );
        Mockito.doReturn( company ).when( companyService ).fetchCompanyProfile( any() );

        final var dto = associationCompanyDaoToDtoMapper.daoToDto( dao );
        final var links = dto.getLinks();

        Assertions.assertEquals( "1", dto.getId() );
        Assertions.assertEquals( "111", dto.getUserId() );
        Assertions.assertEquals( "111111", dto.getCompanyNumber() );
        Assertions.assertEquals( "Wayne Enterprises", dto.getCompanyName() );
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

        final var company = new CompanyProfileApi();
        company.setCompanyName( "Wayne Enterprises" );
        Mockito.doReturn( company ).when( companyService ).fetchCompanyProfile( any() );

        final var dto = associationCompanyDaoToDtoMapper.daoToDto( dao );
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
        Assertions.assertEquals( 1, dto.getInvitations().size() );
        Assertions.assertEquals( invitationDto.getInvitedBy(), dto.getInvitations().get(0).getInvitedBy() );
        Assertions.assertEquals( reduceTimestampResolution( invitationDto.getInvitedAt() ), reduceTimestampResolution( dto.getInvitations().get(0).getInvitedAt() ) );
        Assertions.assertEquals( "theTag", dto.getEtag() );
        Assertions.assertEquals( "Wayne Enterprises", dto.getCompanyName() );
        Assertions.assertEquals( "/1", links.getSelf() );
        Assertions.assertEquals( DEFAULT_KIND, dto.getKind() );
    }

}
