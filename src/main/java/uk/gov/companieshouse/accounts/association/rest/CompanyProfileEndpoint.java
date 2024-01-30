package uk.gov.companieshouse.accounts.association.rest;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;
import uk.gov.companieshouse.api.model.company.CompanyProfileApi;

@Service
public class CompanyProfileEndpoint {

    private final InternalApiClient internalApiClient;

    public CompanyProfileEndpoint(InternalApiClient internalApiClient) {
        this.internalApiClient = internalApiClient;
    }

    public ApiResponse<CompanyProfileApi> fetchCompanyProfile( final String companyNumber ) throws ApiErrorResponseException, URIValidationException {
        final var getCompanyProfileUrl = String.format("/company/%s", companyNumber );
        return internalApiClient.company()
                                .get( getCompanyProfileUrl )
                                .execute();
    }

}
