package uk.gov.companieshouse.accounts.association.model.email.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import uk.gov.companieshouse.accounts.association.models.email.data.YourAuthorisationRemovedEmailData;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
public class YourAuthorisationRemovedEmailDataTest {

    @Test
    void toNotificationSentLoggingMessageComputesCorrectMessage(){
        final var message = new YourAuthorisationRemovedEmailData( "buzz.lightyear@toystory.com", "Netflix", "Buzz" ).getSubject();
        Assertions.assertEquals( "Companies House: Authorisation removed to file online for Netflix", message );
    }
}
