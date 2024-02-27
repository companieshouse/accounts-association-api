package uk.gov.companieshouse.accounts.association.integration;

import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.ApiClientService;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;

@SpringBootTest
@Testcontainers
@Tag("integration-test")
class AssociationsServiceTest {

    @MockBean
    ApiClientService apiClientService;

    @Container
    @ServiceConnection
    static MongoDBContainer container = new MongoDBContainer("mongo:5");

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    AssociationsRepository associationsRepository;

    @Autowired
    private AssociationsService associationsService;

    @BeforeEach
    public void setup() {
        final var associationOne = new Association();
        associationOne.setCompanyNumber("111111");
        associationOne.setUserId("111");
        associationOne.setStatus( StatusEnum.CONFIRMED );

        final var associationTwo = new Association();
        associationTwo.setCompanyNumber("222222");
        associationTwo.setUserId("111");
        associationTwo.setStatus( StatusEnum.CONFIRMED );

        final var associationThree = new Association();
        associationThree.setCompanyNumber("111111");
        associationThree.setUserId("222");
        associationThree.setStatus( StatusEnum.CONFIRMED );

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
    void fetchAssociatedUsersWithNullOrMalformedOrNonexistentCompanyNumberReturnsEmptyPage(){
        Assertions.assertTrue( associationsService.fetchAssociatedUsers( null, true, 15, 0 ).isEmpty() );
        Assertions.assertTrue( associationsService.fetchAssociatedUsers( "$", true, 15, 0 ).isEmpty() );
        Assertions.assertTrue( associationsService.fetchAssociatedUsers( "999999", true, 15, 0 ).isEmpty() );
    }

    @Test
    void fetchAssociatedUsersAppliesIncludeRemovedFilterCorrectly(){

        final var unfilteredPage =
        associationsService.fetchAssociatedUsers( "333333", true, 15, 0 )
                           .getContent()
                           .stream()
                           .map( Association::getUserId )
                           .toList();
        Assertions.assertEquals( 5, unfilteredPage.size() );
        Assertions.assertTrue( unfilteredPage.containsAll( List.of( "444", "555", "666", "777", "888" ) ) );

        final var filteredPage =
        associationsService.fetchAssociatedUsers( "333333", false, 15, 0 )
                           .getContent()
                           .stream()
                           .map( Association::getUserId )
                           .toList();
        Assertions.assertEquals( 4, filteredPage.size() );
        Assertions.assertTrue( filteredPage.containsAll( List.of( "444", "555", "666", "777" ) ) );
    }

    @Test
    void fetchAssociatedUsersPaginatesCorrectly(){
        final var secondPage = associationsService.fetchAssociatedUsers( "333333", true, 2, 1 );
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