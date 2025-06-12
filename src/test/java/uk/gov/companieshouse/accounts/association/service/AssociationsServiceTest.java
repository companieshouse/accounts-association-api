package uk.gov.companieshouse.accounts.association.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.query.Update;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Preprocessors.ReduceTimeStampResolutionPreprocessor;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListCompanyMapper;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListUserMapper;
import uk.gov.companieshouse.accounts.association.mapper.InvitationsCollectionMappers;
import uk.gov.companieshouse.accounts.association.mapper.InvitationMapper;
import uk.gov.companieshouse.accounts.association.mapper.PreviousStatesCollectionMappers;
import uk.gov.companieshouse.accounts.association.mapper.PreviousStatesMapperImpl;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
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
import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.fetchAllStatusesWithout;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum.AUTH_CODE;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum.INVITATION;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.CONFIRMED;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AssociationsServiceTest {

    @InjectMocks
    private AssociationsService associationsService;

    @Mock
    private AssociationsRepository associationsRepository;

    @Mock
    private InvitationMapper invitationMapper;

    @Mock
    private InvitationsCollectionMappers invitationsCollectionMappers;

    @Mock
    private AssociationsListCompanyMapper associationsListCompanyMapper;

    @Mock
    private AssociationsListUserMapper associationsListUserMapper;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final ComparisonUtils comparisonUtils = new ComparisonUtils();

    @BeforeEach
    public void setup() {
        final var previousStatesCollectionMappers = new PreviousStatesCollectionMappers( new PreviousStatesMapperImpl() );
        associationsService = new AssociationsService( associationsRepository, associationsListUserMapper, associationsListCompanyMapper, previousStatesCollectionMappers, invitationsCollectionMappers );
    }

    @Test
    void fetchAssociationsForUserReturnEmptyItemsWhenNoAssociationFound() { //todo: do these next few need changed to fetchAssociationsForUserAndPartialCompanyNumberAndStatuses
        final var user = testDataManager.fetchUserDtos( "111" ).getFirst();
        when(associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("111","bruce.wayne@gotham.city", Set.of( "confirmed" ), "", PageRequest.of(0, 15))).thenReturn(Page.empty());
        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(user, "", Set.of( "confirmed" ), 0, 15);
        verify(associationsListUserMapper).daoToDto( Page.empty(), user );
    }

    @Test
    void fetchAssociationsForUserUsesStatusConfirmedAsDefaultWhenStatusNotProvided() throws ApiErrorResponseException, URIValidationException {
        final var user = testDataManager.fetchUserDtos( "111" ).getFirst();
        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(user, "", null, 0, 15);
        verify(associationsRepository).fetchAssociationsForUserAndStatusesAndPartialCompanyNumber("111", "bruce.wayne@gotham.city", Set.of("confirmed"), "", PageRequest.of(0, 15));
        verify(associationsListUserMapper).daoToDto( null, user);

    }

    @Test
    void fetchAssociatedUsersWithNullCompanyThrowsNullPointerException() {
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.fetchUnexpiredAssociationsForCompanyAndStatuses(null, fetchAllStatusesWithout( Set.of() ), 0, 15 ) );
    }

    @Test
    void fetchAssociatedUsersWithIncludeRemovedTrueDoesNotApplyFilter() {
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" );
        final var pageRequest = PageRequest.of(0, 20);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        Mockito.doReturn(page).when(associationsRepository).fetchUnexpiredAssociationsForCompanyAndStatuses(any(), any(), any(), any());

        associationsService.fetchUnexpiredAssociationsForCompanyAndStatuses( companyDetails, fetchAllStatusesWithout( Set.of() ), 0,20);

        Mockito.verify(associationsListCompanyMapper).daoToDto( eq( page ), eq(companyDetails));
    }

    @Test
    void fetchAssociatedUsersWithIncludeRemovedFalseAppliesFilter() {
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" );
        final var pageRequest = PageRequest.of(0, 20);
        final var page = new PageImpl<>(content, pageRequest, content.size());

        Mockito.doReturn(page).when(associationsRepository).fetchUnexpiredAssociationsForCompanyAndStatuses(any(), any(), any(), any());

        associationsService.fetchUnexpiredAssociationsForCompanyAndStatuses( companyDetails, fetchAllStatusesWithout( Set.of( StatusEnum.REMOVED ) ),0, 20);

        Mockito.verify(associationsListCompanyMapper).daoToDto(eq( page ), eq(companyDetails));
    }

    @Test
    void fetchAssociatedUsersAppliesPaginationCorrectly() {
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "16" );
        final var pageRequest = PageRequest.of(1, 15);
        final var page = new PageImpl<>(content, pageRequest, 16);

        Mockito.doReturn(page).when(associationsRepository).fetchUnexpiredAssociationsForCompanyAndStatuses(any(), any(), any(), any());

        associationsService.fetchUnexpiredAssociationsForCompanyAndStatuses( companyDetails, fetchAllStatusesWithout( Set.of() ),1, 15);

        Mockito.verify(associationsListCompanyMapper).daoToDto(eq( page ), eq(companyDetails));
    }

    @Test
    void fetchAssociatedUsersCanFetchMigratedAssociations(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "MKCOMP001" ).getFirst();

        final var content = testDataManager.fetchAssociationDaos( "MKAssociation001", "MKAssociation002", "MKAssociation003" );
        final var pageRequest = PageRequest.of( 0, 15 );
        final var page = new PageImpl<>( content, pageRequest, 3 );

        Mockito.doReturn( page ).when( associationsRepository ).fetchUnexpiredAssociationsForCompanyAndStatuses( any(), any(), any(), any() );

        associationsService.fetchUnexpiredAssociationsForCompanyAndStatuses( companyDetails, fetchAllStatusesWithout( Set.of() ), 0, 15);

        Mockito.verify(associationsListCompanyMapper).daoToDto( eq( page ), eq( companyDetails ) );
    }

    @Test
    void fetchAssociationDtoReturnsAssociationDtoWhenAssociationFound() {
        final var associationDao = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        Mockito.when(associationsRepository.findById("1")).thenReturn(Optional.of(associationDao));
        associationsService.fetchAssociationDto("1");
        Mockito.verify(associationsListCompanyMapper).daoToDto(associationDao, null, null);

    }

    @Test
    void fetchAssociationDtoReturnsEmptyWhenAssociationNotFound() {
        Mockito.when(associationsRepository.findById("1111")).thenReturn(Optional.empty());
        assertTrue(associationsService.fetchAssociationDto("1111").isEmpty());
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNullInputsThrowsNullPointerException(){ //todo: should these be fetchAssociationsForUserAndPartialCompanyNumberAndStatuses or no change
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = Set.of( StatusEnum.CONFIRMED.getValue() );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses( null, "333333", status, 0, 15 ) );
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithInvalidPageIndexOrItemsPerPageThrowsIllegalArgumentException() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = Set.of( StatusEnum.CONFIRMED.getValue() );
        Assertions.assertThrows( IllegalArgumentException.class, () -> associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses( user, "333333", status, -1, 15 ) );
        Assertions.assertThrows( IllegalArgumentException.class, () -> associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses( user, "333333", status, 0, 0) );
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyPaginatesCorrectly() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = Set.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue() );
        final var content = testDataManager.fetchAssociationDaos( "33" );
        final var pageRequest = PageRequest.of(1, 15);
        final var page = new PageImpl<>(content, pageRequest, 16 );

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociationsForUserAndStatusesAndPartialCompanyNumber( "9999", "scrooge.mcduck@disney.land", status,"", pageRequest );

        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses( user, null, status, 1, 15 );

        Mockito.verify(associationsListUserMapper).daoToDto(eq( page ), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersByCompanyNumber(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = Set.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue() );
        final var content = testDataManager.fetchAssociationDaos( "18", "27" );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociationsForUserAndStatusesAndPartialCompanyNumber( "9999","scrooge.mcduck@disney.land", status, "333333", pageRequest );

        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses( user, "333333", status, 0, 15 );

        Mockito.verify(associationsListUserMapper).daoToDto(eq(page), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersBasedOnStatus(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = Set.of( StatusEnum.REMOVED.getValue() );
        final var content = testDataManager.fetchAssociationDaos( "31", "32", "33" );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociationsForUserAndStatusesAndPartialCompanyNumber( "9999", "scrooge.mcduck@disney.land",  status, "", pageRequest );

        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses( user, null, status, 0, 15 );

        Mockito.verify(associationsListUserMapper).daoToDto( eq(page), eq(user) );
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersBasedOnAwaitingApprovalStatus(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = Set.of( StatusEnum.AWAITING_APPROVAL.getValue() );
        final var content = testDataManager.fetchAssociationDaos( "25", "26", "27" );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociationsForUserAndStatusesAndPartialCompanyNumber( "9999", "scrooge.mcduck@disney.land", status, "", pageRequest );

        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses( user, null, status, 0, 15 );

        Mockito.verify(associationsListUserMapper).daoToDto( eq(page), eq(user) );
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNullStatusDefaultsToConfirmedStatus(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22" );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociationsForUserAndStatusesAndPartialCompanyNumber( "9999","scrooge.mcduck@disney.land", Set.of( StatusEnum.CONFIRMED.getValue() ), "", pageRequest );

        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses( user, null, null, 0, 15 );

        Mockito.verify(associationsListUserMapper).daoToDto(eq(page), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithEmptyStatusDefaultsToConfirmedStatus(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22" );
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociationsForUserAndStatusesAndPartialCompanyNumber( "9999", "scrooge.mcduck@disney.land", Set.of( StatusEnum.CONFIRMED.getValue() ),"", pageRequest);

        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses( user, null, Collections.emptySet(), 0, 15 );

        Mockito.verify(associationsListUserMapper).daoToDto(eq(page), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithInvalidStatusReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = new ArrayList<AssociationDao>();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociationsForUserAndStatusesAndPartialCompanyNumber( "9999", "scrooge.mcduck@disney.land", Set.of( "complicated" ),"", pageRequest );

        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses( user, null, Set.of( "complicated" ), 0, 15 );

        Mockito.verify(associationsListUserMapper).daoToDto(eq(page), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNonexistentOrInvalidCompanyNumberReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = new ArrayList<AssociationDao>();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociationsForUserAndStatusesAndPartialCompanyNumber( "9999",  "scrooge.mcduck@disney.land", Set.of( StatusEnum.CONFIRMED.getValue()),"$$$$$$",pageRequest );

        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses( user, "$$$$$$", Collections.emptySet(), 0, 15);

        Mockito.verify(associationsListUserMapper).daoToDto(eq(page), eq(user));
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNonexistentUserIdReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var content = new ArrayList<AssociationDao>();
        final var pageRequest = PageRequest.of(0, 15);
        final var page = new PageImpl<>(content, pageRequest, content.size() );

        Mockito.doReturn( page ).when( associationsRepository ).fetchAssociationsForUserAndStatusesAndPartialCompanyNumber( "9999" , "scrooge.mcduck@disney.land", Set.of( StatusEnum.CONFIRMED.getValue() ),"",pageRequest);

        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses( user, null, Collections.emptySet(), 0, 15 );
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
        Mockito.doReturn(false).when(associationsRepository).confirmedAssociationExists(Mockito.any(), Mockito.any() );
        Assertions.assertFalse(associationsService.confirmedAssociationExists(companyNumber, userId));
    }

    @Test
    void associationExistsWithExistingConfirmedAssociationReturnsTrue(){
        Mockito.doReturn( true ).when( associationsRepository ).confirmedAssociationExists( "111111", "111" );
        assertTrue( associationsService.confirmedAssociationExists( "111111", "111" ) );
    }

    @Test
    void updateAssociationStatusWithMalformedOrNonexistentAssociationIdThrowsInternalServerError(){
        final var update = new Update();
        Mockito.doReturn( 0 ).when( associationsRepository ).updateAssociation( any(), any() );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> associationsService.updateAssociation( "$$$", update) );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> associationsService.updateAssociation( "9191", update) );
    }

    @Test
    void updateAssociationWithNullAssociationIdThrowsNullPointerException(){
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> associationsService.updateAssociation( null, null) );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserIdWithNullOrMalformedOrNonexistentCompanyNumberReturnsNothing(){
        Mockito.doReturn( Optional.empty() ).when( associationsRepository ).fetchAssociation( any(), anyString(), isNull() );
        assertTrue( associationsService.fetchAssociationDao( null, "111", null ).isEmpty() );
        assertTrue( associationsService.fetchAssociationDao( "$$$$$$", "111", null ).isEmpty() );
        assertTrue( associationsService.fetchAssociationDao( "919191", "111", null ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserIdWithMalformedOrNonexistentUserIdReturnsNothing() {
        Mockito.doReturn( Optional.empty() ).when( associationsRepository ).fetchAssociation( anyString(), anyString(), isNull() );
        assertTrue( associationsService.fetchAssociationDao( "111111", "$$$", null ).isEmpty() );
        assertTrue( associationsService.fetchAssociationDao( "111111", "9191", null ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserIdShouldFetchAssociation(){
        final var association = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        Mockito.doReturn( Optional.of( association ) ).when( associationsRepository ).fetchAssociation( anyString(), anyString(), isNull() );
        assertEquals( "1", associationsService.fetchAssociationDao( "111111", "111", null ).get().getId() );
    }

    @Test
    void fetchInvitationsWithNullInvitationsListReturnsEmptyList() {
        final var associationDao = testDataManager.fetchAssociationDaos( "18" ).getFirst();
        Mockito.doReturn( Optional.of( associationDao ) ).when( associationsRepository ).findById( "18" );
        associationsService.fetchInvitations( "18", 0, 1);
        Mockito.verify( invitationsCollectionMappers ).daoToDto( associationDao, 0, 1 );
    }

    private static Stream<String> userIdsProvider() {
        return Stream.of(null, "$$$", "9191");
    }

    @ParameterizedTest
    @MethodSource("userIdsProvider")
    void fetchActiveInvitationsWithNullOrMalformedOrNonexistentUserIdReturnsEmptyList(String userId) {
        associationsService.fetchActiveInvitations(new User().userId(userId), 0, 1);
        Mockito.verify( invitationsCollectionMappers ).daoToDto( List.of(), 0, 1 );
    }

    @Test
    void fetchAssociationDaosRetrieveUsersAssociatedWithCompany(){
        final var associations = testDataManager.fetchAssociationDaos( "1" ).stream();

        Mockito.doReturn( associations ).when( associationsRepository ).fetchConfirmedAssociations( eq( "111111" ) );

        Assertions.assertEquals( "111", associationsService.fetchConfirmedUserIds( "111111" ).blockFirst() );
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

    @Test
    void createAssociationWithAuthCodeApprovalRouteCreatesAssociationWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.createAssociationWithAuthCodeApprovalRoute( null, "111" ) );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.createAssociationWithAuthCodeApprovalRoute( "111111", null ) );
    }

    @Test
    void createAssociationWithAuthCodeApprovalRouteCreatesAssociation(){
        final var expectedAssociation = new AssociationDao()
                .companyNumber( "111111" )
                .userId( "111" )
                .status( CONFIRMED.getValue() )
                .approvalRoute( AUTH_CODE.getValue() )
                .etag( generateEtag() );

        associationsService.createAssociationWithAuthCodeApprovalRoute( "111111", "111" );

        Mockito.verify( associationsRepository ).insert( argThat( comparisonUtils.compare( expectedAssociation, List.of( "companyNumber", "userId", "status", "approvalRoute" ), List.of(), Map.of() ) ) );
    }

    @Test
    void createAssociationWithInvitationApprovalRouteWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.createAssociationWithInvitationApprovalRoute( null, "111", null, "222" ) );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.createAssociationWithInvitationApprovalRoute( "111111", null, null, "222" ) );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.createAssociationWithInvitationApprovalRoute( "111111", null, "bruce.wayne@gotham.city", null ) );
    }

    private static Stream<Arguments> createAssociationWithInvitationApprovalRouteScenarios(){
        return Stream.of(
                Arguments.of( "111", null ),
                Arguments.of( null, "bruce.wayne@gotham.city" )
        );
    }

    @ParameterizedTest
    @MethodSource( "createAssociationWithInvitationApprovalRouteScenarios" )
    void createAssociationWithInvitationApprovalRouteCreatesAssociation( final String userId, final String userEmail ){
        final var expectedAssociation = new AssociationDao()
                .companyNumber( "111111" )
                .userId( userId )
                .userEmail( userEmail )
                .status( AWAITING_APPROVAL.getValue() )
                .approvalRoute( INVITATION.getValue() )
                .approvalExpiryAt( LocalDateTime.now().plusDays( 7 ) )
                .invitations( List.of( new InvitationDao()
                        .invitedBy( "222" )
                        .invitedAt( LocalDateTime.now() )
                ) )
                .etag( generateEtag() );


        associationsService.createAssociationWithInvitationApprovalRoute( "111111", userId, userEmail, "222" );

        Mockito.verify( associationsRepository ).insert( argThat( comparisonUtils.compare( expectedAssociation, List.of( "companyNumber", "userId", "userEmail", "status", "approvalRoute", "approvalExpiryAt" ), List.of(), Map.of( "approvalExpiryAt", new ReduceTimeStampResolutionPreprocessor() ) ) ) );
    }

}