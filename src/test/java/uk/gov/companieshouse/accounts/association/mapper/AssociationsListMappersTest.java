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
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AssociationsListMappersTest {

    @Mock
    private UsersService usersService;

    @Mock
    private CompanyService companyService;

    private AssociationsListMappers associationsListMappers;

    private Mockers mockers;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final ComparisonUtils comparisonUtils = new ComparisonUtils();

    @BeforeEach
    void setup() {
        associationsListMappers = new AssociationsListMappersImpl();
        associationsListMappers.usersService = usersService;
        associationsListMappers.companyService = companyService;
        mockers = new Mockers( null, null, companyService, usersService );
    }

    @Test
    void daoToDtoWithNullAssociationsListThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> associationsListMappers.daoToDto( (Page<AssociationDao>) null, null, null ) );
    }

    @Test
    void daoToDtoWithNullUserAndNullCompanyPerformsMapping(){
        final var userDtos = testDataManager.fetchUserDtos( "111", "9999" );
        final var companyDtos = testDataManager.fetchCompanyDetailsDtos( "111111", "333333" );
        final var firstExpectedAssociation = testDataManager.fetchAssociationDto( "1", userDtos.getFirst() );
        final var secondExpectedAssociation = testDataManager.fetchAssociationDto( "18", userDtos.getLast() );
        final var content = testDataManager.fetchAssociationDaos( "1", "18" );
        final var page = new PageImpl<>( content, PageRequest.of( 0, 5 ), content.size() );

        Mockito.doReturn( Map.of( "111", userDtos.getFirst(), "9999", userDtos.getLast() ) ).when( usersService ).fetchUserDetails( any( Stream.class ) );
        Mockito.doReturn( Map.of( "111111", companyDtos.getFirst(), "333333", companyDtos.getLast() ) ).when( companyService ).fetchCompanyProfiles( any( Stream.class ) );

        final var associationsList = associationsListMappers.daoToDto( page, null, null );
        final var associations = associationsList.getItems();
        final var links = associationsList.getLinks();

        comparisonUtils.compare( firstExpectedAssociation, List.of( "createdAt", "approvedAt", "removedAt", "etag", "id", "userId", "userEmail", "displayName", "companyNumber", "companyName", "status", "companyStatus", "kind", "approvalRoute", "approvalExpiryAt", "links" ), List.of(), Map.of() ).matches( associations.getFirst() );
        comparisonUtils.compare( secondExpectedAssociation, List.of( "createdAt", "approvedAt", "removedAt", "etag", "id", "userId", "userEmail", "displayName", "companyNumber", "companyName", "status", "companyStatus", "kind", "approvalRoute", "approvalExpiryAt", "links" ), List.of(), Map.of() ).matches( associations.getLast() );

        Assertions.assertEquals( 2, associations.size() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 5, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 1, associationsList.getTotalPages() );
        Assertions.assertEquals( 2, associationsList.getTotalResults() );
        Assertions.assertEquals( "/associations?page_index=0&items_per_page=5", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
    }

    @Test
    void daoToDtoWithUserAndCompanyPerformsMapping(){
        final var userDto = testDataManager.fetchUserDtos( "111" ).getFirst();
        final var companyDto = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var expectedAssociation = testDataManager.fetchAssociationDto( "1", userDto );
        final var content = testDataManager.fetchAssociationDaos( "1");
        final var page = new PageImpl<>( content, PageRequest.of( 0, 1 ), 2 );

        final var associationsList = associationsListMappers.daoToDto( page, userDto, companyDto );
        final var associations = associationsList.getItems();
        final var links = associationsList.getLinks();

        comparisonUtils.compare( expectedAssociation, List.of( "createdAt", "approvedAt", "removedAt", "etag", "id", "userId", "userEmail", "displayName", "companyNumber", "companyName", "status", "companyStatus", "kind", "approvalRoute", "approvalExpiryAt", "links" ), List.of(), Map.of() ).matches( associations.getFirst() );

        Assertions.assertEquals( 1, associations.size() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 1, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 2, associationsList.getTotalPages() );
        Assertions.assertEquals( 2, associationsList.getTotalResults() );
        Assertions.assertEquals( "/associations/companies/111111?page_index=0&items_per_page=1", links.getSelf() );
        Assertions.assertEquals( "/associations/companies/111111?page_index=1&items_per_page=1", links.getNext() );
    }

}
