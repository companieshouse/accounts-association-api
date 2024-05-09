package uk.gov.companieshouse.accounts.association.models.email.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationCancelledEmailData;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class InvitationCancelledEmailBuilderTest {

    @Test
    void buildInstantiatesEmailData() {
        final var expectedEmailData = new InvitationCancelledEmailData();
        expectedEmailData.setTo( "kpatel@companieshouse.gov.uk" );
        expectedEmailData.setSubject( "Companies House: Invitation cancelled for Elon Musk to be authorised to file online for Tesla" );
        expectedEmailData.setPersonWhoCancelledInvite( "Krishna Patel" );
        expectedEmailData.setPersonWhoWasCancelled( "Elon Musk" );
        expectedEmailData.setCompanyName( "Tesla" );

        final var actualEmailData = new InvitationCancelledEmailBuilder()
                .setRecipientEmail( "kpatel@companieshouse.gov.uk" )
                .setCancelledByDisplayName( "Krishna Patel" )
                .setCancelledUserDisplayName( "Elon Musk" )
                .setCompanyName( "Tesla" )
                .build();

        Assertions.assertEquals( expectedEmailData, actualEmailData );
    }

    @Test
    void buildWithNullsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> new InvitationCancelledEmailBuilder().build() );
    }

}
