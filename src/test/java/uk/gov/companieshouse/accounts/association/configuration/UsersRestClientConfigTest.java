package uk.gov.companieshouse.accounts.association.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

@Tag("unit-test")
class UsersRestClientConfigTest {

    @Test
    void webClientIsCreatedCorrectly(){
        Assertions.assertTrue(WebClient.class.isAssignableFrom(new UsersRestClientConfig().usersRestClient().getClass()));
    }

}
