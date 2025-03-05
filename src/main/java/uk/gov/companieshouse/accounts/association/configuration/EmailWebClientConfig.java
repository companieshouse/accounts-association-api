package uk.gov.companieshouse.accounts.association.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class EmailWebClientConfig {

    @Value( "${internal.api.url}" )
    private String internalApiUrl;

    @Value( "${chs.internal.api.key}" )
    private String chsInternalApiKey;

    @Bean
    public WebClient emailWebClient(){
        return WebClient.builder()
                .baseUrl( internalApiUrl )
                .defaultHeader( "Authorization", chsInternalApiKey )
                .build();
    }

}
