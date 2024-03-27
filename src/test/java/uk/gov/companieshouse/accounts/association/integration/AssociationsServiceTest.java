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
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListUserMapper;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.sdk.ApiClientService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Testcontainers
@Tag("integration-test")
public class AssociationsServiceTest {

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

    @MockBean
    AssociationsListUserMapper associationsListUserMapper;

    @Autowired
    private AssociationsService associationsService;

    @MockBean
    StaticPropertyUtil staticPropertyUtil;

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
        associationOne.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
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
        associationTwo.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
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
        associationThree.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
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
        associationFour.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
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
        associationFive.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
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
        associationSix.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
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
        associationSeven.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
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
        associationEight.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
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
        associationNine.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
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
        associationTen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
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
        associationEleven.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
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
        associationTwelve.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
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
        associationThirteen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
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
        associationFourteen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
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
        associationFifteen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
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
        associationSixteen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationSixteen.setApprovalExpiryAt( now.plusDays(63) );
        associationSixteen.setInvitations( List.of( invitationSixteen ) );
        associationSixteen.setEtag("p");

        final var invitationEighteen = new InvitationDao();
        invitationEighteen.setInvitedBy("666");
        invitationEighteen.setInvitedAt(now.plusDays(4));

        final var associationEighteen = new AssociationDao();
        associationEighteen.setCompanyNumber("333333");
        associationEighteen.setUserId("9999");
        associationEighteen.setUserEmail("scrooge.mcduck@disney.land");
        associationEighteen.setStatus(StatusEnum.CONFIRMED.getValue());
        associationEighteen.setId("18");
        associationEighteen.setApprovedAt(now.plusDays(1));
        associationEighteen.setRemovedAt(now.plusDays(2));
        associationEighteen.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationEighteen.setApprovalExpiryAt(now.plusDays(3));
        associationEighteen.setInvitations( List.of( invitationEighteen ) );
        associationEighteen.setEtag( "aa" );

        final var invitationNineteen = new InvitationDao();
        invitationNineteen.setInvitedBy("666");
        invitationNineteen.setInvitedAt( now.plusDays(8) );

        final var associationNineteen = new AssociationDao();
        associationNineteen.setCompanyNumber("444444");
        associationNineteen.setUserId("9999");
        associationNineteen.setUserEmail("scrooge.mcduck@disney.land");
        associationNineteen.setStatus(StatusEnum.CONFIRMED.getValue());
        associationNineteen.setId("19");
        associationNineteen.setApprovedAt( now.plusDays(5) );
        associationNineteen.setRemovedAt( now.plusDays(6) );
        associationNineteen.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationNineteen.setApprovalExpiryAt( now.plusDays(7) );
        associationNineteen.setInvitations( List.of( invitationNineteen ) );
        associationNineteen.setEtag("bb");

        final var invitationTwenty = new InvitationDao();
        invitationTwenty.setInvitedBy("666");
        invitationTwenty.setInvitedAt( now.plusDays(12) );

        final var associationTwenty = new AssociationDao();
        associationTwenty.setCompanyNumber("555555");
        associationTwenty.setUserId("9999");
        associationTwenty.setUserEmail("scrooge.mcduck@disney.land");
        associationTwenty.setStatus(StatusEnum.CONFIRMED.getValue());
        associationTwenty.setId("20");
        associationTwenty.setApprovedAt( now.plusDays(9) );
        associationTwenty.setRemovedAt( now.plusDays(10) );
        associationTwenty.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationTwenty.setApprovalExpiryAt( now.plusDays(11) );
        associationTwenty.setInvitations( List.of( invitationTwenty ) );
        associationTwenty.setEtag("cc");

        final var invitationTwentyOne = new InvitationDao();
        invitationTwentyOne.setInvitedBy("666");
        invitationTwentyOne.setInvitedAt( now.plusDays(16) );

