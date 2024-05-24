package uk.gov.companieshouse.accounts.association.integration;

import java.time.LocalDateTime;
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
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.repositories.InvitationsRepository;
import uk.gov.companieshouse.accounts.association.utils.ApiClientUtil;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

@SpringBootTest
@Testcontainers(parallel = true)
@Tag("integration-test")
public class InvitationsRepositoryTest {

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

    @MockBean
    StaticPropertyUtil staticPropertyUtil;

    @Autowired
    InvitationsRepository invitationsRepository;

    @Autowired
    AssociationsRepository associationsRepository;

    private final LocalDateTime now = LocalDateTime.now();

    @BeforeEach
    public void setup(){

        final var invitationSeventeen = new InvitationDao();
        invitationSeventeen.setInvitedBy("111");
        invitationSeventeen.setInvitedAt( now.minusDays(68) );

        final var associationSeventeen = new AssociationDao();
        associationSeventeen.setCompanyNumber("222222");
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
        associationEighteen.setCompanyNumber("333333");
        associationEighteen.setUserId("9999");
        associationEighteen.setUserEmail("scrooge.mcduck@disney.land");
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
        associationNineteen.setCompanyNumber("444444");
        associationNineteen.setUserId("9999");
        associationNineteen.setUserEmail("scrooge.mcduck@disney.land");
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
        associationTwenty.setCompanyNumber("555555");
        associationTwenty.setUserId("9999");
        associationTwenty.setUserEmail("scrooge.mcduck@disney.land");
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
        associationTwentyOne.setCompanyNumber("666666");
        associationTwentyOne.setUserId("9999");
        associationTwentyOne.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentyOne.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentyOne.setId("21");
        associationTwentyOne.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentyOne.setApprovalExpiryAt( now.minusDays(15) );
        associationTwentyOne.setInvitations( List.of( invitationTwentyOne ) );
        associationTwentyOne.setEtag("dd");

        associationsRepository.saveAll( List.of( associationSeventeen, associationEighteen, associationNineteen, associationTwenty, associationTwentyOne ) );
    }

    @Test
    void fetchActiveInvitationsAppliesFiltersCorrectly(){
        final var associationIds =
        invitationsRepository.fetchActiveInvitations( "9999" )
                .stream()
                .map( AssociationDao::getId )
                .toList();

        Assertions.assertEquals( 2, associationIds.size() );
        Assertions.assertTrue( associationIds.containsAll( List.of( "18", "19" ) ) );
    }

    private String reduceTimestampResolution( String timestamp ){
        return timestamp.substring( 0, timestamp.indexOf( ":" ) );
    }

    private String localDateTimeToNormalisedString( LocalDateTime localDateTime ){
        final var timestamp = localDateTime.toString();
        return reduceTimestampResolution( timestamp );
    }

    @Test
    void fetchActiveInvitationsOnlyRetrievesMostRecentInvitationsAndReturnsInvitationsInCorrectOrder(){
        final var invitations =
        invitationsRepository.fetchActiveInvitations( "9999" )
                .stream()
                .map( AssociationDao::getInvitations )
                .map( List::getFirst )
                .toList();

        Assertions.assertEquals( 2, invitations.size() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.minusDays( 1 ) ), localDateTimeToNormalisedString( invitations.getFirst().getInvitedAt() ) );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.minusDays( 4 ) ), localDateTimeToNormalisedString( invitations.getLast().getInvitedAt() ) );
    }

    @Test
    void fetchActiveInvitationsWithNullOrMalformedOrNonexistentUserReturnsEmptyList(){
        Assertions.assertTrue( invitationsRepository.fetchActiveInvitations( null ).isEmpty() );
        Assertions.assertTrue( invitationsRepository.fetchActiveInvitations( "$$$" ).isEmpty() );
        Assertions.assertTrue( invitationsRepository.fetchActiveInvitations( "9191" ).isEmpty() );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }
}
