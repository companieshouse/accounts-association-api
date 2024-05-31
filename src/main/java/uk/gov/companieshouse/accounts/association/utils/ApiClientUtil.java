package uk.gov.companieshouse.accounts.association.utils;

import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.sdk.manager.ApiSdkManager;


@Component
public class ApiClientUtil {


    public InternalApiClient getInternalApiClient(final String internalApiUrl) {
        final var internalApiClient = ApiSdkManager.getInternalSDK();
        internalApiClient.setInternalBasePath(internalApiUrl);
        return internalApiClient;
    }

    public InternalApiClient getInternalApiClientForSession( final String internalApiUrl ) throws IOException {
        final var requestAttributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        final var request = requestAttributes.getRequest();
        final var ericPassthroughTokenHeader = request.getHeader( ApiSdkManager.getEricPassthroughTokenHeader() );
        final var internalApiClient = ApiSdkManager.getPrivateSDK( ericPassthroughTokenHeader );
        internalApiClient.setInternalBasePath(internalApiUrl);
        return internalApiClient;
    }

}