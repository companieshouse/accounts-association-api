package uk.gov.companieshouse.accounts.association.common;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Arrays;
import java.util.List;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;
import uk.gov.companieshouse.api.accounts.user.model.User;

public class Mockers {

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private final WebClient webClient;
    private final EmailProducer emailProducer;
    private final CompanyService companyService;
    private final UsersService usersService;

    public Mockers( final WebClient webClient, final EmailProducer emailProducer, final CompanyService companyService, final UsersService usersService ) {
        this.webClient = webClient;
        this.emailProducer = emailProducer;
        this.companyService = companyService;
        this.usersService = usersService;
    }

    private void mockWebClientSuccessResponse( final String uri, final Mono<String> jsonResponse ){
        final var requestHeadersUriSpec = Mockito.mock( WebClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        Mockito.doReturn( requestHeadersUriSpec ).when(webClient).get();
        Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uri );
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( jsonResponse ).when( responseSpec ).bodyToMono( String.class );
    }

    public void mockWebClientForFetchUserDetails( final String... userIds ) throws JsonProcessingException {
        for ( final String userId: userIds ){
            final var user = testDataManager.fetchUserDtos( userId ).getFirst();
            final var uri = String.format( "/users/%s", userId );
            final var jsonResponse = new ObjectMapper().writeValueAsString( user );
            mockWebClientSuccessResponse( uri, Mono.just( jsonResponse ) );
        }
    }

    private void mockWebClientErrorResponse( final String uri, int responseCode ){
        final var requestHeadersUriSpec = Mockito.mock( WebClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        Mockito.doReturn( requestHeadersUriSpec ).when(webClient).get();
        Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uri );
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( Mono.error( new WebClientResponseException( responseCode, "Error", null, null, null ) ) ).when( responseSpec ).bodyToMono( String.class );
    }

    public void mockWebClientForFetchUserDetailsErrorResponse( final String userId, int responseCode ){
        final var uri = String.format( "/users/%s", userId );
        mockWebClientErrorResponse( uri, responseCode );
    }

    public void mockWebClientForFetchUserDetailsNotFound( final String... userIds ){
        for ( final String userId: userIds ){
            mockWebClientForFetchUserDetailsErrorResponse( userId,404 );
        }
    }

    private void mockWebClientJsonParsingError( final String uri ){
        final var requestHeadersUriSpec = Mockito.mock( WebClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        Mockito.doReturn( requestHeadersUriSpec ).when(webClient).get();
        Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uri );
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( Mono.just( "}{" ) ).when( responseSpec ).bodyToMono( String.class );
    }

    public void mockWebClientForFetchUserDetailsJsonParsingError( final String userId ){
        final var uri = String.format( "/users/%s", userId );
        mockWebClientJsonParsingError( uri );
    }

    public void mockWebClientForSearchUserDetails( final String... userIds ) throws JsonProcessingException {
        final var users = testDataManager.fetchUserDtos( userIds );
        final var uri = String.format( "/users/search?user_email=" + String.join( "&user_email=", users.stream().map( User::getEmail ).toList() ) );
        final var jsonResponse = new ObjectMapper().writeValueAsString( users );
        mockWebClientSuccessResponse( uri, Mono.just( jsonResponse ) );
    }

    public void mockWebClientForSearchUserDetailsErrorResponse( final String userEmail, int responseCode ){
        final var uri = String.format( "/users/search?user_email=%s", userEmail );
        mockWebClientErrorResponse( uri, responseCode );
    }

    public void mockWebClientForSearchUserDetailsNonexistentEmail( final String... emails ) {
        final var uri = String.format( "/users/search?user_email=" + String.join( "&user_email=", Arrays.stream( emails ).toList() ) );
        mockWebClientSuccessResponse( uri, Mono.empty() );
    }

    public void mockWebClientForSearchUserDetailsJsonParsingError( final String... emails ){
        final var uri = String.format( "/users/search?user_email=" + String.join( "&user_email=", Arrays.stream( emails ).toList() ) );
        mockWebClientJsonParsingError( uri );
    }

    public void mockWebClientForFetchCompanyProfile( final String... companyNumbers ) throws JsonProcessingException {
        for ( final String companyNumber: companyNumbers ){
            final var company = testDataManager.fetchCompanyDetailsDtos( companyNumber ).getFirst();
            final var uri = String.format( "/company/%s/company-detail", companyNumber );
            final var jsonResponse = new ObjectMapper().writeValueAsString( company );
            mockWebClientSuccessResponse( uri, Mono.just( jsonResponse ) );
        }
    }

    public void mockWebClientForFetchCompanyProfileErrorResponse( final String companyNumber, int responseCode ){
        final var uri = String.format( "/company/%s/company-detail", companyNumber );
        mockWebClientErrorResponse( uri, responseCode );
    }

    public void mockWebClientForFetchCompanyProfileNotFound( final String... companyNumbers ){
        for ( final String companyNumber: companyNumbers ){
            mockWebClientForFetchCompanyProfileErrorResponse( companyNumber, 404 );
        }
    }

    public void mockWebClientForFetchCompanyProfileJsonParsingError( final String companyNumber ){
        final var uri = String.format( "/company/%s/company-detail", companyNumber );
        mockWebClientJsonParsingError( uri );
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
            Mockito.doReturn( userDetails ).when( usersService ).fetchUserDetails( userId );
        }
    }

    public void mockUsersServiceSearchUserDetails( final String... userIds ){
        for ( final String userId: userIds ){
            final var userDetails = testDataManager.fetchUserDtos( userId ).getFirst();
            final var userEmails = List.of( userDetails.getEmail() );
            final var usersList = new UsersList();
            usersList.add( userDetails );
            Mockito.doReturn( usersList ).when( usersService ).searchUserDetails( userEmails );
        }
    }

    public void mockUsersServiceSearchUserDetailsEmptyList( final String... userEmails ){
        for ( final String userEmail: userEmails ) {
            Mockito.doReturn( new UsersList() ).when( usersService ).searchUserDetails( List.of( userEmail ) );
        }
    }

    public void mockUsersServiceFetchUserDetailsNotFound( final String... userIds ){
        for ( final String userId: userIds ){
            Mockito.doThrow( new NotFoundRuntimeException( "Not found.", new Exception( "Not found." ) ) ).when( usersService ).fetchUserDetails( userId );
        }
    }

}
