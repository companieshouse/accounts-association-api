package uk.gov.companieshouse.accounts.association.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.companieshouse.api.ApiClient;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.sdk.ApiClientService;
import uk.gov.companieshouse.api.sdk.impl.ApiClientServiceImpl;

@Configuration
public class ApiClientConfig {

    @Bean
    public ApiClientService apiClientService(){
        return new ApiClientServiceImpl();
    }

    @Bean
    public ApiClient apiClient( final ApiClientService apiClientService ){
        return apiClientService.getApiClient();
    }

    @Bean
    public InternalApiClient internalApiClient( final ApiClientService apiClientService ){
        return apiClientService.getInternalApiClient();
    }

}
