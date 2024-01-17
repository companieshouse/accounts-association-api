package uk.gov.companieshouse.accounts.association.service;

import org.bson.Document;
import org.junit.jupiter.api.Assertions;
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

}
