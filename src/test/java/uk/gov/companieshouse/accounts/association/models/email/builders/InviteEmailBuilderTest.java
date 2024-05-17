package uk.gov.companieshouse.accounts.association.models.email.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.InviteEmailData;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class InviteEmailBuilderTest {

    @Test
    void buildInstantiatesEmailData() {
        final var expectedEmailData = new InviteEmailData();
        expectedEmailData.setTo( "kpatel@companieshouse.gov.uk" );
        expectedEmailData.setSubject( "Companies House: invitation to be authorised to file online for Tesla" );
        expectedEmailData.setInviterDisplayName( "Krishna Patel" );
        expectedEmailData.setCompanyName( "Tesla" );
        expectedEmailData.setInvitationExpiryTimestamp( "1992-05-01T10:30:00.000000" );
        expectedEmailData.setInvitationLink( "https://companieshouse/authorised-person-confirmation-5xbk3ft88" );

        final var actualEmailData = new InviteEmailBuilder()
                .setRecipientEmail( "kpatel@companieshouse.gov.uk" )
                .setInviterDisplayName( "Krishna Patel" )
                .setCompanyName( "Tesla" )
                .setInvitationExpiryTimestamp( "1992-05-01T10:30:00.000000" )
                .setInvitationLink( "https://companieshouse/authorised-person-confirmation-5xbk3ft88" )
                .build();

        Assertions.assertEquals( expectedEmailData, actualEmailData );
    }

    @Test
    void buildWithNullsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> new InviteEmailBuilder().build() );
    }

}
