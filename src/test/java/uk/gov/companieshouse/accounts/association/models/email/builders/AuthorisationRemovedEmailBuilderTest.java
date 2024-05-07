package uk.gov.companieshouse.accounts.association.models.email.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.AuthorisationRemovedEmailData;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class AuthorisationRemovedEmailBuilderTest {

    @Test
    void buildInstantiatesEmailData() {
        final var expectedEmailData = new AuthorisationRemovedEmailData();
        expectedEmailData.setTo( "kpatel@companieshouse.gov.uk" );
        expectedEmailData.setSubject( "Companies House: Elon Musk's authorisation removed to file online for Tesla" );
        expectedEmailData.setPersonWhoRemovedAuthorisation( "Krishna Patel" );
        expectedEmailData.setPersonWhoWasRemoved( "Elon Musk" );
        expectedEmailData.setCompanyName( "Tesla" );

        final var actualEmailData = new AuthorisationRemovedEmailBuilder()
                .setRecipientEmail( "kpatel@companieshouse.gov.uk" )
                .setRemovedByDisplayName( "Krishna Patel" )
                .setRemovedUserDisplayName( "Elon Musk" )
                .setCompanyName( "Tesla" )
                .build();

        Assertions.assertEquals( expectedEmailData, actualEmailData );
    }

    @Test
    void buildWithNullsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> new AuthorisationRemovedEmailBuilder().build() );
    }

}
