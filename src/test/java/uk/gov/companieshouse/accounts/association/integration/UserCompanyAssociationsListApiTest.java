package uk.gov.companieshouse.accounts.association.integration;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.api.accounts.associations.model.CompanyInfo;
import uk.gov.companieshouse.api.accounts.associations.model.UserCompanyAssociationsResponse;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
@Tag("integration-test")
public class UserCompanyAssociationsListApiTest {

    @Container
    @ServiceConnection
    static MongoDBContainer container = new MongoDBContainer("mongo:4.4.22");

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    public MockMvc mockMvc;

    @Autowired
    AssociationsRepository associationsRepository;

    @BeforeEach
    public void setup(){
        final var batmanConfirmedAssociation = new Association();
        batmanConfirmedAssociation.setCompanyNumber( "111111" );
        batmanConfirmedAssociation.setUserId( "111" );
        batmanConfirmedAssociation.setStatus( "Confirmed" );

        final var batmanAwaitingConfirmationAssociation = new Association();
        batmanAwaitingConfirmationAssociation.setCompanyNumber( "222222" );
        batmanAwaitingConfirmationAssociation.setUserId( "111" );
        batmanAwaitingConfirmationAssociation.setStatus( "Awaiting Confirmation" );

        final var batmanRemovedAssociation = new Association();
        batmanRemovedAssociation.setCompanyNumber( "333333" );
        batmanRemovedAssociation.setUserId( "111" );
        batmanRemovedAssociation.setStatus( "Removed" );

        final var batmanGhostAssociation = new Association();
        batmanGhostAssociation.setCompanyNumber( "444444" );
        batmanGhostAssociation.setUserId( "111" );
        batmanGhostAssociation.setStatus( "Confirmed" );

        final var michaelJacksonConfirmedAssociation = new Association();
        michaelJacksonConfirmedAssociation.setCompanyNumber( "111111" );
        michaelJacksonConfirmedAssociation.setUserId( "222" );
        michaelJacksonConfirmedAssociation.setStatus( "Confirmed" );

        final var michaelJacksonRemovedAssociation = new Association();
        michaelJacksonRemovedAssociation.setCompanyNumber( "222222" );
        michaelJacksonRemovedAssociation.setUserId( "222" );
        michaelJacksonRemovedAssociation.setStatus( "Removed" );

        final var devilRemovedAssociation = new Association();
        devilRemovedAssociation.setCompanyNumber( "222222" );
        devilRemovedAssociation.setUserId( "666" );
        devilRemovedAssociation.setStatus( "Removed" );

        associationsRepository.insert( List.of(
                batmanConfirmedAssociation, batmanAwaitingConfirmationAssociation, batmanRemovedAssociation, batmanGhostAssociation,
                michaelJacksonConfirmedAssociation, michaelJacksonRemovedAssociation,
                devilRemovedAssociation ) );
    }

    @Test
    void getCompaniesAssociatedWithUserWithMissingPathVariablesReturnsNotFound() throws Exception {
        mockMvc.perform( get( "/associations/users/{user_email}/companies", "" ).header( "X-Request-Id", "theId" ) ).andExpect( status().isNotFound() );
    }

    @Test
    void getCompaniesAssociatedWithUserWithMalformedOrNonexistentEmailReturnsBadRequest() throws Exception {
        mockMvc.perform( get( "/associations/users/{user_email}/companies", "123" ).header( "X-Request-Id", "theId" ) ).andExpect( status().isBadRequest() );
        mockMvc.perform( get( "/associations/users/{user_email}/companies", "guybrush.threepwood@monkeyisland.com" ).header( "X-Request-Id", "theId" ) ).andExpect( status().isBadRequest() );
    }

    @Test
    void getCompaniesAssociatedWithUserWithNoAssociationsReturnsEmptyList() throws Exception {
        final var response =
        mockMvc.perform( get( "/associations/users/{user_email}/companies", "mr.blobby@nightmare.co.uk" ).queryParam( "include-unauthorized", "true" ).header( "X-Request-Id", "theId" ) )
               .andExpect( status().isOk() )
               .andReturn()
               .getResponse()
               .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        final var companies =
        mapper.readValue( response, UserCompanyAssociationsResponse.class )
              .getSuccessBody();

        Assertions.assertTrue( companies.isEmpty() );
    }

