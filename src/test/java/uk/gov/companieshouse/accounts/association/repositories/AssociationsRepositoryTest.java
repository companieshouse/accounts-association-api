package uk.gov.companieshouse.accounts.association.repositories;

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
import uk.gov.companieshouse.accounts.association.models.Notification;
import uk.gov.companieshouse.accounts.association.service.ApiClientService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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


    @BeforeEach
    public void setup() {
        final var associationOne = new Association();
        associationOne.setId("111");
        associationOne.setCompanyNumber("12345");
        associationOne.setUserId("555");
        associationOne.setStatus("confirmed");
        associationOne.setApprovalRoute("auth_code");
        associationOne.setUserEmail("abc@abc.com");
        associationOne.setApprovalExpiryAt(LocalDateTime.now().plusDays(10));
        associationOne.setEtag("OCRS7");

        final var invitation = new Invitation();
        invitation.setInvitedBy("123456");

        associationOne.setInvitations(Collections.singletonList(invitation));

        final var notification = new Notification();
        notification.setNotificationEvent("added");
        notification.setNotifiedAt(LocalDateTime.now().plusDays(15));
        associationOne.setNotifications(Collections.singletonList(notification));

        final var associationTwo = new Association();
        associationTwo.setId("222");
        associationTwo.setCompanyNumber("12345");
        associationTwo.setUserId("666");
        associationTwo.setStatus("confirmed");
        associationTwo.setApprovalRoute("auth_code");
        associationTwo.setUserEmail("abc@abc.com");
        associationOne.setApprovalExpiryAt(LocalDateTime.now().plusDays(10));
        associationTwo.setInvitations(new ArrayList<>());
        associationTwo.setNotifications(new ArrayList<>());
        associationTwo.setEtag("OCRS7");


        associations = associationsRepository.saveAll( List.of( associationOne, associationTwo ) );

    }

    @Test
    void testInsertAssociation() {
        final var associationUser = new Association();
        Pageable pageable = PageRequest.of(1, 10);
        associationUser.setCompanyNumber("1114422");
        associationUser.setUserId("9999");
        associationUser.setStatus("Confirmed");
        associationUser.setApprovalRoute("auth_code");
        associationUser.setEtag("OCRS7");
        associationsRepository.insert(associationUser);

        Assertions.assertEquals( 1, associationsRepository.findAllByUserId( "9999" , pageable).getTotalElements() );
    }

    @Test
    void fetchAssociationWithInvalidUserID() {
        Pageable pageable = PageRequest.of(1, 10);
        Assertions.assertEquals( 0, associationsRepository.findAllByUserId( "567" , pageable).getTotalElements() );
        Assertions.assertEquals( 0, associationsRepository.findAllByUserId( "@£@" , pageable).getTotalElements() );
    }

    @Test
    void fetchAssociationWithValidUserID() {
        Pageable pageable = PageRequest.of(1, 10);
        Assertions.assertEquals( 1, associationsRepository.findAllByUserId( "666" , pageable).getTotalElements() );
    }

    @Test
    void updateStatusBasedOnId(){
        final var update = new Update().set("status", "Removed");
        associationsRepository.updateUser( "111", update );
        Assertions.assertEquals( "Removed", associationsRepository.findAllById( "111" ).get().getStatus() );
    }

    @Test
    void fetchNotificationAssociationWithWithNonexistentIDReturnsEmptyList(){
        final var emptyList = new ArrayList<String>();
        emptyList.add( null );

        Assertions.assertEquals( List.of(), associationsRepository.findAllNotificationsById( "xxxx" ) );
        Assertions.assertEquals( List.of(), associationsRepository.findAllNotificationsById( "777" ) );
        Assertions.assertEquals( List.of(), associationsRepository.findAllNotificationsById( "5rtQ" ) );

    }

    @Test
    void fetchNotificationAssociationWithValidIDReturnsValues(){
        final var validUser = associationsRepository.findAllNotificationsById("111");
        Assertions.assertEquals( 1, validUser.size() );

    }

//    @Test
//    void fetchInvitationsAssociationWithWithNonexistentIDReturnsEmptyList(){
//        final var emptyList = new ArrayList<String>();
//        emptyList.add( null );
//
//        Assertions.assertEquals( List.of(), associationsRepository.findAllInvitationsById( "qwer" ) );
//        Assertions.assertEquals( List.of(), associationsRepository.findAllInvitationsById( "222344" ) );
//        Assertions.assertEquals( List.of(), associationsRepository.findAllInvitationsById( "22dfg44" ) );
//
//    }

    @Test
    void fetchInvitationsAssociationWithWithValidIDReturnsValues(){
        final var validUser = associationsRepository.findAllInvitationsById("111");
        Assertions.assertEquals( 1, validUser.iterator() );

    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Association.class);
    }
}
