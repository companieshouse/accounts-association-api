package uk.gov.companieshouse.accounts.association.mapper;

import java.util.ArrayList;
import java.util.Arrays;
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
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.MapperUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;

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

        associationsListUserMapper = new AssociationsListUserMapper(baseMapper, mapperUtil);


        associationBatmanDao = new AssociationDao();
        associationBatmanDao.setUserId("111");
        associationBatmanDao.setCompanyNumber("111111");
        associationBatmanDao.setStatus(StatusEnum.CONFIRMED.getValue());
        associationBatmanDao.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());

        associationAlfieDao = new AssociationDao();
        associationAlfieDao.setUserEmail("111");
        associationAlfieDao.setCompanyNumber("222222");
        associationAlfieDao.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationAlfieDao.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());

    }

    @Test
    void daoToDtoWithAllFieldsSuccessfullyPerformsMapping() {

        final var content = new ArrayList<>(List.of(associationBatmanDao, associationAlfieDao));
        final var pageRequest = PageRequest.of(0, 2);
        final var page = new PageImpl<>(content, pageRequest, 3);
        CompanyDetails companyDetails = new CompanyDetails();
        companyDetails.setCompanyName("Bruce Enterprise");
        companyDetails.setCompanyNumber("111111");

        CompanyDetails companyDetails2 = new CompanyDetails();
        companyDetails2.setCompanyName("Alfi Company");
        companyDetails2.setCompanyNumber("222222");

        final var user = new User().email("bruce.wayne@gotham.city").userId("111");
        Mockito.doReturn(companyDetails).when(companyService).fetchCompanyProfile("111111");
        Mockito.doReturn(companyDetails2).when(companyService).fetchCompanyProfile("222222");

        final var dto = associationsListUserMapper.daoToDto(page, user);
        final var links = dto.getLinks();
        final var items = dto.getItems();

        Assertions.assertEquals(2, items.size());
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getUserId).toList().contains("111"));
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getUserEmail).toList().contains("bruce.wayne@gotham.city"));
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getCompanyName).toList().containsAll(Arrays.asList("Alfi Company", "Bruce Enterprise")));
        Assertions.assertEquals("/associations?page_index=0&items_per_page=2", links.getSelf());
        Assertions.assertEquals(String.format("/associations?page_index=%d&items_per_page=%d", 1, 2), links.getNext());
        Assertions.assertEquals(0, dto.getPageNumber());
        Assertions.assertEquals(2, dto.getItemsPerPage());
        Assertions.assertEquals(3, dto.getTotalResults());
        Assertions.assertEquals(2, dto.getTotalPages());
    }


}
