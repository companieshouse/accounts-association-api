package uk.gov.companieshouse.accounts.association.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.Arrays;
import java.util.List;
import org.eclipse.jetty.http.HttpStatus;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;
import uk.gov.companieshouse.api.accounts.user.model.User;

@RestClientTest
public class Mockers {

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private final RestClient restClient;
    private final EmailProducer emailProducer;
    private final CompanyService companyService;
    private final UsersService usersService;

    public Mockers( final RestClient restClient, final EmailProducer emailProducer, final CompanyService companyService, final UsersService usersService ) {
        this.restClient = restClient;
        this.emailProducer = emailProducer;
        this.companyService = companyService;
        this.usersService = usersService;
    }

    private void mockRestClientSuccessResponse( final String uri, final String jsonResponse ){
        final var requestHeadersUriSpec = Mockito.mock( RestClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( RestClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( RestClient.ResponseSpec.class );

        Mockito.doReturn( requestHeadersUriSpec ).when(restClient).get();
        Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( URI.create(uri));
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.lenient().doReturn( responseSpec ).when( responseSpec ).onStatus(any(), any());
        Mockito.doReturn( jsonResponse ).when( responseSpec ).body( String.class );
    }

    public void mockRestClientForFetchUserDetails( final String... userIds ) throws JsonProcessingException {
        for ( final String userId: userIds ){
            final var user = testDataManager.fetchUserDtos( userId ).getFirst();
            final var uri = String.format( "/users/%s", userId );
            final var jsonResponse = new ObjectMapper().writeValueAsString( user );
            mockRestClientSuccessResponse(uri, jsonResponse);
        }
    }

    private void mockRestClientErrorResponse(final String uri, int responseCode){
        final var requestHeadersUriSpec = Mockito.mock( RestClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( RestClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( RestClient.ResponseSpec.class );

        Mockito.lenient().doReturn( requestHeadersUriSpec ).when(restClient).get();
        Mockito.lenient().doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uri == null ? URI.create("") : URI.create(uri) );
        Mockito.lenient().doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        if (responseCode == 404) {
            Mockito.lenient().doThrow(new NotFoundRuntimeException("Not Found", new Exception("Not Found"))).when(responseSpec).onStatus(any(), any());
        } else if (responseCode == 400) {
            Mockito.doThrow(new RestClientResponseException("Bad Request", HttpStatus.BAD_REQUEST_400, "Bad Request", null, null, null)).when(responseSpec).onStatus(any(), any());
        } else {
            Mockito.doReturn(responseSpec).when(responseSpec).onStatus(any(), any());
        }
    }

    public void mockRestClientForFetchUserDetailsErrorResponse( final String userId, int responseCode ){
        final var uri = String.format( "/users/%s", userId );
        mockRestClientErrorResponse( uri, responseCode );
    }

    public void mockRestClientForFetchUserDetailsNotFound( final String... userIds ){
        for ( final String userId: userIds ){
            mockRestClientForFetchUserDetailsErrorResponse( userId,404 );
        }
    }

    private void mockRestClientJsonParsingError( final String uri ){
        final var requestHeadersUriSpec = Mockito.mock( RestClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( RestClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( RestClient.ResponseSpec.class );

        Mockito.doReturn( requestHeadersUriSpec ).when(restClient).get();
        Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( URI.create(uri) );
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.lenient().doReturn( responseSpec ).when( responseSpec ).onStatus(any(), any());
        Mockito.doReturn("}{").when(responseSpec).body(String.class);
    }

    public void mockRestClientForFetchUserDetailsJsonParsingError( final String userId ){
        final var uri = String.format( "/users/%s", userId );
        mockRestClientJsonParsingError( uri );
    }

    public void mockRestClientForSearchUserDetails( final String... userIds ) throws JsonProcessingException {
        final var users = testDataManager.fetchUserDtos( userIds );
        final var uri = String.format( "/users/search?user_email=" + String.join( "&user_email=", users.stream().map( User::getEmail ).toList() ) );
        final var jsonResponse = new ObjectMapper().writeValueAsString( users.getFirst() );
        mockRestClientSuccessResponse( uri, jsonResponse );
    }

    public void mockRestClientForSearchUserDetailsErrorResponse( final String userEmail, int responseCode ){
        final var uri = String.format( "/users/search?user_email=%s", userEmail );
        mockRestClientErrorResponse( uri, responseCode );
    }

    public void mockRestClientForSearchUserDetailsNonexistentEmail( final String... emails ) {
        final var uri = String.format( "/users/search?user_email=" + String.join( "&user_email=", Arrays.stream( emails ).toList() ) );
        mockRestClientSuccessResponse( uri, null );
    }

    public void mockRestClientForSearchUserDetailsJsonParsingError( final String... emails ){
        final var uri = String.format( "/users/search?user_email=" + String.join( "&user_email=", Arrays.stream( emails ).toList() ) );
        mockRestClientJsonParsingError( uri );
    }

    public void mockRestClientForFetchCompanyProfile( final String... companyNumbers ) throws JsonProcessingException {
        for ( final String companyNumber: companyNumbers ){
            final var company = testDataManager.fetchCompanyDetailsDtos( companyNumber ).getFirst();
            final var uri = String.format( "/company/%s/company-detail", companyNumber );
            final var jsonResponse = new ObjectMapper().writeValueAsString( company );
            mockRestClientSuccessResponse(uri, jsonResponse);
        }
    }

    public void mockRestClientForFetchCompanyProfileErrorResponse( final String companyNumber, int responseCode ){
        final var uri = String.format( "/company/%s/company-detail", companyNumber );
        mockRestClientErrorResponse( uri, responseCode );
    }

    public void mockRestClientForFetchCompanyProfileNotFound( final String... companyNumbers ){
        for ( final String companyNumber: companyNumbers ){
            mockRestClientForFetchCompanyProfileErrorResponse( companyNumber, 404 );
        }
    }

    public void mockRestClientForFetchCompanyProfileJsonParsingError( final String companyNumber ){
        final var uri = String.format( "/company/%s/company-detail", companyNumber );
        mockRestClientJsonParsingError( uri );
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
            Mockito.doThrow( new NotFoundRuntimeException( "Not found", new Exception( "Not found" ) ) ).when( companyService ).fetchCompanyProfile( companyNumber );
        }
    }

    public void mockUsersServiceFetchUserDetails( final String... userIds ){
        for ( final String userId: userIds ){
            final var userDetails = testDataManager.fetchUserDtos( userId ).getFirst();
            Mockito.doReturn( userDetails ).when( usersService ).fetchUserDetails( eq(userId), any() );
        }
    }

    public void mockUsersServiceToFetchUserDetailsRequest( final String... userIds ){
        for ( final String userId: userIds ){
            final var userDetails = testDataManager.fetchUserDtos( userId ).getFirst();
            Mockito.doReturn(userDetails).when( usersService ).retrieveUserDetails( eq(userId), any() );
        }
    }

    public void mockUsersServiceSearchUserDetails( final String... userIds ){
        for ( final String userId: userIds ){
            final var userDetails = testDataManager.fetchUserDtos( userId ).getFirst();
            final var userEmails = List.of( userDetails.getEmail() );
            final var usersList = new UsersList();
            usersList.add( userDetails );
            Mockito.doReturn( usersList ).when( usersService ).searchUserDetailsByEmail(userEmails);
        }
    }

    public void mockUsersServiceSearchUserDetailsEmptyList( final String... userEmails ){
        for ( final String userEmail: userEmails ) {
            Mockito.doReturn( new UsersList() ).when( usersService ).searchUserDetailsByEmail( List.of( userEmail ) );
        }
    }

    public void mockUsersServiceFetchUserDetailsNotFound( final String... userIds ){
        for ( final String userId: userIds ){
            Mockito.doThrow( new NotFoundRuntimeException( "Not found.", new Exception( "Not found." ) ) ).when( usersService ).fetchUserDetails( eq(userId), any() );
        }
    }

}
