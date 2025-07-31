package uk.gov.companieshouse.accounts.association.model.email.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.AuthCodeConfirmationEmailData;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class AuthCodeConfirmedEmailDataTest {

    @Test
    void toNotificationSentLoggingMessageComputesCorrectMessage(){
        final var message = new AuthCodeConfirmationEmailData( "buzz.lightyear@toystory.com", "Woody", "Netflix" ).getSubject();
        Assertions.assertEquals( "Companies House: Woody is now authorised to file online for Netflix", message );
    }

}
