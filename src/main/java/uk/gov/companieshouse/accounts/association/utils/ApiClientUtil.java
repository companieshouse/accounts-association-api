package uk.gov.companieshouse.accounts.association.utils;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.InternalApiClient;


@Component
public class ApiClientUtil {

    @Autowired
    InternalApiClient internalApiClient;


    public InternalApiClient getInternalApiClient(final String internalApiUrl) {
        internalApiClient.setInternalBasePath(internalApiUrl);
        return internalApiClient;
    }

}