package uk.gov.companieshouse.accounts.association.unit.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import uk.gov.companieshouse.accounts.association.models.Associations;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;

@ComponentScan("uk.gov.companieshouse.accounts.association")
@AutoConfigureMockMvc
@SpringBootTest
@ExtendWith(SpringExtension.class)
@Tag("integration-test")
public class UserCompanyAssociationStatusApiTest {

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    public MockMvc mockMvc;

    @Autowired
    public AssociationsRepository associationsRepository;

    private final String unforeseeableFuture = Instant.now().plus( 1000000, ChronoUnit.DAYS ).toString();
    private final String yesterday = Instant.now().minus( 1, ChronoUnit.DAYS ).toString();

    @BeforeEach
    public void setup(){
        final var associationWithoutInvitation = new Associations();
        associationWithoutInvitation.setUserId("000");
        associationWithoutInvitation.setCompanyNumber("000000");

        final var associationWithValidInvitation = new Associations();
        associationWithValidInvitation.setUserId( "111" );
        associationWithValidInvitation.setCompanyNumber( "111111" );
        associationWithValidInvitation.setStatus( "Awaiting approval" );
        associationWithValidInvitation.setConfirmationExpirationTime( unforeseeableFuture );

        final var associationWithExpiredInvitation = new Associations();
        associationWithExpiredInvitation.setUserId( "222" );
        associationWithExpiredInvitation.setCompanyNumber( "222222" );
        associationWithExpiredInvitation.setStatus( "Awaiting approval" );
        associationWithExpiredInvitation.setConfirmationExpirationTime( yesterday );

        associationsRepository.insert( associationWithoutInvitation );
        associationsRepository.insert( associationWithValidInvitation );
        associationsRepository.insert( associationWithExpiredInvitation );
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithNonExistentAssociationReturnsNoContent() throws Exception {
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_id}/{status}", "333333", "333", "RedundantInput" ).header("X-Request-Id", "theId") )
               .andExpect(status().isNoContent());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithAssociationThatHasNoConfirmationExpirationTimeReturnsNoContent() throws Exception {
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_id}/{status}", "000000", "000", "RedundantInput" ).header("X-Request-Id", "theId") )
                .andExpect(status().isNoContent());
    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithExpiredInvitationSoftDeletesAssociationAndReturnsOk() throws Exception {
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_id}/{status}", "222222", "222", "RedundantInput" ).header("X-Request-Id", "theId") )
               .andExpect( status().isOk() );

        for ( Associations association: associationsRepository.findAll() ) {
            if ( association.getUserId().equals( "222" ) && association.getCompanyNumber().equals( "222222" ) ) {
                Assertions.assertEquals("Deleted", association.getStatus());
                Assertions.assertNotNull(association.getDeletionTime());
            } else if ( association.getUserId().equals( "111" ) && association.getCompanyNumber().equals( "111111" ) ) {
                Assertions.assertEquals("Awaiting approval", association.getStatus());
                Assertions.assertNull(association.getDeletionTime());
            } else if ( association.getUserId().equals( "000" ) && association.getCompanyNumber().equals( "000000" ) ) {
                Assertions.assertNull(association.getStatus());
                Assertions.assertNull(association.getDeletionTime());
            }

        }

    }

    @Test
    void updateAssociationStatusForUserAndCompanyWithUnexpiredInvitationConfirmsAssociationAndReturnsOk() throws Exception {
        mockMvc.perform( put( "/associations/companies/{company_number}/users/{user_id}/{status}", "111111", "111", "RedundantInput" ).header("X-Request-Id", "theId") )
                .andExpect( status().isOk() );

        for ( Associations association: associationsRepository.findAll() ) {
            if ( association.getUserId().equals( "222" ) && association.getCompanyNumber().equals( "222222" ) ) {
                Assertions.assertEquals("Awaiting approval", association.getStatus());
                Assertions.assertNull(association.getConfirmationApprovalTime());
            } else if ( association.getUserId().equals( "111" ) && association.getCompanyNumber().equals( "111111" ) ) {
                Assertions.assertEquals("Confirmed", association.getStatus());
                Assertions.assertNotNull(association.getConfirmationApprovalTime());
            } else if ( association.getUserId().equals( "000" ) && association.getCompanyNumber().equals( "000000" ) ) {
                Assertions.assertNull(association.getStatus());
                Assertions.assertNull(association.getDeletionTime());
            }

        }

    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Associations.class);
    }

}
