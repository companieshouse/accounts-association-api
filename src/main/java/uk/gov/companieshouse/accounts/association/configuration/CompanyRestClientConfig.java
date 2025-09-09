package uk.gov.companieshouse.accounts.association.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class CompanyRestClientConfig {

    @Value( "${private.api.url}" )
    private String privateApiUrl;

    @Value( "${chs.internal.api.key}" )
    private String chsInternalApiKey;

    @Bean
    public RestClient companyWebClient(){
        return RestClient.builder()
                .baseUrl( privateApiUrl )
                .defaultHeader( "Authorization", chsInternalApiKey )
                .build();
    }

}
