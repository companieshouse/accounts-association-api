package uk.gov.companieshouse.accounts.association.mapper;


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
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.company.CompanyDetails;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;

@Tag( "unit-test" )
@ExtendWith( MockitoExtension.class )
class AssociationListCompanyMapperTest {

    @Mock
    private UsersService usersService;

    @Mock
    private CompanyService companyService;

    private AssociationsListCompanyMapper associationsListCompanyMapper;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup(){
        associationsListCompanyMapper = new AssociationsListCompanyMapperImpl();
        associationsListCompanyMapper.usersService = usersService;
        associationsListCompanyMapper.companyService = companyService;
    }

    @Test
    void daoToDtoThrowsIllegalArgumentExceptionWhenCompanyIsNull(){
        Assertions.assertThrows( IllegalArgumentException.class, () -> associationsListCompanyMapper.daoToDto( Page.empty(), null ) );
    }

    @Test
    void daoToDtoThrowsNullPointerExceptionWhenPageIsNull(){
        Assertions.assertThrows( NullPointerException.class, () -> associationsListCompanyMapper.daoToDto( null, new CompanyDetails() ) );
    }

    @Test
    void daoToDtoDoesMappingCorrectlyForLastPage(){
        final var associationDaos = testDataManager.fetchAssociationDaos( "1", "2" );
        final var users = testDataManager.fetchUserDtos( "111", "222" );
        final var company = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();

        final var content = new PageImpl<>( associationDaos, PageRequest.of( 0, 15 ), associationDaos.size() );

        Mockito.doReturn( Map.of( "111", users.getFirst(), "222", users.getLast() ) ).when( usersService ).fetchUserDetails( any( Stream.class ) );

        final var associations = associationsListCompanyMapper.daoToDto( content, company );
        final var links = associations.getLinks();

        Assertions.assertEquals( 15, associations.getItemsPerPage() );
        Assertions.assertEquals( 0, associations.getPageNumber() );
        Assertions.assertEquals( 2, associations.getTotalResults() );
        Assertions.assertEquals( 1, associations.getTotalPages() );
        Assertions.assertEquals( "/associations/companies/111111?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 2, associations.getItems().size() );
    }

    @Test
    void daoToDtoDoesMappingCorrectlyForIntermediatePage(){
        final var associationDaos = testDataManager.fetchAssociationDaos( "1", "2" );
        final var users = testDataManager.fetchUserDtos( "111", "222" );
        final var company = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();

        final var content = new PageImpl<>( associationDaos, PageRequest.of( 0, 2 ), 3 );

        Mockito.doReturn( Map.of( "111", users.getFirst(), "222", users.getLast() ) ).when( usersService ).fetchUserDetails( any( Stream.class ) );

        final var associations = associationsListCompanyMapper.daoToDto( content, company );
        final var links = associations.getLinks();

        Assertions.assertEquals( 2, associations.getItemsPerPage() );
        Assertions.assertEquals( 0, associations.getPageNumber() );
        Assertions.assertEquals( 3, associations.getTotalResults() );
        Assertions.assertEquals( 2, associations.getTotalPages() );
        Assertions.assertEquals( "/associations/companies/111111?page_index=0&items_per_page=2", links.getSelf() );
        Assertions.assertEquals( "/associations/companies/111111?page_index=1&items_per_page=2", links.getNext() );
        Assertions.assertEquals( 2, associations.getItems().size() );
    }

    @Test
    void daoToDtoDoesNothingWhenPageIsEmpty(){
        final var users = testDataManager.fetchUserDtos( "111", "222" );
        final var company = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();

        final var content = new PageImpl<Association>( List.of(), PageRequest.of( 0, 2 ), 0 );

        Mockito.doReturn( Map.of( "111", users.getFirst(), "222", users.getLast() ) ).when( usersService ).fetchUserDetails( any( Stream.class ) );

        final var associations = associationsListCompanyMapper.daoToDto( content, company );
        final var links = associations.getLinks();

        Assertions.assertEquals( 2, associations.getItemsPerPage() );
        Assertions.assertEquals( 0, associations.getPageNumber() );
        Assertions.assertEquals( 0, associations.getTotalResults() );
        Assertions.assertEquals( 0, associations.getTotalPages() );
        Assertions.assertEquals( "/associations/companies/111111?page_index=0&items_per_page=2", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associations.getItems().size() );
    }
}
