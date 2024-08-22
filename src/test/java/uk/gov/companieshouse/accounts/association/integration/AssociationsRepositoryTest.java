package uk.gov.companieshouse.accounts.association.integration;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.MethodMode;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Tag("integration-test")
class AssociationsRepositoryTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @MockBean
    private EmailProducer emailProducer;

    @MockBean
    private KafkaProducerFactory kafkaProducerFactory;

    @Autowired
    private AssociationsRepository associationsRepository;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void testInsertAssociation() {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        Assertions.assertEquals( 1, associationsRepository.findAllByUserId( "111" , PageRequest.of( 0, 1 )) .getTotalElements() );
    }

    @Test
    void fetchAssociationWithInvalidOrNonexistentUserIDReturnsEmpty() {
        final var pageable = PageRequest.of(1, 10);
        Assertions.assertTrue( associationsRepository.findAllByUserId( "567" , pageable).isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserId( "@£@" , pageable).isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserId( null , pageable).isEmpty() );
        Assertions.assertTrue( associationsRepository.findAllByUserId( "" ,pageable).isEmpty() );
    }

    @Test
    void fetchAssociationWithValidUserID() {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        Assertions.assertEquals( 1, associationsRepository.findAllByUserId( "111" , PageRequest.of(1, 10)).getTotalElements() );
    }

    @Test
    void updateEtagWithNullThrowsConstraintViolationException() {
        final var associationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        associationDao.setEtag(null);
        assertThrows( ConstraintViolationException.class, () -> associationsRepository.save(associationDao) );
    }

    @Test
    void updateApprovalRouteWithNullThrowsConstraintViolationException() {
        final var associationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        associationDao.setApprovalRoute(null);
        assertThrows(ConstraintViolationException.class, () -> associationsRepository.save(associationDao) );
    }

    @Test
    void fetchAssociationWithValidID() {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        Assertions.assertTrue( associationsRepository.findById( "1").isPresent() );
    }

    @Test
    void findByIdWithMalformedInputOrNonexistentIdReturnsEmptyOptional(){
        Assertions.assertFalse( associationsRepository.findById( "" ).isPresent() );
        Assertions.assertFalse( associationsRepository.findById( "$" ).isPresent() );
        Assertions.assertFalse( associationsRepository.findById( "999" ).isPresent() );
    }
    @Test
    void fetchAssociationWithValidValues() {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        final var associationDao = associationsRepository.findById( "1" ).get();
        Assertions.assertEquals( "111", associationDao.getUserId());
        Assertions.assertEquals( "111111", associationDao.getCompanyNumber());
        Assertions.assertEquals( "bruce.wayne@gotham.city", associationDao.getUserEmail());
        Assertions.assertEquals( StatusEnum.CONFIRMED.getValue(), associationDao.getStatus());
    }
    @Test
    void fetchAssociationWithInValidUserEmail() {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "38" ) );
        Assertions.assertNull(associationsRepository.findById("38").get().getUserEmail());
    }

    @Test
    void updateStatusBasedOnId(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        final var update = new Update().set("status", StatusEnum.REMOVED.getValue());
        associationsRepository.updateUser( "1", update );
        Assertions.assertEquals( StatusEnum.REMOVED.getValue(), associationsRepository.findById( "1" ).get().getStatus() );
    }

    @Test
    void updateStatusWithNullThrowsIllegalStateException(){
        assertThrows( IllegalStateException.class, () -> associationsRepository.updateUser( "1", null ) );
    }

    @Test
    void getAssociationsForCompanyNumberAndStatusConfirmedAndCompanyNumberLikeShouldReturnAAssociation() {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3" ) );
        Assertions.assertEquals(1, associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("111", null, Collections.singletonList("confirmed"),"111111", PageRequest.of(0, 10)).getTotalElements());
        Assertions.assertEquals(1, associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("222", "the.joker@gotham.city", Collections.singletonList("confirmed"),"111111", PageRequest.of(0, 10)).getTotalElements());
        Assertions.assertEquals(1, associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("333", "harley.quinn@gotham.city", Collections.singletonList("confirmed"),"111111",PageRequest.of(0, 10)).getTotalElements());
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
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22" ) );

        final var page = associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("9999",null, List.of(StatusEnum.CONFIRMED.getValue() ),"",PageRequest.of(1, 1) );
        final var ids = page.getContent().stream().map(AssociationDao::getCompanyNumber).toList();
        Assertions.assertTrue( ids.contains( "444444" ) );
        Assertions.assertEquals( 1, ids.size() );
    }

    @Test
    void findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLikeWithNullPageableReturnsAllAssociations(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22" ) );

        final var page = associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("9999",null, List.of(StatusEnum.CONFIRMED.getValue() ),"", null);
        final var ids = page.getContent().stream().map(AssociationDao::getCompanyNumber).toList();
        Assertions.assertTrue( ids.containsAll( List.of( "333333", "444444", "555555", "666666", "777777" ) ) );
        Assertions.assertEquals( 5, ids.size() );
    }

    @Test
    void findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLikeFiltersBasedOnCompanyNumber(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22" ) );

        final var page = associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("9999",null, List.of(StatusEnum.CONFIRMED.getValue() ),"333333", null);
        final var ids = page.getContent().stream().map(AssociationDao::getCompanyNumber).toList();
        Assertions.assertTrue( ids.contains( "333333" ) );
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
        var associationDao = associationsRepository.save( testDataManager.fetchAssociationDaos( "1" ).getFirst() );
        final var savedAssociationDao = associationsRepository.findById(associationDao.getId()).orElseThrow();

        savedAssociationDao.setCompanyNumber("555555");
        associationsRepository.save(savedAssociationDao);
        final var updatedAssociationDao = associationsRepository.findById(savedAssociationDao.getId()).orElseThrow();

        Assertions.assertEquals(associationDao.getVersion() + 1, updatedAssociationDao.getVersion());
    }

    @Test
    void fetchAssociatedUsersWithNullOrMalformedOrNonexistentCompanyNumberReturnsEmptyPage(){
        Assertions.assertTrue( associationsRepository.fetchAssociatedUsers( null, Set.of( StatusEnum.CONFIRMED.getValue() ), LocalDateTime.now(), PageRequest.of( 0, 3 ) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociatedUsers( "$", Set.of( StatusEnum.CONFIRMED.getValue() ), LocalDateTime.now(), PageRequest.of( 0, 3 ) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociatedUsers( "999999", Set.of( StatusEnum.CONFIRMED.getValue() ), LocalDateTime.now(), PageRequest.of( 0, 3 ) ).isEmpty() );
    }

    @Test
    void fetchAssociatedUsersWithNullStatusesThrowsUncategorizedMongoDbException(){
        Assertions.assertThrows( UncategorizedMongoDbException.class, () -> associationsRepository.fetchAssociatedUsers( "111111", null, LocalDateTime.now(), PageRequest.of( 0, 3 ) ) );
    }

    @Test
    void fetchAssociatedUsersWithNullPageableReturnsAllAssociatedUsers(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2" ) );
        Assertions.assertEquals( 2, associationsRepository.fetchAssociatedUsers( "111111", Set.of( StatusEnum.CONFIRMED.getValue() ), LocalDateTime.now(), null ).getNumberOfElements() );
    }

    @Test
    void fetchAssociatedUsersFiltersBasedOnSpecifiedStatusesAndExpiryTime(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "6", "14" ) );

        Assertions.assertTrue( associationsRepository.fetchAssociatedUsers( "111111", Set.of(), LocalDateTime.now(), PageRequest.of( 0, 15 ) ).isEmpty() );

        final var queryWithConfirmedFilter =
                associationsRepository.fetchAssociatedUsers( "111111", Set.of( StatusEnum.CONFIRMED.getValue() ), LocalDateTime.now(), PageRequest.of( 0, 15 ) )
                        .getContent()
                        .stream()
                        .map( AssociationDao::getUserId )
                        .toList();
        Assertions.assertEquals( 1, queryWithConfirmedFilter.size() );
        Assertions.assertTrue( queryWithConfirmedFilter.contains( "111" ) );

        final var queryWithConfirmedAndAwaitingFilter =
                associationsRepository.fetchAssociatedUsers( "111111", Set.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue() ), LocalDateTime.now(), PageRequest.of( 0, 15 ) )
                        .getContent()
                        .stream()
                        .map( AssociationDao::getUserId )
                        .toList();
        Assertions.assertEquals( 2, queryWithConfirmedAndAwaitingFilter.size() );
        Assertions.assertTrue( queryWithConfirmedAndAwaitingFilter.containsAll( List.of( "111", "666" ) ) );
    }

    @Test
    void fetchAssociatedUsersPaginatesCorrectly(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "6", "14" ) );

        final var secondPage = associationsRepository.fetchAssociatedUsers( "111111", Set.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue() ), LocalDateTime.now(), PageRequest.of( 1, 2 ) );
        final var secondPageContent =
                secondPage.getContent()
                        .stream()
                        .map( AssociationDao::getUserId )
                        .toList();

        Assertions.assertEquals( 3, secondPage.getTotalElements() );
        Assertions.assertEquals( 2, secondPage.getTotalPages() );
        Assertions.assertEquals( 1, secondPageContent.size() );
        Assertions.assertTrue( secondPageContent.contains( "5555" ) );
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
    void associationExistsWithStatusesWithNullOrMalformedOrNonExistentCompanyNumberOrUserReturnsFalse( final String companyNumber, final String userId) {
        final var statuses = Arrays.stream(StatusEnum.values()).map(StatusEnum::toString).toList();
        Assertions.assertFalse(associationsRepository.associationExistsWithStatuses(companyNumber, userId, statuses));
    }

    @Test
    void associationExistsWithExistingConfirmedAssociationReturnsTrue(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
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
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        Assertions.assertEquals( "1", associationsRepository.fetchAssociationForCompanyNumberUserEmailAndStatus( "111111", "bruce.wayne@gotham.city", Set.of( StatusEnum.CONFIRMED.getValue() ), PageRequest.of( 0, 15 ) ).getContent().getFirst().getId() );
    }

    @Test
    void fetchAssociationForCompanyNumberUserEmailAndStatusWithoutPageableRetrievesAllAssociationsThatSatisfyQuery(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "6", "14" ) );

        final var associations = associationsRepository.fetchAssociationForCompanyNumberUserEmailAndStatus( "111111", "bruce.wayne@gotham.city", Set.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue() ), null );
        Assertions.assertEquals( 1, associations.getTotalElements() );
        Assertions.assertEquals( "1", associations.getContent().getFirst().getId() );
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
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );

        final var setStatusToRemoved = new Update().set( "status", StatusEnum.REMOVED.getValue() );
        final var numRowsUpdated = associationsRepository.updateAssociation( "1", setStatusToRemoved );

        Assertions.assertEquals( 1, numRowsUpdated );
        Assertions.assertEquals( StatusEnum.REMOVED.getValue(), associationsRepository.findById( "1" ).get().getStatus() );
    }

    private AssociationDao createMinimalistAssociationForCompositeKeyTests( final String userId, final String userEmail, final String companyNumber ){
        final var association = new AssociationDao();
        association.setUserId( userId );
        association.setUserEmail( userEmail );
        association.setCompanyNumber( companyNumber );
        association.setStatus( StatusEnum.REMOVED.getValue() );
        association.setEtag( "a" );
        association.setApprovalRoute( ApprovalRouteEnum.AUTH_CODE.getValue() );
        return association;
    }

    private static Stream<Arguments> compositeKeyTestDataset(){
        return Stream.of(
                Arguments.of( "K001", null, "K000001", "K001", null, "K000001", true ),
                Arguments.of( null, "madonna@singer.com", "K000001", null, "madonna@singer.com", "K000001", true ),
                Arguments.of( "K001", "madonna@singer.com", "K000001", "K001", "madonna@singer.com", "K000001", true ),
                Arguments.of( null, null, "K000001", null, null, "K000001", true ),
                Arguments.of( "K001", null, "K000001", null, null, "K000001", false ),
                Arguments.of( null, null, "K000001", "K001", null, "K000001", false ),
                Arguments.of( "K001", null, "K000001", "K002", null, "K000001", false ),
                Arguments.of( null, "madonna@singer.com", "K000001", null, null, "K000001", false ),
                Arguments.of( "K001", "madonna@singer.com", "K000001", null, null, "K000001", false ),
                Arguments.of( null, "madonna@singer.com", "K000001", "K001", null, "K000001", false ),
                Arguments.of( "K001", "madonna@singer.com", "K000001", "K001", null, "K000001", false ),
                Arguments.of( "K001", "madonna@singer.com", "K000001", "K002", null, "K000001", false ),
                Arguments.of( null, null, "K000001", null, "madonna@singer.com", "K000001", false ),
                Arguments.of( "K001", null, "K000001", null, "madonna@singer.com", "K000001", false ),
                Arguments.of( null, null, "K000001", "K001", "madonna@singer.com", "K000001", false ),
                Arguments.of( "K001", null, "K000001", "K001", "madonna@singer.com", "K000001", false ),
                Arguments.of( "K001", null, "K000001", "K002", "madonna@singer.com", "K000001", false ),
                Arguments.of( "K001", "madonna@singer.com", "K000001", null, "madonna@singer.com", "K000001", false ),
                Arguments.of( null, "madonna@singer.com", "K000001", "K001", "madonna@singer.com", "K000001", false ),
                Arguments.of( "K001", "madonna@singer.com", "K000001", "K002", "madonna@singer.com", "K000001", false ),
                Arguments.of( null, "madonna@singer.com", "K000001", null, "micahel.jackson@singer.com", "K000001", false ),
                Arguments.of( "K001", "madonna@singer.com", "K000001", null, "micahel.jackson@singer.com", "K000001", false ),
                Arguments.of( null, "madonna@singer.com", "K000001", "K001", "micahel.jackson@singer.com", "K000001", false ),
                Arguments.of( "K001", "madonna@singer.com", "K000001", "K001", "micahel.jackson@singer.com", "K000001", false ),
                Arguments.of( "K001", "madonna@singer.com", "K000001", "K002", "micahel.jackson@singer.com", "K000001", false ),
                Arguments.of( "K001", null, "K000001", "K001", null, "K000002", false ),
                Arguments.of( null, "madonna@singer.com", "K000001", null, "madonna@singer.com", "K000002", false )
        );
    }

    @ParameterizedTest
    @MethodSource("compositeKeyTestDataset")
    @DirtiesContext( methodMode = MethodMode.BEFORE_METHOD )
    void compositeKeyTests( final String firstAssociationUserId, final String firstAssociationUserEmail, final String firstAssociationCompanyNumber, final String secondAssociationUserId, final String secondAssociationUserEmail, final String secondAssociationCompanyNumber, final boolean throwsDuplicateKeyException ){
        final var associationOne = createMinimalistAssociationForCompositeKeyTests( firstAssociationUserId, firstAssociationUserEmail, firstAssociationCompanyNumber );
        final var associationTwo = createMinimalistAssociationForCompositeKeyTests( secondAssociationUserId, secondAssociationUserEmail, secondAssociationCompanyNumber );
        final Executable insertAssociations = () -> associationsRepository.insert( List.of( associationOne, associationTwo ) );

        if ( throwsDuplicateKeyException ){
            Assertions.assertThrows( DuplicateKeyException.class, insertAssociations );
        } else {
            Assertions.assertDoesNotThrow( insertAssociations );
        }
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
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        Assertions.assertEquals( "1", associationsRepository.fetchAssociationForCompanyNumberAndUserEmail( "111111", "bruce.wayne@gotham.city" ).get().getId() );
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
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        Assertions.assertEquals( "1", associationsRepository.fetchAssociationForCompanyNumberAndUserId( "111111", "111" ).get().getId() ); ;
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
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "23" ) );

        final var associationIds = associationsRepository.fetchAssociationsWithActiveInvitations( "9999", null, LocalDateTime.now() )
                .map( AssociationDao::getId )
                .toList();

        Assertions.assertEquals( 1, associationIds.size() );
        Assertions.assertTrue( associationIds.contains( "23" ) );
    }

    @Test
    void fetchAssociationsWithActiveInvitationsBasedOnUserEmailAppliesFiltersCorrectly(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "6" ) );

        final var associationIds = associationsRepository.fetchAssociationsWithActiveInvitations( null, "homer.simpson@springfield.com", LocalDateTime.now() )
                .map( AssociationDao::getId )
                .toList();

        Assertions.assertEquals( 1, associationIds.size() );
        Assertions.assertTrue( associationIds.contains( "6" ) );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }
}
