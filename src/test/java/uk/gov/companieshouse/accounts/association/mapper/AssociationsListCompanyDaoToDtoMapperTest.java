package uk.gov.companieshouse.accounts.association.mapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationLinks;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.model.company.CompanyProfileApi;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class AssociationsListCompanyDaoToDtoMapperTest {

    @Value("${internal.api.url}")
    private String internalApiUrl;

    @Mock
    CompanyService companyService;

    @Mock
    AssociationUserDaoToDtoMapper associationUserDaoToDtoMapper;

    @InjectMocks
    AssociationsListCompanyDaoToDtoMapper associationsListCompanyDaoToDtoMapper = new AssociationsListCompanyDaoToDtoMapperImpl();

    private Association associationBatmanDao;

    private Association associationAlfieDao;

    private uk.gov.companieshouse.api.accounts.associations.model.Association associationBatmanDto;

    private uk.gov.companieshouse.api.accounts.associations.model.Association associationAlfieDto;

    private static final String DEFAULT_KIND = "association";

    @BeforeEach
    void setup(){
        associationBatmanDao = new Association();
        associationBatmanDao.setUserId( "111" );
        associationBatmanDao.setCompanyNumber( "111111" );

        associationAlfieDao = new Association();
        associationAlfieDao.setUserId( "222" );
        associationAlfieDao.setCompanyNumber( "222222" );

        associationBatmanDto = new uk.gov.companieshouse.api.accounts.associations.model.Association()
                .userId( "111" )
                .companyNumber( "111111" )
                .links( new AssociationLinks().self( "/null" ) )
                .kind( DEFAULT_KIND )
                .companyName( "Wayne Enterprises" )
                .userEmail( "bruce.wayne@gotham.city" )
                .displayName( "Batman" );

        associationAlfieDto = new uk.gov.companieshouse.api.accounts.associations.model.Association()
                .userId( "222" )
                .companyNumber( "222222" )
                .links( new AssociationLinks().self( "/null" ) )
                .kind( DEFAULT_KIND )
                .companyName( "Queen Victoria Pub" )
                .userEmail( "alfie.moon@walford.square" )
                .displayName( "Alfie" );
    }

    @Test
    void enrichWithItemsWithNullOrEmptyContextThrowsNullPointerException(){
        final var content = new ArrayList<Association>();
        final var pageRequest = PageRequest.of( 0, 15 );
        final var page = new PageImpl<>( content, pageRequest, content.size() );

        Assertions.assertThrows( NullPointerException.class, () -> associationsListCompanyDaoToDtoMapper.enrichWithItems( page, new AssociationsList(), null ) );
    }

    @Test
    void enrichWithItemsWithNoResultsReturnsAssociationsList(){
        final var content = new ArrayList<Association>();
        final var pageRequest = PageRequest.of( 0, 15 );
        final var page = new PageImpl<>( content, pageRequest, content.size() );

        final var associationsList = new AssociationsList();
        associationsListCompanyDaoToDtoMapper.enrichWithItems( page, associationsList, Map.of( "companyNumber", "111111" ) );

        Assertions.assertEquals( List.of(), associationsList.getItems() );
    }

    private ArgumentMatcher<Association> associationDaoMatches( final String userId ){
        return association -> association.getUserId().equals( userId );
    }

    @Test
    void enrichWithItemsReturnsAssociationsList(){
        final var content = new ArrayList<>( List.of(associationBatmanDao, associationAlfieDao) );
        final var pageRequest = PageRequest.of( 0, 2 );
        final var page = new PageImpl<>( content, pageRequest, 3 );

        final var company = new CompanyProfileApi();
        company.setCompanyName( "Wayne Enterprises" );
        Mockito.doReturn(company).when( companyService ).fetchCompanyProfile( any() );

        Mockito.doReturn( associationBatmanDto).when( associationUserDaoToDtoMapper ).daoToDto( argThat( associationDaoMatches("111") ));
        Mockito.doReturn( associationAlfieDto).when( associationUserDaoToDtoMapper ).daoToDto( argThat( associationDaoMatches("222") ));

        final var associationsList = new AssociationsList();
        associationsListCompanyDaoToDtoMapper.enrichWithItems( page, associationsList, Map.of( "companyNumber", "111111" ) );
        final var items = associationsList.getItems();

        Assertions.assertEquals( 2, items.size() );
        Assertions.assertTrue( items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getUserId).toList().containsAll( List.of("111","222") ) );
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getCompanyName).allMatch( companyName -> companyName.equals( "Wayne Enterprises") ) );
    }

    @Test
    void daoToDtoWithNullPageReturnsNull() {
        Assertions.assertNull( associationsListCompanyDaoToDtoMapper.daoToDto(null, Map.of("companyNumber", "111111", "endpointUri", "/associations/companies/111111" )));
    }

    @Test
    void daoToDtoWithNullContextThrowsNullPointerException() {
        final var content = new ArrayList<Association>();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        Assertions.assertThrows(NullPointerException.class, () -> associationsListCompanyDaoToDtoMapper.daoToDto(page, null));
    }

    @Test
    void daoToDtoWithEmptyPageReturnsAssociationsList(){
        final var content = new ArrayList<Association>();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        final var associationsList = associationsListCompanyDaoToDtoMapper.daoToDto( page, Map.of("companyNumber", "111111", "endpointUri", "/associations/companies/111111" ) );
        final var links = associationsList.getLinks();

        Assertions.assertEquals( List.of(), associationsList.getItems() );
        Assertions.assertEquals( String.format("%s/associations", internalApiUrl), links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 15, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 0, associationsList.getTotalResults() );
        Assertions.assertEquals( 0, associationsList.getTotalPages() );
    }

    @Test
    void daoToDtoReturnsAssociationsList(){
        final var content = new ArrayList<>( List.of(associationBatmanDao, associationAlfieDao) );
        final var pageRequest = PageRequest.of( 0, 2 );
        final var page = new PageImpl<>( content, pageRequest, 3 );

        final var company = new CompanyProfileApi();
        company.setCompanyName( "Wayne Enterprises" );
        Mockito.doReturn(company).when( companyService ).fetchCompanyProfile( any() );

        Mockito.doReturn( associationBatmanDto).when( associationUserDaoToDtoMapper ).daoToDto( argThat( associationDaoMatches("111") ));
        Mockito.doReturn( associationAlfieDto).when( associationUserDaoToDtoMapper ).daoToDto( argThat( associationDaoMatches("222") ));

        final var associationsList = associationsListCompanyDaoToDtoMapper.daoToDto( page, Map.of("companyNumber", "111111", "endpointUri", "/associations/companies/111111" ) );
        final var items = associationsList.getItems();
        final var links = associationsList.getLinks();

        Assertions.assertEquals( 2, items.size() );
        Assertions.assertTrue( items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getUserId).toList().containsAll( List.of("111","222") ) );
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getCompanyName).allMatch( companyName -> companyName.equals( "Wayne Enterprises") ) );
        Assertions.assertEquals( String.format("%s/associations", internalApiUrl), links.getSelf() );
        Assertions.assertEquals( String.format("%s/associations/companies/111111?page_index=%d&items_per_page=%d", internalApiUrl, 1, 2), links.getNext() );
        Assertions.assertEquals( 0, associationsList.getPageNumber() );
        Assertions.assertEquals( 2, associationsList.getItemsPerPage() );
        Assertions.assertEquals( 3, associationsList.getTotalResults() );
        Assertions.assertEquals( 2, associationsList.getTotalPages() );
    }

}
