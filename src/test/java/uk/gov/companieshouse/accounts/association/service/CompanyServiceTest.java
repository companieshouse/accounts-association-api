package uk.gov.companieshouse.accounts.association.service;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
import java.util.Map;
import java.util.stream.Stream;
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
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.rest.CompanyProfileEndpoint;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.company.request.PrivateCompanyDetailsGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class CompanyServiceTest {

    @Mock
    private CompanyProfileEndpoint companyProfileEndpoint;

    @InjectMocks
    private CompanyService companyService;

    @Mock
    private PrivateCompanyDetailsGet privateCompanyDetailsGet;

    private Mockers mockers;

    private TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup(){
        mockers = new Mockers( null, companyProfileEndpoint, null, null, null );
    }

    @Test
    void fetchCompanyProfileWithNullOrNonExistentCompanyNumberReturnsNotFound() throws ApiErrorResponseException, URIValidationException {
        mockers.mockFetchCompanyProfileNotFound( null, "", "111111" );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfile( null ) );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfile( "" ) );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfile( "111111" ) );
    }

    @Test
    void fetchCompanyProfileWithMalformedUriReturnsInternalServerError() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( privateCompanyDetailsGet ).when( companyProfileEndpoint ).createFetchCompanyProfileRequest( any() );
        Mockito.doThrow( new URIValidationException( "Uri incorrectly formatted" ) ).when( privateCompanyDetailsGet ).execute();
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfile( "$" ) );
    }

    @Test
    void fetchCompanyProfileReturnsInternalServerErrorWhenItReceivesApiErrorResponseWithNon404StatusCode() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( privateCompanyDetailsGet ).when( companyProfileEndpoint ).createFetchCompanyProfileRequest( any() );
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 500, "Something unexpected happened", new HttpHeaders() ) ) ).when( privateCompanyDetailsGet ).execute();
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfile( "NI038379" ) );
    }

    @Test
    void fetchCompanyProfileSuccessfullyFetchesCompanyData() throws ApiErrorResponseException, URIValidationException {
        mockers.mockFetchCompanyProfile( "111111" );
        Assertions.assertEquals( "111111", companyService.fetchCompanyProfile( "111111" ).getCompanyNumber() );
    }

    @Test
    void fetchCompanyProfileReturnsInternalServerErrorForUnexpectedCase() throws ApiErrorResponseException, URIValidationException {
        Mockito.doReturn( privateCompanyDetailsGet ).when( companyProfileEndpoint ).createFetchCompanyProfileRequest( any() );
        Mockito.doThrow( new NullPointerException() ).when( privateCompanyDetailsGet ).execute();
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfile( "xxx" ) );
    }

    @Test
    void fetchCompanyProfilesWithNullThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> companyService.fetchCompanyProfiles( null ) );
    }

    @Test
    void fetchCompanyProfilesWithEmptyStreamReturnsEmptyMap(){
        Assertions.assertEquals( Map.of(), companyService.fetchCompanyProfiles( Stream.of() ) );
    }

    @Test
    void fetchCompanyProfilesRetrievesCompanyDetails() throws ApiErrorResponseException, URIValidationException {
        final var associationDaos = testDataManager.fetchAssociationDaos( "1", "2" );

        final var company = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();

        Mockito.doReturn( privateCompanyDetailsGet ).when( companyProfileEndpoint ).createFetchCompanyProfileRequest( any() );

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), company );
        Mockito.doReturn( intendedResponse ).when( privateCompanyDetailsGet ).execute();

        Assertions.assertEquals( Map.of( "111111", company ), companyService.fetchCompanyProfiles( associationDaos.stream() ) );
    }

}
