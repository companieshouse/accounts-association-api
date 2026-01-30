package uk.gov.companieshouse.accounts.association.integration;

import org.junit.jupiter.api.BeforeAll;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

abstract class BaseMongoIntegration {

    public static final MongoDBContainer mongoDBContainer = new MongoDBContainer( DockerImageName.parse( "mongo:7.0.17-jammy" ) );

    @DynamicPropertySource
    public static void setProperties( final DynamicPropertyRegistry registry ) {
        registry.add( "spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl );
        mongoDBContainer.start();
    }

    @BeforeAll
    static void init(){
        mongoDBContainer.start();
    }
}