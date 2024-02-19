package uk.gov.companieshouse.accounts.association.rest;

import static org.mockito.ArgumentMatchers.any;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.user.model.Role;
import uk.gov.companieshouse.api.accounts.user.model.RolesList;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.accountsuser.PrivateAccountsUserResourceHandler;
import uk.gov.companieshouse.api.handler.accountsuser.request.PrivateAccountsUserFindUserBasedOnEmailGet;
import uk.gov.companieshouse.api.handler.accountsuser.request.PrivateAccountsUserRolesGet;
import uk.gov.companieshouse.api.handler.accountsuser.request.PrivateAccountsUserRolesPut;
import uk.gov.companieshouse.api.handler.accountsuser.request.PrivateAccountsUserUserGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class AccountsUserEndpointTest {

    @Mock
    InternalApiClient internalApiClient;

    @Mock
    PrivateAccountsUserResourceHandler privateAccountsUserResourceHandler;

    @Mock
    PrivateAccountsUserFindUserBasedOnEmailGet privateAccountsUserFindUserBasedOnEmailGet;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet;

    @Mock
    PrivateAccountsUserRolesGet privateAccountsUserRolesGet;

    @Mock
    PrivateAccountsUserRolesPut privateAccountsUserRolesPut;

    @InjectMocks
    AccountsUserEndpoint accountsUserEndpoint;

    @Test
    void searchUserDetailsWithNullInputThrowsNullPointerException() {
        Assertions.assertThrows( NullPointerException.class, () -> accountsUserEndpoint.searchUserDetails( null ) );
    }

    @Test
    void searchUserDetailsWithEmptyListOrNullElementOrMalformedEmailReturnsBadRequest() throws Exception {
        final var listWithNull = new ArrayList<String>( );
        listWithNull.add( null );

        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserFindUserBasedOnEmailGet ).when( privateAccountsUserResourceHandler ).searchUserDetails( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 400, "Bad request", new HttpHeaders() ) ) ).when( privateAccountsUserFindUserBasedOnEmailGet ).execute();

        Assertions.assertThrows( ApiErrorResponseException.class, () -> accountsUserEndpoint.searchUserDetails( List.of() ) );
        Assertions.assertThrows( ApiErrorResponseException.class , () -> accountsUserEndpoint.searchUserDetails( listWithNull ) );
        Assertions.assertThrows( ApiErrorResponseException.class , () -> accountsUserEndpoint.searchUserDetails( List.of( "xxx" ) ) );
    }

    @Test
    void searchUserDetailsFetchesSpecifiedUsers() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserFindUserBasedOnEmailGet ).when( privateAccountsUserResourceHandler ).searchUserDetails( any() );

        final var usersList = new UsersList();
        usersList.add( new User().userId("111") );
        final var intendedResponse = new ApiResponse<>( 200, Map.of(), usersList );
        Mockito.doReturn( intendedResponse ).when( privateAccountsUserFindUserBasedOnEmailGet ).execute();
        final var response = accountsUserEndpoint.searchUserDetails( List.of( "111" ) );

        Assertions.assertEquals( 200, response.getStatusCode() );
        Assertions.assertEquals( "111", response.getData().get(0).getUserId() );
    }

    @Test
    void searchUserDetailsWithNonexistentEmailReturnsNoContent() throws ApiErrorResponseException, URIValidationException {

        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserFindUserBasedOnEmailGet ).when( privateAccountsUserResourceHandler ).searchUserDetails( any() );

        final var intendedResponse = new ApiResponse<>( 204, Map.of(), new UsersList() );
        Mockito.doReturn( intendedResponse ).when( privateAccountsUserFindUserBasedOnEmailGet ).execute();
        final var response = accountsUserEndpoint.searchUserDetails( List.of( "666" ) );

        Assertions.assertEquals( 204, response.getStatusCode() );
        Assertions.assertTrue( response.getData().isEmpty() );
    }

    @Test
    void getUserDetailsWithNullInputThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> accountsUserEndpoint.getUserDetails( null ) );
    }

    @Test
    void getUserDetailsWithMalformedInputReturnsBadRequest() throws Exception {
        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 400, "Bad request", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet ).execute();

        Assertions.assertThrows( ApiErrorResponseException.class, () -> accountsUserEndpoint.getUserDetails( "$" ) );
    }

    @Test
    void getUserDetailsFetchesUser() throws Exception {
        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), new User().userId( "111" ) );
        Mockito.doReturn( intendedResponse ).when( privateAccountsUserUserGet ).execute();
        final var response = accountsUserEndpoint.getUserDetails( "111" );

        Assertions.assertEquals( 200, response.getStatusCode() );
        Assertions.assertEquals( "111", response.getData().getUserId() );
    }

    @Test
    void getUserDetailsWithNonexistentUserReturnsNotFound() throws Exception {
        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet ).execute();

        Assertions.assertThrows( ApiErrorResponseException.class, () -> accountsUserEndpoint.getUserDetails( "666" ) );
    }

    @Test
    void getUserRolesWithNullInputThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> accountsUserEndpoint.getUserRoles( null ) );
    }

    @Test
    void getUserRolesWithMalformedInputReturnsBadRequest() throws Exception {
        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserRolesGet ).when( privateAccountsUserResourceHandler ).getUserRoles( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 400, "Bad request", new HttpHeaders() ) ) ).when( privateAccountsUserRolesGet ).execute();

        Assertions.assertThrows( ApiErrorResponseException.class, () -> accountsUserEndpoint.getUserRoles( "$" ) );
    }

    @Test
    void getUserRolesFetchesRoles() throws Exception {
        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserRolesGet ).when( privateAccountsUserResourceHandler ).getUserRoles( any() );

        final var rolesList = new RolesList();
        rolesList.add(Role.BADOS_USER);
        final var intendedResponse = new ApiResponse<>( 200, Map.of(), rolesList );
        Mockito.doReturn( intendedResponse ).when( privateAccountsUserRolesGet ).execute();
        final var response = accountsUserEndpoint.getUserRoles( "111" );

        Assertions.assertEquals( 200, response.getStatusCode() );
        Assertions.assertEquals( "bados-user", response.getData().get( 0 ).getValue() );
    }

    @Test
    void getUserRolesWithNonexistentUserReturnsNotFound() throws Exception {
        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserRolesGet ).when( privateAccountsUserResourceHandler ).getUserRoles( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( privateAccountsUserRolesGet ).execute();

        Assertions.assertThrows( ApiErrorResponseException.class, () -> accountsUserEndpoint.getUserRoles( "666" ) );
    }

    @Test
    void setUserRolesWithNullInputsThrowsNullPointerException(){
        final var roles = new RolesList();
        roles.add( Role.BADOS_USER );

        Assertions.assertThrows( NullPointerException.class, () -> accountsUserEndpoint.setUserRoles( null, roles ) );
        Assertions.assertThrows( NullPointerException.class, () -> accountsUserEndpoint.setUserRoles( "111", null ) );
    }

    @Test
    void setUserRolesWithMalformedUserIdOrEmptyRolesOrNullRoleReturnsBadRequest() throws ApiErrorResponseException, URIValidationException {
        final var nullRoles = new RolesList();
        nullRoles.add( null );

        final var roles = new RolesList();
        roles.add( Role.BADOS_USER );

        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserRolesPut ).when( privateAccountsUserResourceHandler ).putUserRoles( any(), any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 400, "Bad request", new HttpHeaders() ) ) ).when( privateAccountsUserRolesPut ).execute();

        Assertions.assertThrows( ApiErrorResponseException.class, () -> accountsUserEndpoint.setUserRoles( "$", roles ) );
        Assertions.assertThrows( ApiErrorResponseException.class, () -> accountsUserEndpoint.setUserRoles( "111", new RolesList() ) );
        Assertions.assertThrows( ApiErrorResponseException.class, () -> accountsUserEndpoint.setUserRoles( "111", nullRoles ) );
    }

    @Test
    void setUserRolesReturnsOk() throws ApiErrorResponseException, URIValidationException {
        final var oneRole = new RolesList();
        oneRole.add( Role.BADOS_USER );

        final var multipleRoles = new RolesList();
        multipleRoles.add( Role.BADOS_USER );
        multipleRoles.add( Role.APPEALS_TEAM );

        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserRolesPut ).when( privateAccountsUserResourceHandler ).putUserRoles( any(), any() );

        final var intendedResponse = new ApiResponse<Void>( 200, Map.of() );
        Mockito.doReturn( intendedResponse ).when( privateAccountsUserRolesPut ).execute();

        Assertions.assertEquals( 200, accountsUserEndpoint.setUserRoles( "111", oneRole ).getStatusCode() );
        Assertions.assertEquals( 200, accountsUserEndpoint.setUserRoles( "111", multipleRoles ).getStatusCode() );
    }

    @Test
    void setUserRolesWithNonexistentUserIdReturnsNotFound() throws ApiErrorResponseException, URIValidationException {
        final var roles = new RolesList();
        roles.add( Role.BADOS_USER );

        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserRolesPut ).when( privateAccountsUserResourceHandler ).putUserRoles( any(), any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( privateAccountsUserRolesPut ).execute();

        Assertions.assertThrows( ApiErrorResponseException.class, () -> accountsUserEndpoint.setUserRoles( "666", roles ).getStatusCode() );
    }

}
