package uk.gov.companieshouse.accounts.association.models.email.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.DelegatedRemovalOfMigratedEmailData;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class DelegatedRemovalOfMigratedEmailBuilderTest {

    @Test
    void buildInstantiatesEmailData() {
        final var expectedEmailData = new DelegatedRemovalOfMigratedEmailData();
        expectedEmailData.setTo("kpatel@companieshouse.gov.uk");
        expectedEmailData.setSubject("Companies House: authorisation to file online for Tesla not restored");
        expectedEmailData.setRemovedBy("Krishna Patel");
        expectedEmailData.setCompanyName("Tesla");

        final var actualEmailData = new DelegatedRemovalOfMigratedEmailBuilder()
                .setRecipientEmail("kpatel@companieshouse.gov.uk")
                .setRemovedBy("Krishna Patel")
                .setCompanyName("Tesla")
                .build();

        Assertions.assertEquals(expectedEmailData, actualEmailData);
    }

    @Test
    void buildWithNullsThrowsNullPointerException(){
        final var builder = new DelegatedRemovalOfMigratedEmailBuilder();
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }
}
