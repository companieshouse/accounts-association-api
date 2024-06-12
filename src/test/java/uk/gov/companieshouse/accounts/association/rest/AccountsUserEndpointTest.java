package uk.gov.companieshouse.accounts.association.rest;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.utils.ApiClientUtil;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.accountsuser.PrivateAccountsUserResourceHandler;
import uk.gov.companieshouse.api.handler.accountsuser.request.PrivateAccountsUserFindUserBasedOnEmailGet;
import uk.gov.companieshouse.api.handler.accountsuser.request.PrivateAccountsUserUserGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
 class AccountsUserEndpointTest {

    @Mock
    ApiClientUtil apiClientService;

    @Mock
    InternalApiClient internalApiClient;

    @Mock
    PrivateAccountsUserResourceHandler privateAccountsUserResourceHandler;

    @Mock
    PrivateAccountsUserFindUserBasedOnEmailGet privateAccountsUserFindUserBasedOnEmailGet;

    @Mock
    PrivateAccountsUserUserGet privateAccountsUserUserGet;

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

        Mockito.doReturn( internalApiClient ).when( apiClientService ).getInternalApiClient(any());
        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserFindUserBasedOnEmailGet ).when( privateAccountsUserResourceHandler ).searchUserDetails( any(), any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 400, "Bad request", new HttpHeaders() ) ) ).when( privateAccountsUserFindUserBasedOnEmailGet ).execute();

        Assertions.assertThrows( ApiErrorResponseException.class, () -> accountsUserEndpoint.searchUserDetails( List.of() ) );
        Assertions.assertThrows( ApiErrorResponseException.class , () -> accountsUserEndpoint.searchUserDetails( listWithNull ) );
        Assertions.assertThrows( ApiErrorResponseException.class , () -> accountsUserEndpoint.searchUserDetails( List.of( "xxx" ) ) );
    }

    @Test
    void searchUserDetailsFetchesSpecifiedUsers() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( internalApiClient ).when( apiClientService ).getInternalApiClient(any());
        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserFindUserBasedOnEmailGet ).when( privateAccountsUserResourceHandler ).searchUserDetails( any(), any() );

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
        Mockito.doReturn( internalApiClient ).when( apiClientService ).getInternalApiClient(any());
        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserFindUserBasedOnEmailGet ).when( privateAccountsUserResourceHandler ).searchUserDetails( any(), any() );

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
        Mockito.doReturn( internalApiClient ).when( apiClientService ).getInternalApiClient(any());
        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 400, "Bad request", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet ).execute();

        Assertions.assertThrows( ApiErrorResponseException.class, () -> accountsUserEndpoint.getUserDetails( "$" ) );
    }

    @Test
    void getUserDetailsFetchesUser() throws Exception {
        Mockito.doReturn( internalApiClient ).when( apiClientService ).getInternalApiClient(any());
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
        Mockito.doReturn( internalApiClient ).when( apiClientService ).getInternalApiClient(any());
        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet ).execute();

        Assertions.assertThrows( ApiErrorResponseException.class, () -> accountsUserEndpoint.getUserDetails( "666" ) );
    }

    @Test
    void createGetUserDetailsRequestWithNullInputThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> accountsUserEndpoint.createGetUserDetailsRequest( null ).execute() );
    }

    @Test
    void createGetUserDetailsRequestWithMalformedInputReturnsBadRequest() throws Exception {
        Mockito.doReturn( internalApiClient ).when( apiClientService ).getInternalApiClient(any());
        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 400, "Bad request", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet ).execute();

        Assertions.assertThrows( ApiErrorResponseException.class, () -> accountsUserEndpoint.createGetUserDetailsRequest( "$" ).execute() );
    }

    @Test
    void createGetUserDetailsRequestFetchesUser() throws Exception {
        Mockito.doReturn( internalApiClient ).when( apiClientService ).getInternalApiClient(any());
        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), new User().userId( "111" ) );
        Mockito.doReturn( intendedResponse ).when( privateAccountsUserUserGet ).execute();
        final var response = accountsUserEndpoint.createGetUserDetailsRequest( "111" ).execute();

        Assertions.assertEquals( 200, response.getStatusCode() );
        Assertions.assertEquals( "111", response.getData().getUserId() );
    }

    @Test
    void createGetUserDetailsRequestWithNonexistentUserReturnsNotFound() throws Exception {
        Mockito.doReturn( internalApiClient ).when( apiClientService ).getInternalApiClient(any());
        Mockito.doReturn( privateAccountsUserResourceHandler ).when( internalApiClient ).privateAccountsUserResourceHandler();
        Mockito.doReturn( privateAccountsUserUserGet ).when( privateAccountsUserResourceHandler ).getUserDetails( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet ).execute();

        Assertions.assertThrows( ApiErrorResponseException.class, () -> accountsUserEndpoint.createGetUserDetailsRequest( "666" ).execute() );
    }

}
