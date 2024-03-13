package uk.gov.companieshouse.accounts.association.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListCompanyDaoToDtoMapper;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.models.Invitation;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.sdk.ApiClientService;

@SpringBootTest
@Tag("unit-test")
class AssociationsServiceTest {

    @MockBean
    AssociationsRepository associationsRepository;

    @MockBean
    ApiClientService apiClientService;

    @MockBean
    InternalApiClient internalApiClient;

    @MockBean
    AssociationsListCompanyDaoToDtoMapper associationsListCompanyDaoToDtoMapper;

    @Autowired
    AssociationsService associationsService;

    private Association associationOne;
    private Association associationTwo;
    private Association associationThree;
    private Association associationFour;
    private Association associationFive;
    private Association associationSix;
    private Association associationSeven;
    private Association associationEight;
    private Association associationNine;
    private Association associationTen;
    private Association associationEleven;
    private Association associationTwelve;
    private Association associationThirteen;
    private Association associationFourteen;
    private Association associationFifteen;
    private Association associationSixteen;

    @BeforeEach
    public void setup() {
        final var now = LocalDateTime.now();

        final var invitationOne = new Invitation();
        invitationOne.setInvitedBy("666");
        invitationOne.setInvitedAt(now.plusDays(4));

        associationOne = new Association();
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

        associationTwo = new Association();
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

        associationThree = new Association();
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

        associationFour = new Association();
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

        associationFive = new Association();
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

        associationSix = new Association();
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

        associationSeven = new Association();
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

        associationEight = new Association();
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

        associationNine = new Association();
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

        associationTen = new Association();
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

        associationEleven = new Association();
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

        associationTwelve = new Association();
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

        associationThirteen = new Association();
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

        associationFourteen = new Association();
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

        associationFifteen = new Association();
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

        associationSixteen = new Association();
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
    }

    @Test
    void fetchAssociatedUsersWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.fetchAssociatedUsers( null, "/associations/company/111111", true, 15, 0 ) );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.fetchAssociatedUsers( "111111", null, true, 15, 0 ) );
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
        final var content = new ArrayList<Association>();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociatedUsers( any(), any(), any() );

        associationsService.fetchAssociatedUsers( "$$$$$$", "/associations/company/$$$$$$", true, 15, 0 ) ;
        Mockito.verify( associationsListCompanyDaoToDtoMapper ).daoToDto( argThat( associationsPageMatches(0, 0, 0, List.of()) ), eq( Map.of( "companyNumber", "$$$$$$", "endpointUri", "/associations/company/$$$$$$" ) ) );

        associationsService.fetchAssociatedUsers( "919191", "/associations/company/919191", true, 15, 0 ) ;
        Mockito.verify( associationsListCompanyDaoToDtoMapper ).daoToDto( argThat( associationsPageMatches(0, 0, 0, List.of()) ), eq( Map.of( "companyNumber", "919191", "endpointUri", "/associations/company/919191" ) ) );
    }

    @Test
    void fetchAssociatedUsersWithIncludeRemovedTrueDoesNotApplyFilter(){
        final var content = List.of( associationOne, associationTwo, associationThree, associationFour, associationFive, associationSix, associationSeven, associationEight, associationNine, associationTen, associationEleven, associationTwelve, associationThirteen, associationFourteen, associationFifteen, associationSixteen );
        final var pageRequest = PageRequest.of(0, 20);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociatedUsers( any(), any(), any() );

        associationsService.fetchAssociatedUsers( "111111", "/associations/company/111111", true, 20, 0 );
        final var expectedAssociationIds = List.of( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" );
        Mockito.verify( associationsListCompanyDaoToDtoMapper ).daoToDto( argThat( associationsPageMatches(16, 1, 16, expectedAssociationIds ) ), eq( Map.of( "companyNumber", "111111", "endpointUri", "/associations/company/111111" ) ) );
    }

    @Test
    void fetchAssociatedUsersWithIncludeRemovedFalseAppliesFilter(){
        final var content = List.of( associationOne, associationTwo, associationThree, associationFour, associationFive, associationSix, associationSeven, associationEight, associationNine, associationTen, associationEleven, associationTwelve, associationThirteen );
        final var pageRequest = PageRequest.of(0, 20);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociatedUsers( any(), any(), any() );

        associationsService.fetchAssociatedUsers( "111111", "/associations/company/111111", false, 20, 0 );
        final var expectedAssociationIds = List.of( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" );
        Mockito.verify( associationsListCompanyDaoToDtoMapper ).daoToDto( argThat( associationsPageMatches(13, 1, 13, expectedAssociationIds ) ), eq( Map.of( "companyNumber", "111111", "endpointUri", "/associations/company/111111" ) ) );
    }

    @Test
    void fetchAssociatedUsersAppliesPaginationCorrectly(){
        final var content = List.of(associationSixteen);
        final var pageRequest = PageRequest.of( 1, 15 );
        final var page = new PageImpl<>( content, pageRequest, 16 );

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociatedUsers( any(), any(), any() );

        associationsService.fetchAssociatedUsers( "111111", "/associations/company/111111", true, 15, 1 );
        Mockito.verify( associationsListCompanyDaoToDtoMapper ).daoToDto( argThat( associationsPageMatches(16, 2, 1, List.of( "16" ) ) ), eq( Map.of( "companyNumber", "111111", "endpointUri", "/associations/company/111111" ) ) );
    }

}