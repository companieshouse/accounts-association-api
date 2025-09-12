package uk.gov.companieshouse.accounts.association.integration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.fetchAllStatusesWithout;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum.AUTH_CODE;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum.INVITATION;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.CONFIRMED;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Preprocessors.ReduceTimeStampResolutionPreprocessor;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListCompanyMapper;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListUserMapper;
import uk.gov.companieshouse.accounts.association.mapper.InvitationMapper;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class AssociationsServiceTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AssociationsRepository associationsRepository;

    @MockitoBean
    private EmailProducer emailProducer;

    @MockitoBean
    private KafkaProducerFactory kafkaProducerFactory;

    @MockitoBean
    private AssociationsListUserMapper associationsListUserMapper;

    @MockitoBean
    private AssociationsListCompanyMapper associationsListCompanyMapper;

    @MockitoBean
    private InvitationMapper invitationsMapper;

    @Autowired
    private AssociationsService associationsService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final ComparisonUtils comparisonUtils = new ComparisonUtils();

    @Test
    void fetchInvitationsFiltersWorkingCorrectly(){
        associationsRepository.insert(testDataManager.fetchAssociationDaos("MiAssociation041", "MiAssociation040", "MiAssociation042", "MiAssociation043", "MiAssociation030"));
        Mockito.doReturn(testDataManager.fetchInvitations("MiAssociation041").getFirst()).when(invitationsMapper).daoToDto(any(), eq("MiAssociation041"));
        final var invitations = associationsService.fetchInvitations("MiAssociation041" , 0, 15).stream().count();
        Assertions.assertEquals(1, invitations);
        Assertions.assertEquals("MiAssociation041", associationsService.fetchInvitations("MiAssociation041", 0, 15).get().getItems().getFirst().getAssociationId());
    }

    @Test
    void fetchInvitationsPaginationWorksCorrectly(){
        associationsRepository.insert(testDataManager.fetchAssociationDaos("MiAssociation007", "MiAssociation040", "MiAssociation042", "MiAssociation043", "MiAssociation030"));
        Mockito.doReturn(testDataManager.fetchInvitations("MiAssociation007").getFirst()).when(invitationsMapper).daoToDto(any(), eq("MiAssociation007"));
        final var invitations = associationsService.fetchInvitations("MiAssociation007" , 1, 2).stream().count();
        Assertions.assertEquals(1, invitations);
        Assertions.assertEquals("MiAssociation007", associationsService.fetchInvitations("MiAssociation007", 1, 2).get().getItems().getFirst().getAssociationId());
    }

    @Test
    void fetchInvitationsWhenAssociationDoesNotExist(){
        Assertions.assertTrue(associationsService.fetchInvitations("MiAssociation040", 0, 15).isEmpty());
    }

    @Test
    void fetchAssociationsForUserAndPartialCompanyNumberFiltersWorkingCorrectly(){
        associationsRepository.insert(testDataManager.fetchAssociationDaos("MiAssociation002", "MiAssociation001", "MiAssociation003", "MiAssociation004", "MiAssociation006"));
        final var user = testDataManager.fetchUserDtos("MiUser002").getFirst();
        final var associations = associationsService.fetchAssociationsForUserAndPartialCompanyNumber(user, "ICOMP001", 0, 15).getContent();
        Assertions.assertEquals(1, associations.size());
        Assertions.assertEquals("MiAssociation002", associations.getFirst().getId());
    }

    @Test
    void fetchAssociationsForUserAndPartialCompanyNumberWithMalformedPaginationParametersThrowsIllegalArgumentException(){
        associationsRepository.insert(testDataManager.fetchAssociationDaos("MiAssociation002", "MiAssociation001", "MiAssociation003", "MiAssociation004", "MiAssociation006"));
        final var user = testDataManager.fetchUserDtos("MiUser002").getFirst();
        Assertions.assertThrows(IllegalArgumentException.class, () -> associationsService.fetchAssociationsForUserAndPartialCompanyNumber(user, "ICOMP001", -1, 15));
        Assertions.assertThrows(IllegalArgumentException.class, () -> associationsService.fetchAssociationsForUserAndPartialCompanyNumber(user, "ICOMP001", 0, -15));
    }

    @Test
    void fetchAssociationsForUserAndPartialCompanyNumberWhereCompanyNumberIsNullThrowsNullPointerException(){
        associationsRepository.insert(testDataManager.fetchAssociationDaos("MiAssociation002", "MiAssociation001", "MiAssociation003", "MiAssociation004", "MiAssociation006"));
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.fetchAssociationsForUserAndPartialCompanyNumber(null, "ICOMP001", 0, 15));
    }

    @Test
    void fetchAssociationsForUserAndPartialCompanyNumberPaginationWorksCorrectly(){
        associationsRepository.insert(testDataManager.fetchAssociationDaos("MiAssociation002", "MiAssociation001", "MiAssociation003", "MiAssociation004", "MiAssociation006"));
        final var user = testDataManager.fetchUserDtos("MiUser002").getFirst();
        final var associations = associationsService.fetchAssociationsForUserAndPartialCompanyNumber(user, null, 1, 2).map(AssociationDao :: getId).getContent();
        Assertions.assertEquals(2, associations.size());
        Assertions.assertTrue(associations.containsAll(List.of ("MiAssociation004" , "MiAssociation006")));
    }

    private static Stream<Arguments> fetchAssociationsForUserAndPartialCompanyNumberEmptyTestData(){
        return Stream.of(
                Arguments.of(testDataManager.fetchUserDtos("MiUser003").getFirst(), "ICOMP001"),
                Arguments.of(testDataManager.fetchUserDtos("MiUser002").getFirst(), "test")
      );
    }

    @ParameterizedTest
    @MethodSource("fetchAssociationsForUserAndPartialCompanyNumberEmptyTestData")
    void fetchAssociationsForUserAndPartialCompanyNumberWhenUserAndCompanyNumberDoesNotExist(final User user, final String companyNumber){
        associationsRepository.insert(testDataManager.fetchAssociationDaos("MiAssociation002", "MiAssociation001", "MiAssociation003", "MiAssociation004", "MiAssociation006"));
        final var associations = associationsService.fetchAssociationsForUserAndPartialCompanyNumber(user, companyNumber, 0, 15).getContent();
        Assertions.assertEquals(0, associations.size());
    }

    @Test
    void fetchConfirmedUserIdsCanRetrieveUsers(){
        associationsRepository.insert(testDataManager.fetchAssociationDaos("MiAssociation003"));
        Assertions.assertEquals("MiUser002", associationsService.fetchConfirmedUserIds("MICOMP002").findFirst().get());
    }

    @Test
    void fetchConfirmedUserIdsRetrievesEmptyFluxWhenNoRecordsFound(){
        Assertions.assertEquals(0, associationsService.fetchConfirmedUserIds("MICOMP002").count());
        Assertions.assertEquals(0, associationsService.fetchConfirmedUserIds(null).count());
    }

    @Test
    void fetchAssociationDaoRetrievesAssociation(){
        associationsRepository.insert(testDataManager.fetchAssociationDaos("MiAssociation024"));
        Assertions.assertEquals("MiAssociation024", associationsService.fetchAssociationDao("MiAssociation024").get().getId());
    }

    @Test
    void fetchAssociationDaoReturnsEmptyOptionalWhenAssociationDoesNotExist(){
        Assertions.assertTrue(associationsService.fetchAssociationDao("MiAssociation024").isEmpty());
    }

    @Test
    void fetchAssociationDaoWithNullInputThrowsIllegalArgumentException(){
        Assertions.assertThrows(IllegalArgumentException.class, () -> associationsService.fetchAssociationDao(null));
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesWithNullCompanyThrowsNullPointerException(){
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.fetchUnexpiredAssociationsForCompanyAndStatuses(null, fetchAllStatusesWithout(Set.of()), null, null, 0,15));
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesWithIncludeRemovedTrueDoesNotApplyFilter(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos("111111").getFirst();
        associationsRepository.insert(testDataManager.fetchAssociationDaos("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"));
        associationsService.fetchUnexpiredAssociationsForCompanyAndStatuses(companyDetails, fetchAllStatusesWithout(Set.of()), null, null, 0, 20);
        final var expectedAssociationIds = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16");
        Mockito.verify(associationsListCompanyMapper).daoToDto(argThat(comparisonUtils.associationsPageMatches(16, 1, 16, expectedAssociationIds)), eq(companyDetails));
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesWithIncludeRemovedFalseAppliesFilter(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos("111111").getFirst();
        associationsRepository.insert(testDataManager.fetchAssociationDaos("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"));
        associationsService.fetchUnexpiredAssociationsForCompanyAndStatuses(companyDetails, fetchAllStatusesWithout(Set.of(StatusEnum.REMOVED)), null, null, 0, 20);
        final var expectedAssociationIds = List.of("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13");
        Mockito.verify(associationsListCompanyMapper).daoToDto(argThat(comparisonUtils.associationsPageMatches(13, 1, 13, expectedAssociationIds)), eq(companyDetails));
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesAppliesPaginationCorrectly() {
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos("111111").getFirst();
        associationsRepository.insert(testDataManager.fetchAssociationDaos("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"));
        associationsService.fetchUnexpiredAssociationsForCompanyAndStatuses(companyDetails, fetchAllStatusesWithout(Set.of()), null, null, 1,15);
        Mockito.verify(associationsListCompanyMapper).daoToDto(argThat(comparisonUtils.associationsPageMatches(16, 2, 1, List.of("16"))), eq(companyDetails));
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesCanFetchMigratedAssociations(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos("MKCOMP001").getFirst();
        associationsRepository.insert(testDataManager.fetchAssociationDaos("MKAssociation001", "MKAssociation002", "MKAssociation003"));
        associationsService.fetchUnexpiredAssociationsForCompanyAndStatuses(companyDetails, fetchAllStatusesWithout(Set.of()), null, null, 0, 15);
        Mockito.verify(associationsListCompanyMapper).daoToDto(argThat(comparisonUtils.associationsPageMatches(3, 1, 3, List.of("MKAssociation001", "MKAssociation002", "MKAssociation003"))), eq(companyDetails));
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesWithUserIdRetrievesAssociations(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos("MICOMP001").getFirst();
        associationsRepository.insert(testDataManager.fetchAssociationDaos("MiAssociation002", "MiAssociation004", "MiAssociation006", "MiAssociation009", "MiAssociation033"));
        associationsService.fetchUnexpiredAssociationsForCompanyAndStatuses(companyDetails, Set.of(StatusEnum.CONFIRMED), "MiUser002", "lechuck.monkey.island@inugami-example.com", 0, 15);
        Mockito.verify(associationsListCompanyMapper).daoToDto(argThat(comparisonUtils.associationsPageMatches(1, 1, 1, List.of("MiAssociation002"))), eq(companyDetails));
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesWithUserEmailRetrievesAssociations(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos("MICOMP005").getFirst();
        final var targetAssociation = testDataManager.fetchAssociationDaos("MiAssociation006").getFirst().userId(null).userEmail("lechuck.monkey.island@inugami-example.com");
        final var associations = testDataManager.fetchAssociationDaos("MiAssociation002", "MiAssociation004", "MiAssociation009", "MiAssociation033");
        associations.add(targetAssociation);
        associationsRepository.insert(associations);
        associationsService.fetchUnexpiredAssociationsForCompanyAndStatuses(companyDetails, Set.of(StatusEnum.AWAITING_APPROVAL), null, "lechuck.monkey.island@inugami-example.com", 0, 15);
        Mockito.verify(associationsListCompanyMapper).daoToDto(argThat(comparisonUtils.associationsPageMatches(1, 1, 1, List.of("MiAssociation006"))), eq(companyDetails));
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyAndStatusesWithNonexistentUserReturnsEmptyList(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos("MICOMP001").getFirst();
        associationsRepository.insert(testDataManager.fetchAssociationDaos("MiAssociation002", "MiAssociation004", "MiAssociation006", "MiAssociation009", "MiAssociation033"));
        associationsService.fetchUnexpiredAssociationsForCompanyAndStatuses(companyDetails, Set.of(StatusEnum.CONFIRMED), "404User", "404@inugami-example.com", 0, 15);
        Mockito.verify(associationsListCompanyMapper).daoToDto(argThat(comparisonUtils.associationsPageMatches(0, 0, 0, List.of())), eq(companyDetails));
    }

    @Test
    void fetchAssociationsForUserAndPartialCompanyNumberAndStatusesWithNullInputsThrowsNullPointerException(){
        final var user = testDataManager.fetchUserDtos("9999").getFirst();
        final var status = Set.of(StatusEnum.CONFIRMED.getValue());
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(null, "333333", status, 0, 15));
    }

    @Test
    void fetchAssociationsForUserAndPartialCompanyNumberAndStatusesWithInvalidPageIndexOrItemsPerPageThrowsIllegalArgumentException() {
        final var user = testDataManager.fetchUserDtos("9999").getFirst();
        final var status = Set.of(StatusEnum.CONFIRMED.getValue());
        Assertions.assertThrows(IllegalArgumentException.class, () -> associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(user, "333333", status, -1, 15));
        Assertions.assertThrows(IllegalArgumentException.class, () -> associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(user, "333333", status, 0, 0));
    }

    @Test
    void fetchAssociationsForUserAndPartialCompanyNumberAndStatusesForUserStatusAndCompanyPaginatesCorrectly() {
        final var user = testDataManager.fetchUserDtos("9999").getFirst();
        final var status = Set.of(StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue());
        associationsRepository.insert(testDataManager.fetchAssociationDaos("18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33"));
        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(user, null, status, 1, 15);
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(comparisonUtils.associationsPageMatches(16, 2, 1, List.of("33"))), eq(user));
    }

    @Test
    void fetchAssociationsForUserAndPartialCompanyNumberAndStatusesFiltersByCompanyNumber(){
        final var user = testDataManager.fetchUserDtos("9999").getFirst();
        final var status = Set.of(StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue());
        associationsRepository.insert(testDataManager.fetchAssociationDaos("18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33"));
        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(user, "333333", status, 0, 15);
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(comparisonUtils.associationsPageMatches(2, 1, 2, List.of("18", "27"))), eq(user));
    }

    @Test
    void fetchAssociationsForUserAndPartialCompanyNumberAndStatusesFiltersBasedOnStatus(){
        final var user = testDataManager.fetchUserDtos("9999").getFirst();
        final var status = Set.of(StatusEnum.REMOVED.getValue());
        associationsRepository.insert(testDataManager.fetchAssociationDaos("18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33"));
        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(user, null, status, 0, 15);
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(comparisonUtils.associationsPageMatches(3, 1, 3, List.of("31", "32", "33"))), eq(user));
    }

    @Test
    void fetchAssociationsForUserAndPartialCompanyNumberAndStatusesWithNullStatusDefaultsToConfirmedStatus(){
        final var user = testDataManager.fetchUserDtos("9999").getFirst();
        associationsRepository.insert(testDataManager.fetchAssociationDaos("18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33"));
        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(user, null, null, 0, 15);
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(comparisonUtils.associationsPageMatches(5, 1, 5, List.of("18", "19", "20", "21", "22"))), eq(user));
    }

    @Test
    void fetchAssociationsForUserAndPartialCompanyNumberAndStatusesWithEmptyStatusDefaultsToConfirmedStatus(){
        final var user = testDataManager.fetchUserDtos("9999").getFirst();
        associationsRepository.insert(testDataManager.fetchAssociationDaos("18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33"));
        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(user, null, Collections.emptySet(), 0, 15);
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(comparisonUtils.associationsPageMatches(5, 1, 5, List.of("18", "19", "20", "21", "22"))), eq(user));
    }

    @Test
    void fetchAssociationsForUserAndPartialCompanyNumberAndStatusesWithInvalidStatusReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos("9999").getFirst();
        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(user, null, Set.of("complicated"), 0, 15);
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(comparisonUtils.associationsPageMatches(0, 0, 0, Collections.emptyList())), eq(user));
    }

    @Test
    void fetchAssociationsForUserAndPartialCompanyNumberAndStatusesWithNonexistentOrInvalidCompanyNumberReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos("9999").getFirst();
        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(user, "$$1234", Collections.emptySet(), 0, 15);
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(comparisonUtils.associationsPageMatches(0, 0, 0, Collections.emptyList())), eq(user));
    }

    @Test
    void fetchAssociationsForUserAndPartialCompanyNumberAndStatusesWithNonexistentUserIdReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos("9999").getFirst();
        associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses(user, "1234", Collections.emptySet(), 0, 15);
        Mockito.verify(associationsListUserMapper).daoToDto(argThat(comparisonUtils.associationsPageMatches(0, 0, 0, Collections.emptyList())), eq(user));
    }

    @Test
    void confirmedAssociationExistsWithNullOrMalformedOrNonExistentCompanyNumberOrUserReturnsFalse(){
        Assertions.assertFalse(associationsService.confirmedAssociationExists(null, "111"));
        Assertions.assertFalse(associationsService.confirmedAssociationExists("$$$$$$", "111"));
        Assertions.assertFalse(associationsService.confirmedAssociationExists("919191", "111"));
        Assertions.assertFalse(associationsService.confirmedAssociationExists("111111", null));
        Assertions.assertFalse(associationsService.confirmedAssociationExists("111111", "$$$"));
        Assertions.assertFalse(associationsService.confirmedAssociationExists("111111", "9191"));
    }

    @Test
    void associationExistsWithExistingConfirmedAssociationReturnsTrue(){
        associationsRepository.insert(testDataManager.fetchAssociationDaos("1"));
        Assertions.assertTrue(associationsService.confirmedAssociationExists("111111", "111"));
    }

    @Test
    void updateAssociationWithMalformedOrNonexistentAssociationIdThrowsInternalServerError(){
        final var update = new Update();
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> associationsService.updateAssociation("$$$", update));
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> associationsService.updateAssociation("9191", update));
    }

    @Test
    void updateAssociationWithNullAssociationIdThrowsInternalServerErrorRuntimeException(){
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> associationsService.updateAssociation(null, null));
    }

    @Test
    void updateAssociationWithNullUpdateThrowsIllegalStateException(){
        Assertions.assertThrows(IllegalStateException.class, () -> associationsService.updateAssociation("1",  null));
    }

    @Test
    void updateAssociationWithRemovedStatusAndSwapUserEmailForUserIdSetToFalseUpdatesAssociationCorrectly(){
        final var oldAssociationData = testDataManager.fetchAssociationDaos("1").getFirst();
        associationsRepository.insert(oldAssociationData);
        associationsService.updateAssociation("1", new Update().set("status","removed"));
        final var newAssociationData = associationsRepository.findById("1").get();
        Assertions.assertTrue(comparisonUtils.compare(oldAssociationData, List.of("approvedAt", "userEmail", "userId"), List.of("status"), Map.of("approvedAt", new ReduceTimeStampResolutionPreprocessor())).matches(newAssociationData));
    }

    @Test
    void fetchAssociationDaoWithNullOrMalformedOrNonexistentCompanyNumberReturnsNothing(){
        Assertions.assertTrue(associationsService.fetchAssociationDao(null, "111", null).isEmpty());
        Assertions.assertTrue(associationsService.fetchAssociationDao("$$$$$$", "111", null).isEmpty());
        Assertions.assertTrue(associationsService.fetchAssociationDao("919191", "111", null).isEmpty());
    }

    @Test
    void fetchAssociationDaoWithMalformedOrNonexistentUserIdReturnsNothing() {
        Assertions.assertTrue(associationsService.fetchAssociationDao("111111", "$$$", null).isEmpty());
        Assertions.assertTrue(associationsService.fetchAssociationDao("111111", "9191", null).isEmpty());
    }

    @Test
    void fetchAssociationDaoShouldFetchAssociation(){
        associationsRepository.insert(testDataManager.fetchAssociationDaos("1"));
        Assertions.assertEquals("1", associationsService.fetchAssociationDao("111111", "111", null).get().getId());
    }

    @Test
    void fetchActiveInvitationsWithNullOrMalformedOrNonexistentUserIdReturnsEmptyList(){
        Assertions.assertEquals(Collections.emptyList(), associationsService.fetchActiveInvitations(new User(), 0, 1).getItems());
        Assertions.assertEquals(Collections.emptyList(), associationsService.fetchActiveInvitations(new User().userId("$$$"), 0, 1).getItems());
        Assertions.assertEquals(Collections.emptyList(), associationsService.fetchActiveInvitations(new User().userId("9191"), 0, 1).getItems());
    }

    @Test
    void fetchActiveInvitationsReturnsPaginatedResultsInCorrectOrderAndOnlyRetainsMostRecentInvitationPerAssociation(){
        final var user = testDataManager.fetchUserDtos("000").getFirst();
        final var firstAssociation = testDataManager.fetchAssociationDaos("37").getFirst();
        final var secondAssociation = testDataManager.fetchAssociationDaos("38").getFirst();
        secondAssociation.setUserId("000");

        associationsRepository.insert(List.of(firstAssociation, secondAssociation));

        final var mostRecentInvitationDaoInFirstAssociation = firstAssociation.getInvitations().getLast();
        final var mostRecentInvitationDtoInFirstAssociation = new Invitation().invitedAt(mostRecentInvitationDaoInFirstAssociation.getInvitedAt().toString()).invitedBy(mostRecentInvitationDaoInFirstAssociation.getInvitedBy());
        Mockito.doReturn(mostRecentInvitationDtoInFirstAssociation).when(invitationsMapper).daoToDto(argThat(comparisonUtils.compare(mostRecentInvitationDaoInFirstAssociation, List.of("invited_by"), List.of(), Map.of())), eq("37"));

        final var invitations = associationsService.fetchActiveInvitations(user, 1, 1);
        final var invitation = invitations.getItems().getFirst();

        Assertions.assertEquals(mostRecentInvitationDaoInFirstAssociation.getInvitedBy(), invitation.getInvitedBy());
        Assertions.assertEquals(localDateTimeToNormalisedString(mostRecentInvitationDaoInFirstAssociation.getInvitedAt()), reduceTimestampResolution(invitation.getInvitedAt()));
    }

    @Test
    void fetchPreviousStatesAppliedToAssociationWithPreviousStatesRetrievesAndMaps(){
        final var now = LocalDateTime.now();

        associationsRepository.insert(testDataManager.fetchAssociationDaos("MKAssociation003"));

        final var previousStatesList = associationsService.fetchPreviousStates("MKAssociation003", 1, 1).get();
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();

        Assertions.assertEquals(1, previousStatesList.getItemsPerPage());
        Assertions.assertEquals(1, previousStatesList.getPageNumber());
        Assertions.assertEquals(4, previousStatesList.getTotalResults());
        Assertions.assertEquals(4, previousStatesList.getTotalPages());
        Assertions.assertEquals("/associations/MKAssociation003/previous-states?page_index=1&items_per_page=1", links.getSelf());
        Assertions.assertEquals("/associations/MKAssociation003/previous-states?page_index=2&items_per_page=1", links.getNext());
        Assertions.assertEquals(1, items.size());
        Assertions.assertEquals(AWAITING_APPROVAL, items.getFirst().getStatus());
        Assertions.assertEquals("MKUser003", items.getFirst().getChangedBy());
        Assertions.assertEquals(localDateTimeToNormalisedString(now.minusDays(7L)), reduceTimestampResolution(items.getFirst().getChangedAt()));
    }

    @Test
    void fetchPreviousStatesAppliedToAssociationWithoutPreviousStatesRetrievesAndMaps(){
        associationsRepository.insert(testDataManager.fetchAssociationDaos("MKAssociation001"));

        final var previousStatesList = associationsService.fetchPreviousStates("MKAssociation001", 0, 15).get();
        final var links = previousStatesList.getLinks();
        final var items = previousStatesList.getItems();

        Assertions.assertEquals(15, previousStatesList.getItemsPerPage());
        Assertions.assertEquals(0, previousStatesList.getPageNumber());
        Assertions.assertEquals(0, previousStatesList.getTotalResults());
        Assertions.assertEquals(0, previousStatesList.getTotalPages());
        Assertions.assertEquals("/associations/MKAssociation001/previous-states?page_index=0&items_per_page=15", links.getSelf());
        Assertions.assertEquals("", links.getNext());
        Assertions.assertTrue(items.isEmpty());
    }

    @Test
    void fetchPreviousStatesAppliedToMalformedOrNonexistentAssociationReturnsEmptyOptional(){
        Assertions.assertTrue(associationsService.fetchPreviousStates("$$$", 0, 15).isEmpty());
        Assertions.assertTrue(associationsService.fetchPreviousStates("404MKAssociation", 0, 15).isEmpty());
    }

    @Test
    void fetchPreviousStatesThrowsIllegalArgumentExceptionWhenAssociationIdIsNull(){
        Assertions.assertThrows(IllegalArgumentException.class, () -> associationsService.fetchPreviousStates(null, 0, 15).isEmpty());
    }

    @Test
    void createAssociationWithAuthCodeApprovalRouteCreatesAssociationWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociationWithAuthCodeApprovalRoute(null, "111"));
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociationWithAuthCodeApprovalRoute("111111", null));
    }

    @Test
    void createAssociationWithAuthCodeApprovalRouteCreatesAssociation(){
        final var association = associationsService.createAssociationWithAuthCodeApprovalRoute("111111", "111");
        Assertions.assertEquals("111111", association.getCompanyNumber());
        Assertions.assertEquals("111", association.getUserId());
        Assertions.assertEquals(CONFIRMED.getValue(), association.getStatus());
        Assertions.assertEquals(AUTH_CODE.getValue(), association.getApprovalRoute());
        Assertions.assertNotNull(association.getEtag());
    }

    @Test
    void createAssociationWithInvitationApprovalRouteWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociationWithInvitationApprovalRoute(null, "111", null, "222"));
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociationWithInvitationApprovalRoute("111111", null, null, "222"));
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociationWithInvitationApprovalRoute("111111", null, "bruce.wayne@gotham.city", null));
    }

    private static Stream<Arguments> createAssociationWithInvitationApprovalRouteScenarios(){
        return Stream.of(
                Arguments.of("111", null),
                Arguments.of(null, "bruce.wayne@gotham.city")
      );
    }

    @ParameterizedTest
    @MethodSource("createAssociationWithInvitationApprovalRouteScenarios")
    void createAssociationWithInvitationApprovalRouteCreatesAssociation(final String userId, final String userEmail){
        final var association = associationsService.createAssociationWithInvitationApprovalRoute("111111", userId, userEmail, "222");
        final var invitations = association.getInvitations().getFirst();

        Assertions.assertEquals("111111", association.getCompanyNumber());
        Assertions.assertEquals(userId, association.getUserId());
        Assertions.assertEquals(userEmail, association.getUserEmail());
        Assertions.assertEquals(AWAITING_APPROVAL.getValue(), association.getStatus());
        Assertions.assertEquals(INVITATION.getValue(), association.getApprovalRoute());
        Assertions.assertEquals(localDateTimeToNormalisedString(LocalDateTime.now().plusDays(30)), localDateTimeToNormalisedString(association.getApprovalExpiryAt()));
        Assertions.assertEquals("222", invitations.getInvitedBy());
        Assertions.assertEquals(localDateTimeToNormalisedString(LocalDateTime.now()), localDateTimeToNormalisedString(invitations.getInvitedAt()));
        Assertions.assertNotNull(association.getEtag());
    }

    @Test
    void fetchUnexpiredAssociationsForCompanyUserAndStatusesRetrievesAssociation(){
        final var company = testDataManager.fetchCompanyDetailsDtos("MKCOMP001").getFirst();
        final var user = testDataManager.fetchUserDtos("MKUser002").getFirst();
        final var associationDao = testDataManager.fetchAssociationDaos("MKAssociation002").getFirst();
        final var association = testDataManager.fetchAssociationDto("MKAssociation002", user);
        associationsRepository.insert(associationDao);
        Mockito.doReturn(association).when(associationsListCompanyMapper).daoToDto(any(), eq(user), eq(company));
        Assertions.assertEquals("MKAssociation002" , associationsService.fetchUnexpiredAssociationsForCompanyUserAndStatuses(company, Set.of(StatusEnum.CONFIRMED), user, user.getEmail()).get().getId());
    }

    private static Stream<Arguments> fetchUnexpiredAssociationsForCompanyUserAndStatusesRetrievesEmptyOptionalScenarios(){
        final var user = testDataManager.fetchUserDtos("MKUser002").getFirst();
        return Stream.of(
                Arguments.of(new CompanyDetails() , Set.of(StatusEnum.CONFIRMED), user, user.getEmail()),
                Arguments.of(new CompanyDetails().companyNumber("404CompanyId") , Set.of(StatusEnum.CONFIRMED), user, user.getEmail()),
                Arguments.of(new CompanyDetails().companyNumber("MKAssociation002"), Set.of(), user, user.getEmail()),
                Arguments.of(new CompanyDetails().companyNumber("MKAssociation002") , Set.of(StatusEnum.CONFIRMED), null, "404User@Email.com"),
                Arguments.of(new CompanyDetails().companyNumber("MKAssociation002") , Set.of(StatusEnum.CONFIRMED), null, null)
      );
    }

    @ParameterizedTest
    @MethodSource("fetchUnexpiredAssociationsForCompanyUserAndStatusesRetrievesEmptyOptionalScenarios")
    void fetchUnexpiredAssociationsForCompanyUserAndStatusesRetrievesEmptyOptional(final CompanyDetails company, final Set<StatusEnum> status, final User user, final String userEmail){
        final var associationDao = testDataManager.fetchAssociationDaos("MKAssociation002").getFirst();
        associationsRepository.insert(associationDao);
        Assertions.assertTrue(associationsService.fetchUnexpiredAssociationsForCompanyUserAndStatuses(company, status, user, userEmail).isEmpty());
    }


    @Test
    void fetchUnexpiredAssociationsForCompanyWithNullInputsThrowsNullPointerException(){
        final var company = testDataManager.fetchCompanyDetailsDtos("MKCOMP001").getFirst();
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.fetchUnexpiredAssociationsForCompanyUserAndStatuses(null, Set.of(StatusEnum.CONFIRMED), null, "null@null.com"));
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.fetchUnexpiredAssociationsForCompanyUserAndStatuses(company, null, null, "null@null.com"));
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }

}
