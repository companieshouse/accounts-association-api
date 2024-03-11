package uk.gov.companieshouse.accounts.association.mapper;

import static org.mockito.ArgumentMatchers.any;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.model.company.CompanyProfileApi;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class AssociationsListUserMapperTest {

    @Mock
    private UsersService usersService;

    @Mock
    private CompanyService companyService;


    @InjectMocks
    AssociationsListUserMapper associationsListUserMapper;

    private AssociationDao associationBatmanDao;

    private AssociationDao associationAlfieDao;


    @BeforeEach
    void setup() {

        BaseMapper baseMapper = new BaseMapperImpl();

        MapperUtil mapperUtil = new MapperUtil(usersService, companyService);

        associationsListUserMapper = new AssociationsListUserMapper(baseMapper,mapperUtil);


        associationBatmanDao = new AssociationDao();
        associationBatmanDao.setUserId("111");
        associationBatmanDao.setCompanyNumber("111111");
        associationBatmanDao.setStatus("confirmed");

        associationAlfieDao = new AssociationDao();
        associationAlfieDao.setUserEmail("batman@gotham.city");
        associationAlfieDao.setCompanyNumber("111111");
        associationAlfieDao.setStatus("awaiting-approval");

    }


//    @Test
//    void enrichAssociationWithUserDetailsWithoutDisplayNameSetsDefaultDisplayName(){
//        final var association = new Association().userId( "111" );
//
//        final var user = new User().email( "jason.manford@comedy.com" );
//        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any() );
//        //associationsListUserMapper.daoToDto(PageRequest.);
//
//        Assertions.assertEquals( "jason.manford@comedy.com", association.getUserEmail() );
//        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, association.getDisplayName() );
//    }
//
//    @Test
//    void enrichAssociationWithUserDetailsSetsDisplayName(){
//        final var association = new Association().userId( "111" );
//
//        final var user = new User().email( "anne@the.chase.com" ).displayName( "The Governess" );
//        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any() );
//        associationsListUserMapper.enrichAssociationWithUserDetails( association );
//
//        Assertions.assertEquals( "anne@the.chase.com", association.getUserEmail() );
//        Assertions.assertEquals( "The Governess", association.getDisplayName() );
//    }
//
//    @Test
//    void daoToDtoWithNullInputReturnsNull(){
//        Assertions.assertNull( associationsListUserMapper.daoToDto( null ) );
//    }
//
//    @Test
//    void daoToDtoWithOnlyMandatoryFieldsSuccessfullyPerformsMapping(){
//        final var dao = new AssociationDao();
//        dao.setId( "1" );
//        dao.setUserId( "111" );
//        dao.setCompanyNumber( "111111" );
//
//        final var user =
//        new User().email( "bruce.wayne@gotham.city" )
//                  .displayName( "Batman" );
//        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any() );
//
//        final var dto = associationsListUserMapper.daoToDto( dao );
//        final var links = dto.getLinks();
//
//        Assertions.assertEquals( "1", dto.getId() );
//        Assertions.assertEquals( "111", dto.getUserId() );
//        Assertions.assertEquals( "111111", dto.getCompanyNumber() );
//        Assertions.assertEquals( "bruce.wayne@gotham.city", dto.getUserEmail() );
//        Assertions.assertEquals( "Batman", dto.getDisplayName() );
//        Assertions.assertEquals( "/1", links.getSelf() );
//        Assertions.assertEquals( DEFAULT_KIND, dto.getKind() );
//    }
//
    @Test
    void daoToDtoWithAllFieldsSuccessfullyPerformsMapping(){

        final var content = new ArrayList<>(List.of(associationBatmanDao, associationAlfieDao));
        final var pageRequest = PageRequest.of(0, 2);
        final var page = new PageImpl<>(content, pageRequest, 2);
//        CompanyDetails companyDetails = new CompanyDetails();
//        companyDetails.setCompanyName("Bruce Enterprise");
//        companyDetails.setCompanyNumber("111111");

        CompanyProfileApi companyProfileApi = new CompanyProfileApi();

        companyProfileApi.setCompanyName("Bruce Enterprise");
        companyProfileApi.setCompanyNumber("111111");

        final var user = new User().email( "bruce.wayne@gotham.city" ).userId("111");
        Mockito.doReturn( companyProfileApi ).when( companyService ).fetchCompanyProfile( "111111" );

        final var dto = associationsListUserMapper.daoToDto( page,user );
        final var links = dto.getLinks();
        final var items = dto.getItems();

        Assertions.assertEquals(2, items.size());
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getUserId).toList().contains("111"));
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getUserEmail).toList().contains("bruce.wayne@gotham.city"));
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getCompanyName).allMatch(companyName -> companyName.equals("Bruce Enterprise")));
        Assertions.assertEquals("null/associations", links.getSelf());
     //   Assertions.assertEquals(String.format("null/associations?page_index=%d&items_per_page=%d", 1, 2), links.getNext());
        Assertions.assertEquals(0, dto.getPageNumber());
        Assertions.assertEquals(2, dto.getItemsPerPage());
        Assertions.assertEquals(2, dto.getTotalResults());
        Assertions.assertEquals(1, dto.getTotalPages());
    }


}
