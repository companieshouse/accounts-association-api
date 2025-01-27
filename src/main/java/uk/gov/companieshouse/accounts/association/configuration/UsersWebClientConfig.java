package uk.gov.companieshouse.accounts.association.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class UsersWebClientConfig {

    @Value( "${account.api.url}" )
    private String accountApiUrl;

    @Value( "${chs.internal.api.key}" )
    private String chsInternalApiKey;

    @Bean
    public WebClient usersWebClient(){
        return WebClient.builder()
                .baseUrl( accountApiUrl )
                .defaultHeader( "Authorization", chsInternalApiKey )
                .build();
    }

}
