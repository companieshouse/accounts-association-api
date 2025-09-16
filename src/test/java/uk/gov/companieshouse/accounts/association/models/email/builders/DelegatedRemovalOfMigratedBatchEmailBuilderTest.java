package uk.gov.companieshouse.accounts.association.models.email.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.DelegatedRemovalOfMigratedBatchEmailData;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class DelegatedRemovalOfMigratedBatchEmailBuilderTest {

    @Test
    void buildInstantiatesEmailData() {
        final var expectedEmailData = new DelegatedRemovalOfMigratedBatchEmailData();
        expectedEmailData.setTo("kpatel@companieshouse.gov.uk");
        expectedEmailData.setSubject("Companies House: Elon Musk's authorisation removed to file online for Tesla");
        expectedEmailData.setRemovedBy("Krishna Patel");
        expectedEmailData.setRemovedUser("Elon Musk");
        expectedEmailData.setCompanyName("Tesla");

        final var actualEmailData = new DelegatedRemovalOfMigratedBatchEmailBuilder()
                .setRecipientEmail("kpatel@companieshouse.gov.uk")
                .setRemovedBy("Krishna Patel")
                .setRemovedUser("Elon Musk")
                .setCompanyName("Tesla")
                .build();

        Assertions.assertEquals(expectedEmailData, actualEmailData);
    }

    @Test
    void buildWithNullsThrowsNullPointerException() {
        final var builder = new DelegatedRemovalOfMigratedBatchEmailBuilder();
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }
}
