package uk.gov.companieshouse.accounts.association.integration;

import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;
import uk.gov.companieshouse.accounts.association.common.ComparisonUtils;
import uk.gov.companieshouse.accounts.association.common.Preprocessors.ReduceTimeStampResolutionPreprocessor;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListMappers;
import uk.gov.companieshouse.accounts.association.mapper.InvitationsMapper;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.factory.KafkaProducerFactory;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.localDateTimeToNormalisedString;
import static uk.gov.companieshouse.accounts.association.common.ParsingUtils.reduceTimestampResolution;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@Tag("integration-test")
class AssociationsServiceTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private AssociationsRepository associationsRepository;

    @MockBean
    private EmailProducer emailProducer;

    @MockBean
    private KafkaProducerFactory kafkaProducerFactory;

    @MockBean
    private AssociationsListMappers associationsListMappers;

    @MockBean
    private InvitationsMapper invitationsMapper;

    @Autowired
    private AssociationsService associationsService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private static final ComparisonUtils comparisonUtils = new ComparisonUtils();

    @Test
    void fetchAssociatedUsersWithNullInputsReturnsNull(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        Assertions.assertNull( associationsService.fetchAssociatedUsers( null, companyDetails,true, 15, 0 ) );
        Assertions.assertNull( associationsService.fetchAssociatedUsers( "111111", null,true, 15, 0 ) );
    }

    @Test
    void fetchAssociatedUsersWithIncludeRemovedTrueDoesNotApplyFilter(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" ) );
        associationsService.fetchAssociatedUsers( "111111", companyDetails, true, 20, 0 );
        final var expectedAssociationIds = List.of( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" );
        Mockito.verify( associationsListMappers ).daoToDto( argThat( comparisonUtils.associationsPageMatches(16, 1, 16, expectedAssociationIds ) ), isNull(), eq( companyDetails ) );
    }

    @Test
    void fetchAssociatedUsersWithIncludeRemovedFalseAppliesFilter(){
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" ) );
        associationsService.fetchAssociatedUsers( "111111", companyDetails, false, 20, 0 );
        final var expectedAssociationIds = List.of( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13" );
        Mockito.verify( associationsListMappers ).daoToDto( argThat( comparisonUtils.associationsPageMatches(13, 1, 13, expectedAssociationIds ) ), isNull(), eq( companyDetails ) );
    }

    @Test
    void fetchAssociatedUsersAppliesPaginationCorrectly() {
        final var companyDetails = testDataManager.fetchCompanyDetailsDtos( "111111" ).getFirst();
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16" ) );
        associationsService.fetchAssociatedUsers("111111", companyDetails, true, 15, 1);
        Mockito.verify( associationsListMappers ).daoToDto(argThat(comparisonUtils.associationsPageMatches(16, 2, 1, List.of("16"))), isNull(), eq(companyDetails));
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
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33" ) );
        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 1, 15, null );
        Mockito.verify( associationsListMappers ).daoToDto(argThat(comparisonUtils.associationsPageMatches(16, 2, 1, List.of("33"))), eq(user), isNull() );
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersByCompanyNumber(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = List.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue(), StatusEnum.REMOVED.getValue() );
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33" ) );
        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, 15, "333333" );
        Mockito.verify( associationsListMappers ).daoToDto(argThat(comparisonUtils.associationsPageMatches(2, 1, 2, List.of("18", "27"))), eq(user), isNull());
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyFiltersBasedOnStatus(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        final var status = List.of( StatusEnum.REMOVED.getValue() );
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33" ) );
        associationsService.fetchAssociationsForUserStatusAndCompany( user, status, 0, 15, null );
        Mockito.verify( associationsListMappers ).daoToDto(argThat(comparisonUtils.associationsPageMatches(3, 1, 3, List.of("31", "32", "33"))), eq(user), isNull());
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNullStatusDefaultsToConfirmedStatus(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33" ) );
        associationsService.fetchAssociationsForUserStatusAndCompany( user, null, 0, 15, null );
        Mockito.verify( associationsListMappers ).daoToDto(argThat(comparisonUtils.associationsPageMatches(5, 1, 5, List.of("18", "19", "20", "21", "22"))), eq(user), isNull());
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithEmptyStatusDefaultsToConfirmedStatus(){
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "18", "19", "20", "21", "22", "23", "24", "25", "26", "27", "28", "29", "30", "31", "32", "33" ) );
        associationsService.fetchAssociationsForUserStatusAndCompany( user, Collections.emptyList(), 0, 15, null );
        Mockito.verify( associationsListMappers ).daoToDto(argThat(comparisonUtils.associationsPageMatches(5, 1, 5, List.of("18", "19", "20", "21", "22"))), eq(user), isNull());
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithInvalidStatusReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        associationsService.fetchAssociationsForUserStatusAndCompany( user, List.of( "complicated" ), 0, 15, null );
        Mockito.verify( associationsListMappers ).daoToDto(argThat(comparisonUtils.associationsPageMatches(0, 0, 0, Collections.emptyList())), eq(user), isNull());
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNonexistentOrInvalidCompanyNumberReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        associationsService.fetchAssociationsForUserStatusAndCompany( user, Collections.emptyList(), 0, 15, "$$1234" );
        Mockito.verify( associationsListMappers ).daoToDto(argThat(comparisonUtils.associationsPageMatches(0, 0, 0, Collections.emptyList())), eq(user), isNull());
    }

    @Test
    void fetchAssociationsForUserStatusAndCompanyWithNonexistentUserIdReturnsEmptyPage() {
        final var user = testDataManager.fetchUserDtos( "9999" ).getFirst();
        associationsService.fetchAssociationsForUserStatusAndCompany( user, Collections.emptyList(), 0, 15, "1234" );
        Mockito.verify( associationsListMappers ).daoToDto(argThat(comparisonUtils.associationsPageMatches(0, 0, 0, Collections.emptyList())), eq(user), isNull());
    }

    @Test
    void confirmedAssociationExistsWithNullOrMalformedOrNonExistentCompanyNumberOrUserReturnsFalse(){
        Assertions.assertFalse( associationsService.confirmedAssociationExists( null, "111" ) );
        Assertions.assertFalse( associationsService.confirmedAssociationExists( "$$$$$$", "111" ) );
        Assertions.assertFalse( associationsService.confirmedAssociationExists( "919191", "111" ) );
        Assertions.assertFalse( associationsService.confirmedAssociationExists( "111111", null ) );
        Assertions.assertFalse( associationsService.confirmedAssociationExists( "111111", "$$$" ) );
        Assertions.assertFalse( associationsService.confirmedAssociationExists( "111111", "9191" ) );
    }

    @Test
    void associationExistsWithExistingConfirmedAssociationReturnsTrue(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        Assertions.assertTrue( associationsService.confirmedAssociationExists( "111111", "111" ) );
    }

    @Test
    void createAssociationWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociation( null, "000", "the.void@space.com", ApprovalRouteEnum.AUTH_CODE ,"111") );
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociation( "000000", null, null, ApprovalRouteEnum.AUTH_CODE ,"111") );
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociation( "000000", "000", "the.void@space.com", null ,"111") );
        Assertions.assertThrows(NullPointerException.class, () -> associationsService.createAssociation( "000000", "000", "the.void@space.com", ApprovalRouteEnum.INVITATION ,null) );
    }

    @Test
    void createAssociationWithUserIdAndAuthCodeSuccessfullyCreatesOrUpdateAssociation(){
        final var association = associationsService.createAssociation( "111111", "000", null, ApprovalRouteEnum.AUTH_CODE, null);
        Assertions.assertEquals( "111111", association.getCompanyNumber() );
        Assertions.assertEquals( "000", association.getUserId() );
        Assertions.assertNull( association.getUserEmail() );
        Assertions.assertEquals( StatusEnum.CONFIRMED.getValue(), association.getStatus() );
        Assertions.assertEquals( ApprovalRouteEnum.AUTH_CODE.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getEtag() );
    }

    @Test
    void createAssociationWithUserIdAndInvitationSuccessfullyCreatesOrUpdateAssociation(){
        final var association = associationsService.createAssociation( "111111", "000", null, ApprovalRouteEnum.INVITATION, "111");
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertEquals( "111111", association.getCompanyNumber() );
        Assertions.assertEquals( "000", association.getUserId() );
        Assertions.assertNull( association.getUserEmail() );
        Assertions.assertEquals( StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus() );
        Assertions.assertEquals( ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getEtag() );
        Assertions.assertNotNull( association.getApprovalExpiryAt() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertEquals( "111", invitation.getInvitedBy() );
    }

    @Test
    void createAssociationWithUserEmailAndAuthCodeSuccessfullyCreatesOrUpdateAssociation(){
        final var association = associationsService.createAssociation( "111111", null, "bruce.wayne@gotham.city", ApprovalRouteEnum.AUTH_CODE, null);

        Assertions.assertEquals( "111111", association.getCompanyNumber() );
        Assertions.assertNull( association.getUserId() );
        Assertions.assertEquals( "bruce.wayne@gotham.city", association.getUserEmail() );
        Assertions.assertEquals( StatusEnum.CONFIRMED.getValue(), association.getStatus() );
        Assertions.assertEquals( ApprovalRouteEnum.AUTH_CODE.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getEtag() );
    }

    @Test
    void createAssociationWithUserEmailAndInvitationSuccessfullyCreatesOrUpdateAssociation(){
        final var association = associationsService.createAssociation( "111111", null, "bruce.wayne@gotham.city", ApprovalRouteEnum.INVITATION, "111");
        final var invitation = association.getInvitations().getFirst();

        Assertions.assertEquals( "111111", association.getCompanyNumber() );
        Assertions.assertNull( association.getUserId() );
        Assertions.assertEquals( "bruce.wayne@gotham.city", association.getUserEmail() );
        Assertions.assertEquals( StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus() );
        Assertions.assertEquals( ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute() );
        Assertions.assertNotNull( association.getEtag() );
        Assertions.assertNotNull( association.getApprovalExpiryAt() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertEquals( "111", invitation.getInvitedBy() );
    }

    @Test
    void sendNewInvitationWithNullInputsThrowsNullPointerException(){
        final var association = testDataManager.fetchAssociationDaos( "1" ).getFirst();

        Assertions.assertThrows( NullPointerException.class, () -> associationsService.sendNewInvitation( null, association ) );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.sendNewInvitation( "111", null ) );
    }

    @Test
    void sendNewInvitationWithNonexistentAssociationCreatesNewAssociation(){
        final var association = testDataManager.fetchAssociationDaos( "36" ).getFirst();

        final var updatedAssociation = associationsService.sendNewInvitation( "9999", association );
        final var invitation = updatedAssociation.getInvitations().getFirst();

        Assertions.assertEquals( "444444", association.getCompanyNumber() );
        Assertions.assertNull( association.getUserId() );
        Assertions.assertEquals( "light.yagami@death.note", association.getUserEmail() );
        Assertions.assertEquals( ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute() );
        Assertions.assertEquals( StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus() );
        Assertions.assertNotNull( association.getEtag() );
        Assertions.assertNotNull( association.getApprovalExpiryAt() );
        Assertions.assertNotNull( invitation.getInvitedAt() );
        Assertions.assertEquals( "9999", invitation.getInvitedBy() );
    }

    @Test
    void sendNewInvitationWithExistingAssociationCreatesNewInvitation(){
        final var association = testDataManager.fetchAssociationDaos( "36" ).getFirst();
        associationsRepository.insert( association );

        final var updatedAssociation = associationsService.sendNewInvitation( "222", association );
        final var invitations = updatedAssociation.getInvitations();
        final var firstInvitation = invitations.getFirst();
        final var secondInvitation = invitations.getLast();

        Assertions.assertEquals( "444444", association.getCompanyNumber() );
        Assertions.assertNull(  association.getUserId() );
        Assertions.assertEquals( "light.yagami@death.note", association.getUserEmail() );
        Assertions.assertEquals( ApprovalRouteEnum.INVITATION.getValue(), association.getApprovalRoute() );
        Assertions.assertEquals( StatusEnum.AWAITING_APPROVAL.getValue(), association.getStatus() );
        Assertions.assertNotNull( association.getEtag() );
        Assertions.assertNotNull( association.getApprovalExpiryAt() );
        Assertions.assertNotNull( firstInvitation.getInvitedAt() );
        Assertions.assertEquals( "9999", firstInvitation.getInvitedBy() );
        Assertions.assertNotNull( secondInvitation.getInvitedAt() );
        Assertions.assertEquals( "222", secondInvitation.getInvitedBy() );
    }

    @Test
    void updateAssociationStatusWithMalformedOrNonexistentAssociationIdThrowsInternalServerError(){
        final var update = new Update();
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> associationsService.updateAssociation( "$$$", update) );
        Assertions.assertThrows( InternalServerErrorRuntimeException.class, () -> associationsService.updateAssociation( "9191", update) );
    }

    @Test
    void updateAssociationStatusWithNullAssociationIdOrUserIdOrNullStatusThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.updateAssociation( null, null) );
        Assertions.assertThrows( NullPointerException.class, () -> associationsService.updateAssociation( "1",  null) );
    }

    @Test
    void updateAssociationStatusWithRemovedStatusAndSwapUserEmailForUserIdSetToFalseUpdatesAssociationCorrectly(){
        final var oldAssociationData = testDataManager.fetchAssociationDaos( "1" ).getFirst();
        associationsRepository.insert( oldAssociationData );
        associationsService.updateAssociation( "1", new Update().set("removed_at", LocalDateTime.now()).set("status","removed"));
        final var newAssociationData = associationsRepository.findById("1").get();
        Assertions.assertTrue( comparisonUtils.compare( oldAssociationData, List.of( "approvedAt", "userEmail", "userId" ), List.of( "removedAt", "etag", "status" ), Map.of( "approvedAt", new ReduceTimeStampResolutionPreprocessor() ) ).matches( newAssociationData ) );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserIdWithNullOrMalformedOrNonexistentCompanyNumberReturnsNothing(){
        Assertions.assertTrue( associationsService.fetchAssociationForCompanyNumberAndUserId( null, "111" ).isEmpty() );
        Assertions.assertTrue( associationsService.fetchAssociationForCompanyNumberAndUserId( "$$$$$$", "111" ).isEmpty() );
        Assertions.assertTrue( associationsService.fetchAssociationForCompanyNumberAndUserId( "919191", "111" ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserIdWithMalformedOrNonexistentUserIdReturnsNothing() {
        Assertions.assertTrue( associationsService.fetchAssociationForCompanyNumberAndUserId( "111111", "$$$" ).isEmpty() );
        Assertions.assertTrue( associationsService.fetchAssociationForCompanyNumberAndUserId( "111111", "9191" ).isEmpty() );
    }

    @Test
    void fetchAssociationForCompanyNumberAndUserIdShouldFetchAssociation(){
        associationsRepository.insert( testDataManager.fetchAssociationDaos( "1" ) );
        Assertions.assertEquals( "1", associationsService.fetchAssociationForCompanyNumberAndUserId( "111111", "111" ).get().getId() );
    }

    @Test
    void fetchActiveInvitationsWithNullOrMalformedOrNonexistentUserIdReturnsEmptyList(){
        Assertions.assertEquals( Collections.emptyList(), associationsService.fetchActiveInvitations( new User(), 0, 1 ).getItems() );
        Assertions.assertEquals( Collections.emptyList(), associationsService.fetchActiveInvitations( new User().userId("$$$"), 0, 1 ).getItems() );
        Assertions.assertEquals( Collections.emptyList(), associationsService.fetchActiveInvitations( new User().userId("9191"), 0, 1 ).getItems() );
    }

    @Test
    void fetchActiveInvitationsReturnsPaginatedResultsInCorrectOrderAndOnlyRetainsMostRecentInvitationPerAssociation(){
        final var user = testDataManager.fetchUserDtos( "000" ).getFirst();
        final var firstAssociation = testDataManager.fetchAssociationDaos( "37" ).getFirst();
        final var secondAssociation = testDataManager.fetchAssociationDaos( "38" ).getFirst();
        secondAssociation.setUserId("000");

        associationsRepository.insert( List.of( firstAssociation, secondAssociation ) );

        final var mostRecentInvitationDaoInFirstAssociation = firstAssociation.getInvitations().getLast();
        final var mostRecentInvitationDtoInFirstAssociation = new Invitation().invitedAt( mostRecentInvitationDaoInFirstAssociation.getInvitedAt().toString() ).invitedBy( mostRecentInvitationDaoInFirstAssociation.getInvitedBy() );
        Mockito.doReturn( Stream.of( mostRecentInvitationDtoInFirstAssociation ) ).when( invitationsMapper ).daoToDto( argThat( comparisonUtils.compare( firstAssociation, List.of( "id" ), List.of(), Map.of() ) ) );

        final var mostRecentInvitationDaoInSecondAssociation = secondAssociation.getInvitations().getLast();
        final var mostRecentInvitationDtoInSecondAssociation = new Invitation().invitedAt( mostRecentInvitationDaoInSecondAssociation.getInvitedAt().toString() ).invitedBy( mostRecentInvitationDaoInSecondAssociation.getInvitedBy() );
        Mockito.doReturn( Stream.of( mostRecentInvitationDtoInSecondAssociation ) ).when( invitationsMapper ).daoToDto( argThat( comparisonUtils.compare( secondAssociation, List.of( "id" ), List.of(), Map.of() ) ) );

        final var invitations = associationsService.fetchActiveInvitations( user, 1, 1 );
        final var invitation = invitations.getItems().getFirst();

        Assertions.assertEquals( mostRecentInvitationDaoInFirstAssociation.getInvitedBy(), invitation.getInvitedBy() );
        Assertions.assertEquals( localDateTimeToNormalisedString( mostRecentInvitationDaoInFirstAssociation.getInvitedAt() ), reduceTimestampResolution( invitation.getInvitedAt() ) );
    }

    @AfterEach
    public void after() {
        mongoTemplate.dropCollection(AssociationDao.class);
    }

}
