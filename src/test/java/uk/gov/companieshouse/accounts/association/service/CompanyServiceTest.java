package uk.gov.companieshouse.accounts.association.service;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
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
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class CompanyServiceTest {

    @Mock
    private CompanyProfileEndpoint companyProfileEndpoint;

    @InjectMocks
    private CompanyService companyService;

    private Mockers mockers;

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
        Mockito.doThrow( new URIValidationException( "Uri incorrectly formatted" ) ).when( companyProfileEndpoint ).fetchCompanyProfile( any() );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfile( "$" ) );
    }

    @Test
    void fetchCompanyProfileReturnsInternalServerErrorWhenItReceivesApiErrorResponseWithNon404StatusCode() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 500, "Something unexpected happened", new HttpHeaders() ) ) ).when( companyProfileEndpoint ).fetchCompanyProfile( any() );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfile( "NI038379" ) );
    }

    @Test
    void fetchCompanyProfileSuccessfullyFetchesCompanyData() throws ApiErrorResponseException, URIValidationException {
        mockers.mockFetchCompanyProfile( "111111" );
        Assertions.assertEquals( "111111", companyService.fetchCompanyProfile( "111111" ).getCompanyNumber() );
    }

    @Test
    void fetchCompanyProfileReturnsInternalServerErrorForUnexpectedCase() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new NullPointerException() ).when( companyProfileEndpoint ).fetchCompanyProfile( any() );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfile( "xxx" ) );
    }

}
