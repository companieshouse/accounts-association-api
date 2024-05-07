package uk.gov.companieshouse.accounts.association.models.email.builders;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.AuthCodeConfirmationEmailData;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class AuthCodeConfirmationEmailBuilderTest {

    @Test
    void buildInstantiatesEmailData(){
        final var expectedEmailData = new AuthCodeConfirmationEmailData();
        expectedEmailData.setTo( "kpatel@companieshouse.gov.uk" );
        expectedEmailData.setSubject( "Companies House: Krishna Patel is now authorised to file online for Tesla" );
        expectedEmailData.setAuthorisedPerson( "Krishna Patel" );
        expectedEmailData.setCompanyName( "Tesla" );

        final var actualEmailData = new AuthCodeConfirmationEmailBuilder()
                .setRecipientEmail( "kpatel@companieshouse.gov.uk" )
                .setDisplayName( "Krishna Patel" )
                .setCompanyName( "Tesla" )
                .build();

        Assertions.assertEquals( expectedEmailData, actualEmailData );
    }

    @Test
    void buildWithNullsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> new AuthCodeConfirmationEmailBuilder().build() );
    }

}
