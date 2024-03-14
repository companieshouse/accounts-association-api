package uk.gov.companieshouse.accounts.association.integration;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.domain.Page;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListCompanyDaoToDtoMapper;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.models.Invitation;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.sdk.ApiClientService;

@SpringBootTest
@Testcontainers
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class AssociationsServiceTest {

    @Container
    @ServiceConnection
    static MongoDBContainer container = new MongoDBContainer("mongo:5");

    @Autowired
    MongoTemplate mongoTemplate;

    @Autowired
    AssociationsRepository associationsRepository;

    @MockBean
    ApiClientService apiClientService;

    @MockBean
    InternalApiClient internalApiClient;

    @MockBean
    AssociationsListCompanyDaoToDtoMapper associationsListCompanyDaoToDtoMapper;

    @Autowired
    private AssociationsService associationsService;

    @BeforeEach
    public void setup() {
        final var now = LocalDateTime.now();

        final var invitationOne = new Invitation();
        invitationOne.setInvitedBy("666");
        invitationOne.setInvitedAt(now.plusDays(4));

        final var associationOne = new Association();
        associationOne.setCompanyNumber("111111");
        associationOne.setUserId("111");
        associationOne.setUserEmail("bruce.wayne@gotham.city");
        associationOne.setStatus(StatusEnum.CONFIRMED);
        associationOne.setId("1");
        associationOne.setApprovedAt(now.plusDays(1));
        associationOne.setRemovedAt(now.plusDays(2));
        associationOne.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE);
        associationOne.setApprovalExpiryAt(now.plusDays(3));
        associationOne.setInvitations( List.of( invitationOne ) );
        associationOne.setEtag( "a" );

        final var invitationTwo = new Invitation();
        invitationTwo.setInvitedBy("666");
        invitationTwo.setInvitedAt( now.plusDays(8) );

        final var associationTwo = new Association();
        associationTwo.setCompanyNumber("111111");
        associationTwo.setUserId("222");
        associationTwo.setUserEmail("the.joker@gotham.city");
        associationTwo.setStatus(StatusEnum.CONFIRMED);
        associationTwo.setId("2");
        associationTwo.setApprovedAt( now.plusDays(5) );
        associationTwo.setRemovedAt( now.plusDays(6) );
        associationTwo.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE);
        associationTwo.setApprovalExpiryAt( now.plusDays(7) );
        associationTwo.setInvitations( List.of( invitationTwo ) );
        associationTwo.setEtag("b");

        final var invitationThree = new Invitation();
        invitationThree.setInvitedBy("666");
        invitationThree.setInvitedAt( now.plusDays(12) );

        final var associationThree = new Association();
        associationThree.setCompanyNumber("111111");
        associationThree.setUserId("333");
        associationThree.setUserEmail("harley.quinn@gotham.city");
        associationThree.setStatus(StatusEnum.CONFIRMED);
        associationThree.setId("3");
        associationThree.setApprovedAt( now.plusDays(9) );
        associationThree.setRemovedAt( now.plusDays(10) );
        associationThree.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE);
        associationThree.setApprovalExpiryAt( now.plusDays(11) );
        associationThree.setInvitations( List.of( invitationThree ) );
        associationThree.setEtag("c");

        final var invitationFour = new Invitation();
        invitationFour.setInvitedBy("666");
        invitationFour.setInvitedAt( now.plusDays(16) );

        final var associationFour = new Association();
        associationFour.setCompanyNumber("111111");
        associationFour.setUserId("444");
        associationFour.setUserEmail("robin@gotham.city");
        associationFour.setStatus(StatusEnum.CONFIRMED);
        associationFour.setId("4");
        associationFour.setApprovedAt( now.plusDays(13) );
        associationFour.setRemovedAt( now.plusDays(14) );
        associationFour.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE);
        associationFour.setApprovalExpiryAt( now.plusDays(15) );
        associationFour.setInvitations( List.of( invitationFour ) );
        associationFour.setEtag("d");

        final var invitationFive = new Invitation();
        invitationFive.setInvitedBy("666");
        invitationFive.setInvitedAt( now.plusDays(20) );

        final var associationFive = new Association();
        associationFive.setCompanyNumber("111111");
        associationFive.setUserId("555");
        associationFive.setUserEmail("barbara.gordon@gotham.city");
        associationFive.setStatus(StatusEnum.CONFIRMED);
        associationFive.setId("5");
        associationFive.setApprovedAt( now.plusDays(17) );
        associationFive.setRemovedAt( now.plusDays(18) );
        associationFive.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationFive.setApprovalExpiryAt( now.plusDays(19) );
        associationFive.setInvitations( List.of( invitationFive ) );
        associationFive.setEtag("e");

        final var invitationSix = new Invitation();
        invitationSix.setInvitedBy("5555");
        invitationSix.setInvitedAt( now.plusDays(24) );

        final var associationSix = new Association();
        associationSix.setCompanyNumber("111111");
        associationSix.setUserId("666");
        associationSix.setUserEmail("homer.simpson@springfield.com");
        associationSix.setStatus(StatusEnum.AWAITING_APPROVAL);
        associationSix.setId("6");
        associationSix.setApprovedAt( now.plusDays(21) );
        associationSix.setRemovedAt( now.plusDays(22) );
        associationSix.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationSix.setApprovalExpiryAt( now.plusDays(23) );
        associationSix.setInvitations( List.of( invitationSix ) );
        associationSix.setEtag("f");

        final var invitationSeven = new Invitation();
        invitationSeven.setInvitedBy("5555");
        invitationSeven.setInvitedAt( now.plusDays(28) );

        final var associationSeven = new Association();
        associationSeven.setCompanyNumber("111111");
        associationSeven.setUserId("777");
        associationSeven.setUserEmail("marge.simpson@springfield.com");
        associationSeven.setStatus(StatusEnum.AWAITING_APPROVAL);
        associationSeven.setId("7");
        associationSeven.setApprovedAt( now.plusDays(25) );
        associationSeven.setRemovedAt( now.plusDays(26) );
        associationSeven.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationSeven.setApprovalExpiryAt( now.plusDays(27) );
        associationSeven.setInvitations( List.of( invitationSeven ) );
        associationSeven.setEtag("g");

        final var invitationEight = new Invitation();
        invitationEight.setInvitedBy("5555");
        invitationEight.setInvitedAt( now.plusDays(32) );

        final var associationEight = new Association();
        associationEight.setCompanyNumber("111111");
        associationEight.setUserId("888");
        associationEight.setUserEmail("bart.simpson@springfield.com");
        associationEight.setStatus(StatusEnum.AWAITING_APPROVAL);
        associationEight.setId("8");
        associationEight.setApprovedAt( now.plusDays(29) );
        associationEight.setRemovedAt( now.plusDays(30) );
        associationEight.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationEight.setApprovalExpiryAt( now.plusDays(31) );
        associationEight.setInvitations( List.of( invitationEight ) );
        associationEight.setEtag("h");

        final var invitationNine = new Invitation();
        invitationNine.setInvitedBy("5555");
        invitationNine.setInvitedAt( now.plusDays(36) );

        final var associationNine = new Association();
        associationNine.setCompanyNumber("111111");
        associationNine.setUserId("999");
        associationNine.setUserEmail("lisa.simpson@springfield.com");
        associationNine.setStatus(StatusEnum.AWAITING_APPROVAL);
        associationNine.setId("9");
        associationNine.setApprovedAt( now.plusDays(33) );
        associationNine.setRemovedAt( now.plusDays(34) );
        associationNine.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationNine.setApprovalExpiryAt( now.plusDays(35) );
        associationNine.setInvitations( List.of( invitationNine ) );
        associationNine.setEtag("i");

        final var invitationTen = new Invitation();
        invitationTen.setInvitedBy("5555");
        invitationTen.setInvitedAt( now.plusDays(40) );

        final var associationTen = new Association();
        associationTen.setCompanyNumber("111111");
        associationTen.setUserId("1111");
        associationTen.setUserEmail("maggie.simpson@springfield.com");
        associationTen.setStatus(StatusEnum.AWAITING_APPROVAL);
        associationTen.setId("10");
        associationTen.setApprovedAt( now.plusDays(37) );
        associationTen.setRemovedAt( now.plusDays(38) );
        associationTen.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationTen.setApprovalExpiryAt( now.plusDays(39) );
        associationTen.setInvitations( List.of( invitationTen ) );
        associationTen.setEtag("j");

        final var invitationEleven = new Invitation();
        invitationEleven.setInvitedBy("5555");
        invitationEleven.setInvitedAt( now.plusDays(44) );

        final var associationEleven = new Association();
        associationEleven.setCompanyNumber("111111");
        associationEleven.setUserId("2222");
        associationEleven.setUserEmail("crusty.the.clown@springfield.com");
        associationEleven.setStatus(StatusEnum.AWAITING_APPROVAL);
        associationEleven.setId("11");
        associationEleven.setApprovedAt( now.plusDays(41) );
        associationEleven.setRemovedAt( now.plusDays(42) );
        associationEleven.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationEleven.setApprovalExpiryAt( now.plusDays(43) );
        associationEleven.setInvitations( List.of( invitationEleven ) );
        associationEleven.setEtag("k");

        final var invitationTwelve = new Invitation();
        invitationTwelve.setInvitedBy("5555");
        invitationTwelve.setInvitedAt( now.plusDays(48) );

        final var associationTwelve = new Association();
        associationTwelve.setCompanyNumber("111111");
        associationTwelve.setUserId("3333");
        associationTwelve.setUserEmail("itchy@springfield.com");
        associationTwelve.setStatus(StatusEnum.AWAITING_APPROVAL);
        associationTwelve.setId("12");
        associationTwelve.setApprovedAt( now.plusDays(45) );
        associationTwelve.setRemovedAt( now.plusDays(46) );
        associationTwelve.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationTwelve.setApprovalExpiryAt( now.plusDays(47) );
        associationTwelve.setInvitations( List.of( invitationTwelve ) );
        associationTwelve.setEtag("l");

        final var invitationThirteen = new Invitation();
        invitationThirteen.setInvitedBy("5555");
        invitationThirteen.setInvitedAt( now.plusDays(52) );

        final var associationThirteen = new Association();
        associationThirteen.setCompanyNumber("111111");
        associationThirteen.setUserId("4444");
        associationThirteen.setUserEmail("scratchy@springfield.com");
        associationThirteen.setStatus(StatusEnum.AWAITING_APPROVAL);
        associationThirteen.setId("13");
        associationThirteen.setApprovedAt( now.plusDays(49) );
        associationThirteen.setRemovedAt( now.plusDays(50) );
        associationThirteen.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationThirteen.setApprovalExpiryAt( now.plusDays(51) );
        associationThirteen.setInvitations( List.of( invitationThirteen ) );
        associationThirteen.setEtag("m");

        final var invitationFourteen = new Invitation();
        invitationFourteen.setInvitedBy("111");
        invitationFourteen.setInvitedAt( now.plusDays(56) );

        final var associationFourteen = new Association();
        associationFourteen.setCompanyNumber("111111");
        associationFourteen.setUserId("5555");
        associationFourteen.setUserEmail("ross@friends.com");
        associationFourteen.setStatus(StatusEnum.REMOVED);
        associationFourteen.setId("14");
        associationFourteen.setApprovedAt( now.plusDays(53) );
        associationFourteen.setRemovedAt( now.plusDays(54) );
        associationFourteen.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationFourteen.setApprovalExpiryAt( now.plusDays(55) );
        associationFourteen.setInvitations( List.of( invitationFourteen ) );
        associationFourteen.setEtag("n");

        final var invitationFifteen = new Invitation();
        invitationFifteen.setInvitedBy("111");
        invitationFifteen.setInvitedAt( now.plusDays(60) );

        final var associationFifteen = new Association();
        associationFifteen.setCompanyNumber("111111");
        associationFifteen.setUserId("6666");
        associationFifteen.setUserEmail("rachel@friends.com");
        associationFifteen.setStatus(StatusEnum.REMOVED);
        associationFifteen.setId("15");
        associationFifteen.setApprovedAt( now.plusDays(57) );
        associationFifteen.setRemovedAt( now.plusDays(58) );
        associationFifteen.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationFifteen.setApprovalExpiryAt( now.plusDays(59) );
        associationFifteen.setInvitations( List.of( invitationFifteen ) );
        associationFifteen.setEtag("o");

        final var invitationSixteen = new Invitation();
        invitationSixteen.setInvitedBy("111");
        invitationSixteen.setInvitedAt( now.plusDays(64) );

        final var associationSixteen = new Association();
        associationSixteen.setCompanyNumber("111111");
        associationSixteen.setUserId("7777");
        associationSixteen.setUserEmail("chandler@friends.com");
        associationSixteen.setStatus(StatusEnum.REMOVED);
        associationSixteen.setId("16");
        associationSixteen.setApprovedAt( now.plusDays(61) );
        associationSixteen.setRemovedAt( now.plusDays(62) );
        associationSixteen.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationSixteen.setApprovalExpiryAt( now.plusDays(63) );
        associationSixteen.setInvitations( List.of( invitationSixteen ) );
        associationSixteen.setEtag("p");

        associationsRepository.insert( List.of( associationOne, associationTwo, associationThree, associationFour,
                associationFive, associationSix, associationSeven, associationEight, associationNine, associationTen,
                associationEleven, associationTwelve, associationThirteen, associationFourteen, associationFifteen,
                associationSixteen ) );
    }

    @Test
    void fetchAssociatedUsersWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.fetchAssociatedUsers( null, true, 15, 0 ) );
    }

    private ArgumentMatcher<Page<Association>> associationsPageMatches( int totalElements, int totalPages, int numElementsOnPage, List<String> expectedAssociationIds ){
        return page -> {
            final var associationIds =
            page.getContent()
                .stream()
                .map( Association::getId )
                .toList();

             return page.getTotalElements() == totalElements &&
                    page.getTotalPages() == totalPages &&
                    associationIds.size() == numElementsOnPage &&
                    associationIds.containsAll( expectedAssociationIds );
        };
    }

    @Test
    void fetchAssociatedUsersWithMalformedOrNonexistentCompanyNumberReturnsEmpty(){
        associationsService.fetchAssociatedUsers( "$$$$$$", true, 15, 0 ) ;
        Mockito.verify( associationsListCompanyDaoToDtoMapper ).daoToDto( argThat( associationsPageMatches(0, 0, 0, List.of()) ), eq( Map.of( "companyNumber", "$$$$$$", "endpointUri", "/associations/companies/$$$$$$" ) ) );

        associationsService.fetchAssociatedUsers( "919191", true, 15, 0 ) ;
        Mockito.verify( associationsListCompanyDaoToDtoMapper ).daoToDto( argThat( associationsPageMatches(0, 0, 0, List.of()) ), eq( Map.of( "companyNumber", "919191", "endpointUri", "/associations/companies/919191" ) ) );
    }

    @Test
    void fetchAssociatedUsersWithIncludeRemovedTrueDoesNotApplyFilter(){
        associationsService.fetchAssociatedUsers( "111111", true, 20, 0 );
        final var expectedAssociationIds = List.of( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" );
        Mockito.verify( associationsListCompanyDaoToDtoMapper ).daoToDto( argThat( associationsPageMatches(16, 1, 16, expectedAssociationIds ) ), eq( Map.of( "companyNumber", "111111", "endpointUri", "/associations/companies/111111" ) ) );
    }

    @Test
    void fetchAssociatedUsersWithIncludeRemovedFalseAppliesFilter(){
        associationsService.fetchAssociatedUsers( "111111", false, 20, 0 );
        final var expectedAssociationIds = List.of( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" );
        Mockito.verify( associationsListCompanyDaoToDtoMapper ).daoToDto( argThat( associationsPageMatches(13, 1, 13, expectedAssociationIds ) ), eq( Map.of( "companyNumber", "111111", "endpointUri", "/associations/companies/111111" ) ) );
    }

    @Test
    void fetchAssociatedUsersAppliesPaginationCorrectly(){
        associationsService.fetchAssociatedUsers( "111111", true, 15, 1 );
        Mockito.verify( associationsListCompanyDaoToDtoMapper ).daoToDto( argThat( associationsPageMatches(16, 2, 1, List.of( "16" ) ) ), eq( Map.of( "companyNumber", "111111", "endpointUri", "/associations/companies/111111" ) ) );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(Association.class);
    }

}