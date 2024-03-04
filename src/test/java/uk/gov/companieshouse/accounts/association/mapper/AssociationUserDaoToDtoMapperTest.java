package uk.gov.companieshouse.accounts.association.mapper;

import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.user.model.User;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class AssociationUserDaoToDtoMapperTest {

    @Mock
    private UsersService usersService;

    @InjectMocks
    private AssociationUserDaoToDtoMapper associationUserDaoToDtoMapper = Mockito.mock(AssociationUserDaoToDtoMapper.class, Mockito.CALLS_REAL_METHODS);

    private static final String DEFAULT_DISPLAY_NAME = "Not provided";

    @Test
    void enrichAssociationWithUserDetailsWithNullInputThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> associationUserDaoToDtoMapper.enrichAssociationWithUserDetails( null ) );
    }

    @Test
    void enrichAssociationWithUserDetailsWithoutDisplayNameSetsDefaultDisplayName(){
        final var association = new Association().userId( "111" );

        final var user = new User().email( "jason.manford@comedy.com" );
        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any() );
        associationUserDaoToDtoMapper.enrichAssociationWithUserDetails( association );

        Assertions.assertEquals( "jason.manford@comedy.com", association.getUserEmail() );
        Assertions.assertEquals( DEFAULT_DISPLAY_NAME, association.getDisplayName() );
    }

    @Test
    void enrichAssociationWithUserDetailsSetsDisplayName(){
        final var association = new Association().userId( "111" );

        final var user = new User().email( "anne@the.chase.com" ).displayName( "The Governess" );
        Mockito.doReturn( user ).when( usersService ).fetchUserDetails( any() );
        associationUserDaoToDtoMapper.enrichAssociationWithUserDetails( association );

        Assertions.assertEquals( "anne@the.chase.com", association.getUserEmail() );
        Assertions.assertEquals( "The Governess", association.getDisplayName() );
    }






    @Test
    void daoToDtoWithNullInputReturnsNull(){
        Assertions.assertNull( associationUserDaoToDtoMapper.daoToDto( null ) );
    }

    @Test
    void daoToDtoWithOnlyMandatoryFieldsSuccessfullyPerformsMapping(){

    }

    // TODO: test daoToDto


}