    @Test
    void getCompaniesAssociatedWithUserWithOneRemovedAssociationAndIncludeUnauthorisedIsTrueShouldReturnOneCompany() throws Exception {

        final var response =
        mockMvc.perform( get( "/associations/users/{user_email}/companies", "devil@hell.com" ).queryParam( "include-unauthorized", "true" ).header( "X-Request-Id", "theId" ) )
               .andExpect( status().isOk() )
               .andReturn()
               .getResponse()
               .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        final var companies =
        mapper.readValue( response, UserCompanyAssociationsResponse.class )
              .getSuccessBody();

        Assertions.assertEquals( 1, companies.size() );
        Assertions.assertEquals( "Springfield Nuclear Power Plant", companies.get( 0 ).getCompanyName() );
    }

    @Test
    void getCompaniesAssociatedWithUserWithOneRemovedAssociationAndIncludeUnauthorisedIsFalseShouldReturnEmptyList() throws Exception {

        final var response =
                mockMvc.perform( get( "/associations/users/{user_email}/companies", "devil@hell.com" ).queryParam( "include-unauthorized", "false" ).header( "X-Request-Id", "theId" ) )
                        .andExpect( status().isOk() )
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        final var companies =
                mapper.readValue( response, UserCompanyAssociationsResponse.class )
                        .getSuccessBody();

        Assertions.assertEquals( 0, companies.size() );
    }

    @Test
    void getCompaniesAssociatedWithUserWithOneRemovedAssociationAndIncludeUnauthorisedIsNotSpecifiedShouldReturnEmptyList() throws Exception {

        final var response =
                mockMvc.perform( get( "/associations/users/{user_email}/companies", "devil@hell.com" ).header( "X-Request-Id", "theId" ) )
                        .andExpect( status().isOk() )
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();

        final var companies =
                mapper.readValue( response, UserCompanyAssociationsResponse.class )
                        .getSuccessBody();

        Assertions.assertEquals( 0, companies.size() );
    }

    @Test
    void getCompaniesAssociatedWithUserWithAVarietyOfAssociationsAndIncludeUnauthorisedIsTrueShouldReturnAllAssociatedCompanies() throws Exception {

        final var response =
        mockMvc.perform( get( "/associations/users/{user_email}/companies", "bruce.wayne@gotham.city" ).queryParam( "include-unauthorized", "true" ).header( "X-Request-Id", "theId" ) )
               .andExpect( status().isOk() )
               .andReturn()
               .getResponse()
               .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        final var companies =
        mapper.readValue( response, UserCompanyAssociationsResponse.class )
              .getSuccessBody();

        Assertions.assertEquals( 3, companies.size() );
        Assertions.assertTrue(  companies.stream().map( CompanyInfo::getCompanyName ).toList().containsAll( List.of( "Wayne Enterprises", "Springfield Nuclear Power Plant", "Queen Victoria Pub" ) ) );
    }

    @Test
    void getCompaniesAssociatedWithUserWithAVarietyOfAssociationsAndIncludeUnauthorisedIsFalseShouldReturnSomeAssociatedCompanies() throws Exception {

        final var response =
        mockMvc.perform( get( "/associations/users/{user_email}/companies", "bruce.wayne@gotham.city" ).queryParam( "include-unauthorized", "false" ).header( "X-Request-Id", "theId" ) )
               .andExpect( status().isOk() )
               .andReturn()
               .getResponse()
               .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        final var companies =
        mapper.readValue( response, UserCompanyAssociationsResponse.class )
              .getSuccessBody();

        Assertions.assertEquals( 2, companies.size() );
        Assertions.assertTrue(  companies.stream().map( CompanyInfo::getCompanyName ).toList().containsAll( List.of( "Wayne Enterprises", "Springfield Nuclear Power Plant" ) ) );
    }

    @Test
    void getCompaniesAssociatedWithUserWithAVarietyOfAssociationsAndIncludeUnauthorisedIsNotSpecifiedShouldReturnSomeAssociatedCompanies() throws Exception {

        final var response =
                mockMvc.perform( get( "/associations/users/{user_email}/companies", "bruce.wayne@gotham.city" ).header( "X-Request-Id", "theId" ) )
                        .andExpect( status().isOk() )
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        ObjectMapper mapper = new ObjectMapper();
        final var companies =
                mapper.readValue( response, UserCompanyAssociationsResponse.class )
                        .getSuccessBody();

        Assertions.assertEquals( 2, companies.size() );
        Assertions.assertTrue(  companies.stream().map( CompanyInfo::getCompanyName ).toList().containsAll( List.of( "Wayne Enterprises", "Springfield Nuclear Power Plant" ) ) );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Association.class);
    }

}
