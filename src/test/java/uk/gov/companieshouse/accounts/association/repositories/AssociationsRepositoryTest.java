package uk.gov.companieshouse.accounts.association.repositories;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.UncategorizedMongoDbException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.service.ApiClientService;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;

import static org.assertj.core.api.Assertions.assertThat;
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

    @BeforeEach
    public void setup() {
        final var associationOne = new Association();
        associationOne.setCompanyNumber("111111");
        associationOne.setUserId("111");
        associationOne.setStatus(StatusEnum.CONFIRMED);

        final var associationTwo = new Association();
        associationTwo.setCompanyNumber("222222");
        associationTwo.setUserId("111");
        associationTwo.setStatus(StatusEnum.CONFIRMED);

        final var associationThree = new Association();
        associationThree.setCompanyNumber("111111");
        associationThree.setUserId("222");
        associationThree.setStatus(StatusEnum.CONFIRMED);

        final var associationFour = new Association();
        associationFour.setCompanyNumber("333333");
        associationFour.setUserId("444");
        associationFour.setStatus(StatusEnum.CONFIRMED);

        final var associationFive = new Association();
        associationFive.setCompanyNumber("333333");
        associationFive.setUserId("555");
        associationFive.setStatus(StatusEnum.CONFIRMED);

        final var associationSix = new Association();
        associationSix.setCompanyNumber("333333");
        associationSix.setUserId("666");
        associationSix.setStatus(StatusEnum.AWAITING_APPROVAL);

        final var associationSeven = new Association();
        associationSeven.setCompanyNumber("333333");
        associationSeven.setUserId("777");
        associationSeven.setStatus(StatusEnum.AWAITING_APPROVAL);

        final var associationEight = new Association();
        associationEight.setCompanyNumber("333333");
        associationEight.setUserId("888");
        associationEight.setStatus(StatusEnum.REMOVED);

        associationsRepository.insert( List.of( associationOne, associationTwo, associationThree, associationFour,
                associationFive, associationSix, associationSeven, associationEight ) );
    }

    @Test
    @DirtiesContext
    void testToNotAllowNotNullStatus() {
        assertThrows(ConstraintViolationException.class, () -> {
            final var associationUser = new Association();
            associationUser.setCompanyNumber("111111");
            associationUser.setUserId("111");
            associationsRepository.save(associationUser);
        });

    }

    @Test
    void findByCompanyNumberSearchQueryShouldReturnTwoAssociations() {
        final var association4 = new Association();
        association4.setCompanyNumber("1112222");
        association4.setUserId("111");
        association4.setStatus(StatusEnum.CONFIRMED);
        associationsRepository.insert(association4);

        assertThat(associationsRepository.findByUserIdAndCompanyNumberLike("111","111", Pageable.ofSize(1)).getTotalElements()).isEqualTo(2);

    }

    @Test
    void findByCompanyNumberSearchQueryShouldReturnTwoAssociationsWhenNoCompanyNumberProvided() {
        final var association4 = new Association();
        association4.setCompanyNumber("1112222");
        association4.setUserId("111");
        association4.setStatus(StatusEnum.CONFIRMED);
        associationsRepository.insert(association4);

        assertThat(associationsRepository.findByUserIdAndCompanyNumberLike("111","", Pageable.ofSize(1)).getTotalElements()).isEqualTo(3);

    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void shouldNotAllowDuplicateAssociations() {
        final var associationUser = new Association();
        associationUser.setCompanyNumber("111111");
        associationUser.setUserId("111");
        associationUser.setStatus(StatusEnum.CONFIRMED);

        assertThrows(DuplicateKeyException.class, () -> {

            associationsRepository.save(associationUser);
        });

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
        Assertions.assertTrue( queryWithConfirmedFilter.containsAll( List.of( "444", "555" ) ) );

        final var queryWithConfirmedAndAwaitingFilter =
        associationsRepository.fetchAssociatedUsers( "333333", Set.of( StatusEnum.CONFIRMED, StatusEnum.AWAITING_APPROVAL ),  PageRequest.of( 0, 15 ) )
                              .getContent()
                              .stream()
                              .map( Association::getUserId )
                              .toList();
        Assertions.assertEquals( 4, queryWithConfirmedAndAwaitingFilter.size() );
        Assertions.assertTrue( queryWithConfirmedAndAwaitingFilter.containsAll( List.of( "444", "555", "666", "777" ) ) );
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
        Assertions.assertTrue( secondPageContent.containsAll( List.of( "666", "777" ) ) );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Association.class);
    }
}