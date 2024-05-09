package uk.gov.companieshouse.accounts.association.models.email.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationRejectedEmailData;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class InvitationRejectedEmailBuilderTest {

    @Test
    void buildInstantiatesEmailData() {
        final var expectedEmailData = new InvitationRejectedEmailData();
        expectedEmailData.setTo( "kpatel@companieshouse.gov.uk" );
        expectedEmailData.setSubject( "Companies House: Elon Musk has declined to be digitally authorised to file online for Tesla" );
        expectedEmailData.setPersonWhoDeclined( "Elon Musk" );
        expectedEmailData.setCompanyName( "Tesla" );

        final var actualEmailData = new InvitationRejectedEmailBuilder()
                .setRecipientEmail( "kpatel@companieshouse.gov.uk" )
                .setInviteeDisplayName( "Elon Musk" )
                .setCompanyName( "Tesla" )
                .build();

        Assertions.assertEquals( expectedEmailData, actualEmailData );
    }

    @Test
    void buildWithNullsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> new InvitationRejectedEmailBuilder().build() );
    }

}
