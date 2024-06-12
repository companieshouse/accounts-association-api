package uk.gov.companieshouse.accounts.association.service;

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
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.rest.CompanyProfileEndpoint;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class CompanyServiceTest {

    @Mock
    private CompanyProfileEndpoint companyProfileEndpoint;

    @InjectMocks
    private CompanyService companyService;

    @Test
    void fetchCompanyProfileWithNullOrNonExistentCompanyNumberReturnsNotFound() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not found", new HttpHeaders() ) ) ).when( companyProfileEndpoint ).fetchCompanyProfile( any() );

        Assertions.assertThrows( NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfile( null ) );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfile( "" ) );
        Assertions.assertThrows( NotFoundRuntimeException.class, () -> companyService.fetchCompanyProfile( "abc" ) );
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
        final var company = new CompanyDetails();
        company.setCompanyNumber( "111111" );

        final var intendedResponse = new ApiResponse<>( 200, Map.of(), company );
        Mockito.doReturn( intendedResponse ).when( companyProfileEndpoint ).fetchCompanyProfile( any() );
        final var response = companyService.fetchCompanyProfile( "111111" );

        Assertions.assertEquals( "111111", response.getCompanyNumber() );
    }

    @Test
    void fetchCompanyProfileReturnsInternalServerErrorForUnexpectedCase() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new NullPointerException() ).when( companyProfileEndpoint ).fetchCompanyProfile( any() );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> companyService.fetchCompanyProfile( "xxx" ) );
    }

}
