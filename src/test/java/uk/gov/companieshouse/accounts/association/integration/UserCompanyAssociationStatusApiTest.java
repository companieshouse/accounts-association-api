package uk.gov.companieshouse.accounts.association.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;

@AutoConfigureMockMvc
@SpringBootTest
@Testcontainers
@Tag("integration-test")
class UserCompanyAssociationStatusApiTest {

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
    public void setup() {
        final var associationWithUserId = new Association();
        associationWithUserId.setCompanyNumber( "111111" );
        associationWithUserId.setUserId( "111" );
        associationWithUserId.setStatus( "Awaiting approval" );

        final var associationWithEmailId = new Association();
        associationWithEmailId.setCompanyNumber( "222222" );
        associationWithEmailId.setUserId( "ronnie.osullivan@snooker.com" );
        associationWithEmailId.setStatus( "Awaiting approval" );

        associationsRepository.insert( associationWithUserId );
        associationsRepository.insert( associationWithEmailId );
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithMissingPathVariablesReturnsNotFound() throws Exception {
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "", "Confirmed" ).header("X-Request-Id", "theId") ).andExpect(status().isNotFound());
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "", "bruce.wayne@gotham.city", "Confirmed" ).header("X-Request-Id", "theId") ).andExpect(status().isNotFound());
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "bruce.wayne@gotham.city", "" ).header("X-Request-Id", "theId") ).andExpect(status().isNotFound());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithMalformedInputReturnsBadRequest() throws Exception {
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "123", "Confirmed" ).header("X-Request-Id", "theId") ).andExpect(status().isBadRequest());
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "abc", "bruce.wayne@gotham.city", "Confirmed" ).header("X-Request-Id", "theId") ).andExpect(status().isBadRequest());
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "bruce.wayne@gotham.city", "Complicated" ).header("X-Request-Id", "theId") ).andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithNonexistentUserEmailOrCompanyNumberReturnsBadRequest() throws Exception {
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "krishna.patel@dahai.art", "Removed" ).header("X-Request-Id", "theId") ).andExpect(status().isBadRequest());
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "333333", "bruce.wayne@gotham.city", "Removed" ).header("X-Request-Id", "theId") ).andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithNonexistentAssociationReturnsBadRequest() throws Exception {
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "michael.jackson@singer.com", "Confirmed" ).header("X-Request-Id", "theId") ).andExpect(status().isBadRequest());
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "222222", "bruce.wayne@gotham.city", "Confirmed" ).header("X-Request-Id", "theId") ).andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithConfirmedStatusAndNonExistentUserReturnsBadRequest() throws Exception {
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "222222", "ronnie.osullivan@snooker.com", "Confirmed" ).header("X-Request-Id", "theId") ).andExpect(status().isBadRequest());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithConfirmedStatusAndExistentUserShouldUpdateStatusAndConfirmationApprovalTimeAndTemporary() throws Exception {
        mockMvc.perform(put("/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "bruce.wayne@gotham.city", "Confirmed").header("X-Request-Id", "theId")).andExpect(status().isOk());

        final var updatedAssociation = associationsRepository.findAll()
                        .stream()
                        .filter(association -> association.getCompanyNumber().equals("111111") && association.getUserId().equals("111"))
                        .findFirst()
                        .get();

        Assertions.assertEquals("Confirmed", updatedAssociation.getStatus());
        Assertions.assertNotNull(updatedAssociation.getConfirmationApprovalTime());
        Assertions.assertFalse(updatedAssociation.isTemporary());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithRemovedStatusAndNonExistentUserSetsStatusToRemovedAndDeletionTime() throws Exception {

        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "222222", "ronnie.osullivan@snooker.com", "Removed" ).header("X-Request-Id", "theId") ).andExpect(status().isOk());

        final var updatedAssociation = associationsRepository.findAll()
                .stream()
                .filter( association -> association.getCompanyNumber().equals( "222222" ) && association.getUserId().equals( "ronnie.osullivan@snooker.com" ) )
                .findFirst()
                .get();

        Assertions.assertEquals( "Removed", updatedAssociation.getStatus() );
        Assertions.assertNotNull( updatedAssociation.getDeletionTime() );
        Assertions.assertFalse( updatedAssociation.isTemporary() );
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithRemovedStatusAndExistingUserSetsStatusToRemovedAndDeletionTimeAndTemporaryToFalse() throws Exception {

        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_email}/{status}", "111111", "bruce.wayne@gotham.city", "Removed" ).header("X-Request-Id", "theId") ).andExpect(status().isOk());

        final var updatedAssociation = associationsRepository.findAll()
                .stream()
                .filter( association -> association.getCompanyNumber().equals( "111111" ) && association.getUserId().equals( "111" ) )
                .findFirst()
                .get();

        Assertions.assertEquals( "Removed", updatedAssociation.getStatus() );
        Assertions.assertNotNull( updatedAssociation.getDeletionTime() );
        Assertions.assertFalse( updatedAssociation.isTemporary() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Association.class);
    }

}