        final var associationTwentyOne = new AssociationDao();
        associationTwentyOne.setCompanyNumber("666666");
        associationTwentyOne.setUserId("9999");
        associationTwentyOne.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentyOne.setStatus(StatusEnum.CONFIRMED.getValue());
        associationTwentyOne.setId("21");
        associationTwentyOne.setApprovedAt( now.plusDays(13) );
        associationTwentyOne.setRemovedAt( now.plusDays(14) );
        associationTwentyOne.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationTwentyOne.setApprovalExpiryAt( now.plusDays(15) );
        associationTwentyOne.setInvitations( List.of( invitationTwentyOne ) );
        associationTwentyOne.setEtag("dd");

        final var invitationTwentyTwo = new InvitationDao();
        invitationTwentyTwo.setInvitedBy("666");
        invitationTwentyTwo.setInvitedAt( now.plusDays(20) );

        final var associationTwentyTwo = new AssociationDao();
        associationTwentyTwo.setCompanyNumber("777777");
        associationTwentyTwo.setUserId("9999");
        associationTwentyTwo.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentyTwo.setStatus(StatusEnum.CONFIRMED.getValue());
        associationTwentyTwo.setId("22");
        associationTwentyTwo.setApprovedAt( now.plusDays(17) );
        associationTwentyTwo.setRemovedAt( now.plusDays(18) );
        associationTwentyTwo.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentyTwo.setApprovalExpiryAt( now.plusDays(19) );
        associationTwentyTwo.setInvitations( List.of( invitationTwentyTwo ) );
        associationTwentyTwo.setEtag("ee");

        final var invitationTwentyThree = new InvitationDao();
        invitationTwentyThree.setInvitedBy("5555");
        invitationTwentyThree.setInvitedAt( now.plusDays(24) );

        final var associationTwentyThree = new AssociationDao();
        associationTwentyThree.setCompanyNumber("888888");
        associationTwentyThree.setUserId("9999");
        associationTwentyThree.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentyThree.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentyThree.setId("23");
        associationTwentyThree.setApprovedAt( now.plusDays(21) );
        associationTwentyThree.setRemovedAt( now.plusDays(22) );
        associationTwentyThree.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentyThree.setApprovalExpiryAt( now.plusDays(23) );
        associationTwentyThree.setInvitations( List.of( invitationTwentyThree ) );
        associationTwentyThree.setEtag("ff");

        final var invitationTwentyFour = new InvitationDao();
        invitationTwentyFour.setInvitedBy("5555");
        invitationTwentyFour.setInvitedAt( now.plusDays(28) );

        final var associationTwentyFour = new AssociationDao();
        associationTwentyFour.setCompanyNumber("999999");
        associationTwentyFour.setUserId("9999");
        associationTwentyFour.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentyFour.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentyFour.setId("24");
        associationTwentyFour.setApprovedAt( now.plusDays(25) );
        associationTwentyFour.setRemovedAt( now.plusDays(26) );
        associationTwentyFour.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentyFour.setApprovalExpiryAt( now.plusDays(27) );
        associationTwentyFour.setInvitations( List.of( invitationTwentyFour ) );
        associationTwentyFour.setEtag("gg");

        final var invitationTwentyFive = new InvitationDao();
        invitationTwentyFive.setInvitedBy("5555");
        invitationTwentyFive.setInvitedAt( now.plusDays(32) );

        final var associationTwentyFive = new AssociationDao();
        associationTwentyFive.setCompanyNumber("x111111");
        associationTwentyFive.setUserId("9999");
        associationTwentyFive.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentyFive.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentyFive.setId("25");
        associationTwentyFive.setApprovedAt( now.plusDays(29) );
        associationTwentyFive.setRemovedAt( now.plusDays(30) );
        associationTwentyFive.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentyFive.setApprovalExpiryAt( now.plusDays(31) );
        associationTwentyFive.setInvitations( List.of( invitationTwentyFive ) );
        associationTwentyFive.setEtag("hh");

        final var invitationTwentySix = new InvitationDao();
        invitationTwentySix.setInvitedBy("5555");
        invitationTwentySix.setInvitedAt( now.plusDays(36) );

