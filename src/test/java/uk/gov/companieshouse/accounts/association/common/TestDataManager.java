package uk.gov.companieshouse.accounts.association.common;


import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToOffsetDateTime;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.models.PreviousStatesDao;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationLinks;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;
import uk.gov.companieshouse.api.accounts.associations.model.Links;
import uk.gov.companieshouse.api.accounts.associations.model.PreviousState;
import uk.gov.companieshouse.api.accounts.associations.model.PreviousStatesList;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;

public class TestDataManager {

    private static TestDataManager instance = null;

    public static TestDataManager getInstance(){
        if ( Objects.isNull( instance ) ){
            instance = new TestDataManager();
        }
        return instance;
    }

    private final LocalDateTime now = LocalDateTime.now();

    private final Map<String, Supplier<AssociationDao>> associationDaoSuppliers = new HashMap<>();
    private final Map<String, Supplier<User>> userDtoSuppliers = new HashMap<>();
    private final Map<String, Supplier<CompanyDetails>> companyDetailsDtoSuppliers = new HashMap<>();

    private void instantiateAssociationDaoSuppliers(){
        final Supplier<AssociationDao> WayneEnterprisesBatmanAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("666");
            invitation.setInvitedAt(now.plusDays(4));

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("111");
            association.setUserEmail("bruce.wayne@gotham.city");
            association.setStatus(StatusEnum.CONFIRMED.getValue());
            association.setId("1");
            association.setApprovedAt(now.plusDays(1));
            association.setRemovedAt(now.plusDays(2));
            association.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
            association.setApprovalExpiryAt(now.plusDays(3));
            association.setInvitations( List.of( invitation ) );
            association.setEtag( "a" );
            return association;
        };
        associationDaoSuppliers.put( "1", WayneEnterprisesBatmanAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesJokerAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("666");
            invitation.setInvitedAt( now.plusDays(8) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("222");
            association.setUserEmail("the.joker@gotham.city");
            association.setStatus(StatusEnum.CONFIRMED.getValue());
            association.setId("2");
            association.setApprovedAt( now.plusDays(5) );
            association.setRemovedAt( now.plusDays(6) );
            association.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
            association.setApprovalExpiryAt( now.plusDays(7) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("b");

            return association;
        };
        associationDaoSuppliers.put( "2", WayneEnterprisesJokerAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesHarleyQuinnAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("666");
            invitation.setInvitedAt( now.plusDays(12) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("333");
            association.setUserEmail("harley.quinn@gotham.city");
            association.setStatus(StatusEnum.CONFIRMED.getValue());
            association.setId("3");
            association.setApprovedAt( now.plusDays(9) );
            association.setRemovedAt( now.plusDays(10) );
            association.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
            association.setApprovalExpiryAt( now.plusDays(11) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("c");

            return association;
        };
        associationDaoSuppliers.put( "3", WayneEnterprisesHarleyQuinnAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesRobinAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("666");
            invitation.setInvitedAt( now.plusDays(16) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("444");
            association.setUserEmail("robin@gotham.city");
            association.setStatus(StatusEnum.CONFIRMED.getValue());
            association.setId("4");
            association.setApprovedAt( now.plusDays(13) );
            association.setRemovedAt( now.plusDays(14) );
            association.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
            association.setApprovalExpiryAt( now.plusDays(15) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("d");

            return association;
        };
        associationDaoSuppliers.put( "4", WayneEnterprisesRobinAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesBatWomanAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("666");
            invitation.setInvitedAt( now.plusDays(20) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("555");
            association.setUserEmail("barbara.gordon@gotham.city");
            association.setStatus(StatusEnum.CONFIRMED.getValue());
            association.setId("5");
            association.setApprovedAt( now.plusDays(17) );
            association.setRemovedAt( now.plusDays(18) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(19) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("e");

            return association;
        };
        associationDaoSuppliers.put( "5", WayneEnterprisesBatWomanAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesHomerAssociationDaoSupplier = () -> {
            final var olderInvitation = new InvitationDao();
            olderInvitation.setInvitedBy("444");
            olderInvitation.setInvitedAt( now.plusDays(14) );

            final var newerInvitation = new InvitationDao();
            newerInvitation.setInvitedBy("222");
            newerInvitation.setInvitedAt( now.plusDays(16) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("666");
            association.setUserEmail("homer.simpson@springfield.com");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("6");
            association.setApprovedAt( now.plusDays(21) );
            association.setRemovedAt( now.plusDays(22) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(23) );
            association.setInvitations( List.of( olderInvitation, newerInvitation ) );
            association.setEtag("f");

            return association;
        };
        associationDaoSuppliers.put( "6", WayneEnterprisesHomerAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesMargeAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("5555");
            invitation.setInvitedAt( now.plusDays(28) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("777");
            association.setUserEmail("marge.simpson@springfield.com");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("7");
            association.setApprovedAt( now.plusDays(25) );
            association.setRemovedAt( now.plusDays(26) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(27) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("g");

            return association;
        };
        associationDaoSuppliers.put( "7", WayneEnterprisesMargeAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesBartAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("5555");
            invitation.setInvitedAt( now.plusDays(32) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("888");
            association.setUserEmail("bart.simpson@springfield.com");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("8");
            association.setApprovedAt( now.plusDays(29) );
            association.setRemovedAt( now.plusDays(30) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(31) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("h");

            return association;
        };
        associationDaoSuppliers.put( "8", WayneEnterprisesBartAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesLisaAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("5555");
            invitation.setInvitedAt( now.plusDays(36) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("999");
            association.setUserEmail("lisa.simpson@springfield.com");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("9");
            association.setApprovedAt( now.plusDays(33) );
            association.setRemovedAt( now.plusDays(34) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(35) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("i");

            return association;
        };
        associationDaoSuppliers.put( "9", WayneEnterprisesLisaAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesMaggieAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("5555");
            invitation.setInvitedAt( now.plusDays(40) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("1111");
            association.setUserEmail("maggie.simpson@springfield.com");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("10");
            association.setApprovedAt( now.plusDays(37) );
            association.setRemovedAt( now.plusDays(38) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(39) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("j");

            return association;
        };
        associationDaoSuppliers.put( "10", WayneEnterprisesMaggieAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesCrustyAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("5555");
            invitation.setInvitedAt( now.plusDays(44) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("2222");
            association.setUserEmail("crusty.the.clown@springfield.com");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("11");
            association.setApprovedAt( now.plusDays(41) );
            association.setRemovedAt( now.plusDays(42) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(43) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("k");

            return association;
        };

        associationDaoSuppliers.put( "11", WayneEnterprisesCrustyAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesItchyAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("5555");
            invitation.setInvitedAt( now.plusDays(48) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("3333");
            association.setUserEmail("itchy@springfield.com");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("12");
            association.setApprovedAt( now.plusDays(45) );
            association.setRemovedAt( now.plusDays(46) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(47) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("l");

            return association;
        };
        associationDaoSuppliers.put( "12", WayneEnterprisesItchyAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesScratchyAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("5555");
            invitation.setInvitedAt( now.plusDays(52) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("4444");
            association.setUserEmail("scratchy@springfield.com");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("13");
            association.setApprovedAt( now.plusDays(49) );
            association.setRemovedAt( now.plusDays(50) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(51) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("m");

            return association;
        };
        associationDaoSuppliers.put( "13", WayneEnterprisesScratchyAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesRossAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("111");
            invitation.setInvitedAt( now.plusDays(56) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("5555");
            association.setUserEmail("ross@friends.com");
            association.setStatus(StatusEnum.REMOVED.getValue());
            association.setId("14");
            association.setApprovedAt( now.plusDays(53) );
            association.setRemovedAt( now.plusDays(54) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(55) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("n");

            return association;
        };
        associationDaoSuppliers.put( "14", WayneEnterprisesRossAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesRachelAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("111");
            invitation.setInvitedAt( now.plusDays(60) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("6666");
            association.setUserEmail("rachel@friends.com");
            association.setStatus(StatusEnum.REMOVED.getValue());
            association.setId("15");
            association.setApprovedAt( now.plusDays(57) );
            association.setRemovedAt( now.plusDays(58) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(59) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("o");

            return association;
        };
        associationDaoSuppliers.put( "15", WayneEnterprisesRachelAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesChandlerAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("111");
            invitation.setInvitedAt( now.plusDays(64) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserId("7777");
            association.setUserEmail("chandler@friends.com");
            association.setStatus(StatusEnum.REMOVED.getValue());
            association.setId("16");
            association.setApprovedAt( now.plusDays(61) );
            association.setRemovedAt( now.plusDays(62) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(63) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("p");

            return association;
        };
        associationDaoSuppliers.put( "16", WayneEnterprisesChandlerAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesMrBlobbyAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("111");
            invitation.setInvitedAt( now.plusDays(68) );

            final var association = new AssociationDao();
            association.setCompanyNumber("222222");
            association.setUserEmail("mr.blobby@nightmare.com");
            association.setStatus(StatusEnum.CONFIRMED.getValue());
            association.setId("17");
            association.setApprovedAt( now.plusDays(65) );
            association.setRemovedAt( now.plusDays(66) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(67) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("q");

            return association;
        };
        associationDaoSuppliers.put( "17", WayneEnterprisesMrBlobbyAssociationDaoSupplier );

        final Supplier<AssociationDao> TescoScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("666");
            invitation.setInvitedAt(now.plusDays(4));

            final var association = new AssociationDao();
            association.setCompanyNumber("333333");
            association.setUserId("9999");
            association.setStatus(StatusEnum.CONFIRMED.getValue());
            association.setId("18");
            association.setApprovedAt(now.plusDays(1));
            association.setRemovedAt(now.plusDays(2));
            association.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
            association.setApprovalExpiryAt(now.plusDays(3));
            association.setInvitations( List.of( invitation ) );
            association.setEtag( "aa" );

            return association;
        };
        associationDaoSuppliers.put( "18", TescoScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> SainsburysScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("666");
            invitation.setInvitedAt( now.plusDays(8) );

            final var association = new AssociationDao();
            association.setCompanyNumber("444444");
            association.setUserId("9999");
            association.setStatus(StatusEnum.CONFIRMED.getValue());
            association.setId("19");
            association.setApprovedAt( now.plusDays(5) );
            association.setRemovedAt( now.plusDays(6) );
            association.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
            association.setApprovalExpiryAt( now.plusDays(7) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("bb");

            return association;
        };
        associationDaoSuppliers.put( "19", SainsburysScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> MorrisonScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("666");
            invitation.setInvitedAt( now.plusDays(12) );

            final var association = new AssociationDao();
            association.setCompanyNumber("555555");
            association.setUserId("9999");
            association.setStatus(StatusEnum.CONFIRMED.getValue());
            association.setId("20");
            association.setApprovedAt( now.plusDays(9) );
            association.setRemovedAt( now.plusDays(10) );
            association.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
            association.setApprovalExpiryAt( now.plusDays(11) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("cc");

            return association;
        };
        associationDaoSuppliers.put( "20", MorrisonScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> AldiScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("666");
            invitation.setInvitedAt( now.plusDays(16) );

            final var association = new AssociationDao();
            association.setCompanyNumber("666666");
            association.setUserId("9999");
            association.setStatus(StatusEnum.CONFIRMED.getValue());
            association.setId("21");
            association.setApprovedAt( now.plusDays(13) );
            association.setRemovedAt( now.plusDays(14) );
            association.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
            association.setApprovalExpiryAt( now.plusDays(15) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("dd");

            return association;
        };
        associationDaoSuppliers.put( "21", AldiScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> LidlScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("666");
            invitation.setInvitedAt( now.plusDays(20) );

            final var association = new AssociationDao();
            association.setCompanyNumber("777777");
            association.setUserId("9999");
            association.setStatus(StatusEnum.CONFIRMED.getValue());
            association.setId("22");
            association.setApprovedAt( now.plusDays(17) );
            association.setRemovedAt( now.plusDays(18) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(19) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("ee");

            return association;
        };
        associationDaoSuppliers.put( "22", LidlScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> McDonaldsScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("5555");
            invitation.setInvitedAt( now.plusDays(24) );

            final var association = new AssociationDao();
            association.setCompanyNumber("888888");
            association.setUserId("9999");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("23");
            association.setApprovedAt( now.plusDays(21) );
            association.setRemovedAt( now.plusDays(22) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(23) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("ff");

            return association;
        };
        associationDaoSuppliers.put( "23", McDonaldsScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> BurgerKingScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("5555");
            invitation.setInvitedAt( now.plusDays(28) );

            final var association = new AssociationDao();
            association.setCompanyNumber("999999");
            association.setUserId("9999");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("24");
            association.setApprovedAt( now.plusDays(25) );
            association.setRemovedAt( now.plusDays(26) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(27) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("gg");

            return association;
        };
        associationDaoSuppliers.put( "24", BurgerKingScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> PizzaHutScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("5555");
            invitation.setInvitedAt( now.plusDays(32) );

            final var association = new AssociationDao();
            association.setCompanyNumber("x111111");
            association.setUserId("9999");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("25");
            association.setApprovedAt( now.plusDays(29) );
            association.setRemovedAt( now.plusDays(30) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(31) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("hh");

            return association;
        };
        associationDaoSuppliers.put( "25", PizzaHutScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> DominosScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("5555");
            invitation.setInvitedAt( now.plusDays(36) );

            final var association = new AssociationDao();
            association.setCompanyNumber("x222222");
            association.setUserId("9999");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("26");
            association.setApprovedAt( now.plusDays(33) );
            association.setRemovedAt( now.plusDays(34) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(35) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("ii");

            return association;
        };
        associationDaoSuppliers.put( "26", DominosScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> PizzaExpressScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("5555");
            invitation.setInvitedAt( now.plusDays(40) );

            final var association = new AssociationDao();
            association.setCompanyNumber("x333333");
            association.setUserId("9999");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("27");
            association.setApprovedAt( now.plusDays(37) );
            association.setRemovedAt( now.plusDays(38) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(39) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("jj");

            return association;
        };
        associationDaoSuppliers.put( "27", PizzaExpressScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> NandosScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("5555");
            invitation.setInvitedAt( now.plusDays(44) );

            final var association = new AssociationDao();
            association.setCompanyNumber("x444444");
            association.setUserId("9999");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("28");
            association.setApprovedAt( now.plusDays(41) );
            association.setRemovedAt( now.plusDays(42) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(43) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("kk");

            return association;
        };
        associationDaoSuppliers.put( "28", NandosScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> SubwayScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("5555");
            invitation.setInvitedAt( now.plusDays(48) );

            final var association = new AssociationDao();
            association.setCompanyNumber("x555555");
            association.setUserId("9999");
            association.setUserEmail("scrooge.mcduck@disney.land");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("29");
            association.setApprovedAt( now.plusDays(45) );
            association.setRemovedAt( now.plusDays(46) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(47) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("ll");

            return association;
        };
        associationDaoSuppliers.put( "29", SubwayScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> GreggsScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("5555");
            invitation.setInvitedAt( now.plusDays(52) );

            final var association = new AssociationDao();
            association.setCompanyNumber("x666666");
            association.setUserId("9999");
            association.setUserEmail("scrooge.mcduck@disney.land");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("30");
            association.setApprovedAt( now.plusDays(49) );
            association.setRemovedAt( now.plusDays(50) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(51) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("mm");

            return association;
        };
        associationDaoSuppliers.put( "30", GreggsScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> FacebookScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("111");
            invitation.setInvitedAt( now.plusDays(56) );

            final var association = new AssociationDao();
            association.setCompanyNumber("x777777");
            association.setUserId("9999");
            association.setStatus(StatusEnum.REMOVED.getValue());
            association.setId("31");
            association.setApprovedAt( now.plusDays(53) );
            association.setRemovedAt( now.plusDays(54) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(55) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("nn");

            return association;
        };
        associationDaoSuppliers.put( "31", FacebookScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> TwitterScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("111");
            invitation.setInvitedAt( now.plusDays(60) );

            final var association = new AssociationDao();
            association.setCompanyNumber("x888888");
            association.setUserId("9999");
            association.setStatus(StatusEnum.REMOVED.getValue());
            association.setId("32");
            association.setApprovedAt( now.plusDays(57) );
            association.setRemovedAt( now.plusDays(58) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(59) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("oo");

            return association;
        };
        associationDaoSuppliers.put( "32", TwitterScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> InstagramScroogeMcDuckAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("111");
            invitation.setInvitedAt( now.plusDays(64) );

            final var association = new AssociationDao();
            association.setCompanyNumber("x999999");
            association.setUserId("9999");
            association.setStatus(StatusEnum.REMOVED.getValue());
            association.setId("33");
            association.setApprovedAt( now.plusDays(61) );
            association.setRemovedAt( now.plusDays(62) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(63) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("pp");

            return association;
        };
        associationDaoSuppliers.put( "33", InstagramScroogeMcDuckAssociationDaoSupplier );

        final Supplier<AssociationDao> WayneEnterprisesLightYagamiAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("111");
            invitation.setInvitedAt( now.plusDays(64) );

            final var association = new AssociationDao();
            association.setCompanyNumber("111111");
            association.setUserEmail( "light.yagami@death.note" );
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("34");
            association.setApprovedAt( now.plusDays(61) );
            association.setRemovedAt( now.plusDays(62) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(63) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("qq");

            return association;
        };
        associationDaoSuppliers.put( "34", WayneEnterprisesLightYagamiAssociationDaoSupplier );

        final Supplier<AssociationDao> TescoLightYagamiAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("9999");
            invitation.setInvitedAt( now.plusDays(64) );

            final var association = new AssociationDao();
            association.setCompanyNumber("333333");
            association.setUserId( "000" );
            association.setStatus(StatusEnum.CONFIRMED.getValue());
            association.setId("35");
            association.setApprovedAt( now.plusDays(61) );
            association.setRemovedAt( now.plusDays(62) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(63) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("rr");

            return association;
        };
        associationDaoSuppliers.put( "35", TescoLightYagamiAssociationDaoSupplier );

        final Supplier<AssociationDao> SainsburysLightYagamiAssociationDaoSupplier = () -> {
            final var invitation = new InvitationDao();
            invitation.setInvitedBy("9999");
            invitation.setInvitedAt( now.plusDays(64) );

            final var association = new AssociationDao();
            association.setCompanyNumber("444444");
            association.setUserEmail( "light.yagami@death.note" );
            association.setStatus(StatusEnum.REMOVED.getValue());
            association.setId("36");
            association.setApprovedAt( now.plusDays(61) );
            association.setRemovedAt( now.plusDays(62) );
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(63) );
            association.setInvitations( List.of( invitation ) );
            association.setEtag("rr");

            return association;
        };
        associationDaoSuppliers.put( "36", SainsburysLightYagamiAssociationDaoSupplier );

        final Supplier<AssociationDao> PizzaHutLightYagamiAssociationDaoSupplier = () -> {
            final var invitationOldest = new InvitationDao();
            invitationOldest.setInvitedBy("666");
            invitationOldest.setInvitedAt(now.minusDays(9));

            final var invitationMedian = new InvitationDao();
            invitationMedian.setInvitedBy("333");
            invitationMedian.setInvitedAt(now.minusDays(6));

            final var invitationNewest = new InvitationDao();
            invitationNewest.setInvitedBy("444");
            invitationNewest.setInvitedAt(now.minusDays(4));

            final var association = new AssociationDao();
            association.setCompanyNumber("X111111");
            association.setUserId("000");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("37");
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt(now.plusDays(11));
            association.setInvitations( List.of( invitationMedian, invitationOldest, invitationNewest ) );
            association.setEtag( "aa" );

            return association;
        };
        associationDaoSuppliers.put( "37", PizzaHutLightYagamiAssociationDaoSupplier );

        final Supplier<AssociationDao> DominosLightYagamiAssociationDaoSupplier = () -> {
            final var invitationOldest = new InvitationDao();
            invitationOldest.setInvitedBy("111");
            invitationOldest.setInvitedAt( now.minusDays(3) );

            final var invitationMedian = new InvitationDao();
            invitationMedian.setInvitedBy("222");
            invitationMedian.setInvitedAt( now.minusDays(2) );

            final var invitationNewest = new InvitationDao();
            invitationNewest.setInvitedBy("444");
            invitationNewest.setInvitedAt( now.plusDays(8) );

            final var association = new AssociationDao();
            association.setCompanyNumber("x222222");
            association.setUserId("9999");
            association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
            association.setId("38");
            association.setApprovalRoute(ApprovalRouteEnum.INVITATION.getValue());
            association.setApprovalExpiryAt( now.plusDays(15) );
            association.setInvitations( List.of( invitationOldest, invitationMedian, invitationNewest ) );
            association.setEtag("bb");

            return association;
        };
        associationDaoSuppliers.put( "38", DominosLightYagamiAssociationDaoSupplier );

        final Supplier<AssociationDao> marioAssociation = () -> new AssociationDao()
                .id( "MKAssociation001" )
                .companyNumber( "MKCOMP001" )
                .userEmail( "mario@mushroom.kingdom" )
                .status( "migrated" )
                .approvalRoute( "migration" )
                .migratedAt( now.minusDays( 10L ) )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MKAssociation001", marioAssociation );

        final Supplier<AssociationDao> luigiAssociation = () -> new AssociationDao()
                .id( "MKAssociation002" )
                .companyNumber( "MKCOMP001" )
                .userId( "MKUser002" )
                .status( "confirmed" )
                .approvalRoute( "auth_code" )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MKAssociation002", luigiAssociation );

        final Supplier<AssociationDao> peachAssociation = () -> new AssociationDao()
                .id( "MKAssociation003" )
                .companyNumber( "MKCOMP001" )
                .userId( "MKUser003" )
                .status( "removed" )
                .approvalRoute( "migration" )
                .invitations( List.of( new InvitationDao().invitedBy( "MKUser002" ).invitedAt( now.minusDays( 8L ) ) ) )
                .approvalExpiryAt( now.minusDays( 1L ) )
                .approvedAt( now.minusDays( 7L ) )
                .removedAt( now.minusDays( 6L ) )
                .migratedAt( now.minusDays( 10L ) )

                .previousStates( List.of(
                        new PreviousStatesDao().status( "migrated" ).changedBy( "MKUser002" ).changedAt( now.minusDays( 9L ) ),
                        new PreviousStatesDao().status( "removed" ).changedBy( "MKUser002" ).changedAt( now.minusDays( 8L ) ),
                        new PreviousStatesDao().status( "awaiting-approval" ).changedBy( "MKUser003" ).changedAt( now.minusDays( 7L ) ),
                        new PreviousStatesDao().status( "confirmed" ).changedBy( "MKUser003" ).changedAt( now.minusDays( 6L ) )
                ) )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MKAssociation003", peachAssociation );

        final Supplier<AssociationDao> MiAssociation001 = () -> new AssociationDao()
                .id( "MiAssociation001" )
                .companyNumber( "MICOMP001" )
                .userId( "MiUser001" )
                .status( "confirmed" )
                .approvalRoute( "auth_code" )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MiAssociation001", MiAssociation001 );

        final Supplier<AssociationDao> MiAssociation002 = () -> new AssociationDao()
                .id( "MiAssociation002" )
                .companyNumber( "MICOMP001" )
                .userId( "MiUser002" )
                .status( "confirmed" )
                .approvalRoute( "auth_code" )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MiAssociation002", MiAssociation002 );

        final Supplier<AssociationDao> MiAssociation003 = () -> new AssociationDao()
                .id( "MiAssociation003" )
                .companyNumber( "MICOMP002" )
                .userId( "MiUser002" )
                .status( "confirmed" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "1992-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "1992-05-08T10:30:00.000000" ) )
                .approvedAt( LocalDateTime.parse( "1992-05-06T10:30:00.000000" ) )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MiAssociation003", MiAssociation003 );

        final Supplier<AssociationDao> MiAssociation004 = () -> new AssociationDao()
                .id( "MiAssociation004" )
                .companyNumber( "MICOMP003" )
                .userId( "MiUser002" )
                .status( "awaiting-approval" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "2020-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "2020-05-08T10:30:00.000000" ) )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MiAssociation004", MiAssociation004 );

        final Supplier<AssociationDao> MiAssociation005 = () -> new AssociationDao()
                .id( "MiAssociation005" )
                .companyNumber( "MICOMP004" )
                .userEmail( "lechuck.monkey.island@inugami-example.com" )
                .status( "awaiting-approval" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "2021-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "2021-05-08T10:30:00.000000" ) )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MiAssociation005", MiAssociation005 );

        final Supplier<AssociationDao> MiAssociation006 = () -> new AssociationDao()
                .id( "MiAssociation006" )
                .companyNumber( "MICOMP005" )
                .userId( "MiUser002" )
                .status( "awaiting-approval" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "2500-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "2500-05-08T10:30:00.000000" ) )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MiAssociation006", MiAssociation006 );

        final Supplier<AssociationDao> MiAssociation007 = () -> new AssociationDao()
                .id( "MiAssociation007" )
                .companyNumber( "MICOMP006" )
                .userEmail( "lechuck.monkey.island@inugami-example.com" )
                .status( "awaiting-approval" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser004" ).invitedAt( LocalDateTime.parse( "2019-05-01T10:30:00.000000" ) ), new InvitationDao().invitedBy( "MiUser001").invitedAt( LocalDateTime.parse( "2600-05-01T10:30:00.000000" ) ), new InvitationDao().invitedBy( "MiUser003" ).invitedAt( LocalDateTime.parse( "2600-05-03T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "2600-05-10T10:30:00.000000" ) )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MiAssociation007", MiAssociation007 );

        final Supplier<AssociationDao> MiAssociation008 = () -> new AssociationDao()
                .id( "MiAssociation008" )
                .companyNumber( "MICOMP007" )
                .userId( "MiUser002" )
                .status( "removed" )
                .approvalRoute( "auth_code" )
                .removedAt( LocalDateTime.parse( "2000-05-01T10:30:00.000000" ) )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MiAssociation008", MiAssociation008 );

        final Supplier<AssociationDao> MiAssociation009 = () -> new AssociationDao()
                .id( "MiAssociation009" )
                .companyNumber( "MICOMP008" )
                .userEmail( "lechuck.monkey.island@inugami-example.com" )
                .status( "removed" )
                .approvalRoute( "auth_code" )
                .removedAt( LocalDateTime.parse( "2001-05-01T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation009", MiAssociation009 );

        final Supplier<AssociationDao> MiAssociation010 = () -> new AssociationDao()
                .id( "MiAssociation010" )
                .companyNumber( "MICOMP009" )
                .userId( "MiUser002" )
                .status( "removed" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "1992-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "1992-05-08T10:30:00.000000" ) )
                .removedAt( LocalDateTime.parse( "1992-05-04T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation010", MiAssociation010 );

        final Supplier<AssociationDao> MiAssociation011 = () -> new AssociationDao()
                .id( "MiAssociation011" )
                .companyNumber( "MICOMP010" )
                .userEmail( "lechuck.monkey.island@inugami-example.com" )
                .status( "removed" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "1992-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "1992-05-08T10:30:00.000000" ) )
                .removedAt( LocalDateTime.parse( "1992-05-05T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation011", MiAssociation011 );

        final Supplier<AssociationDao> MiAssociation012 = () -> new AssociationDao()
                .id( "MiAssociation012" )
                .companyNumber( "MICOMP001" )
                .userId( "MiUser003" )
                .status( "confirmed" )
                .approvalRoute( "auth_code" )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation012", MiAssociation012 );

        final Supplier<AssociationDao> MiAssociation013 = () -> new AssociationDao()
                .id( "MiAssociation013" )
                .companyNumber( "MICOMP001" )
                .userId( "MiUser004" )
                .status( "confirmed" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "1992-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "1992-05-08T10:30:00.000000" ) )
                .approvedAt( LocalDateTime.parse( "1992-05-06T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation013", MiAssociation013 );

        final Supplier<AssociationDao> MiAssociation014 = () -> new AssociationDao()
                .id( "MiAssociation014" )
                .companyNumber( "MICOMP001" )
                .userId( "MiUser005" )
                .status( "awaiting-approval" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "2020-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "2020-05-08T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation014", MiAssociation014 );

        final Supplier<AssociationDao> MiAssociation015 = () -> new AssociationDao()
                .id( "MiAssociation015" )
                .companyNumber( "MICOMP001" )
                .userEmail( "largo.lagrande.monkey.island@inugami-example.com" )
                .status( "awaiting-approval" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "2021-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "2021-05-08T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation015", MiAssociation015 );

        final Supplier<AssociationDao> MiAssociation016 = () -> new AssociationDao()
                .id( "MiAssociation016" )
                .companyNumber( "MICOMP001" )
                .userId( "MiUser007" )
                .status( "awaiting-approval" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "2500-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "2500-05-08T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation016", MiAssociation016 );

        final Supplier<AssociationDao> MiAssociation017 = () -> new AssociationDao()
                .id( "MiAssociation017" )
                .companyNumber( "MICOMP001" )
                .userEmail( "meathook.monkey.island@inugami-example.com" )
                .status( "awaiting-approval" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "2600-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "2600-05-08T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation017", MiAssociation017 );

        final Supplier<AssociationDao> MiAssociation018 = () -> new AssociationDao()
                .id( "MiAssociation018" )
                .companyNumber( "MICOMP001" )
                .userId( "MiUser009" )
                .status( "removed" )
                .approvalRoute( "auth_code" )
                .removedAt( LocalDateTime.parse(  "2000-05-01T10:30:00.000000" ) )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MiAssociation018", MiAssociation018 );

        final Supplier<AssociationDao> MiAssociation019 = () -> new AssociationDao()
                .id( "MiAssociation019" )
                .companyNumber( "MICOMP001" )
                .userEmail( "haggis.mcmutton.monkey.island@inugami-example.com" )
                .status( "removed" )
                .approvalRoute( "auth_code" )
                .removedAt( LocalDateTime.parse(  "2001-05-01T10:30:00.000000" ) )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MiAssociation019", MiAssociation019 );

        final Supplier<AssociationDao> MiAssociation020 = () -> new AssociationDao()
                .id( "MiAssociation020" )
                .companyNumber( "MICOMP001" )
                .userId( "MiUser011" )
                .status( "removed" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "1992-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "1992-05-08T10:30:00.000000" ) )
                .removedAt( LocalDateTime.parse(  "1992-05-04T10:30:00.000000" ) )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MiAssociation020", MiAssociation020 );

        final Supplier<AssociationDao> MiAssociation021 = () -> new AssociationDao()
                .id( "MiAssociation021" )
                .companyNumber( "MICOMP001" )
                .userEmail( "morgan.leflay.monkey.island@inugami-example.com" )
                .status( "removed" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "1992-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "1992-05-08T10:30:00.000000" ) )
                .removedAt( LocalDateTime.parse("1992-05-05T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation021", MiAssociation021 );

        final Supplier<AssociationDao> MiAssociation022 = () -> new AssociationDao()
                .id( "MiAssociation022" )
                .companyNumber( "MICOMP001" )
                .userId( "MiUser013" )
                .status( "removed" )
                .approvalRoute( "auth_code" )
                .removedAt( LocalDateTime.parse("2000-05-01T10:30:00.000000" ) )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MiAssociation022", MiAssociation022 );

        final Supplier<AssociationDao> MiAssociation023 = () -> new AssociationDao()
                .id( "MiAssociation023" )
                .companyNumber( "MICOMP001" )
                .userEmail( "three.headed.monkey.monkey.island@inugami-example.com" )
                .status( "removed" )
                .approvalRoute( "auth_code" )
                .removedAt( LocalDateTime.parse("2000-05-01T10:30:00.000000" ) )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MiAssociation023", MiAssociation023 );

        final Supplier<AssociationDao> MiAssociation024 = () -> new AssociationDao()
                .id( "MiAssociation024" )
                .companyNumber( "MICOMP001" )
                .userEmail( "apple.bob.monkey.island@inugami-example.com" )
                .status( "awaiting-approval" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser002" ).invitedAt( LocalDateTime.parse( "1992-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "1992-05-08T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation024", MiAssociation024 );

        final Supplier<AssociationDao> MiAssociation025 = () -> new AssociationDao()
                .id( "MiAssociation025" )
                .companyNumber( "MICOMP001" )
                .userId( "MiUser016" )
                .status( "removed" )
                .approvalRoute( "auth_code" )
                .removedAt( LocalDateTime.parse("2000-05-01T10:30:00.000000" ) )
                .etag( generateEtag() );
        associationDaoSuppliers.put( "MiAssociation025", MiAssociation025 );

        final Supplier<AssociationDao> MiAssociation026 = () -> new AssociationDao()
                .id( "MiAssociation026" )
                .companyNumber( "MICOMP001" )
                .userEmail( "lemonhead.monkey.island@inugami-example.com" )
                .status( "awaiting-approval" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser002" ).invitedAt( LocalDateTime.parse( "2992-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "2992-05-08T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation026", MiAssociation026 );

        final Supplier<AssociationDao> MiAssociation027 = () -> new AssociationDao()
                .id( "MiAssociation027" )
                .companyNumber( "MICOMP002" )
                .userEmail( "guybrush.threepwood.monkey.island@inugami-example.com" )
                .status( "awaiting-approval" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser002" ).invitedAt( LocalDateTime.parse( "2992-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "2992-05-08T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation027", MiAssociation027 );

        final Supplier<AssociationDao> MiAssociation028 = () -> new AssociationDao()
                .id( "MiAssociation028" )
                .companyNumber( "MICOMP003" )
                .userEmail( "guybrush.threepwood.monkey.island@inugami-example.com" )
                .status( "awaiting-approval" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser002" ).invitedAt( LocalDateTime.parse( "2992-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "2992-05-08T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation028", MiAssociation028 );

        final Supplier<AssociationDao> MiAssociation029 = () -> new AssociationDao()
                .id( "MiAssociation029" )
                .companyNumber( "MICOMP003" )
                .userId( "MiUser018" )
                .status( "confirmed" )
                .approvalRoute( "auth_code" )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation029", MiAssociation029 );

        final Supplier<AssociationDao> MiAssociation030 = () -> new AssociationDao()
                .id( "MiAssociation030" )
                .companyNumber( "MICOMP001" )
                .userId( "MiUser019" )
                .status( "confirmed" )
                .approvalRoute( "auth_code" )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation030", MiAssociation030 );

        final Supplier<AssociationDao> MiAssociation031 = () -> new AssociationDao()
                .id( "MiAssociation031" )
                .companyNumber( "MICOMP001" )
                .userEmail( "navigator.head.monkey.island@inugami-example.com" )
                .status( "awaiting-approval" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser002" ).invitedAt( LocalDateTime.parse( "2992-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "2992-05-08T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation031", MiAssociation031 );

        final Supplier<AssociationDao> MiAssociation032 = () -> new AssociationDao()
                .id( "MiAssociation032" )
                .companyNumber( "MICOMP001" )
                .userEmail( "carla.monkey.island@inugami-example.com" )
                .status( "migrated" )
                .approvalRoute( "migration" )
                .migratedAt( LocalDateTime.parse( "2025-03-01T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation032", MiAssociation032 );

        final Supplier<AssociationDao> MiAssociation033 = () -> new AssociationDao()
                .id( "MiAssociation033" )
                .companyNumber( "MICOMP011" )
                .userEmail( "lechuck.monkey.island@inugami-example.com" )
                .status( "migrated" )
                .approvalRoute( "migration" )
                .migratedAt( LocalDateTime.parse( "2025-03-01T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation033", MiAssociation033 );

        final Supplier<AssociationDao> MiAssociation034 = () -> new AssociationDao()
                .id( "MiAssociation034" )
                .companyNumber( "MICOMP001" )
                .userEmail( "mad.marty.monkey.island@inugami-example.com" )
                .status( "migrated" )
                .approvalRoute( "migration" )
                .migratedAt( LocalDateTime.parse( "2025-03-01T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation034", MiAssociation034 );

        final Supplier<AssociationDao> MiAssociation035 = () -> new AssociationDao()
                .id( "MiAssociation035" )
                .companyNumber( "MICOMP001" )
                .userEmail( "pegnose.pete.monkey.island@inugami-example.com" )
                .status( "migrated" )
                .approvalRoute( "migration" )
                .migratedAt( LocalDateTime.parse( "2025-03-01T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation035", MiAssociation035 );

        final Supplier<AssociationDao> MiAssociation036 = () -> new AssociationDao()
                .id( "MiAssociation036" )
                .companyNumber( "MICOMP001" )
                .userEmail( "captain.smirk.monkey.island@inugami-example.com" )
                .status( "migrated" )
                .approvalRoute( "migration" )
                .migratedAt( LocalDateTime.parse( "2025-03-01T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation036", MiAssociation036 );

        final Supplier<AssociationDao> MiAssociation037 = () -> new AssociationDao()
                .id( "MiAssociation037" )
                .companyNumber( "MICOMP001" )
                .userEmail( "cutthroat.bill.monkey.island@inugami-example.com" )
                .status( "migrated" )
                .approvalRoute( "migration" )
                .migratedAt( LocalDateTime.parse( "2025-03-01T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation037", MiAssociation037 );

        final Supplier<AssociationDao> MiAssociation038 = () -> new AssociationDao()
                .id( "MiAssociation038" )
                .companyNumber( "MICOMP001" )
                .userEmail( "rapp.scallion.monkey.island@inugami-example.com" )
                .status( "migrated" )
                .approvalRoute( "migration" )
                .migratedAt( LocalDateTime.parse( "2025-03-01T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation038", MiAssociation038 );

        final Supplier<AssociationDao> MiAssociation039 = () -> new AssociationDao()
                .id( "MiAssociation039" )
                .companyNumber( "MICOMP001" )
                .userEmail( "edward.van.helgen.monkey.island@inugami-example.com" )
                .status( "migrated" )
                .approvalRoute( "migration" )
                .migratedAt( LocalDateTime.parse( "2025-03-01T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation039", MiAssociation039 );

        final Supplier<AssociationDao> MiAssociation040 = () -> new AssociationDao()
                .id( "MiAssociation040" )
                .companyNumber( "MICOMP001" )
                .userEmail( "griswold.goodsoup.monkey.island@inugami-example.com" )
                .status( "migrated" )
                .approvalRoute( "migration" )
                .migratedAt( LocalDateTime.parse( "2025-03-01T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation040", MiAssociation040 );

        final Supplier<AssociationDao> MiAssociation041 = () -> new AssociationDao()
                .id( "MiAssociation041" )
                .companyNumber( "MICOMP001" )
                .userId( "MiUser026" )
                .status( "removed" )
                .approvalRoute( "migration" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "2025-03-03T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "2025-03-10T10:30:00.000000" ) )
                .approvedAt( LocalDateTime.parse( "2025-03-04T10:30:00.000000" ) )
                .removedAt( LocalDateTime.parse( "2025-03-05T10:30:00.000000" ) )
                .migratedAt( LocalDateTime.parse( "2025-03-01T10:30:00.000000") )
                .previousStates( List.of( new PreviousStatesDao().status( "migrated" ).changedBy( "MKUser001" ).changedAt( LocalDateTime.parse( "2025-03-03T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "2025-03-10T10:30:00.000000" ) )
                .approvedAt( LocalDateTime.parse( "2025-03-04T10:30:00.000000" ) )
                .removedAt( LocalDateTime.parse( "2025-03-05T10:30:00.000000" ) )
                .migratedAt( LocalDateTime.parse( "2025-03-01T10:30:00.000000" ) )
                .previousStates( List.of( new PreviousStatesDao().status( "migrated" ).changedBy( "MiUser001").changedAt( LocalDateTime.parse( "2025-03-02T10:30:00.000000" ) ),
                        new PreviousStatesDao().status( "removed" ).changedBy( "MiUser001" ).changedAt( LocalDateTime.parse("2025-03-03T10:30:00.000000" ) ),
                        new PreviousStatesDao().status( "awaiting-approval" ).changedBy( "MiUser026" ).changedAt( LocalDateTime.parse( "2025-03-04T10:30:00.000000" ) ),
                        new PreviousStatesDao().status( "confirmed" ).changedBy( "MiUser026" ).changedAt( LocalDateTime.parse( "2025-03-05T10:30:00.000000" ) ) ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation041", MiAssociation041 );

        final Supplier<AssociationDao> MiAssociation042 = () -> new AssociationDao()
                .id( "MiAssociation042" )
                .companyNumber( "MICOMP001" )
                .userId( "MiUser028" )
                .status( "confirmed" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "1992-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "1992-05-08T10:30:00.000000" ) )
                .approvedAt( LocalDateTime.parse( "1992-05-06T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation042", MiAssociation042 );

        final Supplier<AssociationDao> MiAssociation043 = () -> new AssociationDao()
                .id( "MiAssociation043" )
                .companyNumber( "MICOMP001" )
                .userId( "MiUser029" )
                .status( "awaiting-approval" )
                .approvalRoute( "invitation" )
                .invitations( List.of( new InvitationDao().invitedBy( "MiUser001" ).invitedAt( LocalDateTime.parse( "2500-05-01T10:30:00.000000" ) ) ) )
                .approvalExpiryAt( LocalDateTime.parse( "2500-05-08T10:30:00.000000" ) )
                .etag( generateEtag() );

        associationDaoSuppliers.put( "MiAssociation043", MiAssociation043 );
    }


    private void instantiateUserDtoSuppliers(){
        userDtoSuppliers.put( "000", () -> new User().userId( "000" ).email( "light.yagami@death.note" ) );
        userDtoSuppliers.put( "111", () -> new User().userId( "111" ).email( "bruce.wayne@gotham.city" ).displayName( "Batman" ) );
        userDtoSuppliers.put( "222", () -> new User().userId( "222" ).email( "the.joker@gotham.city" ) );
        userDtoSuppliers.put( "333", () -> new User().userId( "333" ).email( "harley.quinn@gotham.city" ).displayName( "Harleen Quinzel" ) );
        userDtoSuppliers.put( "444", () -> new User().userId( "444" ).email( "robin@gotham.city" ).displayName( "Boy Wonder" ) );
        userDtoSuppliers.put( "555", () -> new User().userId( "555" ).email( "barbara.gordon@gotham.city" ).displayName( "Batwoman" ) );
        userDtoSuppliers.put( "666", () -> new User().userId( "666" ).email( "homer.simpson@springfield.com" ) );
        userDtoSuppliers.put( "777", () -> new User().userId( "777" ).email( "marge.simpson@springfield.com" ) );
        userDtoSuppliers.put( "888", () -> new User().userId( "888" ).email( "bart.simpson@springfield.com" ) );
        userDtoSuppliers.put( "999", () -> new User().userId( "999" ).email( "lisa.simpson@springfield.com" ) );
        userDtoSuppliers.put( "1111", () -> new User().userId( "1111" ).email( "maggie.simpson@springfield.com" ) );
        userDtoSuppliers.put( "2222", () -> new User().userId( "2222" ).email( "crusty.the.clown@springfield.com" ) );
        userDtoSuppliers.put( "3333", () -> new User().userId( "3333" ).email( "itchy@springfield.com" ) );
        userDtoSuppliers.put( "4444", () -> new User().userId( "4444" ).email( "scratchy@springfield.com" ) );
        userDtoSuppliers.put( "5555", () -> new User().userId( "5555" ).email( "ross@friends.com" ) );
        userDtoSuppliers.put( "6666", () -> new User().userId( "6666" ).email( "rachel@friends.com" ) );
        userDtoSuppliers.put( "7777", () -> new User().userId( "7777" ).email( "chandler@friends.com" ) );
        userDtoSuppliers.put( "8888", () -> new User().userId( "8888" ).email( "mr.blobby@nightmare.com" ) );
        userDtoSuppliers.put( "9999", () -> new User().userId( "9999" ).email( "scrooge.mcduck@disney.land" ).displayName( "Scrooge McDuck" ) );
        userDtoSuppliers.put( "MKUser001", () -> new User().userId( "MKUser001" ).email( "mario@mushroom.kingdom" ).displayName( "Mario" ) );
        userDtoSuppliers.put( "MKUser002", () -> new User().userId( "MKUser002" ).email( "luigi@mushroom.kingdom" ).displayName( "Luigi" ) );
        userDtoSuppliers.put( "MKUser003", () -> new User().userId( "MKUser003" ).email( "peach@mushroom.kingdom" ).displayName( "Peach" ) );
        userDtoSuppliers.put( "MiUser001", () -> new User().userId( "MiUser001" ).email( "guybrush.threepwood.monkey.island@inugami-example.com" ).displayName( "Guybrush Threepwood" ) );
        userDtoSuppliers.put( "MiUser002", () -> new User().userId( "MiUser002" ).email( "lechuck.monkey.island@inugami-example.com" ).displayName( "LeChuck" ) );
        userDtoSuppliers.put( "MiUser003", () -> new User().userId( "MiUser003" ).email( "elaine.marley.monkey.island@inugami-example.com" ).displayName( "Elaine Marley" ) );
        userDtoSuppliers.put( "MiUser004", () -> new User().userId( "MiUser004" ).email( "stan.monkey.island@inugami-example.com" ).displayName( "Stan" ) );
        userDtoSuppliers.put( "MiUser005", () -> new User().userId( "MiUser005" ).email( "wally.monkey.island@inugami-example.com" ).displayName( "Wally" ) );
        userDtoSuppliers.put( "MiUser006", () -> new User().userId( "MiUser006" ).email( "largo.lagrande.monkey.island@inugami-example.com" ).displayName( "Largo LaGrande" ) );
        userDtoSuppliers.put( "MiUser007", () -> new User().userId( "MiUser007" ).email( "murray.monkey.island@inugami-example.com" ).displayName( "Murray" ) );
        userDtoSuppliers.put( "MiUser008", () -> new User().userId( "MiUser008" ).email( "meathook.monkey.island@inugami-example.com" ).displayName( "Meathook" ) );
        userDtoSuppliers.put( "MiUser009", () -> new User().userId( "MiUser009" ).email( "herman.toothrot.monkey.island@inugami-example.com" ).displayName( "Herman Toothrot" ) );
        userDtoSuppliers.put( "MiUser010", () -> new User().userId( "MiUser010" ).email( "haggis.mcmutton.monkey.island@inugami-example.com" ).displayName( "Haggis McMutton" ) );
        userDtoSuppliers.put( "MiUser011", () -> new User().userId( "MiUser011" ).email( "otis.monkey.island@inugami-example.com" ).displayName( "Otis" ) );
        userDtoSuppliers.put( "MiUser012", () -> new User().userId( "MiUser012" ).email( "morgan.leflay.monkey.island@inugami-example.com" ).displayName( "Morgan LeFlay" ) );
        userDtoSuppliers.put( "MiUser013", () -> new User().userId( "MiUser013" ).email( "voodoo.lady.monkey.island@inugami-example.com" ).displayName( "Voodoo Lady" ) );
        userDtoSuppliers.put( "MiUser014", () -> new User().userId( "MiUser014" ).email( "captain.dread.monkey.island@inugami-example.com" ).displayName( "Captain Dread" ) );
        userDtoSuppliers.put( "MiUser015", () -> new User().userId( "MiUser015" ).email( "three.headed.monkey.monkey.island@inugami-example.com" ).displayName( "Three Headed Monkey" ) );
        userDtoSuppliers.put( "MiUser016", () -> new User().userId( "MiUser016" ).email( "kate.capsize.monkey.island@inugami-example.com" ).displayName( "Kate Capsize" ) );
        userDtoSuppliers.put( "MiUser017", () -> new User().userId( "MiUser017" ).email( "mancomb.seepgood.monkey.island@inugami-example.com" ).displayName( "Mancomb Seepgood" ) );
        userDtoSuppliers.put( "MiUser018", () -> new User().userId( "MiUser018" ).email( "pineapplehead.monkey.island@inugami-example.com" ).displayName( "Pineapplehead" ) );
        userDtoSuppliers.put( "MiUser019", () -> new User().userId( "MiUser019" ).email( "bananahead.monkey.island@inugami-example.com" ).displayName( "Bananahead" ) );
        userDtoSuppliers.put( "MiUser020", () -> new User().userId( "MiUser020" ).email( "carla.monkey.island@inugami-example.com" ).displayName( "Sword Master" ) );
        userDtoSuppliers.put( "MiUser021", () -> new User().userId( "MiUser021" ).email( "mad.marty.monkey.island@inugami-example.com" ).displayName( "Mad Marty" ) );
        userDtoSuppliers.put( "MiUser022", () -> new User().userId( "MiUser022" ).email( "pegnose.pete.monkey.island@inugami-example.com" ).displayName( "Pegnose Pete" ) );
        userDtoSuppliers.put( "MiUser023", () -> new User().userId( "MiUser023" ).email( "captain.smirk.monkey.island@inugami-example.com" ).displayName( "Captain Smirk" ) );
        userDtoSuppliers.put( "MiUser024", () -> new User().userId( "MiUser024" ).email( "cutthroat.bill.monkey.island@inugami-example.com" ).displayName( "Cutthroat Bill" ) );
        userDtoSuppliers.put( "MiUser025", () -> new User().userId( "MiUser025" ).email( "rapp.scallion.monkey.island@inugami-example.com" ).displayName( "Rapp Scallion" ) );
        userDtoSuppliers.put( "MiUser026", () -> new User().userId( "MiUser026" ).email( "blondebeard.monkey.island@inugami-example.com" ).displayName( "Blondebeard" ) );
        userDtoSuppliers.put( "MiUser027", () -> new User().userId( "MiUser027" ).email( "ozzie.mandrill.monkey.island@inugami-example.com" ).displayName( "Ozzie Mandrill" ) );
        userDtoSuppliers.put( "MiUser028", () -> new User().userId( "MiUser028" ).email( "ignatius.cheese.monkey.island@inugami-example.com" ).displayName( "Ignatius Cheese" ) );
        userDtoSuppliers.put( "MiUser029", () -> new User().userId( "MiUser029" ).email( "miss.rivers.monkey.island@inugami-example.com" ).displayName( "Miss Rivers" ) );
    }

    private void instantiateCompanyDtoSuppliers(){
        companyDetailsDtoSuppliers.put( "111111", () -> new CompanyDetails().companyNumber( "111111" ).companyName( "Wayne Enterprises" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "333333", () -> new CompanyDetails().companyNumber( "333333" ).companyName( "Tesco" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "444444", () -> new CompanyDetails().companyNumber( "444444" ).companyName( "Sainsbury's" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "555555", () -> new CompanyDetails().companyNumber( "555555" ).companyName( "Morrison" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "666666", () -> new CompanyDetails().companyNumber( "666666" ).companyName( "Aldi" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "777777", () -> new CompanyDetails().companyNumber( "777777" ).companyName( "Lidl" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "888888", () -> new CompanyDetails().companyNumber( "888888" ).companyName( "McDonald's" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "999999", () -> new CompanyDetails().companyNumber( "999999" ).companyName( "Burger King" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "x111111", () -> new CompanyDetails().companyNumber( "x111111" ).companyName( "Pizza Hut" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "x222222", () -> new CompanyDetails().companyNumber( "x222222" ).companyName( "Dominos" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "x333333", () -> new CompanyDetails().companyNumber( "x333333" ).companyName( "Pizza Express" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "x444444", () -> new CompanyDetails().companyNumber( "x444444" ).companyName( "Nandos" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "x555555", () -> new CompanyDetails().companyNumber( "x555555" ).companyName( "Subway" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "x666666", () -> new CompanyDetails().companyNumber( "x666666" ).companyName( "Greggs" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "x777777", () -> new CompanyDetails().companyNumber( "x777777" ).companyName( "Facebook" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "x888888", () -> new CompanyDetails().companyNumber( "x888888" ).companyName( "Twitter" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "x999999", () -> new CompanyDetails().companyNumber( "x999999" ).companyName( "Instram" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "MKCOMP001", () -> new CompanyDetails().companyNumber( "MKCOMP001" ).companyName( "Mushroom Kingdom" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "MICOMP001", () -> new CompanyDetails().companyNumber( "MICOMP001" ).companyName( "Scumm Bar" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "MICOMP002", () -> new CompanyDetails().companyNumber( "MICOMP002" ).companyName( "Stan's Previously Owned Vessels" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "MICOMP003", () -> new CompanyDetails().companyNumber( "MICOMP003" ).companyName( "Stan's Previously Owned Coffins" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "MICOMP004", () -> new CompanyDetails().companyNumber( "MICOMP004" ).companyName( "The International House of Mojo" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "MICOMP005", () -> new CompanyDetails().companyNumber( "MICOMP005" ).companyName( "Wally's Maps-n-More" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "MICOMP006", () -> new CompanyDetails().companyNumber( "MICOMP006" ).companyName( "Blondebeard's Chicken Shoppe" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "MICOMP007", () -> new CompanyDetails().companyNumber( "MICOMP007" ).companyName( "The Goodsoup Hotel" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "MICOMP008", () -> new CompanyDetails().companyNumber( "MICOMP008" ).companyName( "The Barbery Coast" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "MICOMP009", () -> new CompanyDetails().companyNumber( "MICOMP009" ).companyName( "The Fantastic Flying Fettucini Brothers Circus" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "MICOMP010", () -> new CompanyDetails().companyNumber( "MICOMP010" ).companyName( "Woodtick Laundry" ).companyStatus( "active" ) );
        companyDetailsDtoSuppliers.put( "MICOMP011", () -> new CompanyDetails().companyNumber( "MICOMP011" ).companyName( "Sword Master's Dojo" ).companyStatus( "active" ) );
    }

    private TestDataManager(){
        instantiateAssociationDaoSuppliers();
        instantiateUserDtoSuppliers();
        instantiateCompanyDtoSuppliers();
    }

    public List<AssociationDao> fetchAssociationDaos( final String... ids  ){
        return Arrays.stream( ids )
                .map( associationDaoSuppliers::get )
                .map( Supplier::get )
                .collect( Collectors.toList() );
    }

    public List<User> fetchUserDtos( final String... ids  ){
        return Arrays.stream( ids )
                .map( userDtoSuppliers::get )
                .map( Supplier::get )
                .collect( Collectors.toList() );
    }

    public List<CompanyDetails> fetchCompanyDetailsDtos( final String... ids  ){
        return Arrays.stream( ids )
                .map( companyDetailsDtoSuppliers::get )
                .map( Supplier::get )
                .collect( Collectors.toList() );
    }

    public Association fetchAssociationDto( final String id, final User user ){
        final var associationDao = fetchAssociationDaos( id ).getFirst();
        final var companyDetails = fetchCompanyDetailsDtos( associationDao.getCompanyNumber() ).getFirst();

        final var associationDto = new Association();
        associationDto.setId( associationDao.getId() );
        associationDto.setUserId( user.getUserId() );
        associationDto.setUserEmail( user.getEmail() );
        associationDto.setDisplayName( Optional.ofNullable( user.getDisplayName() ).orElse( "Not provided" )  );
        associationDto.setCompanyNumber( associationDao.getCompanyNumber() );
        associationDto.setCompanyName( companyDetails.getCompanyName() );
        associationDto.setCompanyStatus( companyDetails.getCompanyStatus() );
        associationDto.setStatus( StatusEnum.fromValue( associationDao.getStatus() ) );
        associationDto.setApprovalRoute( ApprovalRouteEnum.fromValue( associationDao.getApprovalRoute() ) );
        associationDto.setCreatedAt( localDateTimeToOffsetDateTime( associationDao.getCreatedAt() ) );
        associationDto.setApprovedAt( localDateTimeToOffsetDateTime( associationDao.getApprovedAt() ) );
        associationDto.setRemovedAt( localDateTimeToOffsetDateTime( associationDao.getRemovedAt() ) );
        associationDto.setApprovalExpiryAt( Optional.ofNullable( associationDao.getApprovalExpiryAt() ).map( LocalDateTime::toString ).orElse( null ) );
        associationDto.setEtag( associationDao.getEtag() );
        associationDto.setKind( "association" );
        associationDto.setLinks( new AssociationLinks().self( String.format( "/associations/%s", associationDao.getId() ) ) );

        return associationDto;
    }

    public List<Invitation> fetchInvitations( final String associationId ){
        return Stream.of( fetchAssociationDaos( associationId ) )
                .map( List::getFirst )
                .map( AssociationDao::getInvitations )
                .flatMap( List::stream )
                .map( invitationDao -> {
                    boolean isActive = LocalDateTime.now().isBefore(invitationDao.getExpiredAt());
                    final var invitationDto = new Invitation();
                    invitationDto.setAssociationId( associationId );
                    invitationDto.setInvitedBy( invitationDao.getInvitedBy() );
                    invitationDto.setInvitedAt( invitationDao.getInvitedAt().toString() );
                    invitationDto.setIsActive( isActive );
                    return invitationDto;
                })
                .collect(Collectors.toList());
    }

    public List<PreviousState> fetchPreviousStates( final String associationId ){
        return Stream.of( fetchAssociationDaos( associationId ) )
                .map( List::getFirst )
                .map( AssociationDao::getPreviousStates )
                .flatMap( List::stream )
                .map( previousStatesDao -> new PreviousState()
                            .status( PreviousState.StatusEnum.fromValue( previousStatesDao.getStatus() ) )
                            .changedBy( previousStatesDao.getChangedBy() )
                            .changedAt( previousStatesDao.getChangedAt().toString() ) )
                .collect( Collectors.toList() );
    }

}
