package uk.gov.companieshouse.accounts.association.unit.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
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
import uk.gov.companieshouse.accounts.association.enums.StatusEnum;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.models.Users;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.repositories.UsersRepository;

@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
public class UserCompanyAssociationStatusApiTest {

    @Container
    @ServiceConnection
    static MongoDBContainer container = new MongoDBContainer("mongo:4.4.22");

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    public MockMvc mockMvc;

    @Autowired
    AssociationsRepository associationsRepository;

    @Autowired
    UsersRepository usersRepository;

    @BeforeEach
    public void setup() {
        final var userBatman = new Users();
        userBatman.setId( "111" );
        userBatman.setForename( "Bruce" );
        userBatman.setSurname( "Wayne" );
        userBatman.setEmail( "batman@gotham.city" );
        userBatman.setDisplayName( "Batman" );

        final var association = new Association();
        association.setCompanyNumber( "111111" );
        association.setUserId( "111" );
        association.setStatus( "Awaiting approval" );

        associationsRepository.insert( association );
        usersRepository.insert( userBatman );
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithMissingPathVariablesReturnsNotFound() throws Exception {
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "", "Confirmed" ).header("X-Request-Id", "theId") ).andExpect(status().isNotFound());
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "", "batman@gotham.city", "Confirmed" ).header("X-Request-Id", "theId") ).andExpect(status().isNotFound());
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "batman@gotham.city", "" ).header("X-Request-Id", "theId") ).andExpect(status().isNotFound());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithMalformedInputReturnsBadRequest() throws Exception {
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "123", "Confirmed" ).header("X-Request-Id", "theId") ).andExpect(status().isBadRequest());
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "abc", "batman@gotham.city", "Confirmed" ).header("X-Request-Id", "theId") ).andExpect(status().isBadRequest());
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "batman@gotham.city", "Complicated" ).header("X-Request-Id", "theId") ).andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithNonexistentAssociationReturnsNotFound() throws Exception {
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "robin@gotham.city", "Confirmed" ).header("X-Request-Id", "theId") ).andExpect(status().isNotFound());
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "222222", "batman@gotham.city", "Confirmed" ).header("X-Request-Id", "theId") ).andExpect(status().isNotFound());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithConfirmedStatusSetsStatusToConfirmedAndConfirmationApprovalTime() throws Exception {
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "batman@gotham.city", "Confirmed" ).header("X-Request-Id", "theId") ).andExpect(status().isOk());

        final var updatedAssociation =
        associationsRepository.findAll()
                .stream()
                .filter( association -> association.getCompanyNumber().equals( "111111" ) && association.getUserId().equals( "111" ) )
                .findFirst()
                .get();

        Assertions.assertEquals( "Confirmed", updatedAssociation.getStatus() );
        Assertions.assertNotNull( updatedAssociation.getConfirmationApprovalTime() );
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithRemovedStatusSetsStatusToRemovedAndDeletionTime() throws Exception {
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "batman@gotham.city", StatusEnum.REMOVED.getValue()).header("X-Request-Id", "theId") ).andExpect(status().isOk());

        final var updatedAssociation = associationsRepository.findAll()
                        .stream()
                        .filter( association -> association.getCompanyNumber().equals( "111111" ) && association.getUserId().equals( "111" ) )
                        .findFirst()
                        .get();

        Assertions.assertEquals( "Removed", updatedAssociation.getStatus() );
        Assertions.assertNotNull( updatedAssociation.getDeletionTime() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Association.class);
        mongoTemplate.dropCollection(Users.class);
    }

}
