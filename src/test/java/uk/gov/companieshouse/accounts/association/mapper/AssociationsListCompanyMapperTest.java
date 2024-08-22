package uk.gov.companieshouse.accounts.association.mapper;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.MapperUtil;
import uk.gov.companieshouse.api.company.CompanyDetails;

import java.util.ArrayList;
import java.util.List;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AssociationsListCompanyMapperTest {

    @Mock
    private UsersService usersService;

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private AssociationsListCompanyMapper associationsListCompanyMapper;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private Mockers mockers;

    @BeforeEach
    void setup() {
        associationsListCompanyMapper = new AssociationsListCompanyMapper(new BaseMapperImpl(), new MapperUtil(usersService, companyService));
        mockers = new Mockers( null, null, null, companyService, usersService );
    }

    @Test
    void enrichWithItemsWithNullOrEmptyCompanyDetailsThrowsNullPointerException() {
        final var content = new ArrayList<AssociationDao>();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, 0);
        Assertions.assertThrows(NullPointerException.class, () -> associationsListCompanyMapper.daoToDto(page, null));
        Assertions.assertThrows(NullPointerException.class, () -> associationsListCompanyMapper.daoToDto(page, new CompanyDetails()));
    }

    @Test
    void daoToDtoReturnsAssociationsList() {
        final var company = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "1", "2" );
        final var pageRequest = PageRequest.of(0, 2);
        final var page = new PageImpl<>(content, pageRequest, 3);

        mockers.mockUsersServiceFetchUserDetails( "111", "222" );

        final var associationsList = associationsListCompanyMapper.daoToDto(page, company);
        final var items = associationsList.getItems();
        final var links = associationsList.getLinks();

        Assertions.assertEquals(2, items.size());
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getUserId).toList().containsAll(List.of("111","222")));
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getUserEmail).toList().containsAll(List.of("bruce.wayne@gotham.city", "the.joker@gotham.city")));
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getCompanyName).allMatch(companyName -> companyName.equals("Wayne Enterprises")));
        Assertions.assertEquals("/associations/companies/111111?page_index=0&items_per_page=2", links.getSelf());
        Assertions.assertEquals(String.format("/associations/companies/111111?page_index=%d&items_per_page=%d", 1, 2), links.getNext());
        Assertions.assertEquals(0, associationsList.getPageNumber());
        Assertions.assertEquals(2, associationsList.getItemsPerPage());
        Assertions.assertEquals(3, associationsList.getTotalResults());
        Assertions.assertEquals(2, associationsList.getTotalPages());
    }

}
