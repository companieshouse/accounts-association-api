package uk.gov.companieshouse.accounts.association.models.email.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.InviteCancelledEmailData;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class InviteCancelledEmailBuilderTest {

    @Test
    void buildInstantiatesEmailData(){
        final var expectedEmailData = new InviteCancelledEmailData();
        expectedEmailData.setTo("kpatel@companieshouse.gov.uk");
        expectedEmailData.setSubject("Companies House: authorisation to file online for Tesla cancelled");
        expectedEmailData.setCompanyName("Tesla");
        expectedEmailData.setCancelledBy("Elon Musk");

        final var actualEmailData = new InviteCancelledEmailBuilder()
                .setRecipientEmail("kpatel@companieshouse.gov.uk")
                .setCompanyName("Tesla")
                .setCancelledBy("Elon Musk")
                .build();

        Assertions.assertEquals(expectedEmailData, actualEmailData);
    }

    @Test
    void buildWithNullsThrowsNullPointerException(){
        final var inviteCancelledEmailBuilder = new InviteCancelledEmailBuilder();
        Assertions.assertThrows(NullPointerException.class, inviteCancelledEmailBuilder::build);
    }

}
