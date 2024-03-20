package uk.gov.companieshouse.accounts.association.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.rest.CompanyProfileEndpoint;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class CompanyService {

    @Value("${spring.application.name}")
    public static String applicationNameSpace;

    private final CompanyProfileEndpoint companyProfileEndpoint;

    private static final Logger LOG = LoggerFactory.getLogger(applicationNameSpace);

    @Autowired
    public CompanyService(CompanyProfileEndpoint companyProfileEndpoint) {
        this.companyProfileEndpoint = companyProfileEndpoint;
    }

    public CompanyDetails fetchCompanyProfile(final String companyNumber) {

        try {
            LOG.debug(String.format("Attempting to fetch company profile for company %s", companyNumber));
            return companyProfileEndpoint.fetchCompanyProfile(companyNumber)
                    .getData();
        } catch (ApiErrorResponseException exception) {
            if (exception.getStatusCode() == 404) {
                LOG.error(String.format("Could not find company profile for company %s", companyNumber));
                throw new NotFoundRuntimeException("accounts-association-api", "Failed to find company");
            } else {
                LOG.error(String.format("Failed to retrieve company profile for company %s", companyNumber));
                throw new InternalServerErrorRuntimeException("Failed to retrieve company profile");
            }
        } catch (URIValidationException exception) {
            LOG.error(String.format("Failed to fetch company profile for company %s, because uri was incorrectly formatted", companyNumber));
            throw new InternalServerErrorRuntimeException("Invalid uri for company-profile-api service");
        } catch (Exception exception) {
            LOG.error(String.format("Failed to retrieve company profile for company %s", companyNumber));
            throw new InternalServerErrorRuntimeException("Failed to retrieve company profile");
        }

    }

}
