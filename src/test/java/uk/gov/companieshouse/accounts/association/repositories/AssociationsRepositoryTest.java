package uk.gov.companieshouse.accounts.association.repositories;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.models.Invitation;
import uk.gov.companieshouse.accounts.association.service.ApiClientService;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers(parallel = true)
@Tag("integration-test")
class AssociationsRepositoryTest {

    @Container
    @ServiceConnection
    static MongoDBContainer container = new MongoDBContainer("mongo:5");
    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    AssociationsRepository associationsRepository;

    @MockBean
    ApiClientService apiClientService;

    List<Association> associations ;
    List<Invitation> invitations = new ArrayList<>();


    @BeforeEach
    public void setup() {
        final var associationOne = new Association();
        associationOne.setId("111");
        associationOne.setCompanyNumber("12345");
        associationOne.setUserId("555");
        associationOne.setStatus("Confirmed");
        associationOne.setApprovalRoute("auth_code");
        associationOne.setUserEmail("abc@abc.com");
        associationOne.setApprovalExpiryAt(LocalDateTime.now().plusDays(10));
        associationOne.setInvitations(new ArrayList<>());
        associationOne.setEtag("OCRS7");

        final var invitation = new Invitation();
        invitation.setInvitedBy("123456");
        invitation.setInvitedAt(LocalDateTime.now().plusDays(10));
        invitations.add(invitation);
        associationOne.setInvitations(invitations);

        final var associationTwo = new Association();
        associationTwo.setId("222");
        associationTwo.setCompanyNumber("12345");
        associationTwo.setUserId("666");
        associationTwo.setStatus("Confirmed");
        associationTwo.setApprovalRoute("auth_code");
        associationTwo.setUserEmail("abc@abc.com");
        associationTwo.setApprovalExpiryAt(LocalDateTime.now().plusDays(10));
        associationTwo.setInvitations(new ArrayList<>());
        associationTwo.setEtag("OCRS7");

        final var invitationTwo = new Invitation();
        invitationTwo.setInvitedBy("123456");
        invitationTwo.setInvitedAt(LocalDateTime.now().plusDays(10));
        invitations.add(invitationTwo);
        associationTwo.setInvitations(invitations);

        final var associationThree = new Association();
        associationThree.setId("333");
        associationThree.setCompanyNumber("10345");
        associationThree.setUserId("777");
        associationThree.setStatus("Confirmed");
        associationThree.setApprovalRoute("auth_code");
        associationThree.setUserEmail(" ");
        associationThree.setEtag("OCRS7");
        associationThree.setApprovalExpiryAt(LocalDateTime.now().plusDays(10));
        associationThree.setInvitations(new ArrayList<>());

        final var invitationThree = new Invitation();
        invitationThree.setInvitedBy("123459");
        invitationThree.setInvitedAt(LocalDateTime.now().plusDays(10));
        invitations.add(invitationThree);
        associationThree.setInvitations(invitations);

        associations = associationsRepository.saveAll( List.of( associationOne, associationTwo, associationThree ) );

    }

    @Test
    void testInsertAssociation() {
        final var associationUser = new Association();
        Pageable pageable = PageRequest.of(1, 10);
        associationUser.setCompanyNumber("1114422");
        associationUser.setUserId("9999");
        associationUser.setStatus("Confirmed");
        associationUser.setApprovalRoute("auth_code");
        associationUser.setUserEmail("abc@abc.com");
        associationUser.setEtag("OCRS7");
        associationUser.setApprovalExpiryAt(LocalDateTime.now().plusDays(10));
        associationUser.setInvitations(new ArrayList<>());
        associationsRepository.insert(associationUser);
        final var invitationUser = new Invitation();
        invitationUser.setInvitedBy("123456");
        invitationUser.setInvitedAt(LocalDateTime.now().plusDays(10));
        invitations.add(invitationUser);
        associationUser.setInvitations(invitations);

        Assertions.assertEquals( 1, associationsRepository.findAllByUserId( "9999" , pageable).getTotalElements() );
    }

    @Test
    void fetchAssociationWithInvalidOrNonexistentUserIDReturnsEmpty() {
        Pageable pageable = PageRequest.of(1, 10);
        Assertions.assertTrue( associationsRepository.findAllByUserId( "567" , pageable).getContent().isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserId( "@£@" , pageable).getContent().isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserId( null , pageable).getContent().isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserId( "" ,pageable).getContent().isEmpty() );
    }
    @Test
    void fetchAssociationWithValidUserID() {
        Pageable pageable = PageRequest.of(1, 10);
        Assertions.assertEquals( 1, associationsRepository.findAllByUserId( "666" , pageable).getTotalElements() );
    }

    @Test
    void updateEtagWithNullThrowsConstraintViolationException() {
        assertThrows(ConstraintViolationException.class, () -> {
            Association association = new Association();
            association.setEtag(null);
            associationsRepository.save(association);
        });
    }

