package uk.gov.companieshouse.accounts.association.service;

import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Update;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.mapper.AssociationMapper;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListCompanyMapper;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListUserMapper;
import uk.gov.companieshouse.accounts.association.mapper.InvitationsMapper;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AssociationsServiceTest {

    @InjectMocks
    AssociationsService associationsService;

    @Mock
    AssociationsRepository associationsRepository;

    @Mock
    InvitationsMapper invitationMapper;

    @Mock
    AssociationsListCompanyMapper associationsListCompanyMapper;

    @Mock
    AssociationsListUserMapper associationsListUserMapper;

    @Mock
    AssociationMapper associationMapper;

    private AssociationDao associationOne;
    private AssociationDao associationTwo;
    private AssociationDao associationThree;
    private AssociationDao associationFour;
    private AssociationDao associationFive;
    private AssociationDao associationSix;
    private AssociationDao associationSeven;
    private AssociationDao associationEight;
    private AssociationDao associationNine;
    private AssociationDao associationTen;
    private AssociationDao associationEleven;
    private AssociationDao associationTwelve;
    private AssociationDao associationThirteen;
    private AssociationDao associationFourteen;
    private AssociationDao associationFifteen;
    private AssociationDao associationSixteen;
    private AssociationDao associationEighteen;
    private AssociationDao associationNineteen;
    private AssociationDao associationTwenty;
    private AssociationDao associationTwentyOne;
    private AssociationDao associationTwentyTwo;
    private AssociationDao associationTwentyThree;
    private AssociationDao associationTwentyFour;
    private AssociationDao associationTwentyFive;
    private AssociationDao associationTwentySix;
    private AssociationDao associationTwentySeven;
    private AssociationDao associationTwentyEight;
    private AssociationDao associationTwentyNine;
    private AssociationDao associationThirty;
    private AssociationDao associationThirtyOne;
    private AssociationDao associationThirtyTwo;
    private AssociationDao associationThirtyThree;
    private AssociationDao associationThirtyFour;

    @BeforeEach
    public void setup() {

        final LocalDateTime now = LocalDateTime.now();

        final var invitationOne = new InvitationDao();
        invitationOne.setInvitedBy("666");
        invitationOne.setInvitedAt(now.plusDays(4));

        associationOne = new AssociationDao();
        associationOne.setCompanyNumber("111111");
        associationOne.setUserId("111");
        associationOne.setUserEmail("bruce.wayne@gotham.city");
        associationOne.setStatus(StatusEnum.CONFIRMED.getValue());
        associationOne.setId("1");
        associationOne.setApprovedAt(now.plusDays(1));
        associationOne.setRemovedAt(now.plusDays(2));
        associationOne.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationOne.setApprovalExpiryAt(now.plusDays(3));
        associationOne.setInvitations(List.of(invitationOne));
        associationOne.setEtag("a");

        final var invitationTwo = new InvitationDao();
        invitationTwo.setInvitedBy("666");
        invitationTwo.setInvitedAt(now.plusDays(8));

        associationTwo = new AssociationDao();
        associationTwo.setCompanyNumber("111111");
        associationTwo.setUserId("222");
        associationTwo.setUserEmail("the.joker@gotham.city");
        associationTwo.setStatus(StatusEnum.CONFIRMED.getValue());
        associationTwo.setId("2");
        associationTwo.setApprovedAt(now.plusDays(5));
        associationTwo.setRemovedAt(now.plusDays(6));
        associationTwo.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationTwo.setApprovalExpiryAt(now.plusDays(7));
        associationTwo.setInvitations(List.of(invitationTwo));
        associationTwo.setEtag("b");

        final var invitationThree = new InvitationDao();
        invitationThree.setInvitedBy("666");
        invitationThree.setInvitedAt(now.plusDays(12));

        associationThree = new AssociationDao();
        associationThree.setCompanyNumber("111111");
        associationThree.setUserId("333");
        associationThree.setUserEmail("harley.quinn@gotham.city");
        associationThree.setStatus(StatusEnum.CONFIRMED.getValue());
        associationThree.setId("3");
        associationThree.setApprovedAt(now.plusDays(9));
        associationThree.setRemovedAt(now.plusDays(10));
        associationThree.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationThree.setApprovalExpiryAt(now.plusDays(11));
        associationThree.setInvitations(List.of(invitationThree));
        associationThree.setEtag("c");

        final var invitationFour = new InvitationDao();
        invitationFour.setInvitedBy("666");
        invitationFour.setInvitedAt(now.plusDays(16));

        associationFour = new AssociationDao();
        associationFour.setCompanyNumber("111111");
        associationFour.setUserId("444");
        associationFour.setUserEmail("robin@gotham.city");
        associationFour.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFour.setId("4");
        associationFour.setApprovedAt(now.plusDays(13));
        associationFour.setRemovedAt(now.plusDays(14));
        associationFour.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
        associationFour.setApprovalExpiryAt(now.plusDays(15));
        associationFour.setInvitations(List.of(invitationFour));
        associationFour.setEtag("d");

        final var invitationFive = new InvitationDao();
        invitationFive.setInvitedBy("666");
        invitationFive.setInvitedAt(now.plusDays(20));

        associationFive = new AssociationDao();
        associationFive.setCompanyNumber("111111");
        associationFive.setUserId("555");
        associationFive.setUserEmail("barbara.gordon@gotham.city");
        associationFive.setStatus(StatusEnum.CONFIRMED.getValue());
        associationFive.setId("5");
        associationFive.setApprovedAt(now.plusDays(17));
        associationFive.setRemovedAt(now.plusDays(18));
        associationFive.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFive.setApprovalExpiryAt(now.plusDays(19));
        associationFive.setInvitations(List.of(invitationFive));
        associationFive.setEtag("e");

        final var invitationSix = new InvitationDao();
        invitationSix.setInvitedBy("5555");
        invitationSix.setInvitedAt(now.plusDays(24));

        associationSix = new AssociationDao();
        associationSix.setCompanyNumber("111111");
        associationSix.setUserId("666");
        associationSix.setUserEmail("homer.simpson@springfield.com");
        associationSix.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationSix.setId("6");
        associationSix.setApprovedAt(now.plusDays(21));
        associationSix.setRemovedAt(now.plusDays(22));
        associationSix.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationSix.setApprovalExpiryAt(now.plusDays(23));
        associationSix.setInvitations(List.of(invitationSix));
        associationSix.setEtag("f");

        final var invitationSeven = new InvitationDao();
        invitationSeven.setInvitedBy("5555");
        invitationSeven.setInvitedAt(now.plusDays(28));

        associationSeven = new AssociationDao();
        associationSeven.setCompanyNumber("111111");
        associationSeven.setUserId("777");
        associationSeven.setUserEmail("marge.simpson@springfield.com");
        associationSeven.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationSeven.setId("7");
        associationSeven.setApprovedAt(now.plusDays(25));
        associationSeven.setRemovedAt(now.plusDays(26));
        associationSeven.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationSeven.setApprovalExpiryAt(now.plusDays(27));
        associationSeven.setInvitations(List.of(invitationSeven));
        associationSeven.setEtag("g");

        final var invitationEight = new InvitationDao();
        invitationEight.setInvitedBy("5555");
        invitationEight.setInvitedAt(now.plusDays(32));

        associationEight = new AssociationDao();
        associationEight.setCompanyNumber("111111");
        associationEight.setUserId("888");
        associationEight.setUserEmail("bart.simpson@springfield.com");
        associationEight.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationEight.setId("8");
        associationEight.setApprovedAt(now.plusDays(29));
        associationEight.setRemovedAt(now.plusDays(30));
        associationEight.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationEight.setApprovalExpiryAt(now.plusDays(31));
        associationEight.setInvitations(List.of(invitationEight));
        associationEight.setEtag("h");

        final var invitationNine = new InvitationDao();
        invitationNine.setInvitedBy("5555");
        invitationNine.setInvitedAt(now.plusDays(36));

        associationNine = new AssociationDao();
        associationNine.setCompanyNumber("111111");
        associationNine.setUserId("999");
        associationNine.setUserEmail("lisa.simpson@springfield.com");
        associationNine.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationNine.setId("9");
        associationNine.setApprovedAt(now.plusDays(33));
        associationNine.setRemovedAt(now.plusDays(34));
        associationNine.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationNine.setApprovalExpiryAt(now.plusDays(35));
        associationNine.setInvitations(List.of(invitationNine));
        associationNine.setEtag("i");

        final var invitationTen = new InvitationDao();
        invitationTen.setInvitedBy("5555");
        invitationTen.setInvitedAt(now.plusDays(40));

        associationTen = new AssociationDao();
        associationTen.setCompanyNumber("111111");
        associationTen.setUserId("1111");
        associationTen.setUserEmail("maggie.simpson@springfield.com");
        associationTen.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTen.setId("10");
        associationTen.setApprovedAt(now.plusDays(37));
        associationTen.setRemovedAt(now.plusDays(38));
        associationTen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTen.setApprovalExpiryAt(now.plusDays(39));
        associationTen.setInvitations(List.of(invitationTen));
        associationTen.setEtag("j");

        final var invitationEleven = new InvitationDao();
        invitationEleven.setInvitedBy("5555");
        invitationEleven.setInvitedAt(now.plusDays(44));

        associationEleven = new AssociationDao();
        associationEleven.setCompanyNumber("111111");
        associationEleven.setUserId("2222");
        associationEleven.setUserEmail("crusty.the.clown@springfield.com");
        associationEleven.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationEleven.setId("11");
        associationEleven.setApprovedAt(now.plusDays(41));
        associationEleven.setRemovedAt(now.plusDays(42));
        associationEleven.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationEleven.setApprovalExpiryAt(now.plusDays(43));
        associationEleven.setInvitations(List.of(invitationEleven));
        associationEleven.setEtag("k");

        final var invitationTwelve = new InvitationDao();
        invitationTwelve.setInvitedBy("5555");
        invitationTwelve.setInvitedAt(now.plusDays(48));

        associationTwelve = new AssociationDao();
        associationTwelve.setCompanyNumber("111111");
        associationTwelve.setUserId("3333");
        associationTwelve.setUserEmail("itchy@springfield.com");
        associationTwelve.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationTwelve.setId("12");
        associationTwelve.setApprovedAt(now.plusDays(45));
        associationTwelve.setRemovedAt(now.plusDays(46));
        associationTwelve.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationTwelve.setApprovalExpiryAt(now.plusDays(47));
        associationTwelve.setInvitations(List.of(invitationTwelve));
        associationTwelve.setEtag("l");

        final var invitationThirteen = new InvitationDao();
        invitationThirteen.setInvitedBy("5555");
        invitationThirteen.setInvitedAt(now.plusDays(52));

        associationThirteen = new AssociationDao();
        associationThirteen.setCompanyNumber("111111");
        associationThirteen.setUserId("4444");
        associationThirteen.setUserEmail("scratchy@springfield.com");
        associationThirteen.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        associationThirteen.setId("13");
        associationThirteen.setApprovedAt(now.plusDays(49));
        associationThirteen.setRemovedAt(now.plusDays(50));
        associationThirteen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationThirteen.setApprovalExpiryAt(now.plusDays(51));
        associationThirteen.setInvitations(List.of(invitationThirteen));
        associationThirteen.setEtag("m");

        final var invitationFourteen = new InvitationDao();
        invitationFourteen.setInvitedBy("111");
        invitationFourteen.setInvitedAt(now.plusDays(56));

        associationFourteen = new AssociationDao();
        associationFourteen.setCompanyNumber("111111");
        associationFourteen.setUserId("5555");
        associationFourteen.setUserEmail("ross@friends.com");
        associationFourteen.setStatus(StatusEnum.REMOVED.getValue());
        associationFourteen.setId("14");
        associationFourteen.setApprovedAt(now.plusDays(53));
        associationFourteen.setRemovedAt(now.plusDays(54));
        associationFourteen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFourteen.setApprovalExpiryAt(now.plusDays(55));
        associationFourteen.setInvitations(List.of(invitationFourteen));
        associationFourteen.setEtag("n");

        final var invitationFifteen = new InvitationDao();
        invitationFifteen.setInvitedBy("111");
        invitationFifteen.setInvitedAt(now.plusDays(60));

        associationFifteen = new AssociationDao();
        associationFifteen.setCompanyNumber("111111");
        associationFifteen.setUserId("6666");
        associationFifteen.setUserEmail("rachel@friends.com");
        associationFifteen.setStatus(StatusEnum.REMOVED.getValue());
        associationFifteen.setId("15");
        associationFifteen.setApprovedAt(now.plusDays(57));
        associationFifteen.setRemovedAt(now.plusDays(58));
        associationFifteen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationFifteen.setApprovalExpiryAt(now.plusDays(59));
        associationFifteen.setInvitations(List.of(invitationFifteen));
        associationFifteen.setEtag("o");

        final var invitationSixteen = new InvitationDao();
        invitationSixteen.setInvitedBy("111");
        invitationSixteen.setInvitedAt(now.plusDays(64));

        associationSixteen = new AssociationDao();
        associationSixteen.setCompanyNumber("111111");
        associationSixteen.setUserId("7777");
        associationSixteen.setUserEmail("chandler@friends.com");
        associationSixteen.setStatus(StatusEnum.REMOVED.getValue());
        associationSixteen.setId("16");
        associationSixteen.setApprovedAt(now.plusDays(61));
        associationSixteen.setRemovedAt(now.plusDays(62));
        associationSixteen.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
        associationSixteen.setApprovalExpiryAt(now.plusDays(63));
        associationSixteen.setInvitations(List.of(invitationSixteen));
        associationSixteen.setEtag("p");

        final var invitationEighteen = new InvitationDao();
        invitationEighteen.setInvitedBy("666");
        invitationEighteen.setInvitedAt(now.plusDays(4));

        associationEighteen = new AssociationDao();
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

        associationNineteen = new AssociationDao();
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

        associationTwenty = new AssociationDao();
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

        associationTwentyOne = new AssociationDao();
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

        associationTwentyTwo = new AssociationDao();
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

        associationTwentyThree = new AssociationDao();
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

        associationTwentyFour = new AssociationDao();
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

        associationTwentyFive = new AssociationDao();
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

        associationTwentySix = new AssociationDao();
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

        associationTwentySeven = new AssociationDao();
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

        associationTwentyEight = new AssociationDao();
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

        associationTwentyNine = new AssociationDao();
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

        associationThirty = new AssociationDao();
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

        associationThirtyOne = new AssociationDao();
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

        associationThirtyTwo = new AssociationDao();
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

        associationThirtyThree = new AssociationDao();
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

        associationsService = new AssociationsService(
                associationsRepository,
                associationsListUserMapper,
                associationsListCompanyMapper,
                associationMapper,
                invitationMapper
        );
    }

    @Test
    void fetchAssociationsForUSerReturnEmptyItemsWhenNoAssociationFound() throws ApiErrorResponseException, URIValidationException {
        User user = new User("abc","kk", "kk@kk.com");
        user.setUserId("5555");
        List<String> status = Collections.singletonList("confirmed");
        Page<AssociationDao> page = Page.empty();
        when(associationsRepository
                .findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike(
                        "5555","kk@kk.com",
                        status,
                        "",
                        PageRequest.of(0, 15)))
                .thenReturn(page);
        associationsService.fetchAssociationsForUserStatusAndCompany(user, status, 0, 15, "");
        verify(associationsListUserMapper).daoToDto(page, user);

    }

    @Test
    void fetchAssociationsForUserUsesStatusConfirmedAsDefaultWhenStatusNotProvided() throws ApiErrorResponseException, URIValidationException {
        User user = new User("abc","kk", "kk@kk.com");
        user.setUserId("5555");
        Page<AssociationDao> page = Page.empty();

        associationsService.fetchAssociationsForUserStatusAndCompany(user, null, 0, 15, "");
        verify(associationsRepository)
                .findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike(
                        "5555", "kk@kk.com",
                        Collections.singletonList("confirmed"),
                        "",
                        PageRequest.of(0, 15));
        verify(associationsListUserMapper).daoToDto(null, user);

    }

    @Test
    void fetchAssociatedUsersWithNullInputsReturnsNull() {
        final var companyDetails =
                new CompanyDetails().companyNumber("111111").companyName("Wayne Enterprises");

        Assertions.assertNull(associationsService.fetchAssociatedUsers(null, companyDetails, true, 15, 0));
        Assertions.assertNull(associationsService.fetchAssociatedUsers("111111", null, true, 15, 0));
    }

    private ArgumentMatcher<Page<AssociationDao>> associationsPageMatches(int totalElements, int totalPages, int numElementsOnPage, List<String> expectedAssociationIds) {
        return page -> {
            final var associationIds =
                    page.getContent()
                            .stream()
                            .map(AssociationDao::getId)
                            .toList();

            return page.getTotalElements() == totalElements &&
                    page.getTotalPages() == totalPages &&
                    associationIds.size() == numElementsOnPage &&
                    associationIds.containsAll(expectedAssociationIds);
        };
    }

    @Test
    void fetchAssociatedUsersWithIncludeRemovedTrueDoesNotApplyFilter() {
        final var content = List.of(associationOne, associationTwo, associationThree, associationFour, associationFive, associationSix, associationSeven, associationEight, associationNine, associationTen, associationEleven, associationTwelve, associationThirteen, associationFourteen, associationFifteen, associationSixteen);
        final var pageRequest = PageRequest.of(0, 20);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        Mockito.doReturn(page).when(associationsRepository).fetchAssociatedUsers(any(), any(), any());

        final var companyDetails =
                new CompanyDetails().companyNumber("111111").companyName("Wayne Enterprises");

        associationsService.fetchAssociatedUsers("111111", companyDetails, true, 20, 0);
        final var expectedAssociationIds = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16");
        Mockito.verify(associationsListCompanyMapper).daoToDto(argThat(associationsPageMatches(16, 1, 16, expectedAssociationIds)), eq(companyDetails));
    }

    @Test
    void fetchAssociatedUsersWithIncludeRemovedFalseAppliesFilter() {

        final var content = List.of(associationOne, associationTwo, associationThree, associationFour, associationFive, associationSix, associationSeven, associationEight, associationNine, associationTen, associationEleven, associationTwelve, associationThirteen);
        final var pageRequest = PageRequest.of(0, 20);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        Mockito.doReturn(page).when(associationsRepository).fetchAssociatedUsers(any(), any(), any());


        final var companyDetails =
                new CompanyDetails().companyNumber("111111").companyName("Wayne Enterprises");

        associationsService.fetchAssociatedUsers("111111", companyDetails, false, 20, 0);
        final var expectedAssociationIds = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13");
        Mockito.verify(associationsListCompanyMapper).daoToDto(argThat(associationsPageMatches(13, 1, 13, expectedAssociationIds)), eq(companyDetails));
    }

    @Test
    void fetchAssociatedUsersAppliesPaginationCorrectly() {
        final var content = List.of(associationSixteen);
        final var pageRequest = PageRequest.of(1, 15);
        final var page = new PageImpl<>(content, pageRequest, 16);

        Mockito.doReturn(page).when(associationsRepository).fetchAssociatedUsers(any(), any(), any());

        final var companyDetails =
                new CompanyDetails().companyNumber("111111").companyName("Wayne Enterprises");

        associationsService.fetchAssociatedUsers("111111", companyDetails, true, 15, 1);
        Mockito.verify(associationsListCompanyMapper).daoToDto(argThat(associationsPageMatches(16, 2, 1, List.of("16"))), eq(companyDetails));
    }



    @Test
    void getAssociationByIdReturnsAssociationDtoWhenAssociationFound() {
        Mockito.when(associationsRepository.findById("1")).thenReturn(Optional.of(associationOne));

        var association = associationsService.findAssociationById("1");
        Mockito.verify(associationMapper).daoToDto(associationOne);

    }

    @Test
    void getAssociationByIdReturnsEmptyWhenAssociationNotFound() {
        Mockito.when(associationsRepository.findById("1111")).thenReturn(Optional.empty());

        var association = associationsService.findAssociationById("1111");
        assertTrue(association.isEmpty());

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
        final var user = new User().userId("5555").email("abcd@abc.com").displayName( "Scrooge McDuck" );
        final var status = List.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue() );

        final var content = List.of( associationThirtyThree );
        final var pageRequest = PageRequest.of(1, 15);
        final var page = new PageImpl<>(content, pageRequest, 16 );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "5555" ), eq ("abcd@abc.com"), eq( status ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 1, 15, null );
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(16, 2, 1, List.of("33"))), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersByCompanyNumber(){
        final var user = new User().userId("5555").email("abcd@abc.com").displayName( "Scrooge McDuck" );
        final var status = List.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue() );

        final var content = List.of( associationEighteen, associationTwentySeven );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "5555" ), eq("abcd@abc.com"), eq( status ), eq("333333"), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, 15, "333333" );

        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(2, 1, 2, List.of("18", "27"))), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersBasedOnStatus(){
        final var user = new User().userId("5555").email("abcd@abc.com").displayName( "Scrooge McDuck" );
        final var status = List.of( StatusEnum.REMOVED.getValue() );

        final var content = List.of( associationThirtyOne, associationThirtyTwo, associationThirtyThree );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "5555" ), eq ("abcd@abc.com"),  eq( status ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, 15, null );
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(3, 1, 3, List.of("31", "32", "33"))), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersBasedOnAwaitingApprovalStatus(){
        final var user = new User().userId("5555").email("abcd@abc.com").displayName( "Scrooge McDuck" );
        final var status = List.of( StatusEnum.AWAITING_APPROVAL.getValue() );

        final var content = List.of( associationTwentyFive, associationTwentySix, associationTwentySeven );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "5555" ), eq ("abcd@abc.com"),  eq( status ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, 15, null );
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(3, 1, 3, List.of("25", "26", "27"))), eq(user));
    }
    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNullStatusDefaultsToConfirmedStatus(){
        final var user = new User().userId("5555").email("abcd@abc.com").displayName( "Scrooge McDuck" );

        final var content = List.of( associationEighteen, associationNineteen, associationTwenty, associationTwentyOne, associationTwentyTwo );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "5555" ), eq("abcd@abc.com"), eq( List.of( StatusEnum.CONFIRMED.getValue() ) ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, null, 0, 15, null );
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(5, 1, 5, List.of("18", "19", "20", "21", "22"))), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithEmptyStatusDefaultsToConfirmedStatus(){
        final var user = new User().userId("5555").email("abcd@abc.com").displayName( "Scrooge McDuck" );

        final var content = List.of( associationEighteen, associationNineteen, associationTwenty, associationTwentyOne, associationTwentyTwo );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "5555" ),  eq ("abcd@abc.com"),eq( List.of( StatusEnum.CONFIRMED.getValue() ) ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, Collections.emptyList(), 0, 15, null );
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(5, 1, 5, List.of("18", "19", "20", "21", "22"))), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithInvalidStatusReturnsEmptyPage() {
        final var user = new User().userId("5555").email("abcd@abc.com").displayName( "Scrooge McDuck" );

        final var content = Collections.emptyList();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "5555" ),  eq ("abcd@abc.com"),eq( List.of( "complicated" ) ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, List.of( "complicated" ), 0, 15, null );
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(0, 0, 0, Collections.emptyList())), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNonexistentOrInvalidCompanyNumberReturnsEmptyPage() {
        final var user = new User().userId("5555").email("abcd@abc.com").displayName( "Scrooge McDuck" );

        final var content = Collections.emptyList();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "5555" ),  eq ("abcd@abc.com"), eq( List.of( StatusEnum.CONFIRMED.getValue()) ), eq("$$$$$$"), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, Collections.emptyList(), 0, 15, "$$$$$$" );
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(0, 0, 0, Collections.emptyList())), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNonexistentUserIdReturnsEmptyPage() {
        final var user = new User().userId("5151").email("abcd@abc.com").displayName( "Scrooge McDuck" );

        final var content = Collections.emptyList();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "5151" ),  eq ("abcd@abc.com"), eq( List.of( StatusEnum.CONFIRMED.getValue()) ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, Collections.emptyList(), 0, 15, null );
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(0, 0, 0, Collections.emptyList())), eq(user));
    }
    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNonexistentUserEmailReturnsEmptyPage() {
        final var user = new User().userId("5555").email("xyz@abc.com").displayName( "Scrooge McDuck" );

        final var content = Collections.emptyList();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "5555" ),  eq ("xyz@abc.com"), eq( List.of( StatusEnum.CONFIRMED.getValue()) ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, Collections.emptyList(), 0, 15, null );
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(associationsPageMatches(0, 0, 0, Collections.emptyList())), eq(user));
    }

    static Stream<Arguments> provideInvalidCompanyNumberOrUserId() {
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
    @MethodSource("provideInvalidCompanyNumberOrUserId")
    void confirmedAssociationExistsWithNullOrMalformedOrNonExistentCompanyNumberOrUserReturnsFalse(String companyNumber, String userId) {
        Mockito.doReturn(false).when(associationsRepository).associationExistsWithStatuses(Mockito.any(), Mockito.any(), Mockito.anyList());
        Assertions.assertFalse(associationsService.confirmedAssociationExists(companyNumber, userId));
    }

    @Test
    void associationExistsWithExistingConfirmedAssociationReturnsTrue(){
        Mockito.doReturn( true ).when( associationsRepository ).associationExistsWithStatuses( "111111", "111", List.of(StatusEnum.CONFIRMED.getValue()));
        assertTrue( associationsService.confirmedAssociationExists( "111111", "111" ) );
    }

    @Test
    void createAssociationWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociation( null, "000", "the.void@space.com", ApprovalRouteEnum.AUTH_CODE ,"111") );
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociation( "000000", null, null, ApprovalRouteEnum.AUTH_CODE ,"111") );
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociation( "000000", "000", "the.void@space.com", null ,"111") );
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociation( "000000", "000", "the.void@space.com", ApprovalRouteEnum.INVITATION ,null) );
    }

    @Test
    void createAssociationThatAlreadyExistsThrowsDuplicateKeyException(){
        associationsService.createAssociation( "000000", "000", null, ApprovalRouteEnum.INVITATION, "111" );
        associationsService.createAssociation( "000000", null, "bruce.wayne@gotham.city", ApprovalRouteEnum.INVITATION, "111" );

        Mockito.doThrow( new DuplicateKeyException( "Association already exists" ) ).when( associationsRepository ).save( any( AssociationDao.class ) );

        Assertions.assertThrows( DuplicateKeyException.class, () -> associationsService.createAssociation( "000000", "000", null, ApprovalRouteEnum.INVITATION, "111" ) );
        Assertions.assertThrows( DuplicateKeyException.class, () -> associationsService.createAssociation( "000000", null, "bruce.wayne@gotham.city", ApprovalRouteEnum.INVITATION, "111" ) );
    }

    private ArgumentMatcher<AssociationDao> createAssociationDaoMatches(final String companyNumber, final String userId, final String userEmail, final StatusEnum statusEnum, final ApprovalRouteEnum approvalRouteEnum, final boolean approvalExpiryAtIsNull, final List<String> inviters ){
        return associationDao -> {
            final var companyNumberIsCorrect = associationDao.getCompanyNumber().equals(companyNumber);
            final var userIdIsCorrect = Objects.isNull(userId) ? Objects.isNull(associationDao.getUserId() ) : userId.equals(associationDao.getUserId());
            final var userEmailIsCorrect = Objects.isNull(userEmail) ? Objects.isNull(associationDao.getUserEmail()) : userEmail.equals(associationDao.getUserEmail());
            final var statusIsCorrect = associationDao.getStatus().equals(statusEnum.getValue());
            final var approvalRouteIsCorrect = associationDao.getApprovalRoute().equals(approvalRouteEnum.getValue());
            final var etagIsCorrect = Objects.nonNull(associationDao.getEtag());
            final var approvalExpiryAtIsCorrect = approvalExpiryAtIsNull ? Objects.isNull(associationDao.getApprovalExpiryAt()) : Objects.nonNull(associationDao.getApprovalExpiryAt());

            boolean invitationsIsCorrect;
            if ( Objects.isNull( inviters ) || inviters.isEmpty() ){
                invitationsIsCorrect = associationDao.getInvitations().isEmpty();
            } else {
                invitationsIsCorrect = inviters.containsAll( associationDao.getInvitations().stream().map( InvitationDao::getInvitedBy ).toList() );
            }

            return companyNumberIsCorrect && userIdIsCorrect && userEmailIsCorrect && statusIsCorrect && approvalRouteIsCorrect && etagIsCorrect && approvalExpiryAtIsCorrect && invitationsIsCorrect;
        };
    }

    @Test
    void createAssociationWithUserIdAndAuthCodeSuccessfullyCreatesOrUpdateAssociation(){
        associationsService.createAssociation( "000000", "000", null, ApprovalRouteEnum.AUTH_CODE, null);
        Mockito.verify( associationsRepository ).save( argThat( createAssociationDaoMatches( "000000", "000", null, StatusEnum.CONFIRMED, ApprovalRouteEnum.AUTH_CODE, true, null ) ) );
    }

    @Test
    void createAssociationWithUserIdAndInvitationSuccessfullyCreatesOrUpdateAssociation(){
        associationsService.createAssociation( "000000", "000", null, ApprovalRouteEnum.INVITATION, "111");
        Mockito.verify( associationsRepository ).save( argThat( createAssociationDaoMatches( "000000", "000", null, StatusEnum.AWAITING_APPROVAL, ApprovalRouteEnum.INVITATION, false, List.of("111") ) ) );
    }

    @Test
    void createAssociationWithUserEmailAndAuthCodeSuccessfullyCreatesOrUpdateAssociation(){
        associationsService.createAssociation( "000000", null, "bruce.wayne@gotham.city", ApprovalRouteEnum.AUTH_CODE, null);
        Mockito.verify( associationsRepository ).save( argThat( createAssociationDaoMatches( "000000", null, "bruce.wayne@gotham.city", StatusEnum.CONFIRMED, ApprovalRouteEnum.AUTH_CODE, true, List.of("111") ) ) );
    }

    @Test
    void createAssociationWithUserEmailAndInvitationSuccessfullyCreatesOrUpdateAssociation(){
        associationsService.createAssociation( "000000", null, "bruce.wayne@gotham.city", ApprovalRouteEnum.INVITATION, "111");
        Mockito.verify( associationsRepository ).save( argThat( createAssociationDaoMatches( "000000", null, "bruce.wayne@gotham.city", StatusEnum.AWAITING_APPROVAL, ApprovalRouteEnum.INVITATION, false, List.of("111") ) ) );
    }

    @Test
    void sendNewInvitationWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.sendNewInvitation( null, associationOne ) );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.sendNewInvitation( "111", null ) );
    }

    @Test
    void sendNewInvitationWithNonexistentAssociationCreatesNewAssociation(){
        final var association = new AssociationDao();
        association.setCompanyNumber( "000000" );
        association.setUserId( null );
        association.setUserEmail( "the.void@space.com" );
        association.setApprovalRoute( ApprovalRouteEnum.INVITATION.getValue() );

        associationsService.sendNewInvitation( "111", association );
        Mockito.verify( associationsRepository ).save( argThat( createAssociationDaoMatches( "000000", null, "the.void@space.com", StatusEnum.AWAITING_APPROVAL, ApprovalRouteEnum.INVITATION, false, List.of("111") ) ) );
    }

    @Test
    void sendNewInvitationWithExistingAssociationCreatesNewInvitation(){
        associationsService.sendNewInvitation( "222", associationOne );
        Mockito.verify( associationsRepository ).save( argThat( createAssociationDaoMatches( "111111", "111", "bruce.wayne@gotham.city", StatusEnum.AWAITING_APPROVAL, ApprovalRouteEnum.AUTH_CODE, false, List.of("666", "222") ) ) );
    }

    @Test
    void updateAssociationStatusWithMalformedOrNonexistentAssociationIdThrowsInternalServerError(){
        Mockito.doReturn( 0 ).when( associationsRepository ).updateAssociation( any(), any() );
        final var update = new Update();
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> associationsService.updateAssociation( "$$$", update) );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> associationsService.updateAssociation( "9191", update) );
    }

    @Test
    void updateAssociationStatusWithNullAssociationIdOrUserIdOrNullStatusThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.updateAssociation( null, null) );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.updateAssociation( "1", null) );
    }

    private ArgumentMatcher<Update> updateAssociationMatches( String expectedStatus, boolean approvedAtShouldBeNull, boolean removedAtShouldBeNull, String expectedUserEmail, String expectedUserId ){
        return update -> {
            final var document = update.getUpdateObject().get("$set", Document.class);
            final var statusIsCorrect = expectedStatus.equals( document.get( "status" ) );
            final var approvedAtIsCorrect = Objects.isNull( document.getOrDefault( "approved_at", null ) ) == approvedAtShouldBeNull;
            final var removedAtIsCorrect = Objects.isNull( document.getOrDefault( "removed_at", null ) ) == removedAtShouldBeNull;
            final var etagIsNotNull = Objects.nonNull( document.getOrDefault( "etag", null ) );
            final var userEmail = document.getOrDefault( "user_email", null );
            boolean userEmailIsCorrect = Objects.isNull( expectedUserEmail ) ? Objects.isNull( userEmail ) : expectedUserEmail.equals( userEmail );
            final var userId = document.getOrDefault( "user_id", null );
            boolean userIdIsCorrect = Objects.isNull( expectedUserId ) ? Objects.isNull( userId ) : expectedUserId.equals( userId );

            return statusIsCorrect &&
                    approvedAtIsCorrect &&
                    removedAtIsCorrect &&
                    etagIsNotNull &&
                    userEmailIsCorrect &&
                    userIdIsCorrect;
        };
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserIdWithNullOrMalformedOrNonexistentCompanyNumberReturnsNothing(){
        Mockito.doReturn( Optional.empty() ).when( associationsRepository ).fetchAssociationForCompanyNumberAndUserId( any(), anyString() );

        assertTrue( associationsService.fetchAssociationForCompanyNumberAndUserId( null, "111" ).isEmpty() );
        assertTrue( associationsService.fetchAssociationForCompanyNumberAndUserId( "$$$$$$", "111" ).isEmpty() );
        assertTrue( associationsService.fetchAssociationForCompanyNumberAndUserId( "919191", "111" ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserIdWithMalformedOrNonexistentUserIdReturnsNothing() {
        Mockito.doReturn( Optional.empty() ).when( associationsRepository ).fetchAssociationForCompanyNumberAndUserId( anyString(), anyString() );

        assertTrue( associationsService.fetchAssociationForCompanyNumberAndUserId( "111111", "$$$" ).isEmpty() );
        assertTrue( associationsService.fetchAssociationForCompanyNumberAndUserId( "111111", "9191" ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserIdShouldFetchAssociation(){
        final var association = new AssociationDao();
        association.setId( "1" );

        Mockito.doReturn( Optional.of( association ) ).when( associationsRepository ).fetchAssociationForCompanyNumberAndUserId( anyString(), anyString() );
        assertEquals( "1", associationsService.fetchAssociationForCompanyNumberAndUserId( "111111", "111" ).get().getId() );
    }

    @Test
    void fetchInvitationsWithNullInvitationsListReturnsEmptyList() {
        AssociationDao associationDao = new AssociationDao();
        associationDao.setId("18");
        associationDao.setInvitations(Collections.emptyList());

        InvitationsList invitations = associationsService.fetchInvitations(associationDao, 0, 1);

        assertTrue(invitations.getItems().isEmpty());
        assertEquals(1, invitations.getItemsPerPage());
        assertEquals(0, invitations.getPageNumber());
        assertEquals(0, invitations.getTotalResults());
        assertEquals(0, invitations.getTotalPages());
        assertEquals("/associations/18/invitations?page_index=0&items_per_page=1", invitations.getLinks().getSelf());
        assertTrue(invitations.getLinks().getNext().isEmpty());
    }

    @ParameterizedTest
    @MethodSource("userIdsProvider")
    void fetchActiveInvitationsWithNullOrMalformedOrNonexistentUserIdReturnsEmptyList(String userId) {
        InvitationsList invitations = associationsService.fetchActiveInvitations(new User().userId(userId), 0, 1);

        assertTrue(invitations.getItems().isEmpty());
        assertEquals(1, invitations.getItemsPerPage());
        assertEquals(0, invitations.getPageNumber());
        assertEquals(0, invitations.getTotalResults());
        assertEquals(0, invitations.getTotalPages());
        assertEquals("/associations/invitations?page_index=0&items_per_page=1", invitations.getLinks().getSelf());
        assertTrue(invitations.getLinks().getNext().isEmpty());
    }

    private static Stream<String> userIdsProvider() {
        return Stream.of(null, "$$$", "9191");
    }

}