        final var associationTwentySix = new AssociationDao();
        associationTwentySix.setCompanyNumber("x222222");
        associationTwentySix.setUserId("9999");
        associationTwentySix.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentySix.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentySix.setId("26");
        associationTwentySix.setApprovedAt( now.plusDays(33) );
        associationTwentySix.setRemovedAt( now.plusDays(34) );
        associationTwentySix.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentySix.setApprovalExpiryAt( now.plusDays(35) );
        associationTwentySix.setInvitations( List.of( invitationTwentySix ) );
        associationTwentySix.setEtag("ii");

        final var invitationTwentySeven = new InvitationDao();
        invitationTwentySeven.setInvitedBy("5555");
        invitationTwentySeven.setInvitedAt( now.plusDays(40) );

        final var associationTwentySeven = new AssociationDao();
        associationTwentySeven.setCompanyNumber("x333333");
        associationTwentySeven.setUserId("9999");
        associationTwentySeven.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentySeven.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentySeven.setId("27");
        associationTwentySeven.setApprovedAt( now.plusDays(37) );
        associationTwentySeven.setRemovedAt( now.plusDays(38) );
        associationTwentySeven.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentySeven.setApprovalExpiryAt( now.plusDays(39) );
        associationTwentySeven.setInvitations( List.of( invitationTwentySeven ) );
        associationTwentySeven.setEtag("jj");

        final var invitationTwentyEight = new InvitationDao();
        invitationTwentyEight.setInvitedBy("5555");
        invitationTwentyEight.setInvitedAt( now.plusDays(44) );

        final var associationTwentyEight = new AssociationDao();
        associationTwentyEight.setCompanyNumber("x444444");
        associationTwentyEight.setUserId("9999");
        associationTwentyEight.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentyEight.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentyEight.setId("28");
        associationTwentyEight.setApprovedAt( now.plusDays(41) );
        associationTwentyEight.setRemovedAt( now.plusDays(42) );
        associationTwentyEight.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentyEight.setApprovalExpiryAt( now.plusDays(43) );
        associationTwentyEight.setInvitations( List.of( invitationTwentyEight ) );
        associationTwentyEight.setEtag("kk");

        final var invitationTwentyNine = new InvitationDao();
        invitationTwentyNine.setInvitedBy("5555");
        invitationTwentyNine.setInvitedAt( now.plusDays(48) );

        final var associationTwentyNine = new AssociationDao();
        associationTwentyNine.setCompanyNumber("x555555");
        associationTwentyNine.setUserId("9999");
        associationTwentyNine.setUserEmail("scrooge.mcduck@disney.land");
        associationTwentyNine.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwentyNine.setId("29");
        associationTwentyNine.setApprovedAt( now.plusDays(45) );
        associationTwentyNine.setRemovedAt( now.plusDays(46) );
        associationTwentyNine.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwentyNine.setApprovalExpiryAt( now.plusDays(47) );
        associationTwentyNine.setInvitations( List.of( invitationTwentyNine ) );
        associationTwentyNine.setEtag("ll");

        final var invitationThirty = new InvitationDao();
        invitationThirty.setInvitedBy("5555");
        invitationThirty.setInvitedAt( now.plusDays(52) );

        final var associationThirty = new AssociationDao();
        associationThirty.setCompanyNumber("x666666");
        associationThirty.setUserId("9999");
        associationThirty.setUserEmail("scrooge.mcduck@disney.land");
        associationThirty.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationThirty.setId("30");
        associationThirty.setApprovedAt( now.plusDays(49) );
        associationThirty.setRemovedAt( now.plusDays(50) );
        associationThirty.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationThirty.setApprovalExpiryAt( now.plusDays(51) );
        associationThirty.setInvitations( List.of( invitationThirty ) );
        associationThirty.setEtag("mm");

        final var invitationThirtyOne = new InvitationDao();
        invitationThirtyOne.setInvitedBy("111");
        invitationThirtyOne.setInvitedAt( now.plusDays(56) );

