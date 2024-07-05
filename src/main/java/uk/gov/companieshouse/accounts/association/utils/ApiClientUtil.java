package uk.gov.companieshouse.accounts.association.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;


@Component
public class ApiClientUtil {

    @Value("${chs.internal.api.key}")
    private String internalApiKey;

    protected static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    public InternalApiClient getInternalApiClient(final String internalApiUrl) {
        final var internalApiClient = new InternalApiClient(new ApiKeyHttpClient(internalApiKey));
        LOG.infoContext("api key: "+internalApiKey,"", null);
        internalApiClient.setInternalBasePath(internalApiUrl);
        return internalApiClient;
    }

}