package uk.gov.companieshouse.accounts.association.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;

@Service
public class ApiClientService {

    private final String internalApiUrl;

    public ApiClientService(@Value("${internal.api.url}") String internalApiUrl) {
        this.internalApiUrl = internalApiUrl;
    }

    public InternalApiClient getInternalApiClient() {
        final var internalApiClient = ApiSdkManager.getInternalSDK();
        internalApiClient.setInternalBasePath(internalApiUrl);
        return internalApiClient;
    }

}