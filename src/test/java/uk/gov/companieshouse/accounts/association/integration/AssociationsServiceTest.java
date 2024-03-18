package uk.gov.companieshouse.accounts.association.integration;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
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
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListCompanyMapper;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.sdk.ApiClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Testcontainers
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
    AssociationsListCompanyMapper associationsListCompanyMapper;

    @Autowired
    private AssociationsService associationsService;

    @BeforeEach
    public void setup() {
        final var now = LocalDateTime.now();

        final var invitationOne = new InvitationDao();
        invitationOne.setInvitedBy("666");
        invitationOne.setInvitedAt(now.plusDays(4));

        final var associationOne = new AssociationDao();
        associationOne.setCompanyNumber("111111");
        associationOne.setUserId("111");
        associationOne.setStatus( StatusEnum.CONFIRMED.getValue() );
        associationOne.setUserEmail("bruce.wayne@gotham.city");
        associationOne.setId("1");
        associationOne.setApprovedAt(now.plusDays(1));
        associationOne.setRemovedAt(now.plusDays(2));
        associationOne.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE);
        associationOne.setApprovalExpiryAt(now.plusDays(3));
        associationOne.setInvitations( List.of( invitationOne ) );
        associationOne.setEtag( "a" );

        final var invitationTwo = new InvitationDao();
        invitationTwo.setInvitedBy("666");
        invitationTwo.setInvitedAt( now.plusDays(8) );

        final var associationTwo = new AssociationDao();
        associationTwo.setCompanyNumber("222222");
        associationTwo.setUserId("111");
        associationTwo.setStatus( StatusEnum.CONFIRMED.getValue() );
        associationTwo.setCompanyNumber("111111");
        associationTwo.setUserId("222");
        associationTwo.setUserEmail("the.joker@gotham.city");
        associationTwo.setId("2");
        associationTwo.setApprovedAt( now.plusDays(5) );
        associationTwo.setRemovedAt( now.plusDays(6) );
        associationTwo.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE);
        associationTwo.setApprovalExpiryAt( now.plusDays(7) );
        associationTwo.setInvitations( List.of( invitationTwo ) );
        associationTwo.setEtag("b");

        final var invitationThree = new InvitationDao();
        invitationThree.setInvitedBy("666");
        invitationThree.setInvitedAt( now.plusDays(12) );

        final var associationThree = new AssociationDao();
        associationThree.setCompanyNumber("111111");
        associationThree.setUserId("222");
        associationThree.setStatus( StatusEnum.CONFIRMED.getValue() );
        associationThree.setUserId("333");
        associationThree.setUserEmail("harley.quinn@gotham.city");
        associationThree.setId("3");
        associationThree.setApprovedAt( now.plusDays(9) );
        associationThree.setRemovedAt( now.plusDays(10) );
        associationThree.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE);
        associationThree.setApprovalExpiryAt( now.plusDays(11) );
        associationThree.setInvitations( List.of( invitationThree ) );
        associationThree.setEtag("c");

        final var invitationFour = new InvitationDao();
        invitationFour.setInvitedBy("666");
        invitationFour.setInvitedAt( now.plusDays(16) );

        final var associationFour = new AssociationDao();
        associationFour.setCompanyNumber("111111");
        associationFour.setUserId("444");
        associationFour.setUserEmail("robin@gotham.city");
        associationFour.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFour.setId("4");
        associationFour.setApprovedAt( now.plusDays(13) );
        associationFour.setRemovedAt( now.plusDays(14) );
        associationFour.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE);
        associationFour.setApprovalExpiryAt( now.plusDays(15) );
        associationFour.setInvitations( List.of( invitationFour ) );
        associationFour.setEtag("d");

        final var invitationFive = new InvitationDao();
        invitationFive.setInvitedBy("666");
        invitationFive.setInvitedAt( now.plusDays(20) );

        final var associationFive = new AssociationDao();
        associationFive.setCompanyNumber("111111");
        associationFive.setUserId("555");
        associationFive.setUserEmail("barbara.gordon@gotham.city");
        associationFive.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFive.setId("5");
        associationFive.setApprovedAt( now.plusDays(17) );
        associationFive.setRemovedAt( now.plusDays(18) );
        associationFive.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationFive.setApprovalExpiryAt( now.plusDays(19) );
        associationFive.setInvitations( List.of( invitationFive ) );
        associationFive.setEtag("e");

        final var invitationSix = new InvitationDao();
        invitationSix.setInvitedBy("5555");
        invitationSix.setInvitedAt( now.plusDays(24) );

        final var associationSix = new AssociationDao();
        associationSix.setCompanyNumber("111111");
        associationSix.setUserId("666");
        associationSix.setUserEmail("homer.simpson@springfield.com");
        associationSix.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationSix.setId("6");
        associationSix.setApprovedAt( now.plusDays(21) );
        associationSix.setRemovedAt( now.plusDays(22) );
        associationSix.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationSix.setApprovalExpiryAt( now.plusDays(23) );
        associationSix.setInvitations( List.of( invitationSix ) );
        associationSix.setEtag("f");

        final var invitationSeven = new InvitationDao();
        invitationSeven.setInvitedBy("5555");
        invitationSeven.setInvitedAt( now.plusDays(28) );

        final var associationSeven = new AssociationDao();
        associationSeven.setCompanyNumber("111111");
        associationSeven.setUserId("777");
        associationSeven.setUserEmail("marge.simpson@springfield.com");
        associationSeven.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationSeven.setId("7");
        associationSeven.setApprovedAt( now.plusDays(25) );
        associationSeven.setRemovedAt( now.plusDays(26) );
        associationSeven.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationSeven.setApprovalExpiryAt( now.plusDays(27) );
        associationSeven.setInvitations( List.of( invitationSeven ) );
        associationSeven.setEtag("g");

        final var invitationEight = new InvitationDao();
        invitationEight.setInvitedBy("5555");
        invitationEight.setInvitedAt( now.plusDays(32) );

        final var associationEight = new AssociationDao();
        associationEight.setCompanyNumber("111111");
        associationEight.setUserId("888");
        associationEight.setUserEmail("bart.simpson@springfield.com");
        associationEight.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationEight.setId("8");
        associationEight.setApprovedAt( now.plusDays(29) );
        associationEight.setRemovedAt( now.plusDays(30) );
        associationEight.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationEight.setApprovalExpiryAt( now.plusDays(31) );
        associationEight.setInvitations( List.of( invitationEight ) );
        associationEight.setEtag("h");

        final var invitationNine = new InvitationDao();
        invitationNine.setInvitedBy("5555");
        invitationNine.setInvitedAt( now.plusDays(36) );

        final var associationNine = new AssociationDao();
        associationNine.setCompanyNumber("111111");
        associationNine.setUserId("999");
        associationNine.setUserEmail("lisa.simpson@springfield.com");
        associationNine.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationNine.setId("9");
        associationNine.setApprovedAt( now.plusDays(33) );
        associationNine.setRemovedAt( now.plusDays(34) );
        associationNine.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationNine.setApprovalExpiryAt( now.plusDays(35) );
        associationNine.setInvitations( List.of( invitationNine ) );
        associationNine.setEtag("i");

        final var invitationTen = new InvitationDao();
        invitationTen.setInvitedBy("5555");
        invitationTen.setInvitedAt( now.plusDays(40) );

        final var associationTen = new AssociationDao();
        associationTen.setCompanyNumber("111111");
        associationTen.setUserId("1111");
        associationTen.setUserEmail("maggie.simpson@springfield.com");
        associationTen.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTen.setId("10");
        associationTen.setApprovedAt( now.plusDays(37) );
        associationTen.setRemovedAt( now.plusDays(38) );
        associationTen.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationTen.setApprovalExpiryAt( now.plusDays(39) );
        associationTen.setInvitations( List.of( invitationTen ) );
        associationTen.setEtag("j");

        final var invitationEleven = new InvitationDao();
        invitationEleven.setInvitedBy("5555");
        invitationEleven.setInvitedAt( now.plusDays(44) );

        final var associationEleven = new AssociationDao();
        associationEleven.setCompanyNumber("111111");
        associationEleven.setUserId("2222");
        associationEleven.setUserEmail("crusty.the.clown@springfield.com");
        associationEleven.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationEleven.setId("11");
        associationEleven.setApprovedAt( now.plusDays(41) );
        associationEleven.setRemovedAt( now.plusDays(42) );
        associationEleven.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationEleven.setApprovalExpiryAt( now.plusDays(43) );
        associationEleven.setInvitations( List.of( invitationEleven ) );
        associationEleven.setEtag("k");

        final var invitationTwelve = new InvitationDao();
        invitationTwelve.setInvitedBy("5555");
        invitationTwelve.setInvitedAt( now.plusDays(48) );

        final var associationTwelve = new AssociationDao();
        associationTwelve.setCompanyNumber("111111");
        associationTwelve.setUserId("3333");
        associationTwelve.setUserEmail("itchy@springfield.com");
        associationTwelve.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwelve.setId("12");
        associationTwelve.setApprovedAt( now.plusDays(45) );
        associationTwelve.setRemovedAt( now.plusDays(46) );
        associationTwelve.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationTwelve.setApprovalExpiryAt( now.plusDays(47) );
        associationTwelve.setInvitations( List.of( invitationTwelve ) );
        associationTwelve.setEtag("l");

        final var invitationThirteen = new InvitationDao();
        invitationThirteen.setInvitedBy("5555");
        invitationThirteen.setInvitedAt( now.plusDays(52) );

        final var associationThirteen = new AssociationDao();
        associationThirteen.setCompanyNumber("111111");
        associationThirteen.setUserId("4444");
        associationThirteen.setUserEmail("scratchy@springfield.com");
        associationThirteen.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationThirteen.setId("13");
        associationThirteen.setApprovedAt( now.plusDays(49) );
        associationThirteen.setRemovedAt( now.plusDays(50) );
        associationThirteen.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationThirteen.setApprovalExpiryAt( now.plusDays(51) );
        associationThirteen.setInvitations( List.of( invitationThirteen ) );
        associationThirteen.setEtag("m");

        final var invitationFourteen = new InvitationDao();
        invitationFourteen.setInvitedBy("111");
        invitationFourteen.setInvitedAt( now.plusDays(56) );

        final var associationFourteen = new AssociationDao();
        associationFourteen.setCompanyNumber("111111");
        associationFourteen.setUserId("5555");
        associationFourteen.setUserEmail("ross@friends.com");
        associationFourteen.setStatus(StatusEnum.REMOVED.getValue());
        associationFourteen.setId("14");
        associationFourteen.setApprovedAt( now.plusDays(53) );
        associationFourteen.setRemovedAt( now.plusDays(54) );
        associationFourteen.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationFourteen.setApprovalExpiryAt( now.plusDays(55) );
        associationFourteen.setInvitations( List.of( invitationFourteen ) );
        associationFourteen.setEtag("n");

        final var invitationFifteen = new InvitationDao();
        invitationFifteen.setInvitedBy("111");
        invitationFifteen.setInvitedAt( now.plusDays(60) );

        final var associationFifteen = new AssociationDao();
        associationFifteen.setCompanyNumber("111111");
        associationFifteen.setUserId("6666");
        associationFifteen.setUserEmail("rachel@friends.com");
        associationFifteen.setStatus(StatusEnum.REMOVED.getValue());
        associationFifteen.setId("15");
        associationFifteen.setApprovedAt( now.plusDays(57) );
        associationFifteen.setRemovedAt( now.plusDays(58) );
        associationFifteen.setApprovalRoute(ApprovalRouteEnum.INVITATION);
        associationFifteen.setApprovalExpiryAt( now.plusDays(59) );
        associationFifteen.setInvitations( List.of( invitationFifteen ) );
        associationFifteen.setEtag("o");

        final var invitationSixteen = new InvitationDao();
        invitationSixteen.setInvitedBy("111");
        invitationSixteen.setInvitedAt( now.plusDays(64) );

        final var associationSixteen = new AssociationDao();
        associationSixteen.setCompanyNumber("111111");
        associationSixteen.setUserId("7777");
        associationSixteen.setUserEmail("chandler@friends.com");
        associationSixteen.setStatus(StatusEnum.REMOVED.getValue());
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
    void fetchAssociatedUsersWithNullInputsReturnsNull(){
        final var companyDetails =
                new CompanyDetails().companyNumber("111111").companyName("Wayne Enterprises");

        Assertions.assertNull( associationsService.fetchAssociatedUsers( null, companyDetails,true, 15, 0 ) );
        Assertions.assertNull( associationsService.fetchAssociatedUsers( "111111", null,true, 15, 0 ) );
    }

    private ArgumentMatcher<Page<AssociationDao>> associationsPageMatches( int totalElements, int totalPages, int numElementsOnPage, List<String> expectedAssociationIds ){
        return page -> {
            final var associationIds =
                    page.getContent()
                            .stream()
                            .map( AssociationDao::getId )
                            .toList();

            return page.getTotalElements() == totalElements &&
                    page.getTotalPages() == totalPages &&
                    associationIds.size() == numElementsOnPage &&
                    associationIds.containsAll( expectedAssociationIds );
        };
    }

    @Test
    void fetchAssociatedUsersWithIncludeRemovedTrueDoesNotApplyFilter(){
        final var companyDetails =
                new CompanyDetails().companyNumber("111111").companyName("Wayne Enterprises");

        associationsService.fetchAssociatedUsers( "111111", companyDetails, true, 20, 0 );
        final var expectedAssociationIds = List.of( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" );
        Mockito.verify( associationsListCompanyMapper ).daoToDto( argThat( associationsPageMatches(16, 1, 16, expectedAssociationIds ) ), eq( companyDetails ) );
    }

    @Test
    void fetchAssociatedUsersWithIncludeRemovedFalseAppliesFilter(){
        final var companyDetails =
                new CompanyDetails().companyNumber("111111").companyName("Wayne Enterprises");

        associationsService.fetchAssociatedUsers( "111111", companyDetails, false, 20, 0 );
        final var expectedAssociationIds = List.of( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" );
        Mockito.verify( associationsListCompanyMapper ).daoToDto( argThat( associationsPageMatches(13, 1, 13, expectedAssociationIds ) ), eq( companyDetails ) );
    }

    @Test
    void fetchAssociatedUsersAppliesPaginationCorrectly() {
        final var companyDetails =
                new CompanyDetails().companyNumber("111111").companyName("Wayne Enterprises");

        associationsService.fetchAssociatedUsers("111111", companyDetails, true, 15, 1);
        Mockito.verify(associationsListCompanyMapper).daoToDto(argThat(associationsPageMatches(16, 2, 1, List.of("16"))), eq(companyDetails));
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }

}