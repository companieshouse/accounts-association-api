package uk.gov.companieshouse.accounts.association.configuration;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Field;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.http.ApiKeyHttpClient;

@Tag("unit-test")
@ExtendWith(MockitoExtension.class)
class EmailInternalApiClientConfigTest {

    private static final String API_KEY = "test-api-key";
    private static final String API_URL = "https://api.test.com";

    private EmailInternalApiClientConfig config;

    @BeforeEach
    void setUp() {
        config = new EmailInternalApiClientConfig();
    }

    @Test
    void internalApiClientSupplierCreatesClientCorrectly() {
        // Arrange
        String apiKey = API_KEY;
        String apiUrl = API_URL;

        // Act
        Supplier<InternalApiClient> supplier = config.internalApiClientSupplier(apiKey, apiUrl);
        InternalApiClient internalApiClient = supplier.get();

        // Assert
        assertNotNull(internalApiClient);
        assertEquals(apiUrl, internalApiClient.getBasePath());
    }

    @Test
    void shouldResolveApiKeyPlaceholderWithoutStrayBraces() {
        contextRunner()
                .withPropertyValues( "chs.internal.api.key=" + API_KEY, "chs.kafka.api.url=" + API_URL )
                .run( context -> {
                    assertThat( context ).hasNotFailed();

                    final var resolvedApiKey = extractApiKey( internalApiClient( context.getBean( Supplier.class ) ) );

                    assertEquals( API_KEY, resolvedApiKey );
                    assertFalse( resolvedApiKey.contains( "}" ), "Resolved API key contains a stray closing brace" );
                    assertFalse( resolvedApiKey.contains( "${" ), "API key placeholder was not resolved" );
                } );
    }

    @Test
    void shouldResolveKafkaApiUrlPlaceholderWithoutStrayBraces() {
        contextRunner()
                .withPropertyValues( "chs.internal.api.key=" + API_KEY, "chs.kafka.api.url=" + API_URL )
                .run( context -> {
                    assertThat( context ).hasNotFailed();

                    final var basePath = internalApiClient( context.getBean( Supplier.class ) ).getBasePath();

                    assertEquals( API_URL, basePath );
                    assertFalse( basePath.contains( "}" ), "Resolved base path contains a stray closing brace" );
                    assertFalse( basePath.contains( "${" ), "Kafka API URL placeholder was not resolved" );
                } );
    }

    @Test
    void shouldFailToStartWhenApiKeyPropertyIsMissing() {
        contextRunner()
                .withPropertyValues( "chs.kafka.api.url=" + API_URL )
                .run( context -> assertThat( context ).hasFailed() );
    }

    private ApplicationContextRunner contextRunner() {
        return new ApplicationContextRunner()
                .withBean( PropertySourcesPlaceholderConfigurer.class )
                .withUserConfiguration( EmailInternalApiClientConfig.class );
    }

    @SuppressWarnings( "unchecked" )
    private InternalApiClient internalApiClient( final Supplier<?> supplier ) {
        return ( (Supplier<InternalApiClient>) supplier ).get();
    }

    private String extractApiKey( final InternalApiClient internalApiClient ) throws Exception {
        final Field apiKeyField = ApiKeyHttpClient.class.getDeclaredField( "apiKey" );
        apiKeyField.setAccessible( true );
        return (String) apiKeyField.get( internalApiClient.getHttpClient() );
    }
}