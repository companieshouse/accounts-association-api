package uk.gov.companieshouse.accounts.association.service;

import java.time.LocalDateTime;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Update;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListMappers;
import uk.gov.companieshouse.accounts.association.mapper.InvitationsMapper;
import uk.gov.companieshouse.accounts.association.mapper.PreviousStatesCollectionMappers;
import uk.gov.companieshouse.accounts.association.mapper.PreviousStatesMapperImpl;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.PreviousStatesDao;
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
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.AWAITING_APPROVAL;

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
    private AssociationsListMappers associationsListMappers;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final ComparisonUtils comparisonUtils = new ComparisonUtils();

    @BeforeEach
    public void setup() {
        final var previousStatesCollectionMappers = new PreviousStatesCollectionMappers( new PreviousStatesMapperImpl() );
        associationsService = new AssociationsService( associationsRepository, invitationMapper, associationsListMappers, previousStatesCollectionMappers );
    }

    @Test
    void fetchAssociationsForUserReturnEmptyItemsWhenNoAssociationFound() {
        final var user = testDataManager.fetchUserDtos( "111" ).getFirst();
        when(associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("111","bruce.wayne@gotham.city", List.of( "confirmed" ), "", PageRequest.of(0, 15))).thenReturn(Page.empty());
        associationsService.fetchAssociationsForUserStatusAndCompany(user, List.of( "confirmed" ), 0, 15, "");
        verify( associationsListMappers ).daoToDto( Page.empty(), user, null );
    }

    @Test
    void fetchAssociationsForUserUsesStatusConfirmedAsDefaultWhenStatusNotProvided() throws ApiErrorResponseException, URIValidationException {
        final var user = testDataManager.fetchUserDtos( "111" ).getFirst();
        associationsService.fetchAssociationsForUserStatusAndCompany(user, null, 0, 15, "");
        verify(associationsRepository).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike("111", "bruce.wayne@gotham.city", Collections.singletonList("confirmed"), "", PageRequest.of(0, 15));
        verify( associationsListMappers ).daoToDto( (Page<AssociationDao>) null, user, null);

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

        Mockito.verify( associationsListMappers ).daoToDto( eq( page ),isNull(), eq(companyDetails));
    }

    @Test
    void fetchAssociatedUsersWithIncludeRemovedFalseAppliesFilter() {
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" );
        final var pageRequest = PageRequest.of(0, 20);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        Mockito.doReturn(page).when(associationsRepository).fetchAssociatedUsers(any(), any(), any(), any());

        associationsService.fetchAssociatedUsers("111111", companyDetails, false, 20, 0);

        Mockito.verify( associationsListMappers ).daoToDto(eq( page ), isNull(), eq(companyDetails));
    }

    @Test
    void fetchAssociatedUsersAppliesPaginationCorrectly() {
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "16" );
        final var pageRequest = PageRequest.of(1, 15);
        final var page = new PageImpl<>(content, pageRequest, 16);

        Mockito.doReturn(page).when(associationsRepository).fetchAssociatedUsers(any(), any(), any(), any());

        associationsService.fetchAssociatedUsers("111111", companyDetails, true, 15, 1);

        Mockito.verify( associationsListMappers ).daoToDto(eq( page ), isNull(), eq(companyDetails));
    }

    @Test
    void fetchAssociatedUsersCanFetchMigratedAssociations(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "MKCOMP001" ).getFirst();

        final var content = testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002", "MKAssociation003" );
        final var pageRequest = PageRequest.of( 0, 15 );
        final var page = new PageImpl<>( content, pageRequest, 3 );

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociatedUsers( any(), any(), any(), any() );

        associationsService.fetchAssociatedUsers( "MKCOMP001", companyDetails, true, 15, 0 );

        Mockito.verify( associationsListMappers ).daoToDto( eq( page ), isNull(), eq( companyDetails ) );
    }

    @Test
    void getAssociationByIdReturnsAssociationDtoWhenAssociationFound() {
        final var associationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        Mockito.when(associationsRepository.findById("1")).thenReturn(Optional.of(associationDao));
        associationsService.findAssociationById("1");
        Mockito.verify( associationsListMappers ).daoToDto(associationDao, null, null);

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

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( "9999", "scrooge.mcduck@disney.land", status,"", pageRequest );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 1, 15, null );

        Mockito.verify( associationsListMappers ).daoToDto(eq( page ), eq(user), isNull());
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersByCompanyNumber(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = List.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue() );
        final var content = testDataManager.fetchAssociationDaos( "18", "27" );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( "9999","scrooge.mcduck@disney.land", status, "333333", pageRequest );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, 15, "333333" );

        Mockito.verify( associationsListMappers ).daoToDto(eq(page), eq(user), isNull());
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersBasedOnStatus(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = List.of( StatusEnum.REMOVED.getValue() );
        final var content = testDataManager.fetchAssociationDaos( "31", "32", "33" );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( "9999", "scrooge.mcduck@disney.land",  status, "", pageRequest );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, 15, null );

        Mockito.verify( associationsListMappers ).daoToDto( eq(page), eq(user), isNull() );
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersBasedOnAwaitingApprovalStatus(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = List.of( StatusEnum.AWAITING_APPROVAL.getValue() );
        final var content = testDataManager.fetchAssociationDaos( "25", "26", "27" );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( "9999", "scrooge.mcduck@disney.land", status, "", pageRequest );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, 15, null );

        Mockito.verify( associationsListMappers ).daoToDto( eq(page), eq(user), isNull() );
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNullStatusDefaultsToConfirmedStatus(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22" );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( "9999","scrooge.mcduck@disney.land", List.of( StatusEnum.CONFIRMED.getValue() ), "", pageRequest );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, null, 0, 15, null );

        Mockito.verify( associationsListMappers ).daoToDto(eq(page), eq(user), isNull());
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithEmptyStatusDefaultsToConfirmedStatus(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22" );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( "9999", "scrooge.mcduck@disney.land", List.of( StatusEnum.CONFIRMED.getValue() ),"", pageRequest);

        associationsService.fetchAssociationsForUserStatusAndCompany( user, Collections.emptyList(), 0, 15, null );

        Mockito.verify( associationsListMappers ).daoToDto(eq(page), eq(user), isNull());
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithInvalidStatusReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = new ArrayList<AssociationDao>();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( "9999", "scrooge.mcduck@disney.land", List.of( "complicated" ),"", pageRequest );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, List.of( "complicated" ), 0, 15, null );

        Mockito.verify( associationsListMappers ).daoToDto(eq(page), eq(user), isNull());
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNonexistentOrInvalidCompanyNumberReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = new ArrayList<AssociationDao>();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( "9999",  "scrooge.mcduck@disney.land", List.of( StatusEnum.CONFIRMED.getValue()),"$$$$$$",pageRequest );

        associationsService.fetchAssociationsForUserStatusAndCompany( user, Collections.emptyList(), 0, 15, "$$$$$$" );

        Mockito.verify( associationsListMappers ).daoToDto(eq(page), eq(user), isNull());
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNonexistentUserIdReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = new ArrayList<AssociationDao>();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike( "9999" , "scrooge.mcduck@disney.land", List.of( StatusEnum.CONFIRMED.getValue() ),"",pageRequest);

        associationsService.fetchAssociationsForUserStatusAndCompany( user, Collections.emptyList(), 0, 15, null );
        Mockito.verify( associationsListMappers ).daoToDto(eq(page), eq(user), isNull());
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
        expectedAssociationData.setPreviousStates( List.of( new PreviousStatesDao().status( StatusEnum.CONFIRMED.getValue() ).changedBy( "111" ).changedAt( LocalDateTime.now() ) ) );
        Mockito.doReturn( expectedAssociationData ).when( associationsRepository ).save( any( AssociationDao.class ) );
        final var updatedAssociation = associationsService.sendNewInvitation( "111", association );
        Assertions.assertEquals( 1, updatedAssociation.getPreviousStates().size() );
        Assertions.assertEquals( StatusEnum.CONFIRMED.getValue(), updatedAssociation.getPreviousStates().getFirst().getStatus() );
        Assertions.assertEquals( "111", updatedAssociation.getPreviousStates().getFirst().getChangedBy() );
        Assertions.assertNotNull( updatedAssociation.getPreviousStates().getFirst().getChangedAt() );
        Mockito.verify( associationsRepository ).save( argThat( comparisonUtils.compare( expectedAssociationData, List.of( "companyNumber", "userId", "userEmail", "approvalRoute", "status" ), List.of( "etag", "approvalExpiryAt", "invitations" ), Map.of() ) ) );
    }

    @Test
    void sendNewInvitationWithExistingAssociationCreatesNewInvitation(){
        final var association = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        final var expectedAssociationData = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        expectedAssociationData.setStatus( StatusEnum.AWAITING_APPROVAL.getValue() );
        expectedAssociationData.setPreviousStates( List.of( new PreviousStatesDao().status( StatusEnum.CONFIRMED.getValue() ).changedBy( "222" ).changedAt( LocalDateTime.now() ) ) );
        Mockito.doReturn( expectedAssociationData ).when( associationsRepository ).save( any( AssociationDao.class ) );
        final var updatedAssociation = associationsService.sendNewInvitation( "222", association );
        Assertions.assertEquals( 1, updatedAssociation.getPreviousStates().size() );
        Assertions.assertEquals( StatusEnum.CONFIRMED.getValue(), updatedAssociation.getPreviousStates().getFirst().getStatus() );
        Assertions.assertEquals( "222", updatedAssociation.getPreviousStates().getFirst().getChangedBy() );
        Assertions.assertNotNull( updatedAssociation.getPreviousStates().getFirst().getChangedAt() );
        Mockito.verify( associationsRepository ).save( argThat( comparisonUtils.compare( expectedAssociationData, List.of( "companyNumber", "userId", "userEmail", "approvalRoute", "status" ), List.of( "etag", "approvalExpiryAt", "invitations" ), Map.of() ) ) );
    }

    @Test
    void updateAssociationStatusWithMalformedOrNonexistentAssociationIdThrowsInternalServerError(){
        final var update = new Update();
        Mockito.doReturn( 0 ).when( associationsRepository ).updateAssociation( any(), any() );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> associationsService.updateAssociation( "$$$", update) );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> associationsService.updateAssociation( "9191", update) );
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

    @Test
    void fetchAssociatedUsersRetrieveUsersAssociatedWithCompany(){
        final var content = testDataManager.fetchAssociationDaos( "1" );
        final var pageRequest = PageRequest.of(0, 20);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociatedUsers( eq( "111111" ), any(), any(), any() );

        Assertions.assertEquals( "111", associationsService.fetchAssociatedUsers( "111111" ).getFirst() );
    }

    @Test
    void fetchPreviousStatesAppliedToAssociationWithPreviousStatesRetrievesAndMaps(){
        final var association = testDataManager.fetchAssociationDaos( "MKAssociation003" ).getFirst();
        final var now = LocalDateTime.now();

        Mockito.doReturn( Optional.of( association ) ).when( associationsRepository ).findById( "MKAssociation003" );

        final var previousStatesList = associationsService.fetchPreviousStates( "MKAssociation003", 1, 1 ).get();
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();

        Assertions.assertEquals( 1, previousStatesList.getItemsPerPage() );
        Assertions.assertEquals( 1, previousStatesList.getPageNumber() );
        Assertions.assertEquals( 4, previousStatesList.getTotalResults() );
        Assertions.assertEquals( 4, previousStatesList.getTotalPages() );
        Assertions.assertEquals( "/associations/MKAssociation003/previous-states?page_index=1&items_per_page=1", links.getSelf() );
        Assertions.assertEquals( "/associations/MKAssociation003/previous-states?page_index=2&items_per_page=1", links.getNext() );
        Assertions.assertEquals( 1, items.size() );
        Assertions.assertEquals( AWAITING_APPROVAL, items.getFirst().getStatus() );
        Assertions.assertEquals( "MKUser003", items.getFirst().getChangedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( now.minusDays( 7L ) ), reduceTimestampResolution( items.getFirst().getChangedAt() ) );
    }

    @Test
    void fetchPreviousStatesAppliedToAssociationWithoutPreviousStatesRetrievesAndMaps(){
        final var association = testDataManager.fetchAssociationDaos( "MKAssociation001" ).getFirst();

        Mockito.doReturn( Optional.of( association ) ).when( associationsRepository ).findById( "MKAssociation001" );

        final var previousStatesList = associationsService.fetchPreviousStates( "MKAssociation001", 0, 15 ).get();
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();

        Assertions.assertEquals( 15, previousStatesList.getItemsPerPage() );
        Assertions.assertEquals( 0, previousStatesList.getPageNumber() );
        Assertions.assertEquals( 0, previousStatesList.getTotalResults() );
        Assertions.assertEquals( 0, previousStatesList.getTotalPages() );
        Assertions.assertEquals( "/associations/MKAssociation001/previous-states?page_index=0&items_per_page=15", links.getSelf() );
        Assertions.assertEquals( "", links.getNext() );
        Assertions.assertTrue( items.isEmpty() );
    }

    @Test
    void fetchPreviousStatesAppliedToMalformedOrNonexistentAssociationReturnsEmptyOptional(){
        Assertions.assertTrue( associationsService.fetchPreviousStates( "$$$", 0, 15 ).isEmpty() );
        Assertions.assertTrue( associationsService.fetchPreviousStates( "404MKAssociation", 0, 15 ).isEmpty() );
    }

    @Test
    void fetchPreviousStatesThrowsIllegalArgumentExceptionWhenAssociationIdIsNull(){
        Mockito.doThrow( new IllegalArgumentException( "associationId cannot be null" ) ).when( associationsRepository ).findById( null );
        Assertions.assertThrows( IllegalArgumentException.class, () -> associationsService.fetchPreviousStates( null, 0, 15 ).isEmpty() );
    }

}