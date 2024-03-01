package uk.gov.companieshouse.accounts.association.service;

import static org.mockito.ArgumentMatchers.any;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.rest.AccountsUserEndpoint;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class UsersServiceTest {

    @Mock
    private AccountsUserEndpoint accountsUserEndpoint;

    @InjectMocks
    private UsersService usersService;

    @Test
    void fetchUserDetailsWithNullInputReturnsInternalServerError() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new NullPointerException() ).when( accountsUserEndpoint ).getUserDetails( any() );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( null ) );
    }

    @Test
    void fetchUserDetailsWithMalformedInputReturnsInternalServerError() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new URIValidationException( "Uri incorrectly formatted" ) ).when( accountsUserEndpoint ).getUserDetails( any() );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( "$" ) );
    }

    @Test
    void fetchUserDetailsWithNonexistentUserReturnsNotFound() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not found", new HttpHeaders() ) ) ).when(accountsUserEndpoint).getUserDetails(any());
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> usersService.fetchUserDetails( "666" ) );
    }

    @Test
    void fetchUserDetailsReturnsInternalServerErrorWhenItReceivesApiErrorResponseWithNon404StatusCode() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 500, "Something unexpected happened", new HttpHeaders() ) ) ).when( accountsUserEndpoint ).getUserDetails( any() );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails( "111" ) );
    }

    @Test
    void fetchUserDetailsSuccessfullyFetchesUserData() throws ApiErrorResponseException, URIValidationException {
        final var user = new User().userId( "333" );

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), user );
        Mockito.doReturn( intendedResponse ).when( accountsUserEndpoint ).getUserDetails( any() );
        final var response = usersService.fetchUserDetails( "333" );

        Assertions.assertEquals( "333", response.getUserId() );
    }

}
