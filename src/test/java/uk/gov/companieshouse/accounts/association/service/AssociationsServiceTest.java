package uk.gov.companieshouse.accounts.association.service;

import java.util.ArrayList;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Update;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.mapper.AssociationMapper;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListCompanyMapper;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListUserMapper;
import uk.gov.companieshouse.accounts.association.mapper.InvitationsMapper;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;

import java.util.Collections;
import java.util.List;
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
    private AssociationsService associationsService;

    @Mock
    private AssociationsRepository associationsRepository;

    @Mock
    private InvitationsMapper invitationMapper;

    @Mock
    private AssociationsListCompanyMapper associationsListCompanyMapper;

    @Mock
    private AssociationsListUserMapper associationsListUserMapper;

    @Mock
    private AssociationMapper associationMapper;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final ComparisonUtils comparisonUtils = new ComparisonUtils();

    @BeforeEach
    public void setup() {
        associationsService = new AssociationsService( associationsRepository, associationsListUserMapper, associationsListCompanyMapper, associationMapper, invitationMapper );
    }

    @Test
    void fetchAssociationsForUserReturnEmptyItemsWhenNoAssociationFound() {
        final var user = testDataManager.fetchUserDtos( "111" ).getFirst();
        when(associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("111","bruce.wayne@gotham.city", List.of( "confirmed" ), "", PageRequest.of(0, 15))).thenReturn(Page.empty());
        associationsService.fetchAssociationsForUserStatusAndCompany(user, List.of( "confirmed" ), 0, 15, "");
        verify(associationsListUserMapper).daoToDto(Page.empty(), user);
    }

    @Test
    void fetchAssociationsForUserUsesStatusConfirmedAsDefaultWhenStatusNotProvided() throws ApiErrorResponseException, URIValidationException {
        final var user = testDataManager.fetchUserDtos( "111" ).getFirst();
        associationsService.fetchAssociationsForUserStatusAndCompany(user, null, 0, 15, "");
        verify(associationsRepository).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("111", "bruce.wayne@gotham.city", Collections.singletonList("confirmed"), "", PageRequest.of(0, 15));
        verify(associationsListUserMapper).daoToDto(null, user);

    }

    @Test
    void fetchAssociatedUsersWithNullInputsReturnsNull() {
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        Assertions.assertNull(associationsService.fetchAssociatedUsers(null, companyDetails, true, 15, 0));
        Assertions.assertNull(associationsService.fetchAssociatedUsers("111111", null, true, 15, 0));
    }

    @Test
    void fetchAssociatedUsersWithIncludeRemovedTrueDoesNotApplyFilter() {
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" );
        final var pageRequest = PageRequest.of(0, 20);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        Mockito.doReturn(page).when(associationsRepository).fetchAssociatedUsers(any(), any(), any(), any());

        associationsService.fetchAssociatedUsers("111111", companyDetails, true, 20, 0);

        Mockito.verify(associationsListCompanyMapper).daoToDto( eq( page ), eq(companyDetails));
    }

    @Test
    void fetchAssociatedUsersWithIncludeRemovedFalseAppliesFilter() {
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" );
        final var pageRequest = PageRequest.of(0, 20);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        Mockito.doReturn(page).when(associationsRepository).fetchAssociatedUsers(any(), any(), any(), any());

        associationsService.fetchAssociatedUsers("111111", companyDetails, false, 20, 0);

        Mockito.verify(associationsListCompanyMapper).daoToDto(eq( page ), eq(companyDetails));
    }

    @Test
    void fetchAssociatedUsersAppliesPaginationCorrectly() {
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "16" );
        final var pageRequest = PageRequest.of(1, 15);
        final var page = new PageImpl<>(content, pageRequest, 16);

        Mockito.doReturn(page).when(associationsRepository).fetchAssociatedUsers(any(), any(), any(), any());

        associationsService.fetchAssociatedUsers("111111", companyDetails, true, 15, 1);

        Mockito.verify(associationsListCompanyMapper).daoToDto(eq( page ), eq(companyDetails));
    }

    @Test
    void getAssociationByIdReturnsAssociationDtoWhenAssociationFound() {
        final var associationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        Mockito.when(associationsRepository.findById("1")).thenReturn(Optional.of(associationDao));
        associationsService.findAssociationById("1");
        Mockito.verify(associationMapper).daoToDto(associationDao);

    }

    @Test
    void getAssociationByIdReturnsEmptyWhenAssociationNotFound() {
        Mockito.when(associationsRepository.findById("1111")).thenReturn(Optional.empty());
        assertTrue(associationsService.findAssociationById("1111").isEmpty());
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNullInputsThrowsNullPointerException(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = List.of( StatusEnum.CONFIRMED.getValue() );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.fetchAssociationsForUserStatusAndCompany( null, status, 0, 15, "333333" ) );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.fetchAssociationsForUserStatusAndCompany( user, status, null, 15, "333333" ) );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, null, "333333" ) );
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithInvalidPageIndexOrItemsPerPageThrowsIllegalArgumentException() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = List.of( StatusEnum.CONFIRMED.getValue() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> associationsService.fetchAssociationsForUserStatusAndCompany( user, status, -1, 15, "333333" ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, 0, "333333" ) );
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyPaginatesCorrectly() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = List.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue() );
        final var content = testDataManager.fetchAssociationDaos( "33" );
        final var pageRequest = PageRequest.of(1, 15);
        final var page = new PageImpl<>(content, pageRequest, 16 );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "9999" ), eq ("scrooge.mcduck@disney.land"), eq( status ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 1, 15, null );

        Mockito.verify(associationsListUserMapper).daoToDto(eq( page ), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersByCompanyNumber(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = List.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue() );
        final var content = testDataManager.fetchAssociationDaos( "18", "27" );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "9999" ), eq("scrooge.mcduck@disney.land"), eq( status ), eq("333333"), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, 15, "333333" );

        Mockito.verify(associationsListUserMapper).daoToDto(eq(page), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersBasedOnStatus(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = List.of( StatusEnum.REMOVED.getValue() );
        final var content = testDataManager.fetchAssociationDaos( "31", "32", "33" );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "9999" ), eq ("scrooge.mcduck@disney.land"),  eq( status ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, 15, null );

        Mockito.verify(associationsListUserMapper).daoToDto(eq(page), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersBasedOnAwaitingApprovalStatus(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = List.of( StatusEnum.AWAITING_APPROVAL.getValue() );
        final var content = testDataManager.fetchAssociationDaos( "25", "26", "27" );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "9999" ), eq ("scrooge.mcduck@disney.land"), eq( status ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, 15, null );

        Mockito.verify(associationsListUserMapper).daoToDto(eq(page), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNullStatusDefaultsToConfirmedStatus(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22" );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "9999" ), eq("scrooge.mcduck@disney.land"), eq( List.of( StatusEnum.CONFIRMED.getValue() ) ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, null, 0, 15, null );

        Mockito.verify(associationsListUserMapper).daoToDto(eq(page), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithEmptyStatusDefaultsToConfirmedStatus(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22" );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "9999" ),  eq ("scrooge.mcduck@disney.land"),eq( List.of( StatusEnum.CONFIRMED.getValue() ) ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, Collections.emptyList(), 0, 15, null );

        Mockito.verify(associationsListUserMapper).daoToDto(eq(page), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithInvalidStatusReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = new ArrayList<AssociationDao>();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "9999" ),  eq ("scrooge.mcduck@disney.land"),eq( List.of( "complicated" ) ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, List.of( "complicated" ), 0, 15, null );

        Mockito.verify(associationsListUserMapper).daoToDto(eq(page), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNonexistentOrInvalidCompanyNumberReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = new ArrayList<AssociationDao>();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "9999" ),  eq ("scrooge.mcduck@disney.land"), eq( List.of( StatusEnum.CONFIRMED.getValue()) ), eq("$$$$$$"), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, Collections.emptyList(), 0, 15, "$$$$$$" );

        Mockito.verify(associationsListUserMapper).daoToDto(eq(page), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNonexistentUserIdReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = new ArrayList<AssociationDao>();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "9999" ),  eq ("scrooge.mcduck@disney.land"), eq( List.of( StatusEnum.CONFIRMED.getValue()) ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, Collections.emptyList(), 0, 15, null );
        Mockito.verify(associationsListUserMapper).daoToDto(eq(page), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNonexistentUserEmailReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = new ArrayList<AssociationDao>();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( eq( "9999" ),  eq ("scrooge.mcduck@disney.land"), eq( List.of( StatusEnum.CONFIRMED.getValue()) ), eq(""), eq(pageRequest) );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, Collections.emptyList(), 0, 15, null );

        Mockito.verify(associationsListUserMapper).daoToDto(eq(page), eq(user));
    }

    private static Stream<Arguments> provideInvalidCompanyNumberOrUserId() {
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
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociation( null, "111", "bruce.wayne@gotham.city", ApprovalRouteEnum.AUTH_CODE ,"222") );
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociation( "111111", null, null, ApprovalRouteEnum.AUTH_CODE ,"222") );
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociation( "111111", "111", "bruce.wayne@gotham.city", null ,"222") );
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociation( "111111", "111", "bruce.wayne@gotham.city", ApprovalRouteEnum.INVITATION ,null) );
    }

    @Test
    void createAssociationThatAlreadyExistsThrowsDuplicateKeyException(){
        associationsService.createAssociation( "111111", "111", null, ApprovalRouteEnum.INVITATION, "222" );
        associationsService.createAssociation( "111111", null, "bruce.wayne@gotham.city", ApprovalRouteEnum.INVITATION, "222" );

        Mockito.doThrow( new DuplicateKeyException( "Association already exists" ) ).when( associationsRepository ).save( any( AssociationDao.class ) );

        Assertions.assertThrows( DuplicateKeyException.class, () -> associationsService.createAssociation( "111111", "111", null, ApprovalRouteEnum.INVITATION, "222" ) );
        Assertions.assertThrows( DuplicateKeyException.class, () -> associationsService.createAssociation( "111111", null, "bruce.wayne@gotham.city", ApprovalRouteEnum.INVITATION, "222" ) );
    }

    @Test
    void createAssociationWithUserIdAndAuthCodeSuccessfullyCreatesOrUpdateAssociation(){
        final var expectedAssociationData = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        expectedAssociationData.setUserEmail( null );
        associationsService.createAssociation( "111111", "111", null, ApprovalRouteEnum.AUTH_CODE, null);
        Mockito.verify( associationsRepository ).save( argThat( comparisonUtils.compare( expectedAssociationData, List.of( "companyNumber", "userId", "userEmail", "approvalRoute", "status" ), List.of( "etag" ), Map.of() ) ) );
    }

    @Test
    void createAssociationWithUserIdAndInvitationSuccessfullyCreatesOrUpdateAssociation(){
        final var expectedAssociationData = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        expectedAssociationData.setUserEmail( null );
        expectedAssociationData.setApprovalRoute( ApprovalRouteEnum.INVITATION.getValue() );
        expectedAssociationData.setStatus( StatusEnum.AWAITING_APPROVAL.getValue() );
        associationsService.createAssociation( "111111", "111", null, ApprovalRouteEnum.INVITATION, "666");
        Mockito.verify( associationsRepository ).save( argThat( comparisonUtils.compare( expectedAssociationData, List.of( "companyNumber", "userId", "userEmail", "approvalRoute", "status" ), List.of( "etag", "approvalExpiryAt", "invitations" ), Map.of() ) ) );
    }

    @Test
    void createAssociationWithUserEmailAndAuthCodeSuccessfullyCreatesOrUpdateAssociation(){
        final var expectedAssociationData = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        expectedAssociationData.setUserId( null );
        associationsService.createAssociation( "111111", null, "bruce.wayne@gotham.city", ApprovalRouteEnum.AUTH_CODE, null);
        Mockito.verify( associationsRepository ).save( argThat( comparisonUtils.compare( expectedAssociationData, List.of( "companyNumber", "userId", "userEmail", "approvalRoute", "status" ), List.of( "etag", "approvalExpiryAt", "invitations" ), Map.of() ) ) );
    }

    @Test
    void createAssociationWithUserEmailAndInvitationSuccessfullyCreatesOrUpdateAssociation(){
        final var expectedAssociationData = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        expectedAssociationData.setUserId( null );
        expectedAssociationData.setApprovalRoute( ApprovalRouteEnum.INVITATION.getValue() );
        expectedAssociationData.setStatus( StatusEnum.AWAITING_APPROVAL.getValue() );
        associationsService.createAssociation( "111111", null, "bruce.wayne@gotham.city", ApprovalRouteEnum.INVITATION, "222");
        Mockito.verify( associationsRepository ).save( argThat( comparisonUtils.compare( expectedAssociationData, List.of( "companyNumber", "userId", "userEmail", "approvalRoute", "status" ), List.of( "etag", "approvalExpiryAt", "invitations" ), Map.of() ) ) );
    }

    @Test
    void sendNewInvitationWithNullInputsThrowsNullPointerException(){
        final var association = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.sendNewInvitation( null, association ) );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.sendNewInvitation( "111", null ) );
    }

    @Test
    void sendNewInvitationWithNonexistentAssociationCreatesNewAssociation(){
        final var association = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        final var expectedAssociationData = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        expectedAssociationData.setStatus( StatusEnum.AWAITING_APPROVAL.getValue() );
        associationsService.sendNewInvitation( "111", association );
        Mockito.verify( associationsRepository ).save( argThat( comparisonUtils.compare( expectedAssociationData, List.of( "companyNumber", "userId", "userEmail", "approvalRoute", "status" ), List.of( "etag", "approvalExpiryAt", "invitations" ), Map.of() ) ) );
    }

    @Test
    void sendNewInvitationWithExistingAssociationCreatesNewInvitation(){
        final var association = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        final var expectedAssociationData = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        expectedAssociationData.setStatus( StatusEnum.AWAITING_APPROVAL.getValue() );
        associationsService.sendNewInvitation( "222", association );
        Mockito.verify( associationsRepository ).save( argThat( comparisonUtils.compare( expectedAssociationData, List.of( "companyNumber", "userId", "userEmail", "approvalRoute", "status" ), List.of( "etag", "approvalExpiryAt", "invitations" ), Map.of() ) ) );
    }

    @Test
    void updateAssociationStatusWithMalformedOrNonexistentAssociationIdThrowsInternalServerError(){
        Mockito.doReturn( 0 ).when( associationsRepository ).updateAssociation( any(), any() );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> associationsService.updateAssociation( "$$$", new Update()) );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> associationsService.updateAssociation( "9191", new Update()) );
    }

    @Test
    void updateAssociationStatusWithNullAssociationIdOrUserIdOrNullStatusThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.updateAssociation( null, null) );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.updateAssociation( "1", null) );
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
        final var association = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        Mockito.doReturn( Optional.of( association ) ).when( associationsRepository ).fetchAssociationForCompanyNumberAndUserId( anyString(), anyString() );
        assertEquals( "1", associationsService.fetchAssociationForCompanyNumberAndUserId( "111111", "111" ).get().getId() );
    }

    @Test
    void fetchInvitationsWithNullInvitationsListReturnsEmptyList() {
        final var associationDao = testDataManager.fetchAssociationDaos( "18" ).getFirst();
        final var invitations = associationsService.fetchInvitations(associationDao, 0, 1);

        assertTrue(invitations.getItems().isEmpty());
        assertEquals(1, invitations.getItemsPerPage());
        assertEquals(0, invitations.getPageNumber());
        assertEquals(0, invitations.getTotalResults());
        assertEquals(0, invitations.getTotalPages());
        assertEquals("/associations/18/invitations?page_index=0&items_per_page=1", invitations.getLinks().getSelf());
        assertTrue(invitations.getLinks().getNext().isEmpty());
    }

    private static Stream<String> userIdsProvider() {
        return Stream.of(null, "$$$", "9191");
    }

    @ParameterizedTest
    @MethodSource("userIdsProvider")
    void fetchActiveInvitationsWithNullOrMalformedOrNonexistentUserIdReturnsEmptyList(String userId) {
        final var invitations = associationsService.fetchActiveInvitations(new User().userId(userId), 0, 1);

        assertTrue(invitations.getItems().isEmpty());
        assertEquals(1, invitations.getItemsPerPage());
        assertEquals(0, invitations.getPageNumber());
        assertEquals(0, invitations.getTotalResults());
        assertEquals(0, invitations.getTotalPages());
        assertEquals("/associations/invitations?page_index=0&items_per_page=1", invitations.getLinks().getSelf());
        assertTrue(invitations.getLinks().getNext().isEmpty());
    }

}