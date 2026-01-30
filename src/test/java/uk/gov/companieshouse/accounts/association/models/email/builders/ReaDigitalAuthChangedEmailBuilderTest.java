package uk.gov.companieshouse.accounts.association.models.email.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.ReaDigitalAuthChangedEmailData;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class ReaDigitalAuthChangedEmailBuilderTest {

    @Test
    void buildInstantiatesEmailData() {
        final var builder = new ReaDigitalAuthChangedEmailBuilder()
                .setRecipientEmail("rea@example.com")
                .setCompanyName("Test Enterprises")
                .setCompanyNumber("111111");

        final ReaDigitalAuthChangedEmailData actual = builder.build();

        Assertions.assertEquals("rea@example.com", actual.getTo());
        Assertions.assertEquals("Test Enterprises", actual.getCompanyName());
        Assertions.assertEquals("111111", actual.getCompanyNumber());

        Assertions.assertNotNull(actual.getSubject());
        final String subjectLower = actual.getSubject().toLowerCase();
        Assertions.assertTrue(subjectLower.contains("digitally authorised"));
        Assertions.assertTrue(actual.getSubject().contains("Test Enterprises"));
    }

    @Test
    void buildWithNullsThrowsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, new ReaDigitalAuthChangedEmailBuilder()::build);
    }
}
