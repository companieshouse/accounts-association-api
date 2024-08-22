package uk.gov.companieshouse.accounts.association.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
import java.util.List;
import java.util.Map;
import org.mockito.Mockito;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.rest.AccountsUserEndpoint;
import uk.gov.companieshouse.accounts.association.rest.CompanyProfileEndpoint;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.accountsuser.request.PrivateAccountsUserUserGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;

public class Mockers {

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private final AccountsUserEndpoint accountsUserEndpoint;
    private final CompanyProfileEndpoint companyProfileEndpoint;
    private final EmailProducer emailProducer;
    private final CompanyService companyService;

    private final UsersService usersService;

    public Mockers( final AccountsUserEndpoint accountsUserEndpoint, final CompanyProfileEndpoint companyProfileEndpoint, final EmailProducer emailProducer, final CompanyService companyService, final UsersService usersService ) {
        this.accountsUserEndpoint = accountsUserEndpoint;
        this.companyProfileEndpoint = companyProfileEndpoint;
        this.emailProducer = emailProducer;
        this.companyService = companyService;
        this.usersService = usersService;
    }

    public void mockGetUserDetails( final User userData ) throws ApiErrorResponseException, URIValidationException {
        final var request = Mockito.mock( PrivateAccountsUserUserGet.class );
        Mockito.doReturn( request ).when( accountsUserEndpoint ).createGetUserDetailsRequest( userData.getUserId() );
        Mockito.lenient().doReturn( new ApiResponse<>( 200, Map.of(), userData ) ).when( request ).execute();
    }

    public void mockGetUserDetails( final String... userIds ) throws ApiErrorResponseException, URIValidationException {
        for ( final String userId: userIds ){
            final var userData = testDataManager.fetchUserDtos( userId ).getFirst();
            mockGetUserDetails( userData );
        }
    }

    public void mockGetUserDetailsNotFound( final String... userIds ) throws ApiErrorResponseException, URIValidationException {
        for ( final String userId: userIds ){
            final var request = Mockito.mock( PrivateAccountsUserUserGet.class );
            Mockito.doReturn( request ).when( accountsUserEndpoint ).createGetUserDetailsRequest( userId );
            Mockito.lenient().doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( request ).execute();
        }
    }
    public void mockFetchCompanyProfile( final String... companyNumbers ) throws ApiErrorResponseException, URIValidationException {
        for ( final String companyNumber: companyNumbers ){
            final var companyDetails = testDataManager.fetchCompanyDetailsDtos( companyNumber ).getFirst();
            final var response = new ApiResponse<>( 200, Map.of(), companyDetails );
            Mockito.doReturn( response ).when( companyProfileEndpoint ).fetchCompanyProfile( eq( companyNumber ) );
        }
    }

    public void mockFetchCompanyProfileNotFound( final String... companyNumbers ) throws ApiErrorResponseException, URIValidationException {
        for ( final String companyNumber: companyNumbers ){
            Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( companyProfileEndpoint ).fetchCompanyProfile( eq( companyNumber ) );
        }
    }

    public void mockSearchUserDetails( final String... userIds ) throws ApiErrorResponseException, URIValidationException {
        for ( final String userId: userIds ){
            final var userDetails = testDataManager.fetchUserDtos( userId ).getFirst();
            final var userEmails = List.of( userDetails.getEmail() );
            final var usersList = new UsersList();
            usersList.add( userDetails );
            final var response = new ApiResponse<>( 200, Map.of(), usersList );
            Mockito.doReturn( response ).when( accountsUserEndpoint ).searchUserDetails( eq( userEmails ) );
        }
    }

    public void mockSearchUserDetailsNotFound( final String... emails ) throws ApiErrorResponseException, URIValidationException {
        for ( final String email: emails ){
            Mockito.doReturn( new ApiResponse<>( 204, Map.of(), new UsersList() ) ).when( accountsUserEndpoint ).searchUserDetails( List.of( email ) );
        }
    }

    public void mockEmailSendingFailure( final String messageType ){
        Mockito.doThrow( new EmailSendingException("Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), eq( messageType ) );
    }

    public void mockCompanyServiceFetchCompanyProfile( final String... companyNumbers ){
        for ( final String companyNumber: companyNumbers ){
            final var companyDetails = testDataManager.fetchCompanyDetailsDtos( companyNumber ).getFirst();
            Mockito.doReturn( companyDetails ).when( companyService ).fetchCompanyProfile( companyNumber );
        }
    }

    public void mockCompanyServiceFetchCompanyProfileNotFound( final String... companyNumbers ){
        for ( final String companyNumber: companyNumbers ){
            Mockito.doThrow( new NotFoundRuntimeException( "accounts-association-api", "Not found" ) ).when( companyService ).fetchCompanyProfile( companyNumber );
        }
    }

    public void mockUsersServiceFetchUserDetails( final String... userIds ){
        for ( final String userId: userIds ){
            final var userDetails = testDataManager.fetchUserDtos( userId ).getFirst();
            Mockito.doReturn( userDetails ).when( usersService ).fetchUserDetails( eq( userId ) );
        }
    }

    public void mockUsersServiceSearchUserDetails( final String... userIds ){
        for ( final String userId: userIds ){
            final var userDetails = testDataManager.fetchUserDtos( userId ).getFirst();
            final var userEmails = List.of( userDetails.getEmail() );
            final var usersList = new UsersList();
            usersList.add( userDetails );
            Mockito.doReturn( usersList ).when( usersService ).searchUserDetails( eq( userEmails ) );
        }
    }

    public void mockUsersServiceSearchUserDetailsEmptyList( final String... userEmails ){
        for ( final String userEmail: userEmails ) {
            Mockito.doReturn( new UsersList() ).when( usersService ).searchUserDetails( List.of( userEmail ) );
        }
    }

    public void mockUsersServiceFetchUserDetailsNotFound( final String... userIds ){
        for ( final String userId: userIds ){
            Mockito.doThrow( new NotFoundRuntimeException( "accounts-association-api", "Not found." ) ).when( usersService ).fetchUserDetails( userId );
        }
    }

}
