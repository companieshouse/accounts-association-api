package uk.gov.companieshouse.accounts.association.mapper;

import static org.mockito.ArgumentMatchers.any;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;

@Tag("unit-test")
@ExtendWith(MockitoExtension.class)
class AssociationsListUserMapperTest {

    @Mock
    private UsersService usersService;

    @Mock
    private CompanyService companyService;

    private AssociationsListUserMapper associationsListUserMapper;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup() {
        associationsListUserMapper = new AssociationsListUserMapperImpl();
        associationsListUserMapper.usersService = usersService;
        associationsListUserMapper.companyService = companyService;
    }

    @Test
    void daoToDtoThrowsIllegalArgumentExceptionWhenUserIsNull() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> associationsListUserMapper.daoToDto(Page.empty(), null));
    }

    @Test
    void daoToDtoThrowsNullPointerExceptionWhenPageIsNull() {
        Assertions.assertThrows(NullPointerException.class, () -> associationsListUserMapper.daoToDto(null, new User()));
    }

    @Test
    void daoToDtoDoesMappingCorrectlyForLastPage() {
        final var associationDaos = testDataManager.fetchAssociationDaos("18", "19");
        final var user = testDataManager.fetchUserDtos("9999").getFirst();
        final var companies = testDataManager.fetchCompanyDetailsDtos("333333", "444444");

        final var content = new PageImpl<>(associationDaos, PageRequest.of(0, 15), associationDaos.size());

        Mockito.doReturn(Map.of("333333", companies.getFirst(), "444444", companies.getLast())).when(companyService).fetchCompanyProfiles(any(Stream.class));

        final var associations = associationsListUserMapper.daoToDto(content, user);
        final var links = associations.getLinks();

        Assertions.assertEquals(15, associations.getItemsPerPage());
        Assertions.assertEquals(0, associations.getPageNumber());
        Assertions.assertEquals(2, associations.getTotalResults());
        Assertions.assertEquals(1, associations.getTotalPages());
        Assertions.assertEquals("/associations?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(2, associations.getItems().size());
    }

    @Test
    void daoToDtoDoesMappingCorrectlyForIntermediatePage() {
        final var associationDaos = testDataManager.fetchAssociationDaos("18", "19");
        final var user = testDataManager.fetchUserDtos("9999").getFirst();
        final var companies = testDataManager.fetchCompanyDetailsDtos("333333", "444444");

        final var content = new PageImpl<>(associationDaos, PageRequest.of(0, 2), 3);

        Mockito.doReturn(Map.of("333333", companies.getFirst(), "444444", companies.getLast())).when(companyService).fetchCompanyProfiles(any(Stream.class));

        final var associations = associationsListUserMapper.daoToDto(content, user);
        final var links = associations.getLinks();

        Assertions.assertEquals(2, associations.getItemsPerPage());
        Assertions.assertEquals(0, associations.getPageNumber());
        Assertions.assertEquals(3, associations.getTotalResults());
        Assertions.assertEquals(2, associations.getTotalPages());
        Assertions.assertEquals("/associations?page_index=0&items_per_page=2", links.getSelf());
        Assertions.assertEquals("/associations?page_index=1&items_per_page=2", links.getNext());
        Assertions.assertEquals(2, associations.getItems().size());
    }

    @Test
    void daoToDtoDoesNothingWhenPageIsEmpty() {
        final var user = testDataManager.fetchUserDtos("9999").getFirst();

        final var content = new PageImpl<AssociationDao>(List.of(), PageRequest.of(0, 2), 0);

        Mockito.doReturn(Map.of()).when(companyService).fetchCompanyProfiles(any(Stream.class));

        final var associations = associationsListUserMapper.daoToDto(content, user);
        final var links = associations.getLinks();

        Assertions.assertEquals(2, associations.getItemsPerPage());
        Assertions.assertEquals(0, associations.getPageNumber());
        Assertions.assertEquals(0, associations.getTotalResults());
        Assertions.assertEquals(0, associations.getTotalPages());
        Assertions.assertEquals("/associations?page_index=0&items_per_page=2", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertEquals(0, associations.getItems().size());
    }

}
