package uk.gov.companieshouse.accounts.association.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class CompanyWebClientConfig {

    @Value( "${private.api.url}" )
    private String privateApiUrl;

    @Value( "${chs.internal.api.key}" )
    private String chsInternalApiKey;

    @Bean
    public WebClient companyWebClient(){
        return WebClient.builder()
                .baseUrl( privateApiUrl )
                .defaultHeader( "Authorization", chsInternalApiKey )
                .build();
    }

}
