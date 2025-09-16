package uk.gov.companieshouse.accounts.association.models.email.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.RemovalOfOwnMigratedEmailData;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class RemovalOfOwnMigratedEmailBuilderTest {

    @Test
    void buildInstantiatesEmailData() {
        final var expectedEmailData = new RemovalOfOwnMigratedEmailData();
        expectedEmailData.setTo("kpatel@companieshouse.gov.uk");
        expectedEmailData.setSubject("Companies House: authorisation to file online for Tesla not restored");
        expectedEmailData.setCompanyName("Tesla");

        final var actualEmailData = new RemovalOfOwnMigratedEmailBuilder()
                .setRecipientEmail("kpatel@companieshouse.gov.uk")
                .setCompanyName("Tesla")
                .build();

        Assertions.assertEquals(expectedEmailData, actualEmailData);
    }

    @Test
    void buildWithNullsThrowsNullPointerException() {
        final var builder = new RemovalOfOwnMigratedEmailBuilder();
        Assertions.assertThrows(NullPointerException.class, builder::build);
    }
}
