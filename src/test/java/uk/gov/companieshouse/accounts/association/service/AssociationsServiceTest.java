package uk.gov.companieshouse.accounts.association.service;

import java.util.List;
import org.bson.Document;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Update;
import uk.gov.companieshouse.accounts.association.enums.StatusEnum;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AssociationsServiceTest {

    @InjectMocks
    AssociationsService associationsService;

    @Mock
    AssociationsRepository associationsRepository;


    private Association associationOne;
    private Association associationTwo;
    private Association awaitingAssociation;
    private Association removedAssociation;


    @BeforeEach
    public void setup(){
        associationOne = new Association();
        associationOne.setCompanyNumber("111111");
        associationOne.setUserId("111");
        associationOne.setStatus( "Confirmed" );

        associationTwo = new Association();
        associationTwo.setCompanyNumber("222222");
        associationTwo.setUserId("111");
        associationTwo.setStatus( "Confirmed" );

        awaitingAssociation = new Association();
        awaitingAssociation.setCompanyNumber("333333");
        awaitingAssociation.setUserId("111");
        awaitingAssociation.setStatus("Awaiting Confirmation");

        removedAssociation = new Association();
        removedAssociation.setCompanyNumber("444444");
        removedAssociation.setUserId("111");
        removedAssociation.setStatus("Removed");
    }

    @Test
    void getByUserIdAndCompanyNumberWithInvalidUserIdOrCompanyNumberReturnsNothing() {
        Mockito.doReturn(Optional.empty()).when(associationsRepository).findByUserIdAndCompanyNumber(any(), any());

        Assertions.assertFalse(associationsService.getByUserIdAndCompanyNumber(null, "111111").isPresent());
        Assertions.assertFalse(associationsService.getByUserIdAndCompanyNumber("", "111111").isPresent());
        Assertions.assertFalse(associationsService.getByUserIdAndCompanyNumber("abc", "111111").isPresent());
        Assertions.assertFalse(associationsService.getByUserIdAndCompanyNumber("111", null).isPresent());
        Assertions.assertFalse(associationsService.getByUserIdAndCompanyNumber("111", "").isPresent());
        Assertions.assertFalse(associationsService.getByUserIdAndCompanyNumber("111", "abc").isPresent());
    }

    @Test
    void getByUserIdAndCompanyNumberWithNonexistentUserIdOrCompanyNumberReturnsNothing() {
        Mockito.doReturn(Optional.empty()).when(associationsRepository).findByUserIdAndCompanyNumber(any(), any());

        Assertions.assertFalse(associationsService.getByUserIdAndCompanyNumber("333", "111111").isPresent());
        Assertions.assertFalse(associationsService.getByUserIdAndCompanyNumber("111", "333333").isPresent());
    }

    @Test
    void getByUserIdAndCompanyNumberReturnsAssociationWhenExistsOrOtherwiseNothing() {

        Mockito.doReturn(Optional.empty())
                .when(associationsRepository)
                .findByUserIdAndCompanyNumber(any(), any());

        Mockito.doReturn(Optional.of(new Association()))
                .when(associationsRepository)
                .findByUserIdAndCompanyNumber("111", "111111");

        Assertions.assertTrue(associationsService.getByUserIdAndCompanyNumber("111", "111111").isPresent());
        Assertions.assertFalse(associationsService.getByUserIdAndCompanyNumber("222", "222222").isPresent());
    }

    private ArgumentMatcher<Update> associationStatusUpdateParametersMatch(String expectedStatus, String timestampName, Boolean expectedTemporary) {
        return update -> {
            final var document = update.getUpdateObject().get("$set", Document.class);
            final var status = document.get("status");
            final var timestamp = document.getOrDefault(timestampName, null);
            final var temporary = (Boolean) document.getOrDefault("temporary", null);
            return status.equals(expectedStatus) && timestamp != null && temporary == expectedTemporary;
        };
    }

    @Test
    void softDeleteAssociationWithNonExistentUserIdOrCompanyNumberRunsQuery() {
        associationsService.softDeleteAssociation(null, "111111", true);
        Mockito.verify(associationsRepository).updateAssociation(isNull(), eq("111111"), argThat(associationStatusUpdateParametersMatch(StatusEnum.REMOVED.getValue(), "deletionTime", false)));

        associationsService.softDeleteAssociation("", "111111", true);
        Mockito.verify(associationsRepository).updateAssociation(eq(""), eq("111111"), argThat(associationStatusUpdateParametersMatch(StatusEnum.REMOVED.getValue(), "deletionTime", false)));

        associationsService.softDeleteAssociation("333", "111111", true);
        Mockito.verify(associationsRepository).updateAssociation(eq("333"), eq("111111"), argThat(associationStatusUpdateParametersMatch(StatusEnum.REMOVED.getValue(), "deletionTime", false)));

        associationsService.softDeleteAssociation("111", null, true);
        Mockito.verify(associationsRepository).updateAssociation(eq("111"), isNull(), argThat(associationStatusUpdateParametersMatch(StatusEnum.REMOVED.getValue(), "deletionTime", false)));

        associationsService.softDeleteAssociation("111", "", true);
        Mockito.verify(associationsRepository).updateAssociation(eq("111"), eq(""), argThat(associationStatusUpdateParametersMatch(StatusEnum.REMOVED.getValue(), "deletionTime", false)));

        associationsService.softDeleteAssociation("111", "333333", true);
        Mockito.verify(associationsRepository).updateAssociation(eq("111"), eq("333333"), argThat(associationStatusUpdateParametersMatch(StatusEnum.REMOVED.getValue(), "deletionTime", false)));
    }

    @Test
    void softDeleteAssociationWithUserInfoExistsRunsQuery() {
        associationsService.softDeleteAssociation("111", "111111", true);
        Mockito.verify(associationsRepository).updateAssociation(eq("111"), eq("111111"), argThat(associationStatusUpdateParametersMatch(StatusEnum.REMOVED.getValue(), "deletionTime", false)));
    }

    @Test
    void softDeleteAssociationWithUserInfoDoesNotExistRunsQuery() {
        associationsService.softDeleteAssociation("111", "111111", false);
        Mockito.verify(associationsRepository).updateAssociation(eq("111"), eq("111111"), argThat(associationStatusUpdateParametersMatch(StatusEnum.REMOVED.getValue(), "deletionTime", null)));
    }

    @Test
    void confirmAssociationWithNonExistentUserIdOrCompanyNumberRunsQuery() {
        associationsService.confirmAssociation(null, "111111");
        Mockito.verify(associationsRepository).updateAssociation(isNull(), eq("111111"), argThat(associationStatusUpdateParametersMatch(StatusEnum.CONFIRMED.getValue(), "confirmationApprovalTime", false)));

        associationsService.confirmAssociation("", "111111");
        Mockito.verify(associationsRepository).updateAssociation(eq(""), eq("111111"), argThat(associationStatusUpdateParametersMatch(StatusEnum.CONFIRMED.getValue(), "confirmationApprovalTime", false)));

        associationsService.confirmAssociation("333", "111111");
        Mockito.verify(associationsRepository).updateAssociation(eq("333"), eq("111111"), argThat(associationStatusUpdateParametersMatch(StatusEnum.CONFIRMED.getValue(), "confirmationApprovalTime", false)));

        associationsService.confirmAssociation("111", null);
        Mockito.verify(associationsRepository).updateAssociation(eq("111"), isNull(), argThat(associationStatusUpdateParametersMatch(StatusEnum.CONFIRMED.getValue(), "confirmationApprovalTime", false)));

        associationsService.confirmAssociation("111", "");
        Mockito.verify(associationsRepository).updateAssociation(eq("111"), eq(""), argThat(associationStatusUpdateParametersMatch(StatusEnum.CONFIRMED.getValue(), "confirmationApprovalTime", false)));

        associationsService.confirmAssociation("111", "333333");
        Mockito.verify(associationsRepository).updateAssociation(eq("111"), eq("333333"), argThat(associationStatusUpdateParametersMatch(StatusEnum.CONFIRMED.getValue(), "confirmationApprovalTime", false)));
    }

    @Test
    void confirmAssociationRunsQuery() {
        associationsService.confirmAssociation("111", "111111");
        Mockito.verify(associationsRepository).updateAssociation(eq("111"), eq("111111"), argThat(associationStatusUpdateParametersMatch(StatusEnum.CONFIRMED.getValue(), "confirmationApprovalTime", false)));
    }

    @Test
    void findAllByUserIdWithMalformedOrNonexistentUserIdReturnsEmptyList(){
        Mockito.doReturn( List.of() ).when( associationsRepository ).findAllByUserId( any() );
        Mockito.doReturn( List.of() ).when( associationsRepository ).findAllConfirmedAndAwaitingAssociationsByUserId( any() );

        Assertions.assertEquals( List.of(), associationsService.findAllByUserId( null, true ) );
        Assertions.assertEquals( List.of(), associationsService.findAllByUserId( "", true ) );
        Assertions.assertEquals( List.of(), associationsService.findAllByUserId( "abc", true ) );
        Assertions.assertEquals( List.of(), associationsService.findAllByUserId( "333", true ) );
        Assertions.assertEquals( List.of(), associationsService.findAllByUserId( null, false ) );
        Assertions.assertEquals( List.of(), associationsService.findAllByUserId( "", false ) );
        Assertions.assertEquals( List.of(), associationsService.findAllByUserId( "abc", false ) );
        Assertions.assertEquals( List.of(), associationsService.findAllByUserId( "333", false ) );
    }

    @Test
    void findAllByUserIdWithOneRemovedAssociationReturnsUsersAssociationIfIncludeUnauthorisedIsTrueOtherwiseReturnsEmptyList(){
        final var removedAssociation = new Association();
        removedAssociation.setCompanyNumber("333333");
        removedAssociation.setUserId("333");
        removedAssociation.setStatus("Removed");

        Mockito.doReturn( List.of( removedAssociation ) ).when( associationsRepository ).findAllByUserId( any() );
        Mockito.doReturn( List.of() ).when( associationsRepository ).findAllConfirmedAndAwaitingAssociationsByUserId( any() );

        Assertions.assertTrue( associationsService.findAllByUserId( "333", false ).isEmpty() );

        final var associations = associationsService.findAllByUserId( "333", true );
        Assertions.assertEquals( 1, associations.size() );
        Assertions.assertEquals( "333333", associations.get( 0 ).getCompanyNumber() );
    }

    @Test
    void findAllByUserIdWithIncludeUnauthorisedIsTrueAndMultipleAssociationsReturnsAllUsersAssociations(){
        Mockito.doReturn( List.of( associationOne, associationTwo, awaitingAssociation, removedAssociation ) ).when( associationsRepository ).findAllByUserId( any() );

        final var associations = associationsService.findAllByUserId( "111", true );

        Assertions.assertEquals( 4, associations.size() );

        final var companyNumbers =
                associations.stream()
                        .map( Association::getCompanyNumber )
                        .toList();

        Assertions.assertTrue( companyNumbers.containsAll( List.of( "111111", "222222", "333333", "444444" ) ) );
    }

    @Test
    void findAllByUserIdWithUnauthorisedIsFalseAndMultipleAssociationsReturnsAllAwaitingConfirmationAndConfirmedUsersAssociations(){
        Mockito.doReturn( List.of( associationOne, associationTwo, awaitingAssociation ) ).when( associationsRepository ).findAllConfirmedAndAwaitingAssociationsByUserId( any() );

        final var associations = associationsService.findAllByUserId( "111", false );

        Assertions.assertEquals( 3, associations.size() );

        final var companyNumbers =
                associations.stream()
                        .map( Association::getCompanyNumber )
                        .toList();

        Assertions.assertTrue( companyNumbers.containsAll( List.of( "111111", "222222", "333333" ) ) );
    }

}
