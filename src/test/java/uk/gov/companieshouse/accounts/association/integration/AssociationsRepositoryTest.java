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
    void fetchAssociationsForUserAndStatusesAndPartialCompanyNumberShouldReturnAAssociation() {
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3" ) );
        Assertions.assertEquals(1, associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("111", null, Set.of("confirmed"),"111111", PageRequest.of(0, 10)).getTotalElements());
        Assertions.assertEquals(1, associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("222", "the.joker@gotham.city", Set.of("confirmed"),"111111", PageRequest.of(0, 10)).getTotalElements());
        Assertions.assertEquals(1, associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("333", "harley.quinn@gotham.city", Set.of("confirmed"),"111111",PageRequest.of(0, 10)).getTotalElements());
    }

    @Test
    void fetchAssociationsForUserAndStatusesAndPartialCompanyNumberWithNonexistentOrMalformedUserIdOrUserEmailOrEmptyOrMalformedStatusReturnsEmptyPage(){
        Assertions.assertTrue( associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("1234", "**@abc.com", Set.of(StatusEnum.CONFIRMED.getValue() ),"",PageRequest.of(0, 5) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("1234","$$$", Set.of(StatusEnum.CONFIRMED.getValue() ),"",PageRequest.of(0, 5) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("9191", "abcde@abc.com", Set.of(StatusEnum.CONFIRMED.getValue() ),"",PageRequest.of(0, 5) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("$$$$","abcde@abc.com", Set.of(StatusEnum.CONFIRMED.getValue() ),"",PageRequest.of(0, 5) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("111","abcde@abc.com", Set.of(),"",PageRequest.of(0, 5) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("111","abcde@abc.com", Set.of("complicated"),"",PageRequest.of(0, 5) ).isEmpty() );
    }

    @Test
    void fetchAssociationsForUserAndStatusesAndPartialCompanyNumberImplementsPaginationCorrectly(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22" ) );

        final var page = associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("9999",null, Set.of(StatusEnum.CONFIRMED.getValue() ),"",PageRequest.of(1, 1) );
        final var ids = page.getContent().stream().map(AssociationDao::getCompanyNumber).toList();
        Assertions.assertTrue( ids.contains( "444444" ) );
        Assertions.assertEquals( 1, ids.size() );
    }

    @Test
    void fetchAssociationsForUserAndStatusesAndPartialCompanyNumberWithNullPageableReturnsAllAssociations(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22" ) );

        final var page = associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("9999",null, Set.of(StatusEnum.CONFIRMED.getValue() ),"", null);
        final var ids = page.getContent().stream().map(AssociationDao::getCompanyNumber).toList();
        Assertions.assertTrue( ids.containsAll( List.of( "333333", "444444", "555555", "666666", "777777" ) ) );
        Assertions.assertEquals( 5, ids.size() );
    }

    @Test
    void fetchAssociationsForUserAndStatusesAndPartialCompanyNumberFiltersBasedOnCompanyNumber(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22" ) );

        final var page = associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("9999",null, Set.of(StatusEnum.CONFIRMED.getValue() ),"333333", null);
        final var ids = page.getContent().stream().map(AssociationDao::getCompanyNumber).toList();
        Assertions.assertTrue( ids.contains( "333333" ) );
        Assertions.assertEquals( 1, ids.size() );
    }

    @Test
    void fetchAssociationsForUserAndStatusesAndPartialCompanyNumberWithNullCompanyNumberThrowsUncategorizedMongoDbException(){
        final var statuses = Set.of(StatusEnum.CONFIRMED.getValue());
        final var pageRequest = PageRequest.of(0, 15);
        Assertions.assertThrows( UncategorizedMongoDbException.class, () -> associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("5555","abcd@abc.com", statuses ,null ,pageRequest ) );
    }

    @Test
    void fetchAssociationsForUserAndStatusesAndPartialCompanyNumberWithMalformedOrNonexistentCompanyNumberReturnsEmptyPage(){
        Assertions.assertTrue( associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("5555","abcd@abc.com", Set.of(StatusEnum.CONFIRMED.getValue() ),"$556",PageRequest.of(0, 15) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("5555","abcd@abc.com", Set.of(StatusEnum.CONFIRMED.getValue() ),"abdef",PageRequest.of(0, 15) ).isEmpty() );
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
    void fetchUnexpiredAssociationsForCompanyAndStatusesWithNullOrMalformedOrNonexistentCompanyNumberReturnsEmptyPage(){
        Assertions.assertTrue( associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatuses( null, Set.of( StatusEnum.CONFIRMED.getValue() ), LocalDateTime.now(), PageRequest.of( 0, 3 ) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatuses( "$", Set.of( StatusEnum.CONFIRMED.getValue() ), LocalDateTime.now(), PageRequest.of( 0, 3 ) ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatuses( "999999", Set.of( StatusEnum.CONFIRMED.getValue() ), LocalDateTime.now(), PageRequest.of( 0, 3 ) ).isEmpty() );
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesWithNullStatusesThrowsUncategorizedMongoDbException(){
        final var now = LocalDateTime.now();
        final var pageRequest = PageRequest.of( 0, 3 );
        Assertions.assertThrows( UncategorizedMongoDbException.class, () -> associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatuses( "111111", null, now, pageRequest ) );
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesWithNullPageableReturnsAllAssociatedUsers(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2" ) );
        Assertions.assertEquals( 2, associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatuses( "111111", Set.of( StatusEnum.CONFIRMED.getValue() ), LocalDateTime.now(), null ).getNumberOfElements() );
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesFiltersBasedOnSpecifiedStatusesAndExpiryTime(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "6", "14" ) );

        Assertions.assertTrue( associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatuses( "111111", Set.of(), LocalDateTime.now(), PageRequest.of( 0, 15 ) ).isEmpty() );

        final var queryWithConfirmedFilter =
                associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatuses( "111111", Set.of( StatusEnum.CONFIRMED.getValue() ), LocalDateTime.now(), PageRequest.of( 0, 15 ) )
                        .getContent()
                        .stream()
                        .map( AssociationDao::getUserId )
                        .toList();
        Assertions.assertEquals( 1, queryWithConfirmedFilter.size() );
        Assertions.assertTrue( queryWithConfirmedFilter.contains( "111" ) );

        final var queryWithConfirmedAndAwaitingFilter =
                associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatuses( "111111", Set.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue() ), LocalDateTime.now(), PageRequest.of( 0, 15 ) )
                        .getContent()
                        .stream()
                        .map( AssociationDao::getUserId )
                        .toList();
        Assertions.assertEquals( 2, queryWithConfirmedAndAwaitingFilter.size() );
        Assertions.assertTrue( queryWithConfirmedAndAwaitingFilter.containsAll( List.of( "111", "666" ) ) );
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesPaginatesCorrectly(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "6", "14" ) );

        final var secondPage = associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatuses( "111111", Set.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue() ), LocalDateTime.now(), PageRequest.of( 1, 2 ) );
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

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesAndUserAppliesCompanyAndStatusAndUserIdFiltersCorrectly(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MiAssociation002", "MiAssociation004", "MiAssociation006", "MiAssociation009", "MiAssociation033" ) );

        final var associations = associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatusesAndUser( "MICOMP001", Set.of( "confirmed" ), "MiUser002", "lechuck.monkey.island@inugami-example.com", LocalDateTime.now(), PageRequest.of( 0, 15 ) );

        Assertions.assertEquals( 1, associations.getContent().size() );
        Assertions.assertEquals( "MiAssociation002", associations.getContent().getFirst().getId() );
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesAndUserAppliesUserEmailAndUnexpiredFilterCorrectly(){
        final var associations = testDataManager.fetchAssociationDaos( "MiAssociation002", "MiAssociation004", "MiAssociation009", "MiAssociation033" );
        associations.add( testDataManager.fetchAssociationDaos( "MiAssociation006" ).getFirst().userId( null ).userEmail( "lechuck.monkey.island@inugami-example.com" ) );
        associationsRepository.insert( associations );

        final var retrievedAssociations = associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatusesAndUser( "MICOMP005", Set.of( "awaiting-approval" ), "MiUser002", "lechuck.monkey.island@inugami-example.com", LocalDateTime.now(), PageRequest.of( 0, 15 ) );

        Assertions.assertEquals( 1, retrievedAssociations.getContent().size() );
        Assertions.assertEquals( "MiAssociation006", retrievedAssociations.getContent().getFirst().getId() );
    }

    private static Stream<Arguments> fetchUnexpiredAssociationsForCompanyAndStatusesAndUserNegativeFiltersTestData(){
        return Stream.of(
                Arguments.of( null, Set.of( "awaiting-approval" ), "MiUser002", "lechuck.monkey.island@inugami-example.com" ),
                Arguments.of( "404COMP", Set.of( "confirmed" ), "MiUser002", "lechuck.monkey.island@inugami-example.com" ),
                Arguments.of( "MICOMP001", Set.of( "complicated" ), "MiUser002", "lechuck.monkey.island@inugami-example.com" ),
                Arguments.of( "MICOMP001", Set.of(), "MiUser002", "lechuck.monkey.island@inugami-example.com" ),
                Arguments.of( "MICOMP001", Set.of( "confirmed" ), "404User", "404User@inugami-example.com" ),
                Arguments.of( "MICOMP001", Set.of( "confirmed" ), null, null ),
                Arguments.of( "MICOMP003", Set.of( "awaiting-approval" ), "MiUser002", "lechuck.monkey.island@inugami-example.com" )
        );
    }

    @ParameterizedTest
    @MethodSource( "fetchUnexpiredAssociationsForCompanyAndStatusesAndUserNegativeFiltersTestData" )
    void fetchUnexpiredAssociationsForCompanyAndStatusesAndUserFiltersOutResultsWhenCompanyOrUserOrStatusesDoNotExistOrInvitationHasExpired( final String companyNumber, final Set<String> statuses, final String userId, final String userEmail ){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MiAssociation002", "MiAssociation004", "MiAssociation006", "MiAssociation009", "MiAssociation033" ) );

        final var associations = associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatusesAndUser( companyNumber, statuses, userId, userEmail, LocalDateTime.now(), PageRequest.of( 0, 15 ) );

        Assertions.assertTrue( associations.isEmpty() );
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesAndUserWithNullStatusesThrowsUncategorizedMongoDbException(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MiAssociation002", "MiAssociation004", "MiAssociation006", "MiAssociation009", "MiAssociation033" ) );

        Assertions.assertThrows( UncategorizedMongoDbException.class, () -> associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatusesAndUser( "MICOMP001", null, "MiUser002", "lechuck.monkey.island@inugami-example.com", LocalDateTime.now(), PageRequest.of( 0, 15 ) ) );
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesAndUserWithUserFiltersFetchesAssociation(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MiAssociation005" ).getFirst().companyNumber( "MICOMP001" ) );
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MiAssociation002", "MiAssociation003" ) );

        final var page = associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatusesAndUser( "MICOMP001", Set.of( StatusEnum.CONFIRMED.getValue() ), "MiUser002", "lechuck.monkey.island@inugami-example.com", LocalDateTime.now(), PageRequest.of( 0, 2 ) );

        Assertions.assertEquals( 1 , page.getContent().size() );
        Assertions.assertEquals( "MiAssociation002", page.getContent().getFirst().getId() );
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesAndUserWithUserFiltersDoesNotFetchesAssociation(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MiAssociation005" ).getFirst().companyNumber( "MICOMP001" ) );
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "MiAssociation002", "MiAssociation003" ) );

        final var page = associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatusesAndUser( "MICOMP001", Set.of( StatusEnum.CONFIRMED.getValue() ), "test", "test@inugami-example.com", LocalDateTime.now(), PageRequest.of( 0, 2 ) );

        Assertions.assertTrue( page.isEmpty() );
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
    void confirmedAssociationExistsWithStatusesWithNullOrMalformedOrNonExistentCompanyNumberOrUserReturnsFalse( final String companyNumber, final String userId) {
        Assertions.assertFalse(associationsRepository.confirmedAssociationExists(companyNumber, userId));
    }

    @Test
    void confirmedAssociationExistsWithExistingConfirmedAssociationReturnsTrue(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        Assertions.assertTrue( associationsRepository.confirmedAssociationExists( "111111", "111" ) );
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
        Assertions.assertTrue( associationsRepository.fetchAssociation( null, null, "abc@abc.com" ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociation( "$$$$$$", null, "abc@abc.com" ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociation( "919191", null, "abc@abc.com" ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserEmailWithMalformedOrNonexistentUserEmailReturnsNothing(){
        Assertions.assertTrue( associationsRepository.fetchAssociation( "12345", null, "$$$$$$" ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociation( "12345", null, "the.void@space.com" ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserEmailRetrievesAssociation(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        Assertions.assertEquals( "1", associationsRepository.fetchAssociation( "111111", null, "bruce.wayne@gotham.city" ).get().getId() );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserIdWithNullOrMalformedOrNonexistentCompanyNumberReturnsNothing(){
        Assertions.assertTrue( associationsRepository.fetchAssociation( null, "99999", null ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociation( "$$$$$$", "99999", null ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociation( "919191", "99999", null ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserIdWithMalformedOrNonexistentUserIdReturnsNothing(){
        Assertions.assertTrue( associationsRepository.fetchAssociation( "12345", "$$$$$$", null ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociation( "12345", "919191", null ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserIdRetrievesAssociation(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        Assertions.assertEquals( "1", associationsRepository.fetchAssociation( "111111", "111", null ).get().getId() ); ;
    }

    @Test
    void fetchAssociationsWithActiveInvitationsWithNullOrMalformedOrNonExistentUserIdOrEmailOrNullTimestampReturnsEmptyStream(){
        Assertions.assertTrue( associationsRepository.fetchAssociationsWithActiveInvitations( null, null, LocalDateTime.now() ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsWithActiveInvitations( "$$$", null,LocalDateTime.now() ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsWithActiveInvitations( "9191", null,LocalDateTime.now() ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsWithActiveInvitations( "99999", null,null ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsWithActiveInvitations( null, "$$$",LocalDateTime.now() ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsWithActiveInvitations( null, "ronald@mcdonalds.com",LocalDateTime.now() ).isEmpty() );
        Assertions.assertTrue( associationsRepository.fetchAssociationsWithActiveInvitations( null, "ronald@mcdonalds.com",null ).isEmpty() );
    }

    @Test
    void fetchAssociationsWithActiveInvitationsBasedOnUserIdAppliesFiltersCorrectly(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "23" ) );

        final var associationIds = associationsRepository.fetchAssociationsWithActiveInvitations( "9999", null, LocalDateTime.now() )
                .stream()
                .map( AssociationDao::getId )
                .toList();

        Assertions.assertEquals( 1, associationIds.size() );
        Assertions.assertTrue( associationIds.contains( "23" ) );
    }

    @Test
    void fetchAssociationsWithActiveInvitationsBasedOnUserEmailAppliesFiltersCorrectly(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "6" ) );

        final var associationIds = associationsRepository.fetchAssociationsWithActiveInvitations( null, "homer.simpson@springfield.com", LocalDateTime.now() )
                .stream()
                .map( AssociationDao::getId )
                .toList();

        Assertions.assertEquals( 1, associationIds.size() );
        Assertions.assertTrue( associationIds.contains( "6" ) );
    }

    private static Stream<Arguments> fetchConfirmedAssociationsEmptyStreamScenarios(){
        return Stream.of(
                Arguments.of( (String) null ),
                Arguments.of( "$$$" ),
                Arguments.of( "404COMP" ),
                Arguments.of( "111111" )
        );
    }

    @ParameterizedTest
    @MethodSource( "fetchConfirmedAssociationsEmptyStreamScenarios" )
    void fetchConfirmedAssociationsWithNullCompanyNumberReturnsEmptyStream( final String companyNumber ){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "6" ) );
        Assertions.assertTrue( associationsRepository.fetchConfirmedAssociations( companyNumber ).toList().isEmpty() );
    }

    @Test
    void fetchConfirmedAssociationsRetrievesConfirmedAssociationsForCompanyNumber(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "5", "6" ) );
        final var confirmedAssociations = associationsRepository.fetchConfirmedAssociations( "111111" ).toList();
        Assertions.assertEquals( 1, confirmedAssociations.size() );
        Assertions.assertEquals( "5", confirmedAssociations.getFirst().getId() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }
}
