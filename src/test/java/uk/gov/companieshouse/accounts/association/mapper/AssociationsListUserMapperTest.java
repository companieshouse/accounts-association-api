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
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.MapperUtil;

import java.util.Arrays;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AssociationsListUserMapperTest {

    @Mock
    private UsersService usersService;

    @Mock
    private CompanyService companyService;

    @InjectMocks
    private AssociationsListUserMapper associationsListUserMapper;

    private Mockers mockers;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup() {
        associationsListUserMapper = new AssociationsListUserMapper(new BaseMapperImpl(), new MapperUtil(usersService, companyService));
        mockers = new Mockers( null, null, null, companyService, usersService );
    }

    @Test
    void daoToDtoWithAllFieldsSuccessfullyPerformsMapping() {
        final var user = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "1", "18" );
        final var pageRequest = PageRequest.of(0, 2);
        final var page = new PageImpl<>(content, pageRequest, 3);

        mockers.mockCompanyServiceFetchCompanyProfile( "111111", "333333" );

        final var dto = associationsListUserMapper.daoToDto(page, user);
        final var links = dto.getLinks();
        final var items = dto.getItems();

        Assertions.assertEquals(2, items.size());
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getUserId).toList().contains("111"));
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getUserEmail).toList().contains("bruce.wayne@gotham.city"));
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getCompanyName).toList().containsAll(Arrays.asList("Wayne Enterprises", "Tesco")));
        Assertions.assertEquals("/associations?page_index=0&items_per_page=2", links.getSelf());
        Assertions.assertEquals(String.format("/associations?page_index=%d&items_per_page=%d", 1, 2), links.getNext());
        Assertions.assertEquals(0, dto.getPageNumber());
        Assertions.assertEquals(2, dto.getItemsPerPage());
        Assertions.assertEquals(3, dto.getTotalResults());
        Assertions.assertEquals(2, dto.getTotalPages());
    }

}