        final var associationThirtyOne = new AssociationDao();
        associationThirtyOne.setCompanyNumber("x777777");
        associationThirtyOne.setUserId("9999");
        associationThirtyOne.setUserEmail("scrooge.mcduck@disney.land");
        associationThirtyOne.setStatus(StatusEnum.REMOVED.getValue());
        associationThirtyOne.setId("31");
        associationThirtyOne.setApprovedAt( now.plusDays(53) );
        associationThirtyOne.setRemovedAt( now.plusDays(54) );
        associationThirtyOne.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationThirtyOne.setApprovalExpiryAt( now.plusDays(55) );
        associationThirtyOne.setInvitations( List.of( invitationThirtyOne ) );
        associationThirtyOne.setEtag("nn");

        final var invitationThirtyTwo = new InvitationDao();
        invitationThirtyTwo.setInvitedBy("111");
        invitationThirtyTwo.setInvitedAt( now.plusDays(60) );

        final var associationThirtyTwo = new AssociationDao();
        associationThirtyTwo.setCompanyNumber("x888888");
        associationThirtyTwo.setUserId("9999");
        associationThirtyTwo.setUserEmail("scrooge.mcduck@disney.land");
        associationThirtyTwo.setStatus(StatusEnum.REMOVED.getValue());
        associationThirtyTwo.setId("32");
        associationThirtyTwo.setApprovedAt( now.plusDays(57) );
        associationThirtyTwo.setRemovedAt( now.plusDays(58) );
        associationThirtyTwo.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationThirtyTwo.setApprovalExpiryAt( now.plusDays(59) );
        associationThirtyTwo.setInvitations( List.of( invitationThirtyTwo ) );
        associationThirtyTwo.setEtag("oo");

        final var invitationThirtyThree = new InvitationDao();
        invitationThirtyThree.setInvitedBy("111");
        invitationThirtyThree.setInvitedAt( now.plusDays(64) );

        final var associationThirtyThree = new AssociationDao();
        associationThirtyThree.setCompanyNumber("x999999");
        associationThirtyThree.setUserId("9999");
        associationThirtyThree.setUserEmail("scrooge.mcduck@disney.land");
        associationThirtyThree.setStatus(StatusEnum.REMOVED.getValue());
        associationThirtyThree.setId("33");
        associationThirtyThree.setApprovedAt( now.plusDays(61) );
        associationThirtyThree.setRemovedAt( now.plusDays(62) );
        associationThirtyThree.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationThirtyThree.setApprovalExpiryAt( now.plusDays(63) );
        associationThirtyThree.setInvitations( List.of( invitationThirtyThree ) );
        associationThirtyThree.setEtag("pp");

