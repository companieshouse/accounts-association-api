package uk.gov.companieshouse.accounts.association.rest;

import com.google.api.client.http.HttpHeaders;
import com.google.api.client.http.HttpResponseException.Builder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.common.Mockers;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class CompanyProfileEndpointTest {

    @Mock
    private CompanyProfileEndpoint companyProfileEndpoint;

    private Mockers mockers;

    @BeforeEach
    void setup(){
        mockers = new Mockers( null, companyProfileEndpoint, null, null, null );
    }

    @Test
    void fetchCompanyProfileWithMalformedCompanyNumberThrowsApiErrorResponseException() throws ApiErrorResponseException, URIValidationException {
        Mockito.doThrow( new ApiErrorResponseException( new Builder( 404, "Not Found", new HttpHeaders() ) ) ).when( companyProfileEndpoint ).fetchCompanyProfile( any() );
        Assertions.assertThrows( ApiErrorResponseException.class, () -> companyProfileEndpoint.fetchCompanyProfile( null ) );
        Assertions.assertThrows( ApiErrorResponseException.class, () -> companyProfileEndpoint.fetchCompanyProfile( "" ) );
        Assertions.assertThrows( ApiErrorResponseException.class, () -> companyProfileEndpoint.fetchCompanyProfile( "abc" ) );
    }

    @Test
    void fetchCompanyProfileFetchesCompanyProfileForCompanyNumber() throws ApiErrorResponseException, URIValidationException {
        final var companyProfileApi = new CompanyDetails().companyName( "THE POLISH BREWERY" );
        final var intendedResponse = new ApiResponse<>( 200, Map.of(), companyProfileApi );
        Mockito.doReturn( intendedResponse ).when( companyProfileEndpoint ).fetchCompanyProfile( any() );

        final var response = companyProfileEndpoint.fetchCompanyProfile( "NI038379" );
        Assertions.assertEquals( 200, response.getStatusCode() );
        Assertions.assertEquals( "THE POLISH BREWERY", response.getData().getCompanyName() );
    }

}