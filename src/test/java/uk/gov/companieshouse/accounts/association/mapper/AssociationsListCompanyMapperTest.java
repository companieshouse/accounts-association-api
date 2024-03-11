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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class AssociationsListCompanyMapperTest {

    @Mock
    private UsersService usersService;

    @Mock
    private CompanyService companyService;


    @InjectMocks
    AssociationsListCompanyMapper associationsListCompanyMapper;

    private AssociationDao associationBatmanDao;

    private AssociationDao associationAlfieDao;


    @BeforeEach
    void setup() {

        BaseMapper baseMapper = new BaseMapperImpl();

        MapperUtil mapperUtil = new MapperUtil(usersService, companyService);

        associationsListCompanyMapper = new AssociationsListCompanyMapper(baseMapper, mapperUtil);


        associationBatmanDao = new AssociationDao();
        associationBatmanDao.setUserId("111");
        associationBatmanDao.setCompanyNumber("111111");
        associationBatmanDao.setStatus("confirmed");

        associationAlfieDao = new AssociationDao();
        associationAlfieDao.setUserEmail("batman@gotham.city");
        associationAlfieDao.setCompanyNumber("111111");
        associationAlfieDao.setStatus("awaiting-approval");

    }

    @Test
    void enrichWithItemsWithNullOrEmptyCompanyDetailsThrowsNullPointerException() {
        final var content = new ArrayList<AssociationDao>();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        Assertions.assertThrows(NullPointerException.class, () -> associationsListCompanyMapper.daoToDto(page, null));
        Assertions.assertThrows(NullPointerException.class, () -> associationsListCompanyMapper.daoToDto(page, new CompanyDetails()));
    }

    @Test
    void daoToDtoReturnsAssociationsList() {
        final var content = new ArrayList<>(List.of(associationBatmanDao, associationAlfieDao));
        final var pageRequest = PageRequest.of(0, 2);
        final var page = new PageImpl<>(content, pageRequest, 3);

        final var company = new CompanyDetails("active", "Wayne Enterprises", "111111");

        final var user = new User().email("bruce.wayne@gotham.city").userId("111");

        Mockito.doReturn(user).when(usersService).fetchUserDetails("111");


        final var associationsList = associationsListCompanyMapper.daoToDto(page, company);
        final var items = associationsList.getItems();
        final var links = associationsList.getLinks();

        Assertions.assertEquals(2, items.size());
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getUserId).toList().containsAll(List.of("111")));
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getUserEmail).toList().containsAll(List.of("bruce.wayne@gotham.city", "batman@gotham.city")));
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getCompanyName).allMatch(companyName -> companyName.equals("Wayne Enterprises")));
        Assertions.assertEquals("null/associations", links.getSelf());
        Assertions.assertEquals(String.format("null/associations/companies/111111?page_index=%d&items_per_page=%d", 1, 2), links.getNext());
        Assertions.assertEquals(0, associationsList.getPageNumber());
        Assertions.assertEquals(2, associationsList.getItemsPerPage());
        Assertions.assertEquals(3, associationsList.getTotalResults());
        Assertions.assertEquals(2, associationsList.getTotalPages());
    }

}