        associationsRepository.insert( List.of( associationOne, associationTwo, associationThree, associationFour,
                associationFive, associationSix, associationSeven, associationEight, associationNine, associationTen,
                associationEleven, associationTwelve, associationThirteen, associationFourteen, associationFifteen,
                associationSixteen, associationEighteen, associationNineteen, associationTwenty, associationTwentyOne,
                associationTwentyTwo, associationTwentyThree, associationTwentyFour, associationTwentyFive, associationTwentySix,
                associationTwentySeven, associationTwentyEight, associationTwentyNine, associationThirty, associationThirtyOne,
                associationThirtyTwo, associationThirtyThree ) );
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

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNullInputsThrowsNullPointerException(){
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName( "Scrooge McDuck" );
        final var status = List.of( StatusEnum.CONFIRMED.getValue() );

        Assertions.assertThrows( NullPointerException.class, () -> associationsService.fetchAssociationsForUserStatusAndCompany( null, status, 0, 15, "333333" ) );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.fetchAssociationsForUserStatusAndCompany( user, status, null, 15, "333333" ) );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, null, "333333" ) );
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithInvalidPageIndexOrItemsPerPageThrowsIllegalArgumentException() {
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName( "Scrooge McDuck" );
        final var status = List.of( StatusEnum.CONFIRMED.getValue() );

        Assertions.assertThrows( IllegalArgumentException.class, () -> associationsService.fetchAssociationsForUserStatusAndCompany( user, status, -1, 15, "333333" ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, 0, "333333" ) );
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyPaginatesCorrectly() {
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName( "Scrooge McDuck" );
        final var status = List.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue() );
        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 1, 15, null );

        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(16, 2, 1, List.of("33"))), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersByCompanyNumber(){
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName( "Scrooge McDuck" );
        final var status = List.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue() );
        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, 15, "333333" );

        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(2, 1, 2, List.of("18", "27"))), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersBasedOnStatus(){
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName( "Scrooge McDuck" );
        final var status = List.of( StatusEnum.REMOVED.getValue() );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, 15, null );
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(3, 1, 3, List.of("31", "32", "33"))), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNullStatusDefaultsToConfirmedStatus(){
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName( "Scrooge McDuck" );
        associationsService.fetchAssociationsForUserStatusAndCompany( user, null, 0, 15, null );
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(5, 1, 5, List.of("18", "19", "20", "21", "22"))), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithEmptyStatusDefaultsToConfirmedStatus(){
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName( "Scrooge McDuck" );
        associationsService.fetchAssociationsForUserStatusAndCompany( user, List.of(), 0, 15, null );
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(5, 1, 5, List.of("18", "19", "20", "21", "22"))), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithInvalidStatusReturnsEmptyPage() {
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName( "Scrooge McDuck" );
        associationsService.fetchAssociationsForUserStatusAndCompany( user, List.of( "complicated" ), 0, 15, null );
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(0, 0, 0, List.of())), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNonexistentOrInvalidCompanyNumberReturnsEmptyPage() {
        final var user = new User().userId("9999").email("scrooge.mcduck@disney.land").displayName( "Scrooge McDuck" );
        associationsService.fetchAssociationsForUserStatusAndCompany( user, List.of(), 0, 15, "$$$$$$" );
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(0, 0, 0, List.of())), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNonexistentUserIdReturnsEmptyPage() {
        final var user = new User().userId("9191").email("scrooge.mcduck@disney.land").displayName( "Scrooge McDuck" );
        associationsService.fetchAssociationsForUserStatusAndCompany( user, List.of(), 0, 15, null );
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(0, 0, 0, List.of())), eq(user));
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }

    @Test
    void associationExistsWithNullOrMalformedOrNonExistentCompanyNumberOrUserReturnsFalse(){
        Assertions.assertFalse( associationsService.associationExists( null, "111" ) );
        Assertions.assertFalse( associationsService.associationExists( "$$$$$$", "111" ) );
        Assertions.assertFalse( associationsService.associationExists( "919191", "111" ) );
        Assertions.assertFalse( associationsService.associationExists( "111111", null ) );
        Assertions.assertFalse( associationsService.associationExists( "111111", "$$$" ) );
        Assertions.assertFalse( associationsService.associationExists( "111111", "9191" ) );
    }

    @Test
    void associationExistsWithExistingAssociationReturnsTrue(){
        Assertions.assertTrue( associationsService.associationExists( "111111", "111" ) );
    }

    @Test
    void createAssociationWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociation( null, "000", ApprovalRouteEnum.AUTH_CODE ) );
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociation( "000000", null, ApprovalRouteEnum.AUTH_CODE ) );
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociation( "000000", "000", null ) );
    }

    @Test
    void createAssociationSuccessfullyCreatesAssociation(){
        final var association = associationsService.createAssociation( "000000", "000", ApprovalRouteEnum.AUTH_CODE );
        Assertions.assertEquals( "000000", association.getCompanyNumber() );
        Assertions.assertEquals( "000", association.getUserId() );
        Assertions.assertEquals( StatusEnum.CONFIRMED.getValue(), association.getStatus() );
        Assertions.assertEquals( ApprovalRouteEnum.AUTH_CODE.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getEtag() );
    }

}
