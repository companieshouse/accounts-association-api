package uk.gov.companieshouse.accounts.association.utils;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;


@Component
public class ApiClientUtil {


    public InternalApiClient getInternalApiClient(final String internalApiUrl) {
        final var internalApiClient = ApiSdkManager.getInternalSDK();
        internalApiClient.setInternalBasePath(internalApiUrl);
        return internalApiClient;
    }

}