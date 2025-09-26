package uk.gov.companieshouse.accounts.association.common;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.util.function.Function;
import org.mockito.Mockito;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

public class Mockers {

    public enum UriType {
        URI,
        STRING,
        FUNCTION
    }

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private final WebClient webClient;
    private final EmailProducer emailProducer;
    private final CompanyService companyService;
    private final UsersService usersService;
    private boolean mockHeader = true;

    public Mockers( final WebClient webClient, final EmailProducer emailProducer, final CompanyService companyService, final UsersService usersService ) {
        this.webClient = webClient;
        this.emailProducer = emailProducer;
        this.companyService = companyService;
        this.usersService = usersService;
    }

    private void mockWebClientSuccessResponse( final URI uri, final Mono<String> jsonResponse) {
        final var requestHeadersUriSpec = Mockito.mock( WebClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        when(requestHeadersUriSpec.uri(any( Function.class ) ) ).thenReturn(requestHeadersSpec );
        Mockito.doReturn( requestHeadersUriSpec ).when(webClient).get();
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( jsonResponse ).when( responseSpec ).bodyToMono( String.class );
    }

    private void mockWebClientSuccessResponse( final String uri, final Mono<String> jsonResponse, final UriType uriType) {
        final var requestHeadersUriSpec = Mockito.mock( WebClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        switch (uriType) {
            case URI -> {
                Mockito.doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(Mockito.any(URI.class));
            }
            case STRING -> {
                Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uri );
            }
            case FUNCTION -> {
                Mockito.doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(Mockito.any(Function.class));
            }
        }
        if( mockHeader ){
            Mockito.doReturn( requestHeadersSpec ).when( requestHeadersSpec ).header( Mockito.anyString(), Mockito.any() );
        }
        Mockito.doReturn( requestHeadersUriSpec ).when(webClient).get();
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( jsonResponse ).when( responseSpec ).bodyToMono( String.class );
    }


    public void mockWebClientForFetchUserDetails( final boolean isUriString, final String... userIds ) throws JsonProcessingException {
        for ( final String userId: userIds ){
            final var user = testDataManager.fetchUserDtos( userId ).getFirst();
            final var uri = String.format( "/users/%s", userId );
            final var jsonResponse = new ObjectMapper().writeValueAsString( user );
            mockWebClientSuccessResponse( uri, Mono.just( jsonResponse ), UriType.FUNCTION );
        }
    }

    /**
     * Mocks a WebClient error response for the given URI string or URI object. <br>
     * <br>
     * Please note that the URI can be mocked using either a String or a {@linkplain java.net.URI URI} object, but not both. <br>
     * If both are provided, the URI string will take precedence.
     * @param uriString the URI string to mock the error response for (should be null if uriUri is provided)
     * @param uriUri the URI object to mock the error response for (should be null if uriString is provided)
     * @param responseCode the HTTP response code to simulate
     */
    private void mockWebClientErrorResponse(final String uriString, final URI uriUri, final int responseCode, final UriType uriType) {
        final var requestHeadersUriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        final var requestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        final var responseSpec = Mockito.mock(WebClient.ResponseSpec.class);

        Mockito.doReturn(requestHeadersUriSpec).when(webClient).get();

        switch (uriType) {
            case URI -> {
                Mockito.doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(uriUri);
            }
            case STRING -> {
                Mockito.doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(uriString);
            }
            case FUNCTION -> {
                Mockito.doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(Mockito.any(Function.class));
            }
        }
        if( mockHeader ){
            Mockito.doReturn( requestHeadersSpec ).when( requestHeadersSpec ).header( Mockito.anyString(), Mockito.any() );
        }
        Mockito.doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        Mockito.doReturn(Mono.error(new WebClientResponseException(responseCode, "Error", null, null, null))).when(responseSpec)
                .bodyToMono(String.class);
    }

    /**
     * Mocks a WebClient error response for the given URI string.
     * @param uri the URI string to mock the error response for
     * @param responseCode the HTTP response code to simulate
     */
    private void mockWebClientErrorResponse(final String uri, int responseCode) {
        mockWebClientErrorResponse(uri, null, responseCode, UriType.STRING);
    }

    /**
     * Mocks a WebClient error response for the given URI object.
     * @param uri the URI object to mock the error response for
     * @param responseCode the HTTP response code to simulate
     */
    private void mockWebClientErrorResponse( final URI uri, int responseCode, final UriType uriType){
        mockWebClientErrorResponse(null, uri, responseCode, uriType);
    }

    public void mockWebClientForFetchUserDetailsErrorResponse( final String userId, int responseCode ){
        final var uri = URI.create( String.format( "/users/%s", userId ) );
        mockWebClientErrorResponse( uri, responseCode, UriType.FUNCTION );
    }

    public void mockWebClientForFetchUserDetailsNotFound( final String... userIds ){
        for ( final String userId: userIds ){
            mockWebClientForFetchUserDetailsErrorResponse( userId,404 );
        }
    }

    /**
     * Mocks a WebClient JSON parsing error for the given URI string or URI object. <br>
     * <br>
     * Please note that the URI can be mocked using either a String or a URI object, but not both.
     * @param uriString the URI string to mock the JSON parsing error for (should be null if uriUri is provided)
     * @param uriUri the URI object to mock the JSON parsing error for (should be null if uriString is provided)
     */
    private void mockWebClientJsonParsingError(final String uriString, final URI uriUri, final UriType uriType) {
        final var requestHeadersUriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        final var requestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        final var responseSpec = Mockito.mock(WebClient.ResponseSpec.class);

        Mockito.doReturn(requestHeadersUriSpec).when(webClient).get();
        switch (uriType) {
            case URI -> {
                Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uriUri );
            }
            case STRING -> {
                Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uriString );
            }
            case FUNCTION -> {
                Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( Mockito.any( Function.class ) );
            }
        }
        if( mockHeader ){
            Mockito.doReturn( requestHeadersSpec ).when( requestHeadersSpec ).header( Mockito.anyString(), Mockito.any() );
        }
        Mockito.doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        Mockito.doReturn(Mono.just("}{")).when(responseSpec).bodyToMono(String.class);
    }

    /**
     * Mocks a WebClient JSON parsing error for the given URI string or URI object. <br>
     * @param uri the URI string or URI object to mock the JSON parsing error for
     * @param isUriString indicates whether the uri parameter is a String (true) or a {@linkplain java.net.URI URI} object. (false)
     */
    private void mockWebClientJsonParsingError(final String uri, final boolean isUriString) {
        if (isUriString) {
            mockWebClientJsonParsingError(uri, null, UriType.STRING);
        } else {
            mockWebClientJsonParsingError(null, URI.create(uri), UriType.URI);
        }
    }

    /**
     * Mocks a WebClient JSON parsing error for the given URI string.
     * @param uri the URI string to mock the JSON parsing error for
     */
    private void mockWebClientJsonParsingError(final String uri){
        mockWebClientJsonParsingError(uri, true);
    }

    /**
     * Mocks a WebClient JSON parsing error for the given URI object.
     * @param uri the URI object to mock the JSON parsing error for
     */
    private void mockWebClientJsonParsingError(final URI uri) {
        mockWebClientJsonParsingError(null, uri, UriType.URI);
    }

    /**
     * Mocks a WebClient JSON parsing error for the given UriComponents object.
     * @param uriComponents the UriComponents object to mock the JSON parsing error for
     * @param uriType indicates whether to use the URI (URI) or URI string (STRING) from the UriComponents object, or to use a function to build the URI (FUNCTION)
     */
    private void mockWebClientJsonParsingError(final UriComponents uriComponents, final UriType uriType) {
        switch (uriType) {
            case URI -> {
                mockWebClientJsonParsingError(uriComponents.toUri());
            }
            case STRING -> {
                mockWebClientJsonParsingError(uriComponents.toUriString());
            }
            case FUNCTION -> {
                mockWebClientJsonParsingError(null, uriComponents.toUri(), UriType.FUNCTION);
            }
        }
    }

    public void mockWebClientForFetchUserDetailsJsonParsingError(final String userId, boolean isUriString) {
        mockWebClientUserDetailsJsonParsingError(userId, UriType.FUNCTION);
    }

    public void mockWebClientForSearchUserDetails( final boolean isUriString, final String... userIds )
            throws JsonProcessingException {
        final var users = testDataManager.fetchUserDtos( userIds );
        final var jsonResponse = new ObjectMapper().writeValueAsString( users );

        mockWebClientSuccessResponse( URI.create( "/users/search" ), Mono.just( jsonResponse ) );
    }

    public UriComponents buildUriComponents(final String userEmail){
        final var validatedUserEmail = userEmail == null ? "null" : userEmail;

        return UriComponentsBuilder.newInstance()
                .path("/users/search")
                .queryParam("user_email", "{emails}")
                .encode()
                .buildAndExpand(validatedUserEmail);
    }

    public void mockWebClientForSearchUserDetailsErrorResponse( final String userEmail, int responseCode, final UriType uriType ){
        final var uriComponents = buildUriComponents(userEmail);

        switch (uriType) {
            case URI -> {
                mockWebClientErrorResponse( uriComponents.toUriString(), responseCode );
            }
            case STRING -> {
                mockWebClientErrorResponse( uriComponents.toString(), responseCode );
            }
            case FUNCTION -> {
                mockWebClientJsonParsingError(null, uriComponents.toUri(), UriType.FUNCTION);
            }
        }

    }

    public void mockWebClientForSearchUserDetailsNonexistentEmail(final boolean isUriString, final String... emails) {
        final var uri = UriComponentsBuilder.fromUri(URI.create("https://api.example.com"))
                .path("/users/search")
                .queryParam("user_email", "{emails}")
                .encode()
                .buildAndExpand(String.join(",", emails))
                .toUri();
        mockWebClientSuccessResponse( uri, Mono.empty());
    }

    public void mockWebClientUserDetailsJsonParsingError(final String userId, UriType uriType) {
        final var uri = UriComponentsBuilder.newInstance()
                .path("/users/{user}")
                .encode()
                .buildAndExpand(userId);
        mockWebClientJsonParsingError(uri, uriType);
    }

    public void mockWebClientForSearchUserDetailsJsonParsingError(final String email, UriType uriType) {
        final var uri = UriComponentsBuilder.newInstance()
                .path("/users/search")
                .queryParam("user_email", "{emails}")
                .encode()
                .buildAndExpand(email);
        mockWebClientJsonParsingError(uri, uriType);
    }

    public void mockWebClientForFetchCompanyProfile( final boolean isUriString, final String... companyNumbers ) throws JsonProcessingException {
        for ( final String companyNumber: companyNumbers ){
            final var company = testDataManager.fetchCompanyDetailsDtos( companyNumber ).getFirst();
            final var uri = String.format( "/company/%s/company-detail", companyNumber );
            final var jsonResponse = new ObjectMapper().writeValueAsString( company );
            UriType uriType;
            if (isUriString) {
                uriType = UriType.STRING;
            } else {
                uriType = UriType.FUNCTION;
            }
            mockWebClientSuccessResponse( uri, Mono.just( jsonResponse ), uriType );
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

    public void mockWebClientForFetchCompanyProfileJsonParsingError( final String companyNumber, final boolean isUriString ){
        final var uri = String.format( "/company/%s/company-detail", companyNumber );
        mockWebClientJsonParsingError( uri, isUriString );
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
            Mockito.doReturn( Mono.just( userDetails ) ).when( usersService ).toFetchUserDetailsRequest( eq(userId), any() );
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
            Mockito.doThrow( new NotFoundRuntimeException( "Not found.", new Exception( "Not found." ) ) ).when( usersService ).fetchUserDetails( eq(userId), any() );
        }
    }

    public void setMockHeader( final boolean mockHeader ) {
        this.mockHeader = mockHeader;
    }

}
