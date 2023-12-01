package uk.gov.companieshouse.authentication.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import uk.gov.ch.oauth.IOAuthCoordinator;
import uk.gov.ch.oauth.OAuthCoordinator;
import uk.gov.companieshouse.authentication.AuthenticationServiceApplication;

@Configuration
public class OauthBean {

    @Bean
    public IOAuthCoordinator oAuthCoordinator() {
        return new OAuthCoordinator(AuthenticationServiceApplication.APPLICATION_NAME_SPACE);
    }

}
