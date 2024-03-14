package uk.gov.companieshouse.accounts.association.repositories;

import jakarta.validation.ConstraintViolationException;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
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
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;

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

    @MockBean
    ApiClientService apiClientService;

    @Autowired
    AssociationsRepository associationsRepository;
    List<Association> associations ;
    List<Invitation> invitations = new ArrayList<>();


    @BeforeEach
    public void setup() {
        final var associationOne = new Association();
        associationOne.setId("111");
        associationOne.setCompanyNumber("12345");
        associationOne.setUserId("555");
        associationOne.setStatus(StatusEnum.CONFIRMED);
        associationOne.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE);
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
        associationTwo.setStatus(StatusEnum.CONFIRMED);
        associationTwo.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE);
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
        associationThree.setStatus(StatusEnum.CONFIRMED);
        associationThree.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE);
        associationThree.setUserEmail(" ");
        associationThree.setEtag("OCRS7");
        associationThree.setApprovalExpiryAt(LocalDateTime.now().plusDays(10));
        associationThree.setInvitations(new ArrayList<>());

        final var invitationThree = new Invitation();
        invitationThree.setInvitedBy("123459");
        invitationThree.setInvitedAt(LocalDateTime.now().plusDays(10));
        invitations.add(invitationThree);
        associationThree.setInvitations(invitations);

        final var associationFour = new Association();
        associationFour.setCompanyNumber("111111");
        associationFour.setUserId("111");
        associationFour.setStatus(StatusEnum.CONFIRMED);
        associationFour.setEtag("x");
        associationFour.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE );

        final var associationFive = new Association();
        associationFive.setCompanyNumber("222222");
        associationFive.setUserId("111");
        associationFive.setStatus(StatusEnum.CONFIRMED);
        associationFive.setEtag("x");
        associationFive.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE );

        final var associationSix = new Association();
        associationSix.setCompanyNumber("111111");
        associationSix.setUserId("222");
        associationSix.setStatus(StatusEnum.CONFIRMED);
        associationSix.setEtag("x");
        associationSix.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE );

        final var associationSeven = new Association();
        associationSeven.setCompanyNumber("333333");
        associationSeven.setUserId("333");
        associationSeven.setStatus(StatusEnum.CONFIRMED);
        associationSeven.setEtag("x");
        associationSeven.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE );

        final var associationEight = new Association();
        associationEight.setCompanyNumber("333333");
        associationEight.setUserId("444");
        associationEight.setStatus(StatusEnum.CONFIRMED);
        associationEight.setEtag("x");
        associationEight.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE );

        final var associationNine = new Association();
        associationNine.setCompanyNumber("333333");
        associationNine.setUserId("888");
        associationNine.setStatus(StatusEnum.REMOVED);
        associationNine.setEtag("x");
        associationNine.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE );

        final var associationTen = new Association();
        associationTen.setCompanyNumber("333333");
        associationTen.setUserId("101010");
        associationTen.setStatus(StatusEnum.AWAITING_APPROVAL);
        associationTen.setEtag("x");
        associationTen.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE );

        final var associationEleven = new Association();
        associationEleven.setCompanyNumber("333333");
        associationEleven.setUserId("111111");
        associationEleven.setStatus(StatusEnum.AWAITING_APPROVAL);
        associationEleven.setEtag("x");
        associationEleven.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE );

        associationsRepository.insert( List.of( associationOne, associationTwo, associationThree,
                associationFour, associationFive, associationSix, associationSeven,
                associationEight, associationNine, associationTen, associationEleven ) );

    }

    @Test
    void testInsertAssociation() {
        final var associationUser = new Association();
        Pageable pageable = PageRequest.of(1, 10);
        associationUser.setCompanyNumber("1114422");
        associationUser.setUserId("9999");
        associationUser.setStatus(StatusEnum.CONFIRMED);
        associationUser.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE);
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

        final var validUser = associationsRepository.findById( "111");
        Assertions.assertEquals( 1, validUser.stream().count() );
    }
    @Test
    void findByIdWithMalformedInputOrNonexistentIdReturnsEmptyOptional(){

        Assertions.assertFalse( associationsRepository.findById( "" ).isPresent() );
        Assertions.assertFalse( associationsRepository.findById( "$" ).isPresent() );
        Assertions.assertFalse( associationsRepository.findById( "999" ).isPresent() );
    }


    @Test
    void fetchAssociationWithValidValues() {

        Assertions.assertEquals( "555", associationsRepository.findById( "111" ).get().getUserId());
        Assertions.assertEquals( "12345", associationsRepository.findById( "111" ).get().getCompanyNumber());
        Assertions.assertEquals( "abc@abc.com", associationsRepository.findById( "111" ).get().getUserEmail());
        Assertions.assertEquals( StatusEnum.CONFIRMED, associationsRepository.findById( "111" ).get().getStatus());

    }
    @Test
    void fetchAssociationWithInValidUserEmail() {
        Assertions.assertEquals(" ", associationsRepository.findById("333").get().getUserEmail());
    }

    @Test
    void updateStatusBasedOnId(){
        final var update = new Update().set("status", StatusEnum.REMOVED);
        associationsRepository.updateUser( "111", update );
        Assertions.assertEquals( StatusEnum.REMOVED, associationsRepository.findById( "111" ).get().getStatus() );
    }

    @Test
    void updateStatusWithNullThrowsIllegalStateException(){
        assertThrows( IllegalStateException.class, () -> associationsRepository.updateUser( "111", null ) );
    }
    @Test
    void setFutureDateToApprovedAtFieldThrowsConstraintViolationException() {
        LocalDateTime futureDateTime = LocalDateTime.now().minusDays(1);
        Association association = new Association();
        association.setApprovedAt(futureDateTime);

        assertThrows(ConstraintViolationException.class, () -> {
            associationsRepository.save(association);
        });
    }

    @Test
    void setFutureDateToRemovedAtFieldThrowsConstraintViolationException() {
        LocalDateTime futureDateTime = LocalDateTime.now().plusDays(5);
        Association association = new Association();
        association.setRemovedAt(futureDateTime);

        assertThrows(ConstraintViolationException.class, () -> {
            associationsRepository.save(association);
        });
    }

    @Test
    void setPastDateToApprovalExpiryAtFieldThrowsConstraintViolationException() {
        LocalDateTime pastDateTime = LocalDateTime.now().minusDays(20);
        Association association = new Association();
        association.setApprovalExpiryAt(pastDateTime);

        assertThrows(ConstraintViolationException.class, () -> {
            associationsRepository.save(association);
        });
    }

    @Test
    void updateVersionFieldIncrementsVersion() {

        Association association = new Association();
        association.setCompanyNumber("1114422");
        association.setUserId("9999");
        association.setStatus(StatusEnum.CONFIRMED);
        association.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE);
        association.setUserEmail("abc@abc.com");
        association.setEtag("OCRS7");
        association = associationsRepository.save(association);

        Association savedAssociation = associationsRepository.findById(association.getId()).orElseThrow();

        savedAssociation.setCompanyNumber("555555");
        associationsRepository.save(savedAssociation);

        Association updatedAssociation = associationsRepository.findById(savedAssociation.getId()).orElseThrow();

        Assertions.assertEquals(association.getVersion() + 1, updatedAssociation.getVersion());
    }

    @Test
    void fetchAssociatedUsersWithNullOrMalformedOrNonexistentCompanyNumberReturnsEmptyPage(){
        Assertions.assertTrue( associationsRepository.fetchAssociatedUsers( null, Set.of( StatusEnum.CONFIRMED ), PageRequest.of( 0, 3 ) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociatedUsers( "$", Set.of( StatusEnum.CONFIRMED ), PageRequest.of( 0, 3 ) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociatedUsers( "999999", Set.of( StatusEnum.CONFIRMED ), PageRequest.of( 0, 3 ) ).isEmpty() );
    }

    @Test
    void fetchAssociatedUsersWithNullStatusesThrowsUncategorizedMongoDbException(){
        Assertions.assertThrows( UncategorizedMongoDbException.class, () -> associationsRepository.fetchAssociatedUsers( "111111", null, PageRequest.of( 0, 3 ) ) );
    }

    @Test
    void fetchAssociatedUsersWithNullPageableReturnsAllAssociatedUsers(){
        Assertions.assertEquals( 2, associationsRepository.fetchAssociatedUsers( "111111", Set.of( StatusEnum.CONFIRMED ), null ).getNumberOfElements() );
    }

    @Test
    void fetchAssociatedUsersFiltersBasedOnSpecifiedStatuses(){
        Assertions.assertTrue( associationsRepository.fetchAssociatedUsers( "333333", Set.of(),  PageRequest.of( 0, 15 ) ).isEmpty() );

        final var queryWithConfirmedFilter =
                associationsRepository.fetchAssociatedUsers( "333333", Set.of( StatusEnum.CONFIRMED ),  PageRequest.of( 0, 15 ) )
                        .getContent()
                        .stream()
                        .map( Association::getUserId )
                        .toList();
        Assertions.assertEquals( 2, queryWithConfirmedFilter.size() );
        Assertions.assertTrue( queryWithConfirmedFilter.containsAll( List.of( "444", "333" ) ) );

        final var queryWithConfirmedAndAwaitingFilter =
                associationsRepository.fetchAssociatedUsers( "333333", Set.of( StatusEnum.CONFIRMED, StatusEnum.AWAITING_APPROVAL ),  PageRequest.of( 0, 15 ) )
                        .getContent()
                        .stream()
                        .map( Association::getUserId )
                        .toList();
        Assertions.assertEquals( 4, queryWithConfirmedAndAwaitingFilter.size() );
        Assertions.assertTrue( queryWithConfirmedAndAwaitingFilter.containsAll( List.of( "444", "333", "101010", "111111" ) ) );
    }

    @Test
    void fetchAssociatedUsersPaginatesCorrectly(){
        final var secondPage = associationsRepository.fetchAssociatedUsers( "333333", Set.of( StatusEnum.CONFIRMED, StatusEnum.AWAITING_APPROVAL, StatusEnum.REMOVED ), PageRequest.of( 1, 2 ) );
        final var secondPageContent =
                secondPage.getContent()
                        .stream()
                        .map( Association::getUserId )
                        .toList();

        Assertions.assertEquals( 5, secondPage.getTotalElements() );
        Assertions.assertEquals( 3, secondPage.getTotalPages() );
        Assertions.assertEquals( 2, secondPageContent.size() );
        Assertions.assertTrue( secondPageContent.containsAll( List.of( "888", "101010" ) ) );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Association.class);
    }
}
