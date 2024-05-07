package uk.gov.companieshouse.accounts.association.models.email.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationAcceptedEmailData;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class InvitationAcceptedEmailBuilderTest {

    @Test
    void buildInstantiatesEmailData() {
        final var expectedEmailData = new InvitationAcceptedEmailData();
        expectedEmailData.setTo( "kpatel@companieshouse.gov.uk" );
        expectedEmailData.setSubject( "Companies House: Elon Musk is now authorised to file online for Tesla" );
        expectedEmailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        expectedEmailData.setAuthorisedPerson( "Elon Musk" );
        expectedEmailData.setCompanyName( "Tesla" );

        final var actualEmailData = new InvitationAcceptedEmailBuilder()
                .setRecipientEmail( "kpatel@companieshouse.gov.uk" )
                .setInviterDisplayName( "Krishna Patel" )
                .setInviteeDisplayName( "Elon Musk" )
                .setCompanyName( "Tesla" )
                .build();

        Assertions.assertEquals( expectedEmailData, actualEmailData );

    }

    @Test
    void buildWithNullsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> new InvitationAcceptedEmailBuilder().build() );
    }


}
