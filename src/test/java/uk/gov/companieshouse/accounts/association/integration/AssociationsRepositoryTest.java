package uk.gov.companieshouse.accounts.association.integration;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.utils.ApiClientUtil;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

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
    EmailProducer emailProducer;

    @MockBean
    KafkaProducerFactory kafkaProducerFactory;

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
        final var now = LocalDateTime.now();

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

        final var invitationThirtyOne = new InvitationDao();
        invitationThirtyOne.setInvitedBy("111");
        invitationThirtyOne.setInvitedAt( now.plusDays(56) );

        final var associationThirtyOne = new AssociationDao();
        associationThirtyOne.setCompanyNumber("x777777");
        associationThirtyOne.setUserId("99999");
        associationThirtyOne.setUserEmail("scrooge.mcduck@disney.land");
        associationThirtyOne.setStatus(StatusEnum.REMOVED.getValue());
        associationThirtyOne.setId("31");
        associationThirtyOne.setApprovedAt( now.plusDays(53) );
        associationThirtyOne.setRemovedAt( now.plusDays(54) );
        associationThirtyOne.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationThirtyOne.setApprovalExpiryAt( now.plusDays(55) );
        associationThirtyOne.setInvitations( List.of( invitationThirtyOne ) );
        associationThirtyOne.setEtag("nn");

        final var associationTest = new AssociationDao();
        associationTest.setId("1111");
        associationTest.setCompanyNumber("123455");
        associationTest.setUserId("5555");
        associationTest.setStatus(StatusEnum.CONFIRMED.getValue());
        associationTest.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        // associationTest.setUserEmail("abc@abc.com");
        associationTest.setApprovalExpiryAt(LocalDateTime.now().plusDays(10));
        associationTest.setInvitations(new ArrayList<>());
        associationTest.setEtag("OCRS7");

        final var invitationTest = new InvitationDao();
        invitationTest.setInvitedBy("123456");
        invitationTest.setInvitedAt(LocalDateTime.now().plusDays(10));
        invitationDaos.add(invitationTest);
        associationTest.setInvitations(invitationDaos);

        final var associationTestTwo = new AssociationDao();
        associationTestTwo.setId("2222");
        associationTestTwo.setCompanyNumber("123456");
        //  associationTwo.setUserId("666");
        associationTestTwo.setStatus(StatusEnum.CONFIRMED.getValue());
        associationTestTwo.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationTestTwo.setUserEmail("abcd@abc.com");
        associationTestTwo.setApprovalExpiryAt(LocalDateTime.now().plusDays(10));
        associationTestTwo.setInvitations(new ArrayList<>());
        associationTestTwo.setEtag("OCRS7");

        final var invitationTestTwo = new InvitationDao();
        invitationTestTwo.setInvitedBy("123456");
        invitationTestTwo.setInvitedAt(LocalDateTime.now().plusDays(10));
        invitationDaos.add(invitationTestTwo);
        associationTestTwo.setInvitations(invitationDaos);

        final var invitationSeventeen = new InvitationDao();
        invitationSeventeen.setInvitedBy("111");
        invitationSeventeen.setInvitedAt( now.minusDays(68) );

        final var associationSeventeen = new AssociationDao();
        associationSeventeen.setCompanyNumber("222222P");
        associationSeventeen.setUserId("8888");
        associationSeventeen.setUserEmail("mr.blobby@nightmare.com");
        associationSeventeen.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationSeventeen.setId("17");
        associationSeventeen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationSeventeen.setApprovalExpiryAt( now.plusDays(67) );
        associationSeventeen.setInvitations( List.of( invitationSeventeen ) );
        associationSeventeen.setEtag("q");

        final var invitationEighteenOldest = new InvitationDao();
        invitationEighteenOldest.setInvitedBy("666");
        invitationEighteenOldest.setInvitedAt(now.minusDays(9));

        final var invitationEighteenMedian = new InvitationDao();
        invitationEighteenMedian.setInvitedBy("333");
        invitationEighteenMedian.setInvitedAt(now.minusDays(6));

        final var invitationEighteenNewest = new InvitationDao();
        invitationEighteenNewest.setInvitedBy("444");
        invitationEighteenNewest.setInvitedAt(now.minusDays(4));

        final var associationEighteen = new AssociationDao();
        associationEighteen.setCompanyNumber("333333P");
        associationEighteen.setUserId("99999");
        associationEighteen.setUserEmail("scrooge.mcduck1@disney.land");
        associationEighteen.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationEighteen.setId("18");
        associationEighteen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationEighteen.setApprovalExpiryAt(now.plusDays(10));
        associationEighteen.setInvitations( List.of( invitationEighteenMedian, invitationEighteenOldest, invitationEighteenNewest ) );
        associationEighteen.setEtag( "aa" );

        final var invitationNineteenOldest = new InvitationDao();
        invitationNineteenOldest.setInvitedBy("111");
        invitationNineteenOldest.setInvitedAt( now.minusDays(3) );

        final var invitationNineteenMedian = new InvitationDao();
        invitationNineteenMedian.setInvitedBy("222");
        invitationNineteenMedian.setInvitedAt( now.minusDays(2) );

        final var invitationNineteenNewest = new InvitationDao();
        invitationNineteenNewest.setInvitedBy("444");
        invitationNineteenNewest.setInvitedAt( now.minusDays(1) );

        final var associationNineteen = new AssociationDao();
        associationNineteen.setCompanyNumber("444444P");
        associationNineteen.setUserId("99999");
        associationNineteen.setUserEmail("scrooge.mcduck1@disney.land");
        associationNineteen.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationNineteen.setId("19");
        associationNineteen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationNineteen.setApprovalExpiryAt( now.plusDays(20) );
        associationNineteen.setInvitations( List.of( invitationNineteenOldest, invitationNineteenMedian, invitationNineteenNewest ) );
        associationNineteen.setEtag("bb");

        final var invitationTwenty = new InvitationDao();
        invitationTwenty.setInvitedBy("666");
        invitationTwenty.setInvitedAt( now.minusDays(12) );

        final var associationTwenty = new AssociationDao();
        associationTwenty.setCompanyNumber("555555P");
        associationTwenty.setUserId("99999");
        associationTwenty.setUserEmail("scrooge.mcduck1@disney.land");
        associationTwenty.setStatus(StatusEnum.CONFIRMED.getValue());
        associationTwenty.setId("20");
        associationTwenty.setApprovedAt( now.plusDays(9) );
        associationTwenty.setRemovedAt( now.plusDays(10) );
        associationTwenty.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwenty.setApprovalExpiryAt( now.plusDays(11) );
        associationTwenty.setInvitations( List.of( invitationTwenty ) );
        associationTwenty.setEtag("cc");

        final var invitationTwentyOne = new InvitationDao();
        invitationTwentyOne.setInvitedBy("666");
        invitationTwentyOne.setInvitedAt( now.minusDays(16) );

        final var associationTwentyOne = new AssociationDao();
        associationTwentyOne.setCompanyNumber("666666P");
        associationTwentyOne.setUserId("99999");
        associationTwentyOne.setUserEmail("scrooge.mcduck1@disney.land");
        associationTwentyOne.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentyOne.setId("21");
        associationTwentyOne.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentyOne.setApprovalExpiryAt( now.minusDays(15) );
        associationTwentyOne.setInvitations( List.of( invitationTwentyOne ) );
        associationTwentyOne.setEtag("dd");


        associationDaos = associationsRepository.saveAll( List.of(
                associationOne, associationTwo, associationThree, associationFour,
                associationFive, associationSix, associationSeven, associationEight,
                associationNine, associationTen, associationEleven, associationThirtyOne, associationTest, associationTestTwo,
                associationSeventeen, associationEighteen, associationNineteen, associationTwenty, associationTwentyOne) );

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
        AssociationDao associationDao = new AssociationDao();
        associationDao.setEtag(null);
        assertThrows(ConstraintViolationException.class, () -> {
            associationsRepository.save(associationDao);
        });
    }

    @Test
    void updateApprovalRouteWithNullThrowsConstraintViolationException() {
        AssociationDao associationDao = new AssociationDao();
        associationDao.setApprovalRoute(null);
        assertThrows(ConstraintViolationException.class, () -> {
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
        Assertions.assertEquals(1, associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("777", " ", Collections.singletonList("confirmed"),"10",Pageable.ofSize(1)).getTotalElements());
        Assertions.assertEquals(1, associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("777", " ", Collections.singletonList("confirmed"),"",Pageable.ofSize(1)).getTotalElements());
        Assertions.assertEquals(1, associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike(" ", "abcd@abc.com", Collections.singletonList("confirmed"),"",Pageable.ofSize(1)).getTotalElements());
    }

    @Test
    void findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLikeWithNonexistentOrMalformedUserIdOrUserEmailOrEmptyOrMalformedStatusReturnsEmptyPage(){
        Assertions.assertTrue( associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("1234", "**@abc.com", List.of(StatusEnum.CONFIRMED.getValue() ),"",PageRequest.of(0, 5) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("1234","$$$", List.of(StatusEnum.CONFIRMED.getValue() ),"",PageRequest.of(0, 5) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("9191", "abcde@abc.com", List.of(StatusEnum.CONFIRMED.getValue() ),"",PageRequest.of(0, 5) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("$$$$","abcde@abc.com", List.of(StatusEnum.CONFIRMED.getValue() ),"",PageRequest.of(0, 5) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("111","abcde@abc.com", List.of(),"",PageRequest.of(0, 5) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("111","abcde@abc.com", List.of("complicated"),"",PageRequest.of(0, 5) ).isEmpty() );
    }

    @Test
    void findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLikeImplementsPaginationCorrectly(){
         final var page = associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("5555","abcd@abc.com", List.of(StatusEnum.CONFIRMED.getValue() ),"12345",PageRequest.of(1, 1) );
         final var ids = page.getContent().stream().map(AssociationDao::getCompanyNumber).toList();
         Assertions.assertTrue( ids.contains( "123456" ) );
         Assertions.assertEquals( 1, ids.size() );
    }

    @Test
    void findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLikeWithNullPageableReturnsAllAssociations(){
        final var page1 = associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("5555","abcd@abc.com" ,List.of(StatusEnum.CONFIRMED.getValue() ),"",null );
        final var ids = page1.getContent().stream().map(AssociationDao::getCompanyNumber).toList();
        Assertions.assertTrue( ids.containsAll( List.of( "123455", "123456" ) ) );
        Assertions.assertEquals( 2, ids.size() );
    }

    @Test
    void findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLikeFiltersBasedOnCompanyNumber(){
        final var page = associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("5555","abcd@abc.com", List.of(StatusEnum.CONFIRMED.getValue() ),"123456",PageRequest.of(0, 15) );
        final var ids = page.getContent().stream().map(AssociationDao::getCompanyNumber).toList();
        Assertions.assertTrue( ids.contains( "123456" ) );
        Assertions.assertEquals( 1, ids.size() );
    }

    @Test
    void findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLikeWithNullCompanyNumberThrowsUncategorizedMongoDbException(){
        Assertions.assertThrows( UncategorizedMongoDbException.class, () -> associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("5555","abcd@abc.com", List.of(StatusEnum.CONFIRMED.getValue() ),null ,PageRequest.of(0, 15) ) );
    }

    @Test
    void findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLikeWithMalformedOrNonexistentCompanyNumberReturnsEmptyPage(){
        Assertions.assertTrue( associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("5555","abcd@abc.com", List.of(StatusEnum.CONFIRMED.getValue() ),"$556",PageRequest.of(0, 15) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("5555","abcd@abc.com", List.of(StatusEnum.CONFIRMED.getValue() ),"abdef",PageRequest.of(0, 15) ).isEmpty() );
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

    static Stream<Arguments> nullAndMalformedParameters() {
        return Stream.of(
                Arguments.of(null, "111"),
                Arguments.of("$$$$$$", "111"),
                Arguments.of("919191", "111"),
                Arguments.of("111111", null),
                Arguments.of("111111", "$$$"),
                Arguments.of("111111", "9191")
        );
    }

    @ParameterizedTest
    @MethodSource("nullAndMalformedParameters")
    void associationExistsWithStatusesWithNullOrMalformedOrNonExistentCompanyNumberOrUserReturnsFalse(String companyNumber, String userId) {
        final List<String> statuses = Arrays.stream(StatusEnum.values()).map(StatusEnum::toString).toList();
        Assertions.assertFalse(associationsRepository.associationExistsWithStatuses(companyNumber, userId, statuses));
    }

    @Test
    void associationExistsWithExistingConfirmedAssociationReturnsTrue(){
        Assertions.assertTrue( associationsRepository.associationExistsWithStatuses( "111111", "111", List.of(StatusEnum.CONFIRMED.getValue()) ) );
    }

    @Test
    void fetchAssociationForCompanyNumberUserEmailAndStatusWithNullOrMalformedOrNonexistentCompanyNumberOrUserEmailReturnsEmptyPage(){
        Assertions.assertTrue(associationsRepository.fetchAssociationForCompanyNumberUserEmailAndStatus( null, "abc@abc.com", Set.of( StatusEnum.CONFIRMED.getValue() ), PageRequest.of( 0, 15 ) ).isEmpty());
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberUserEmailAndStatus( "$$$$$$", "abc@abc.com", Set.of( StatusEnum.CONFIRMED.getValue() ), PageRequest.of( 0, 15 ) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberUserEmailAndStatus( "919191", "abc@abc.com", Set.of( StatusEnum.CONFIRMED.getValue() ), PageRequest.of( 0, 15 ) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberUserEmailAndStatus( "12345", null, Set.of( StatusEnum.CONFIRMED.getValue() ), PageRequest.of( 0, 15 ) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberUserEmailAndStatus( "12345", "$$$", Set.of( StatusEnum.CONFIRMED.getValue() ), PageRequest.of( 0, 15 ) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberUserEmailAndStatus( "12345", "the.void@space.com", Set.of( StatusEnum.CONFIRMED.getValue() ), PageRequest.of( 0, 15 ) ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberUserEmailAndStatusWithNullStatusThrowsException(){
        Assertions.assertThrows( UncategorizedMongoDbException.class, () -> associationsRepository.fetchAssociationForCompanyNumberUserEmailAndStatus( "12345", "abc@abc.com", null, PageRequest.of( 0, 15 ) ) );
    }

    @Test
    void fetchAssociationForCompanyNumberUserEmailAndStatusWithEmptyStatusesOrNullOrMalformedStatusReturnsEmptyPage(){
        final var nullSet = new HashSet<String>();
        nullSet.add(null);

        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberUserEmailAndStatus( "12345", "abc@abc.com", Set.of(), PageRequest.of( 0, 15 ) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberUserEmailAndStatus( "12345", "abc@abc.com", nullSet, PageRequest.of( 0, 15 ) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberUserEmailAndStatus( "12345", "abc@abc.com", Set.of( "complicated" ), PageRequest.of( 0, 15 ) ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberUserEmailAndStatusRetrievesAssociation(){
         Assertions.assertEquals( "111", associationsRepository.fetchAssociationForCompanyNumberUserEmailAndStatus( "12345", "abc@abc.com", Set.of( StatusEnum.CONFIRMED.getValue() ), PageRequest.of( 0, 15 ) ).getContent().getFirst().getId() );
    }

    @Test
    void fetchAssociationForCompanyNumberUserEmailAndStatusWithoutPageableRetrievesAllAssociationsThatSatisfyQuery(){
        final var associations = associationsRepository.fetchAssociationForCompanyNumberUserEmailAndStatus( "12345", "abc@abc.com", Set.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue() ), null );
        Assertions.assertEquals( 2, associations.getTotalElements() );
        Assertions.assertEquals( "111", associations.getContent().getFirst().getId() );
        Assertions.assertEquals( "222", associations.getContent().getLast().getId() );
    }

    @Test
    void updateAssociationWithNullOrMalformedOrNonexistentAssociationIdDoesNotUpdateAnyRows(){
        final var setStatusToRemoved = new Update().set( "status", StatusEnum.REMOVED.getValue() );

        Assertions.assertEquals( 0, associationsRepository.updateAssociation( null, setStatusToRemoved ) );
        Assertions.assertEquals( 0, associationsRepository.updateAssociation( "$$$", setStatusToRemoved ) );
        Assertions.assertEquals( 0, associationsRepository.updateAssociation( "919", setStatusToRemoved ) );
    }

    @Test
    void updateAssociationWithNullUpdateThrowsIllegalStateException(){
        Assertions.assertThrows( IllegalStateException.class, () -> associationsRepository.updateAssociation( "111", null ) );
    }

    @Test
    void updateAssociationPerformsUpdate(){
        final var setStatusToRemoved = new Update().set( "status", StatusEnum.REMOVED.getValue() );
        final var numRowsUpdated = associationsRepository.updateAssociation( "111", setStatusToRemoved );

        Assertions.assertEquals( 1, numRowsUpdated );
        Assertions.assertEquals( StatusEnum.REMOVED.getValue(), associationsRepository.findById( "111" ).get().getStatus() );
    }

    private AssociationDao createMinimalistAssociationForCompositeKeyTests( String userId, String userEmail, String companyNumber ){
        final var association = new AssociationDao();
        association.setUserId( userId );
        association.setUserEmail( userEmail );
        association.setCompanyNumber( companyNumber );
        association.setStatus( StatusEnum.REMOVED.getValue() );
        association.setEtag( "a" );
        association.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE.getValue() );
        return association;
    }

    @Test
    @DirtiesContext( methodMode = MethodMode.BEFORE_METHOD)
    void compositeKeyWithNullAndNullEmailAndNullAndNullUserIdThrowsDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( null, null, "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( null, null, "K000001" );
        Assertions.assertThrows( DuplicateKeyException.class, () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithNullAndNullEmailAndNotNullAndNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", null, "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( null, null, "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithNullAndNullEmailAndNullAndNotNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( null, null, "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K001", null, "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    @DirtiesContext( methodMode = MethodMode.BEFORE_METHOD)
    void compositeKeyWithNullAndNullEmailAndNullDuplicateNotAndNotNullUserIdThrowsDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", null, "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K001", null, "K000001" );
        Assertions.assertThrows( DuplicateKeyException.class, () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithNullAndNullEmailAndDistinctNullNotAndNotNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", null, "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K002", null, "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithNotNullAndNullEmailAndNullAndNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( null, "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( null, null, "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithNotNullAndNullEmailAndNotNullAndNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( null, null, "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithNotNullAndNullEmailAndNullAndNotNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( null, "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K001", null, "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithNotNullAndNullEmailAndDuplicateNotNullAndNotNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K001", null, "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithNotNullAndNullEmailAndDistinctNotNullAndNotNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K002", null, "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithNullAndNotNullEmailAndNullAndNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( null, null, "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( null, "madonna@singer.com", "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithNullAndNotNullEmailAndNotNullAndNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", null, "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( null, "madonna@singer.com", "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithNullAndNotNullEmailAndNullAndNotNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( null, null, "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K001", "madonna@singer.com", "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithNullAndNotNullEmailAndDuplicateNotNullAndNotNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", null, "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K001", "madonna@singer.com", "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithNullAndNotNullEmailAndUniqueNotNullAndNotNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", null, "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K002", "madonna@singer.com", "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    @DirtiesContext( methodMode = MethodMode.BEFORE_METHOD)
    void compositeKeyWithDuplicateNotNullAndNotNullEmailAndNullAndNullUserIdThrowsDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( null, "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( null, "madonna@singer.com", "K000001" );
        Assertions.assertThrows( DuplicateKeyException.class, () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithDuplicateNotNullAndNotNullEmailAndNotNullAndNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( null, "madonna@singer.com", "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithDuplicateNotNullAndNotNullEmailAndNullAndNotNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( null, "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K001", "madonna@singer.com", "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    @DirtiesContext( methodMode = MethodMode.BEFORE_METHOD)
    void compositeKeyWithDuplicateNotNullAndNotNullEmailAndDuplicateNotNullAndNotNullUserIdThrowsDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K001", "madonna@singer.com", "K000001" );
        Assertions.assertThrows( DuplicateKeyException.class, () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithDuplicateNotNullAndNotNullEmailAndUniqueNotNullAndNotNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K002", "madonna@singer.com", "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithUniqueNotNullAndNotNullEmailAndNullAndNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( null, "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( null, "micahel.jackson@singer.com", "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithUniqueNotNullAndNotNullEmailAndNotNullAndNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( null, "micahel.jackson@singer.com", "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithUniqueNotNullAndNotNullEmailAndNullAndNotNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( null, "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K001", "micahel.jackson@singer.com", "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithUniqueNotNullAndNotNullEmailAndDuplicateNotNullAndNotNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K001", "micahel.jackson@singer.com", "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithUniqueNotNullAndNotNullEmailAndUniqueNotNullAndNotNullUserIdDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K002", "micahel.jackson@singer.com", "K000001" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithTheSameUserIdButDifferentCompanyNumbersDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( "K001", null, "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( "K001", null, "K000002" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void compositeKeyWithTheSameUserEmailButDifferentCompanyNumbersDoesNotThrowDuplicateKeyException(){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( null, "madonna@singer.com", "K000001" );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( null, "madonna@singer.com", "K000002" );
        Assertions.assertDoesNotThrow( () -> associationsRepository.insert(List.of(associationOne, associationTwo)) );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserEmailWithNullOrMalformedOrNonexistentCompanyNumberReturnsNothing(){
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberAndUserEmail( null, "abc@abc.com" ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberAndUserEmail( "$$$$$$", "abc@abc.com" ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberAndUserEmail( "919191", "abc@abc.com" ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserEmailWithMalformedOrNonexistentUserEmailReturnsNothing(){
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberAndUserEmail( "12345", "$$$$$$" ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberAndUserEmail( "12345", "the.void@space.com" ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserEmailRetrievesAssociation(){
        Assertions.assertEquals( "31", associationsRepository.fetchAssociationForCompanyNumberAndUserEmail( "x777777", "scrooge.mcduck@disney.land" ).get().getId() ); ;
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserIdWithNullOrMalformedOrNonexistentCompanyNumberReturnsNothing(){
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberAndUserId( null, "99999" ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberAndUserId( "$$$$$$", "99999" ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberAndUserId( "919191", "99999" ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserIdWithMalformedOrNonexistentUserIdReturnsNothing(){
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberAndUserId( "12345", "$$$$$$" ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationForCompanyNumberAndUserId( "12345", "919191" ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserIdRetrievesAssociation(){
        Assertions.assertEquals( "31", associationsRepository.fetchAssociationForCompanyNumberAndUserId( "x777777", "99999" ).get().getId() ); ;
    }

    @Test
    void fetchAssociationsWithActiveInvitationsWithNullOrMalformedOrNonExistentUserIdOrEmailOrNullTimestampReturnsEmptyStream(){
        Assertions.assertTrue( associationsRepository.fetchAssociationsWithActiveInvitations( null, null, LocalDateTime.now() ).toList().isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsWithActiveInvitations( "$$$", null,LocalDateTime.now() ).toList().isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsWithActiveInvitations( "9191", null,LocalDateTime.now() ).toList().isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsWithActiveInvitations( "99999", null,null ).toList().isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsWithActiveInvitations( null, "$$$",LocalDateTime.now() ).toList().isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsWithActiveInvitations( null, "ronald@mcdonalds.com",LocalDateTime.now() ).toList().isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsWithActiveInvitations( null, "ronald@mcdonalds.com",null ).toList().isEmpty() );
    }

    @Test
    void fetchAssociationsWithActiveInvitationsBasedOnUserIdAppliesFiltersCorrectly(){
        final var associationIds = associationsRepository.fetchAssociationsWithActiveInvitations( "99999", null, LocalDateTime.now() )
                .map( AssociationDao::getId )
                .toList();

        Assertions.assertEquals( 2, associationIds.size() );
        Assertions.assertTrue( associationIds.containsAll( List.of( "18", "19" ) ) );
    }
    
    @Test
    void fetchAssociationsWithActiveInvitationsBasedOnUserEmailAppliesFiltersCorrectly(){
        final var associationIds = associationsRepository.fetchAssociationsWithActiveInvitations( null, "scrooge.mcduck1@disney.land", LocalDateTime.now() )
                .map( AssociationDao::getId )
                .toList();

        Assertions.assertEquals( 2, associationIds.size() );
        Assertions.assertTrue( associationIds.containsAll( List.of( "18", "19" ) ) );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }
}
