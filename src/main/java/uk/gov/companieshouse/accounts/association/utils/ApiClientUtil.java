package uk.gov.companieshouse.accounts.association.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;


@Component
public class ApiClientUtil {

    @Value("${chs.internal.api.key}")
    private String internalApiKey;

    public InternalApiClient getInternalApiClient(final String internalApiUrl) {
        final var internalApiClient = new InternalApiClient(new ApiKeyHttpClient(internalApiKey));
        internalApiClient.setInternalBasePath(internalApiUrl);
        return internalApiClient;
    }

}