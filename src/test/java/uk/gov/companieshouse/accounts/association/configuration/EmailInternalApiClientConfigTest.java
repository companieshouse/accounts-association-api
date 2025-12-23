package uk.gov.companieshouse.accounts.association.configuration;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("unit-test")
@ExtendWith(MockitoExtension.class)
class EmailInternalApiClientConfigTest {

    private EmailInternalApiClientConfig config;

    @Mock
    private ApiKeyHttpClient apiKeyHttpClient;

    @BeforeEach
    void setUp() {
        config = new EmailInternalApiClientConfig();
    }

    @Test
    void internalApiClientSupplierCreatesClientCorrectly() {
        // Arrange
        String apiKey = "test-api-key";
        String apiUrl = "https://api.test.com";

        // Act
        Supplier<InternalApiClient> supplier = config.internalApiClientSupplier(apiKey, apiUrl);
        InternalApiClient internalApiClient = supplier.get();

        // Assert
        assertNotNull(internalApiClient);
        assertEquals(apiUrl, internalApiClient.getBasePath());
    }
}