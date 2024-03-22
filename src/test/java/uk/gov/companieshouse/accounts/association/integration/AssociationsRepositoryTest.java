package uk.gov.companieshouse.accounts.association.integration;

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
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.utils.ApiClientUtil;
import java.util.Set;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
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
    ApiClientUtil apiClientUtil;

    @Autowired
    AssociationsRepository associationsRepository;
    List<AssociationDao> associationDaos;
    List<InvitationDao> invitationDaos = new ArrayList<>();

    @MockBean
    StaticPropertyUtil staticPropertyUtil;


    @BeforeEach
    public void setup() {
        final var associationOne = new AssociationDao();
        associationOne.setId("111");
        associationOne.setCompanyNumber("12345");
        associationOne.setUserId("555");
        associationOne.setStatus(StatusEnum.CONFIRMED.getValue());
        associationOne.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationOne.setUserEmail("abc@abc.com");
        associationOne.setApprovalExpiryAt(LocalDateTime.now().plusDays(10));
        associationOne.setInvitations(new ArrayList<>());
        associationOne.setEtag("OCRS7");

        final var invitation = new InvitationDao();
        invitation.setInvitedBy("123456");
        invitation.setInvitedAt(LocalDateTime.now().plusDays(10));
        invitationDaos.add(invitation);
        associationOne.setInvitations(invitationDaos);

        final var associationTwo = new AssociationDao();
        associationTwo.setId("222");
        associationTwo.setCompanyNumber("12345");
        associationTwo.setUserId("666");
        associationTwo.setStatus(StatusEnum.CONFIRMED.getValue());
        associationTwo.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationTwo.setUserEmail("abc@abc.com");
        associationTwo.setApprovalExpiryAt(LocalDateTime.now().plusDays(10));
        associationTwo.setInvitations(new ArrayList<>());
        associationTwo.setEtag("OCRS7");

        final var invitationTwo = new InvitationDao();
        invitationTwo.setInvitedBy("123456");
        invitationTwo.setInvitedAt(LocalDateTime.now().plusDays(10));
        invitationDaos.add(invitationTwo);
        associationTwo.setInvitations(invitationDaos);

        final var associationThree = new AssociationDao();
        associationThree.setId("333");
        associationThree.setCompanyNumber("10345");
        associationThree.setUserId("777");
        associationThree.setStatus(StatusEnum.CONFIRMED.getValue());
        associationThree.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationThree.setUserEmail(" ");
        associationThree.setEtag("OCRS7");
        associationThree.setApprovalExpiryAt(LocalDateTime.now().plusDays(10));
        associationThree.setInvitations(new ArrayList<>());

        final var invitationThree = new InvitationDao();
        invitationThree.setInvitedBy("123459");
        invitationThree.setInvitedAt(LocalDateTime.now().plusDays(10));
        invitationDaos.add(invitationThree);
        associationThree.setInvitations(invitationDaos);

        final var associationFour = new AssociationDao();
        associationFour.setCompanyNumber("111111");
        associationFour.setUserId("111");
        associationFour.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFour.setEtag("x");
        associationFour.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE.getValue() );

        final var associationFive = new AssociationDao();
        associationFive.setCompanyNumber("222222");
        associationFive.setUserId("111");
        associationFive.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFive.setEtag("x");
        associationFive.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE.getValue() );

        final var associationSix = new AssociationDao();
        associationSix.setCompanyNumber("111111");
        associationSix.setUserId("222");
        associationSix.setStatus(StatusEnum.CONFIRMED.getValue());
        associationSix.setEtag("x");
        associationSix.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE.getValue());

        final var associationSeven = new AssociationDao();
        associationSeven.setCompanyNumber("333333");
        associationSeven.setUserId("333");
        associationSeven.setStatus(StatusEnum.CONFIRMED.getValue());
        associationSeven.setEtag("x");
        associationSeven.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE.getValue() );

        final var associationEight = new AssociationDao();
        associationEight.setCompanyNumber("333333");
        associationEight.setUserId("444");
        associationEight.setStatus(StatusEnum.CONFIRMED.getValue());
        associationEight.setEtag("x");
        associationEight.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE.getValue() );

        final var associationNine = new AssociationDao();
        associationNine.setCompanyNumber("333333");
        associationNine.setUserId("888");
        associationNine.setStatus(StatusEnum.REMOVED.getValue());
        associationNine.setEtag("x");
        associationNine.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE.getValue() );

        final var associationTen = new AssociationDao();
        associationTen.setCompanyNumber("333333");
        associationTen.setUserId("101010");
        associationTen.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTen.setEtag("x");
        associationTen.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE.getValue() );

        final var associationEleven = new AssociationDao();
        associationEleven.setCompanyNumber("333333");
        associationEleven.setUserId("111111");
        associationEleven.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationEleven.setEtag("x");
        associationEleven.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE.getValue() );

        associationDaos = associationsRepository.saveAll( List.of(
                associationOne, associationTwo, associationThree, associationFour,
                associationFive, associationSix, associationSeven, associationEight,
                associationNine, associationTen, associationEleven ) );

    }

    @Test
    void testInsertAssociation() {
        final var associationUser = new AssociationDao();
        Pageable pageable = PageRequest.of(1, 10);
        associationUser.setCompanyNumber("1114422");
        associationUser.setUserId("9999");
        associationUser.setStatus(StatusEnum.CONFIRMED.getValue());
        associationUser.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationUser.setUserEmail("abc@abc.com");
        associationUser.setEtag("OCRS7");
        associationUser.setApprovalExpiryAt(LocalDateTime.now().plusDays(10));
        associationUser.setInvitations(new ArrayList<>());
        associationsRepository.insert(associationUser);
        final var invitationUser = new InvitationDao();
        invitationUser.setInvitedBy("123456");
        invitationUser.setInvitedAt(LocalDateTime.now().plusDays(10));
        invitationDaos.add(invitationUser);
        associationUser.setInvitations(invitationDaos);

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
            AssociationDao associationDao = new AssociationDao();
            associationDao.setEtag(null);
            associationsRepository.save(associationDao);
        });
    }

    @Test
    void updateApprovalRouteWithNullThrowsConstraintViolationException() {
        assertThrows(ConstraintViolationException.class, () -> {
            AssociationDao associationDao = new AssociationDao();
            associationDao.setApprovalRoute(null);
            associationsRepository.save(associationDao);
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
        Assertions.assertEquals( StatusEnum.CONFIRMED.getValue(), associationsRepository.findById( "111" ).get().getStatus());

    }
    @Test
    void fetchAssociationWithInValidUserEmail() {
        Assertions.assertEquals(" ", associationsRepository.findById("333").get().getUserEmail());
    }

    @Test
    void updateStatusBasedOnId(){
        final var update = new Update().set("status", StatusEnum.REMOVED.getValue());
        associationsRepository.updateUser( "111", update );
        Assertions.assertEquals( StatusEnum.REMOVED.getValue(), associationsRepository.findById( "111" ).get().getStatus() );
    }

    @Test
    void updateStatusWithNullThrowsIllegalStateException(){
        assertThrows( IllegalStateException.class, () -> associationsRepository.updateUser( "111", null ) );
    }
    @Test
    void setFutureDateToApprovedAtFieldThrowsConstraintViolationException() {
        LocalDateTime futureDateTime = LocalDateTime.now().minusDays(1);
        AssociationDao associationDao = new AssociationDao();
        associationDao.setApprovedAt(futureDateTime);

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
            associationsRepository.save(associationDao);
        });
    }

    @Test
    void setFutureDateToRemovedAtFieldThrowsConstraintViolationException() {
        LocalDateTime futureDateTime = LocalDateTime.now().plusDays(5);
        AssociationDao associationDao = new AssociationDao();
        associationDao.setRemovedAt(futureDateTime);

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
            associationsRepository.save(associationDao);
        });
    }

    @Test
    void setPastDateToApprovalExpiryAtFieldThrowsConstraintViolationException() {
        LocalDateTime pastDateTime = LocalDateTime.now().minusDays(20);
        AssociationDao associationDao = new AssociationDao();
        associationDao.setApprovalExpiryAt(pastDateTime);

        assertThrows(jakarta.validation.ConstraintViolationException.class, () -> {
            associationsRepository.save(associationDao);
        });
    }

    @Test
    void getAssociationsForCompanyNumberAndStatusConfirmedAndCompanyNumberLikeShouldReturnAAssociation() {
        Assertions.assertEquals(1, associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike("777", Collections.singletonList("confirmed"),"10",Pageable.ofSize(1)).getTotalElements());
        Assertions.assertEquals(1, associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike("777", Collections.singletonList("confirmed"),"",Pageable.ofSize(1)).getTotalElements());
    }

    @Test
    void findAllByUserIdAndStatusIsInAndCompanyNumberLikeWithNullOrNonexistentOrMalformedUserIdOrNullOrEmptyOrMalformedStatusReturnsEmptyPage(){
        Assertions.assertTrue( associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike(null, List.of(StatusEnum.CONFIRMED.getValue() ),"",PageRequest.of(0, 5) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike("9191", List.of(StatusEnum.CONFIRMED.getValue() ),"",PageRequest.of(0, 5) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike("$$$$", List.of(StatusEnum.CONFIRMED.getValue() ),"",PageRequest.of(0, 5) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike("111", null,"",PageRequest.of(0, 5) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike("111", List.of(),"",PageRequest.of(0, 5) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike("111", List.of("complicated"),"",PageRequest.of(0, 5) ).isEmpty() );
    }

    @Test
    void findAllByUserIdAndStatusIsInAndCompanyNumberLikeImplementsPaginationCorrectly(){
         final var page = associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike("111", List.of(StatusEnum.CONFIRMED.getValue() ),"",PageRequest.of(1, 1) );
         final var ids = page.getContent().stream().map(AssociationDao::getCompanyNumber).toList();
         Assertions.assertTrue( ids.contains( "222222" ) );
         Assertions.assertEquals( 1, ids.size() );
    }

    @Test
    void findAllByUserIdAndStatusIsInAndCompanyNumberLikeWithNullPageableReturnsAllAssociations(){
        final var page = associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike("111", List.of(StatusEnum.CONFIRMED.getValue() ),"",null );
        final var ids = page.getContent().stream().map(AssociationDao::getCompanyNumber).toList();
        Assertions.assertTrue( ids.containsAll( List.of( "111111", "222222" ) ) );
        Assertions.assertEquals( 2, ids.size() );
    }

    @Test
    void findAllByUserIdAndStatusIsInAndCompanyNumberLikeFiltersBasedOnCompanyNumber(){
        final var page = associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike("111", List.of(StatusEnum.CONFIRMED.getValue() ),"222222",PageRequest.of(0, 15) );
        final var ids = page.getContent().stream().map(AssociationDao::getCompanyNumber).toList();
        Assertions.assertTrue( ids.contains( "222222" ) );
        Assertions.assertEquals( 1, ids.size() );
    }

    @Test
    void findAllByUserIdAndStatusIsInAndCompanyNumberLikeWithNullCompanyNumberThrowsIllegalArgumentException(){
        Assertions.assertThrows( IllegalArgumentException.class, () -> associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike("111", List.of(StatusEnum.CONFIRMED.getValue() ),null,PageRequest.of(0, 15) ) );
    }

    @Test
    void findAllByUserIdAndStatusIsInAndCompanyNumberLikeWithMalformedOrNonexistentCompanyNumberReturnsEmptyPage(){
        Assertions.assertTrue( associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike("111", List.of(StatusEnum.CONFIRMED.getValue() ),"$$$$$$",PageRequest.of(0, 15) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike("111", List.of(StatusEnum.CONFIRMED.getValue() ),"abdef",PageRequest.of(0, 15) ).isEmpty() );
    }

    @Test
    void updateVersionFieldIncrementsVersion() {

        AssociationDao associationDao = new AssociationDao();
        associationDao.setCompanyNumber("1114422");
        associationDao.setUserId("9999");
        associationDao.setStatus(StatusEnum.CONFIRMED.getValue());
        associationDao.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationDao.setUserEmail("abc@abc.com");
        associationDao.setEtag("OCRS7");
        associationDao = associationsRepository.save(associationDao);

        AssociationDao savedAssociationDao = associationsRepository.findById(associationDao.getId()).orElseThrow();

        savedAssociationDao.setCompanyNumber("555555");
        associationsRepository.save(savedAssociationDao);

        AssociationDao updatedAssociationDao = associationsRepository.findById(savedAssociationDao.getId()).orElseThrow();

        Assertions.assertEquals(associationDao.getVersion() + 1, updatedAssociationDao.getVersion());
    }

    @Test
    void fetchAssociatedUsersWithNullOrMalformedOrNonexistentCompanyNumberReturnsEmptyPage(){
        Assertions.assertTrue( associationsRepository.fetchAssociatedUsers( null, Set.of( StatusEnum.CONFIRMED.getValue() ), PageRequest.of( 0, 3 ) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociatedUsers( "$", Set.of( StatusEnum.CONFIRMED.getValue() ), PageRequest.of( 0, 3 ) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociatedUsers( "999999", Set.of( StatusEnum.CONFIRMED.getValue() ), PageRequest.of( 0, 3 ) ).isEmpty() );
    }

    @Test
    void fetchAssociatedUsersWithNullStatusesThrowsUncategorizedMongoDbException(){
        Assertions.assertThrows( UncategorizedMongoDbException.class, () -> associationsRepository.fetchAssociatedUsers( "111111", null, PageRequest.of( 0, 3 ) ) );
    }

    @Test
    void fetchAssociatedUsersWithNullPageableReturnsAllAssociatedUsers(){
        Assertions.assertEquals( 2, associationsRepository.fetchAssociatedUsers( "111111", Set.of( StatusEnum.CONFIRMED.getValue() ), null ).getNumberOfElements() );
    }

    @Test
    void fetchAssociatedUsersFiltersBasedOnSpecifiedStatuses(){
        Assertions.assertTrue( associationsRepository.fetchAssociatedUsers( "333333", Set.of(),  PageRequest.of( 0, 15 ) ).isEmpty() );

        final var queryWithConfirmedFilter =
                associationsRepository.fetchAssociatedUsers( "333333", Set.of( StatusEnum.CONFIRMED.getValue() ),  PageRequest.of( 0, 15 ) )
                        .getContent()
                        .stream()
                        .map( AssociationDao::getUserId )
                        .toList();
        Assertions.assertEquals( 2, queryWithConfirmedFilter.size() );
        Assertions.assertTrue( queryWithConfirmedFilter.containsAll( List.of( "444", "333" ) ) );

        final var queryWithConfirmedAndAwaitingFilter =
                associationsRepository.fetchAssociatedUsers( "333333", Set.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue() ),  PageRequest.of( 0, 15 ) )
                        .getContent()
                        .stream()
                        .map( AssociationDao::getUserId )
                        .toList();
        Assertions.assertEquals( 4, queryWithConfirmedAndAwaitingFilter.size() );
        Assertions.assertTrue( queryWithConfirmedAndAwaitingFilter.containsAll( List.of( "444", "333", "101010", "111111" ) ) );
    }

    @Test
    void fetchAssociatedUsersPaginatesCorrectly(){
        final var secondPage = associationsRepository.fetchAssociatedUsers( "333333", Set.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue() ), PageRequest.of( 1, 2 ) );
        final var secondPageContent =
                secondPage.getContent()
                        .stream()
                        .map( AssociationDao::getUserId )
                        .toList();

        Assertions.assertEquals( 5, secondPage.getTotalElements() );
        Assertions.assertEquals( 3, secondPage.getTotalPages() );
        Assertions.assertEquals( 2, secondPageContent.size() );
        Assertions.assertTrue( secondPageContent.containsAll( List.of( "888", "101010" ) ) );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }
}
