package uk.gov.companieshouse.accounts.association.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.company.CompanyDetails;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class ReactiveCompanyServiceTest {

    @Mock
    private WebClient companyWebClient;

    @InjectMocks
    private ReactiveCompanyService companyService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private void mockWebClientSuccessResponse( final String uri, final Mono<String> jsonResponse ){
        final var requestHeadersUriSpec = Mockito.mock( WebClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        Mockito.doReturn( requestHeadersUriSpec ).when( companyWebClient ).get();
        Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uri );
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( jsonResponse ).when( responseSpec ).bodyToMono( String.class );
    }

    private void mockWebClientForFetchCompanyProfile( final String companyNumber ) throws JsonProcessingException {
        final var company = testDataManager.fetchCompanyDetailsDtos( companyNumber ).getFirst();
        final var uri = String.format( "/company/%s/company-detail", companyNumber );
        final var jsonResponse = new ObjectMapper().writeValueAsString( company );
        mockWebClientSuccessResponse( uri, Mono.just( jsonResponse ) );
    }

    private void mockWebClientErrorResponse( final String uri, int responseCode ){
        final var requestHeadersUriSpec = Mockito.mock( WebClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        Mockito.doReturn( requestHeadersUriSpec ).when( companyWebClient ).get();
        Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uri );
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( Mono.error( new WebClientResponseException( responseCode, "Error", null, null, null ) ) ).when( responseSpec ).bodyToMono( String.class );
    }

    private void mockWebClientForFetchCompanyProfileErrorResponse( final String companyNumber, int responseCode ){
        final var uri = String.format( "/company/%s/company-detail", companyNumber );
        mockWebClientErrorResponse( uri, responseCode );
    }

    private void mockWebClientJsonParsingError( final String uri ){
        final var requestHeadersUriSpec = Mockito.mock( WebClient.RequestHeadersUriSpec.class );
        final var requestHeadersSpec = Mockito.mock( WebClient.RequestHeadersSpec.class );
        final var responseSpec = Mockito.mock( WebClient.ResponseSpec.class );

        Mockito.doReturn( requestHeadersUriSpec ).when(companyWebClient).get();
        Mockito.doReturn( requestHeadersSpec ).when( requestHeadersUriSpec ).uri( uri );
        Mockito.doReturn( responseSpec ).when( requestHeadersSpec ).retrieve();
        Mockito.doReturn( Mono.just( "}{" ) ).when( responseSpec ).bodyToMono( String.class );
    }

    private void mockWebClientForFetchCompanyProfileJsonParsingError( final String companyNumber ){
        final var uri = String.format( "/company/%s/company-detail", companyNumber );
        mockWebClientJsonParsingError( uri );
    }

    @Test
    void fetchCompanyProfileForNullOrMalformedOrNonexistentCompanyReturnsNotFoundRuntimeException() {
        mockWebClientForFetchCompanyProfileErrorResponse( null, 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfile( null ) );

        mockWebClientForFetchCompanyProfileErrorResponse( "!@£", 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfile( "!@£" ) );

        mockWebClientForFetchCompanyProfileErrorResponse( "404COMP", 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfile( "404COMP" ) );
    }

    @Test
    void fetchCompanyProfileWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        mockWebClientForFetchCompanyProfileJsonParsingError( "111111" );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfile( "111111" ) );
    }

    @Test
    void fetchCompanyProfileReturnsSpecifiedCompany() throws JsonProcessingException {
        mockWebClientForFetchCompanyProfile( "111111" );
        Assertions.assertEquals( "Wayne Enterprises", companyService.fetchCompanyProfile( "111111" ).getCompanyName() );
    }

    @Test
    void fetchCompanyProfilesWithNullStreamThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> companyService.fetchCompanyProfiles( null ) );
    }

    @Test
    void fetchCompanyProfilesWithEmptyStreamReturnsEmptyMap() {
        Assertions.assertEquals( 0, companyService.fetchCompanyProfiles( Stream.of() ).size() );
    }

    @Test
    void fetchCompanyProfilesWithStreamThatHasNonExistentCompanyReturnsNotFoundRuntimeException(){
        final var associationDao = new AssociationDao();
        associationDao.setCompanyNumber( "404COMP" );
        mockWebClientForFetchCompanyProfileErrorResponse( "404COMP", 404 );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfiles( Stream.of( associationDao ) ) );
    }

    @Test
    void fetchCompanyProfilesWithStreamThatHasMalformedCompanyNumberReturnsInternalServerErrorRuntimeException(){
        final var associationDao = new AssociationDao();
        associationDao.setCompanyNumber( "£$@123" );
        mockWebClientForFetchCompanyProfileErrorResponse( "£$@123", 400 );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfiles( Stream.of( associationDao ) ) );
    }

    @Test
    void fetchCompanyProfilesWithStreamWithArbitraryErrorReturnsInternalServerErrorRuntimeException(){
        final var associationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        mockWebClientForFetchCompanyProfileJsonParsingError( "111111" );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfiles( Stream.of( associationDao ) ) );
    }

    @Test
    void fetchCompanyProfilesWithStreamReturnsMap() throws JsonProcessingException {
        final var associationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        mockWebClientForFetchCompanyProfile( "111111" );
        final var companies = companyService.fetchCompanyProfiles( Stream.of( associationDao, associationDao ) );

        Assertions.assertEquals( 1, companies.size() );
        Assertions.assertTrue( companies.containsKey( "111111" ) );
        Assertions.assertTrue( companies.values().stream().map( CompanyDetails::getCompanyNumber ).toList().contains( "111111" ) );
    }

}
