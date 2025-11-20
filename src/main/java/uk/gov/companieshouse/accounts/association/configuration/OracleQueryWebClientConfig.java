package uk.gov.companieshouse.accounts.association.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OracleQueryWebClientConfig {
    @Value( "${oracle.query.api.url}" )
    private String oracleQueryApiUrl;

    @Value( "${chs.internal.api.key}" )
    private String chsInternalApiKey;

    @Bean
    public WebClient oracleQueryWebClient(){
        return WebClient.builder()
                .baseUrl( oracleQueryApiUrl )
                .defaultHeader( "Authorization", chsInternalApiKey )
                .build();
    }
}
