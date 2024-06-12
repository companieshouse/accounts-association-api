package uk.gov.companieshouse.accounts.association.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.MapperUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
 class AssociationMapperTest {


    @Mock
    private CompanyService companyService;
    @Mock
    private UsersService usersService;



    @InjectMocks
    private AssociationMapper associationUserDaoToDtoMapper ;

    private static final String DEFAULT_DISPLAY_NAME = "Not provided";
    private static final String DEFAULT_KIND = "association";

    @BeforeEach
    public void setup(){
        associationUserDaoToDtoMapper = new AssociationMapper(new MapperUtil(usersService,companyService),new BaseMapperImpl());
    }

    @Test
    void enrichAssociationWithUserDetailsWithNullInputThrowsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> associationUserDaoToDtoMapper.enrichAssociation(null));
    }

    @Test
    void enrichAssociationWithUserDetailsWithoutDisplayNameSetsDefaultDisplayName() {
        final var association = new Association().userId("111");
        final var company = new CompanyDetails();
        company.setCompanyNumber("111111");
        company.setCompanyName("Hogwarts");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile(any());

        final var user = new User().email("jason.manford@comedy.com");
        Mockito.doReturn(user).when(usersService).fetchUserDetails(any());
        associationUserDaoToDtoMapper.enrichAssociation(association);

        Assertions.assertEquals("jason.manford@comedy.com", association.getUserEmail());
        Assertions.assertEquals(DEFAULT_DISPLAY_NAME, association.getDisplayName());
    }

    @Test
    void enrichAssociationWithUserDetailsSetsDisplayName() {
        final var association = new Association().userId("111");

        final var user = new User().email("anne@the.chase.com").displayName("The Governess");
        Mockito.doReturn(user).when(usersService).fetchUserDetails(any());
        final var company = new CompanyDetails();
        company.setCompanyNumber("111111");
        company.setCompanyName("Hogwarts");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile(any());

        associationUserDaoToDtoMapper.enrichAssociation(association);

        Assertions.assertEquals("anne@the.chase.com", association.getUserEmail());
        Assertions.assertEquals("The Governess", association.getDisplayName());
    }

    @Test
    void daoToDtoWithNullInputReturnsNull() {
        Assertions.assertNull(associationUserDaoToDtoMapper.daoToDto(null));
    }

    @Test
    void daoToDtoWithOnlyMandatoryFieldsSuccessfullyPerformsMapping() {
        final var dao = new AssociationDao();
        dao.setId("1");
        dao.setUserId("111");
        dao.setCompanyNumber("111111");
        dao.setStatus(StatusEnum.CONFIRMED.getValue());
        dao.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        final var company = new CompanyDetails();
        company.setCompanyNumber("111111");
        company.setCompanyName("Hogwarts");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile(any());

        final var user =
                new User().email("bruce.wayne@gotham.city")
                        .displayName("Batman");
        Mockito.doReturn(user).when(usersService).fetchUserDetails(any());


        final var dto = associationUserDaoToDtoMapper.daoToDto(dao);
        final var links = dto.getLinks();

        Assertions.assertEquals("1", dto.getId());
        Assertions.assertEquals("111", dto.getUserId());
        Assertions.assertEquals("111111", dto.getCompanyNumber());
        Assertions.assertEquals("bruce.wayne@gotham.city", dto.getUserEmail());
        Assertions.assertEquals("Batman", dto.getDisplayName());
        Assertions.assertEquals("/associations/1", links.getSelf());
        Assertions.assertEquals(DEFAULT_KIND, dto.getKind());
    }


    private String reduceTimestampResolution( String timestamp ){
        return timestamp.substring( 0, timestamp.indexOf( ":" ) );
    }

    private String localDateTimeToNormalisedString( LocalDateTime localDateTime ){
        final var timestamp = localDateTime.toString();
        return reduceTimestampResolution( timestamp );
    }

    @Test
    void daoToDtoWithAllFieldsSuccessfullyPerformsMapping() {
        final var now = LocalDateTime.now();
        final var twoWeeksAgo = now.minusWeeks(2);

        final var invitationDto = new Invitation();
        invitationDto.setInvitedBy("bruce.wayne@gotham.city");
        invitationDto.setInvitedAt(twoWeeksAgo.toString());

        final var invitationDao = new InvitationDao();
        invitationDao.setInvitedBy("222");
        invitationDao.setInvitedAt(twoWeeksAgo);

        final var dao = new AssociationDao();
        dao.setId("1");
        dao.setUserId("111");
        dao.setUserEmail("bruce.wayne@gotham.city");
        dao.setCompanyNumber("111111");
        dao.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        dao.setStatus(StatusEnum.REMOVED.getValue());
        dao.setRemovedAt(now);
        dao.setInvitations(List.of(invitationDao));
        dao.setEtag("theTag");

        final var company = new CompanyDetails();
        company.setCompanyName("Wayne Enterprises");
        Mockito.doReturn(company).when(companyService).fetchCompanyProfile(any());

        final var user = new User().email("bruce.wayne@gotham.city");
        Mockito.doReturn(user).when(usersService).fetchUserDetails(any());


        final var dto = associationUserDaoToDtoMapper.daoToDto(dao);
        final var links = dto.getLinks();

        Assertions.assertEquals("1", dto.getId());
        Assertions.assertEquals("111", dto.getUserId());
        Assertions.assertEquals("bruce.wayne@gotham.city", dto.getUserEmail());
        Assertions.assertEquals("111111", dto.getCompanyNumber());
        Assertions.assertEquals(ApprovalRouteEnum.AUTH_CODE, dto.getApprovalRoute());
        Assertions.assertEquals(StatusEnum.REMOVED, dto.getStatus());
        Assertions.assertEquals( localDateTimeToNormalisedString( now ), localDateTimeToNormalisedString( dto.getRemovedAt().toLocalDateTime() ) );
        Assertions.assertEquals("theTag", dto.getEtag());
        Assertions.assertEquals("Wayne Enterprises", dto.getCompanyName());
        Assertions.assertEquals(DEFAULT_DISPLAY_NAME, dto.getDisplayName());
        Assertions.assertEquals("/associations/1", links.getSelf());
        Assertions.assertEquals(DEFAULT_KIND, dto.getKind());
    }


}
