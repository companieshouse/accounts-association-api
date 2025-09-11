package uk.gov.companieshouse.accounts.association.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class UsersRestClientConfig {

    @Value("${account.api.url}")
    private String accountApiUrl;

    @Value("${chs.internal.api.key}")
    private String chsInternalApiKey;

    @Bean
    public RestClient usersRestClient(){
        return RestClient.builder()
                .baseUrl(accountApiUrl)
                .defaultHeader("Authorization", chsInternalApiKey)
                .build();
    }

}
