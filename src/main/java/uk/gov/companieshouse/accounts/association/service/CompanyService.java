package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonTo;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.company.CompanyDetails;

@Service
public class CompanyService {

    private final RestClient companyRestClient;

    private static final String REST_CLIENT_EXCEPTION = "Encountered rest client exception when fetching company details for: %s";

    @Autowired
    public CompanyService( @Qualifier( "companyRestClient" ) final RestClient companyRestClient) {
        this.companyRestClient = companyRestClient;
    }

    private CompanyDetails fetchCompanyProfileRequest( final String companyNumber, final String xRequestId ) {
        if (StringUtils.isBlank(companyNumber)) {
            IllegalArgumentException exception = new IllegalArgumentException("CompanyNumber cannot be blank");
            LOGGER.errorContext(xRequestId, "CompanyNumber cannot be blank", exception, null);
            throw exception;
        }

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

        return parseJsonTo(CompanyDetails.class).apply(response);
    }

    public Map<String, CompanyDetails> fetchCompanyProfiles( final Stream<AssociationDao> associations ) {
        final var xRequestId = getXRequestId();
        final var companyDetailsMap = new ConcurrentHashMap<String, CompanyDetails>();

        associations.parallel()
                .map(AssociationDao::getCompanyNumber)
                .forEach(companyNumber -> {
                    try {
                        final var companyDetails = fetchCompanyProfileRequest(companyNumber, xRequestId);
                        companyDetailsMap.put(companyNumber, companyDetails);
                    } catch (RestClientException exception) {
                        LOGGER.errorContext(xRequestId, String.format(REST_CLIENT_EXCEPTION, companyNumber), exception, null);
                    }
                });
        return companyDetailsMap;
    }
}



