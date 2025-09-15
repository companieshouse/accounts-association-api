package uk.gov.companieshouse.accounts.association.integration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.accounts.association.configuration.MongoConfig;

@Import(MongoConfig.class)
@SpringBootTest(webEnvironment= WebEnvironment.RANDOM_PORT)
public abstract class AbstractBaseIntegrationTest {

    private static final MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0").withExposedPorts(27017);

    // without this the container is recreated, ports change, but properties stay the same, so we can't connect
    // https://stackoverflow.com/a/74274007
    static {
        mongoDBContainer.start();
    }

    @DynamicPropertySource
    static void containersProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
}