    @Test
    void updateApprovalRouteWithNullThrowsConstraintViolationException() {
        assertThrows(ConstraintViolationException.class, () -> {
            Association association = new Association();
            association.setApprovalRoute(null);
            associationsRepository.save(association);
        });
    }
    @Test
    void fetchAssociationWithValidID() {

        final var validUser = associationsRepository.findAssociationById( "111");
        Assertions.assertEquals( 1, validUser.stream().count() );
    }
    @Test
    void findAssociationByIdWithMalformedInputOrNonexistentIdReturnsEmptyOptional(){
        Assertions.assertFalse( associationsRepository.findAssociationById( null ).isPresent() );
        Assertions.assertFalse( associationsRepository.findAssociationById( "" ).isPresent() );
        Assertions.assertFalse( associationsRepository.findAssociationById( "$" ).isPresent() );
        Assertions.assertFalse( associationsRepository.findAssociationById( "999" ).isPresent() );
    }


    @Test
    void fetchAssociationWithValidValues() {

        Assertions.assertEquals( "555", associationsRepository.findAssociationById( "111" ).get().getUserId());
        Assertions.assertEquals( "12345", associationsRepository.findAssociationById( "111" ).get().getCompanyNumber());
        Assertions.assertEquals( "abc@abc.com", associationsRepository.findAssociationById( "111" ).get().getUserEmail());
        Assertions.assertEquals( "Confirmed", associationsRepository.findAssociationById( "111" ).get().getStatus());

    }
    @Test
    void fetchAssociationWithInValidUserEmail() {
        Assertions.assertEquals(" ", associationsRepository.findAssociationById("333").get().getUserEmail());
    }

    @Test
    void updateStatusBasedOnId(){
        final var update = new Update().set("status", "Removed");
        associationsRepository.updateUser( "111", update );
        Assertions.assertEquals( "Removed", associationsRepository.findAssociationById( "111" ).get().getStatus() );
    }

    @Test
    void updateStatusWithNullThrowsIllegalStateException(){
        assertThrows( IllegalStateException.class, () -> associationsRepository.updateUser( "111", null ) );
    }

    @Test
    void setFutureDateToCreatedAtFieldThrowsConstraintViolationException() {
        LocalDateTime futureDateTime = LocalDateTime.now().plusDays(1);
        Association association = new Association();
        association.setCreatedAt(futureDateTime);

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
            associationsRepository.save(association);
        });
    }
    @Test
    void setFutureDateToApprovedAtFieldThrowsConstraintViolationException() {
        LocalDateTime futureDateTime = LocalDateTime.now().plusDays(5);
        Association association = new Association();
        association.setApprovedAt(futureDateTime);

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
            associationsRepository.save(association);
        });
    }

    @Test
    void setFutureDateToRemovedAtFieldThrowsConstraintViolationException() {
        LocalDateTime futureDateTime = LocalDateTime.now().plusDays(5);
        Association association = new Association();
        association.setRemovedAt(futureDateTime);

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
            associationsRepository.save(association);
        });
    }

    @Test
    void setPastDateToApprovalExpiryAtFieldThrowsConstraintViolationException() {
        LocalDateTime pastDateTime = LocalDateTime.now().minusDays(20);
        Association association = new Association();
        association.setRemovedAt(pastDateTime);

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
            associationsRepository.save(association);
        });
    }

    @Test
    void updateVersionFieldIncrementsVersion() {

        Association association = new Association();
        association.setCompanyNumber("1114422");
        association.setUserId("9999");
        association.setStatus("Confirmed");
        association.setApprovalRoute("auth_code");
        association.setUserEmail("abc@abc.com");
        association.setEtag("OCRS7");
        association = associationsRepository.save(association);

        Association savedAssociation = associationsRepository.findAssociationById(association.getId()).orElseThrow();

        savedAssociation.setCompanyNumber("555555");
        associationsRepository.save(savedAssociation);

        Association updatedAssociation = associationsRepository.findAssociationById(savedAssociation.getId()).orElseThrow();

        Assertions.assertEquals(association.getVersion() + 1, updatedAssociation.getVersion());
    }
    @Test
    void fetchInvitationsAssociationWithWithValidIDReturnsValues(){
        final var validUser = associationsRepository.findAllInvitationsById( "111");
        Assertions.assertEquals( 1, validUser.size() );

    }
    @Test
    void fetchInvitationsAssociationWithWithNonexistentIDReturnsEmptyList(){
        final var emptyList = new ArrayList<String>();
        emptyList.add( null );

        Assertions.assertEquals( List.of(), associationsRepository.findAllInvitationsById( "qwer" ) );
        Assertions.assertEquals( List.of(), associationsRepository.findAllInvitationsById( "222344" ) );
        Assertions.assertEquals( List.of(), associationsRepository.findAllInvitationsById( "22dfg44" ) );

    }
    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Association.class);
    }
}
