package uk.gov.companieshouse.accounts.association.repositories;

import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.service.ApiClientService;

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
        associationOne.setStatus("Confirmed");

        final var associationTwo = new Association();
        associationTwo.setCompanyNumber("222222");
        associationTwo.setUserId("111");
        associationTwo.setStatus("Confirmed");

        final var associationThree = new Association();
        associationThree.setCompanyNumber("111111");
        associationThree.setUserId("222");
        associationThree.setStatus("Confirmed");

        associationsRepository.insert(associationOne);
        associationsRepository.insert(associationTwo);
        associationsRepository.insert(associationThree);

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
        association4.setStatus("Confirmed");
        associationsRepository.insert(association4);

        assertThat(associationsRepository.findByUserIdAndCompanyNumberLike("111","111", Pageable.ofSize(1)).getTotalElements()).isEqualTo(2);

    }

    @Test
    void findByCompanyNumberSearchQueryShouldReturnTwoAssociationsWhenNoCompanyNumberProvided() {
        final var association4 = new Association();
        association4.setCompanyNumber("1112222");
        association4.setUserId("111");
        association4.setStatus("Confirmed");
        associationsRepository.insert(association4);

        assertThat(associationsRepository.findByUserIdAndCompanyNumberLike("111","", Pageable.ofSize(1)).getTotalElements()).isEqualTo(3);

    }

    @Test
    @DirtiesContext(methodMode = DirtiesContext.MethodMode.BEFORE_METHOD)
    void shouldNotAllowDuplicateAssociations() {
        final var associationUser = new Association();
        associationUser.setCompanyNumber("111111");
        associationUser.setUserId("111");
        associationUser.setStatus("New");

        assertThrows(DuplicateKeyException.class, () -> {

            associationsRepository.save(associationUser);
        });

    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Association.class);
    }
}