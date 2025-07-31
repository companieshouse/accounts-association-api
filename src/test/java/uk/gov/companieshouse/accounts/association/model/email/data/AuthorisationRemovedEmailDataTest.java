package uk.gov.companieshouse.accounts.association.model.email.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.AuthorisationRemovedEmailData;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class AuthorisationRemovedEmailDataTest {

    @Test
    void toNotificationSentLoggingMessageComputesCorrectMessage(){
        final var message = new AuthorisationRemovedEmailData( "buzz.lightyear@toystory.com", "Woody", "Netflix" , "Buzz").getSubject();
        Assertions.assertEquals( "Companies House: Woody's authorisation removed to file online for Netflix", message );
    }

}
