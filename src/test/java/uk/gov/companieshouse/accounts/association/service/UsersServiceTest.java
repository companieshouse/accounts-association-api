package uk.gov.companieshouse.accounts.association.service;

import static org.mockito.ArgumentMatchers.any;

import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import uk.gov.companieshouse.accounts.association.rest.UserInfoMockEndpoint;
import uk.gov.companieshouse.api.accounts.associations.model.UserInfo;

@SpringBootTest
@Tag("unit-test")
public class UsersServiceTest {

    @Autowired
    UsersService usersService;

    @MockBean
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
