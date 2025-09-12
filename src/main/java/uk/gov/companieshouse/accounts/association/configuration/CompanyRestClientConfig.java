package uk.gov.companieshouse.accounts.association.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class CompanyRestClientConfig {

    @Value("${private.api.url}")
    private String privateApiUrl;

    @Value("${chs.internal.api.key}")
    private String chsInternalApiKey;

    @Bean(name = "companyRestClient")
    public RestClient companyRestClient(){
        return RestClient.builder()
                .baseUrl(privateApiUrl)
                .defaultHeader("Authorization", chsInternalApiKey)
                .build();
    }

}
