package uk.gov.companieshouse.accounts.association.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.rest.UserInfoMockEndpoint;
import uk.gov.companieshouse.api.accounts.associations.model.UserInfo;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class UsersServiceTest {

    @InjectMocks
    UsersService usersService;

    @Mock
    UserInfoMockEndpoint userInfoMockEndpoint;

    @Test
    void fetchUserInfoWithMalformedOrNonexistentEmailReturnsNothing(){
        Mockito.doReturn( Optional.empty() )
               .when( userInfoMockEndpoint ).fetchUserInfo( any() );

        Assertions.assertTrue( usersService.fetchUserInfo( null ).isEmpty() );
        Assertions.assertTrue( usersService.fetchUserInfo( "" ).isEmpty() );
        Assertions.assertTrue( usersService.fetchUserInfo( "abc" ).isEmpty() );
        Assertions.assertTrue( usersService.fetchUserInfo( "chips@food.com" ).isEmpty() );
    }

    @Test
    void fetchUserInfoReturnsUserInfoForEmail(){
        final var userEmail = "ronald@mcdonald.com";
        final var userId = "111";

        Mockito.doReturn( Optional.of( new UserInfo().userId( userId ) ) )
                .when( userInfoMockEndpoint ).fetchUserInfo( userEmail );

        Assertions.assertEquals( "111", usersService.fetchUserInfo( userEmail ).get().getUserId() );
    }

}
