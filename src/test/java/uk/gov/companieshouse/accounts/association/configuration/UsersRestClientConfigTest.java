package uk.gov.companieshouse.accounts.association.configuration;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@Tag("unit-test")
class UsersRestClientConfigTest {

    @Test
    void webClientIsCreatedCorrectly() {
        Assertions.assertTrue(RestClient.class.isAssignableFrom(new UsersRestClientConfig().usersRestClient().getClass()));
    }

}
