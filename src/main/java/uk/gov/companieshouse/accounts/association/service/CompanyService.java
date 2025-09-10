package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonTo;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.BadRequest;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.company.CompanyDetails;

@Service
public class CompanyService {

    private final RestClient companyRestClient;

    private static final String REST_CLIENT_EXCEPTION = "Encountered rest client exception when fetching company details for: %s";
    private static final String BLANK_COMPANY_NUMBER = "Company number cannot be blank";

    @Autowired
    public CompanyService( @Qualifier( "companyRestClient" ) final RestClient companyRestClient) {
        this.companyRestClient = companyRestClient;
    }

    private CompanyDetails fetchCompanyProfileRequest(final String companyNumber, final String xRequestId) {
        final var uri = UriComponentsBuilder.newInstance()
                .path("/company/{companyNumber}/company-detail")
                .buildAndExpand(companyNumber)
                .toUri();

        LOGGER.infoContext(xRequestId, String.format("Starting request to %s. Attempting to retrieve company profile for company: %s", uri, companyNumber), null);

        final var response = companyRestClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        LOGGER.infoContext(xRequestId, String.format("Finished request to %s for company: %s.", uri, companyNumber), null);

        return parseJsonTo(response, CompanyDetails.class);
    }

    public Map<String, CompanyDetails> fetchCompanyProfiles(final Stream<AssociationDao> associations) {
        final var xRequestId = getXRequestId();
        final var companyDetailsMap = new ConcurrentHashMap<String, CompanyDetails>();

        associations.parallel().map(AssociationDao::getCompanyNumber).forEach(companyNumber -> {
            final var companyDetails = fetchCompanyProfile(companyNumber, xRequestId);
            companyDetailsMap.put(companyNumber, companyDetails);
        });
        return companyDetailsMap;
    }

    public CompanyDetails fetchCompanyProfile(final String companyNumber) {
        return fetchCompanyProfile(companyNumber, getXRequestId());
    }

    public CompanyDetails fetchCompanyProfile(final String companyNumber, String xRequestId) {
        xRequestId = xRequestId != null ? xRequestId : getXRequestId();
        if (StringUtils.isBlank(companyNumber)) {
            NotFoundRuntimeException exception = new NotFoundRuntimeException(BLANK_COMPANY_NUMBER, new Exception(BLANK_COMPANY_NUMBER));
            LOGGER.errorContext(xRequestId, BLANK_COMPANY_NUMBER, exception, null);
            throw exception;
        }

        CompanyDetails companyDetails;
        try {
            companyDetails = fetchCompanyProfileRequest(companyNumber, xRequestId);
        } catch (NotFound exception) {
            LOGGER.infoContext(xRequestId, String.format("No company found for email: %s", companyNumber), null);
            throw new NotFoundRuntimeException(exception.getMessage(), exception);
        } catch (BadRequest exception) {
            LOGGER.errorContext(xRequestId,String.format("Bad request made when searching for company: %s", companyNumber), exception, null);
            throw new InternalServerErrorRuntimeException(exception.getMessage(), exception);
        } catch (RestClientException exception) {
            LOGGER.errorContext(xRequestId, String.format(REST_CLIENT_EXCEPTION, companyNumber), exception, null);
            throw new InternalServerErrorRuntimeException(exception.getMessage(), exception);
        }

        return companyDetails;
    }
}



