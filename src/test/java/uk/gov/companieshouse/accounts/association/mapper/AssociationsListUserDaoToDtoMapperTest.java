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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationLinks;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.model.company.CompanyProfileApi;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class AssociationsListUserDaoToDtoMapperTest {

    @Mock
    private UsersService usersService;

    @Mock
    private AssociationCompanyDaoToDtoMapper associationCompanyDaoToDtoMapper;

    @InjectMocks
    AssociationsListUserDaoToDtoMapper associationsListUserDaoToDtoMapper = new AssociationsListUserDaoToDtoMapperImpl();

    private Association associationBatmanDao;

    private Association associationAlfieDao;

    private uk.gov.companieshouse.api.accounts.associations.model.Association associationBatmanDto;

    private uk.gov.companieshouse.api.accounts.associations.model.Association associationAlfieDto;

    private static final String DEFAULT_KIND = "association";

    private static final String DEFAULT_DISPLAY_NAME = "Not provided";

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

        Assertions.assertThrows( NullPointerException.class, () -> associationsListUserDaoToDtoMapper.enrichWithItems( page, new AssociationsList(), null ) );
        Assertions.assertThrows( NullPointerException.class, () -> associationsListUserDaoToDtoMapper.enrichWithItems( page, new AssociationsList(), Map.of() ) );
    }

    @Test
    void enrichWithItemsWithNoResultsReturnsAssociationsList(){
        final var content = new ArrayList<Association>();
        final var pageRequest = PageRequest.of( 0, 15 );
        final var page = new PageImpl<>( content, pageRequest, content.size() );

        final var user = new User().email( "bruce.wayne@gotham.city" ).displayName("Batman");
        Mockito.doReturn(user).when( usersService ).fetchUserDetails( any() );

        final var associationsList = new AssociationsList();
        associationsListUserDaoToDtoMapper.enrichWithItems( page, associationsList, Map.of( "userId", "111" ) );

        Assertions.assertEquals( List.of(), associationsList.getItems() );
    }

    private ArgumentMatcher<Association> associationDaoMatches( final String userId ){
        return association -> association.getUserId().equals( userId );
    }

    @Test
    void enrichWithItemsWithDisplayNameReturnsAssociationsList(){
        final var content = new ArrayList<>( List.of(associationBatmanDao, associationAlfieDao) );
        final var pageRequest = PageRequest.of( 0, 2 );
        final var page = new PageImpl<>( content, pageRequest, 3 );

        final var user = new User().email( "bruce.wayne@gotham.city" ).displayName("Batman");
        Mockito.doReturn(user).when( usersService ).fetchUserDetails( any() );

        Mockito.doReturn( associationBatmanDto).when( associationCompanyDaoToDtoMapper ).daoToDto( argThat( associationDaoMatches("111") ));
        Mockito.doReturn( associationAlfieDto).when( associationCompanyDaoToDtoMapper ).daoToDto( argThat( associationDaoMatches("222") ));

        final var associationsList = new AssociationsList();
        associationsListUserDaoToDtoMapper.enrichWithItems( page, associationsList, Map.of( "userId", "111" ) );
        final var items = associationsList.getItems();

        Assertions.assertEquals( 2, items.size() );
        Assertions.assertTrue( items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getCompanyNumber).toList().containsAll( List.of("111111","222222") ) );
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getUserEmail).allMatch( userEmail -> userEmail.equals( "bruce.wayne@gotham.city") ) );
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getDisplayName ).allMatch( displayName -> displayName.equals( "Batman") ) );
    }

    @Test
    void enrichWithItemsWithoutDisplayNameReturnsAssociationsList(){
        final var content = new ArrayList<>( List.of(associationBatmanDao, associationAlfieDao) );
        final var pageRequest = PageRequest.of( 0, 2 );
        final var page = new PageImpl<>( content, pageRequest, 3 );

        final var user = new User().email( "bruce.wayne@gotham.city" );
        Mockito.doReturn(user).when( usersService ).fetchUserDetails( any() );

        Mockito.doReturn( associationBatmanDto).when( associationCompanyDaoToDtoMapper ).daoToDto( argThat( associationDaoMatches("111") ));
        Mockito.doReturn( associationAlfieDto).when( associationCompanyDaoToDtoMapper ).daoToDto( argThat( associationDaoMatches("222") ));

        final var associationsList = new AssociationsList();
        associationsListUserDaoToDtoMapper.enrichWithItems( page, associationsList, Map.of( "userId", "111" ) );
        final var items = associationsList.getItems();

        Assertions.assertEquals( 2, items.size() );
        Assertions.assertTrue( items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getCompanyNumber).toList().containsAll( List.of("111111","222222") ) );
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getUserEmail).allMatch( userEmail -> userEmail.equals( "bruce.wayne@gotham.city") ) );
        Assertions.assertTrue(items.stream().map(uk.gov.companieshouse.api.accounts.associations.model.Association::getDisplayName ).allMatch( displayName -> displayName.equals( DEFAULT_DISPLAY_NAME ) ) );
    }


    // TODO: test daoToDto


}
