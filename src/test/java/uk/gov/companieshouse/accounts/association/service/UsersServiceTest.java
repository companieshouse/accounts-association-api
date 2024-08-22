package uk.gov.companieshouse.accounts.association.service;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.rest.AccountsUserEndpoint;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.accountsuser.request.PrivateAccountsUserUserGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

import java.util.ArrayList;
import java.util.List;
import uk.gov.companieshouse.api.model.ApiResponse;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class UsersServiceTest {

    @Mock
    private AccountsUserEndpoint accountsUserEndpoint;

    @Mock
    private PrivateAccountsUserUserGet privateAccountsUserUserGet;

    @InjectMocks
    private UsersService usersService;

    private Mockers mockers;

    @BeforeEach
    void setup(){
        mockers = new Mockers( accountsUserEndpoint, null, null, null, null );
    }

    @Test
    void fetchUserDetailsWithNullInputReturnsInternalServerError() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( privateAccountsUserUserGet ).when( accountsUserEndpoint ).createGetUserDetailsRequest( any() );
        Mockito.doThrow( NullPointerException.class ).when( privateAccountsUserUserGet ).execute();
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( null ) );
    }

    @Test
    void fetchUserDetailsWithMalformedInputReturnsInternalServerError() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( privateAccountsUserUserGet ).when( accountsUserEndpoint ).createGetUserDetailsRequest( any() );
        Mockito.doThrow( new URIValidationException( "Uri incorrectly formatted" ) ).when( privateAccountsUserUserGet ).execute();
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( "$" ) );
    }

    @Test
    void fetchUserDetailsWithNonexistentUserReturnsNotFound() throws ApiErrorResponseException, URIValidationException {
        mockers.mockGetUserDetailsNotFound( "666" );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> usersService.fetchUserDetails( "666" ) );
    }

    @Test
    void fetchUserDetailsReturnsInternalServerErrorWhenItReceivesApiErrorResponseWithNon404StatusCode() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( privateAccountsUserUserGet ).when( accountsUserEndpoint ).createGetUserDetailsRequest( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 500, "Something unexpected happened", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet ).execute();
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( "111" ) );
    }

    @Test
    void fetchUserDetailsSuccessfullyFetchesUserData() throws ApiErrorResponseException, URIValidationException {
        mockers.mockGetUserDetails( "333" );
        Assertions.assertEquals( "333", usersService.fetchUserDetails( "333" ).getUserId() );
    }

    @Test
    void createFetchUserDetailsRequestWithNullInputReturnsInternalServerError() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( privateAccountsUserUserGet ).when( accountsUserEndpoint ).createGetUserDetailsRequest( any() );
        Mockito.doThrow( NullPointerException.class ).when( privateAccountsUserUserGet ).execute();
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.createFetchUserDetailsRequest( null ).get() );
    }

    @Test
    void createFetchUserDetailsRequestWithMalformedInputReturnsInternalServerError() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( privateAccountsUserUserGet ).when( accountsUserEndpoint ).createGetUserDetailsRequest( any() );
        Mockito.doThrow( new URIValidationException( "Uri incorrectly formatted" ) ).when( privateAccountsUserUserGet ).execute();
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.createFetchUserDetailsRequest( "$" ).get() );
    }

    @Test
    void createFetchUserDetailsRequestWithNonexistentUserReturnsNotFound() throws ApiErrorResponseException, URIValidationException {
        mockers.mockGetUserDetailsNotFound( "666" );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> usersService.createFetchUserDetailsRequest( "666" ).get() );
    }

    @Test
    void createFetchUserDetailsRequestReturnsInternalServerErrorWhenItReceivesApiErrorResponseWithNon404StatusCode() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( privateAccountsUserUserGet ).when( accountsUserEndpoint ).createGetUserDetailsRequest( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 500, "Something unexpected happened", new HttpHeaders() ) ) ).when( privateAccountsUserUserGet ).execute();
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.createFetchUserDetailsRequest( "111" ).get() );
    }

    @Test
    void createFetchUserDetailsRequestSuccessfullyFetchesUserData() throws ApiErrorResponseException, URIValidationException {
        mockers.mockGetUserDetails( "333" );
        Assertions.assertEquals( "333", usersService.createFetchUserDetailsRequest( "333" ).get().getUserId() );
    }

    @Test
    void searchUserDetailsWithNullInputThrowsNullPointerException() {
        Assertions.assertThrows( NullPointerException.class, () -> usersService.searchUserDetails( null ) );
    }

    @Test
    void searchUserDetailsWithEmptyListOrListContainingNullOrMalformedEmailReturnInternalServerError() throws ApiErrorResponseException, URIValidationException {
        final var nullList = new ArrayList<String>();
        nullList.add( null );

        Mockito.doThrow( new ApiErrorResponseException( new Builder( 400, "Bad input given", new HttpHeaders() ) ) ).when( accountsUserEndpoint ).searchUserDetails( any() );

        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails( new ArrayList<>() ) );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails( nullList ) );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails( List.of( "$$$" ) ) );
    }

    @Test
    void searchUserDetailsWithOneUserRetrievesUserDetails() throws ApiErrorResponseException, URIValidationException {
        mockers.mockSearchUserDetails( "111" );
        Assertions.assertEquals( "111", usersService.searchUserDetails( List.of( "bruce.wayne@gotham.city" ) ).getFirst().getUserId() );
    }

    @Test
    void searchUserDetailsWithMultipleUsersRetrievesUserDetails() throws ApiErrorResponseException, URIValidationException {
        final var usersList = new UsersList();
        usersList.addAll( List.of( new User().userId( "111" ), new User().userId( "333" ) ) );
        final var intendedResponse = new ApiResponse<>( 200, Map.of(), usersList );

        Mockito.doReturn( intendedResponse ).when( accountsUserEndpoint ).searchUserDetails( List.of( "bruce.wayne@gotham.city", "harley.quinn@gotham.city" ) );

        final var result = usersService.searchUserDetails( List.of( "bruce.wayne@gotham.city", "harley.quinn@gotham.city" ) );

        Assertions.assertEquals( "111", result.getFirst().getUserId() );
        Assertions.assertEquals( "333", result.getLast().getUserId() );
    }

    @Test
    void searchUserDetailsWithNonexistentEmailReturnsEmptyList() throws ApiErrorResponseException, URIValidationException {
        mockers.mockSearchUserDetailsNotFound( "bruce.wayne@gotham.city" );
        Assertions.assertTrue( usersService.searchUserDetails( List.of( "bruce.wayne@gotham.city" ) ).isEmpty());
    }

    @Test
    void searchUserDetailsWithIncorrectlyFormattedUriThrowsInternalServerError() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new URIValidationException( "Uri incorrectly formatted" ) ).when( accountsUserEndpoint ).searchUserDetails( List.of( "" ) );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails( List.of( "" ) ) );
    }

}
