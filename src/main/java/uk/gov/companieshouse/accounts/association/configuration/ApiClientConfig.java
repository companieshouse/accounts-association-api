package uk.gov.companieshouse.accounts.association.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;

@Configuration
public class ApiClientConfig {

    @Value("${chs.internal.api.key}")
    private String internalApiKey;

    @Bean
    public InternalApiClient getInternalApiClient(){
        return new InternalApiClient(new ApiKeyHttpClient(internalApiKey));
    }

}
