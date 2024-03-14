package uk.gov.companieshouse.accounts.association.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.utils.ApiClientUtil;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

@Service
public class CompanyProfileEndpoint {
    @Value("${private.api.url}")
    private String privateApiUrl;

    private final ApiClientUtil apiClientUtil;

    @Autowired
    public CompanyProfileEndpoint(ApiClientUtil apiClientUtil) {
        this.apiClientUtil = apiClientUtil;
    }

    public ApiResponse<CompanyDetails> fetchCompanyProfile(final String companyNumber) throws ApiErrorResponseException, URIValidationException {
        final var getCompanyProfileUrl = String.format("/company/%s/company-detail", companyNumber);

        return apiClientUtil.getInternalApiClient(privateApiUrl)
                .privateCompanyDetailResourceHandler().getCompanyDetails(getCompanyProfileUrl).execute();
    }

}
