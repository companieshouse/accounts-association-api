package uk.gov.companieshouse.accounts.association.model.email.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationCancelledEmailData;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class InvitationCancelledEmailDataTest {

    @Test
    void toNotificationSentLoggingMessageComputesCorrectMessage(){
        final var message = new InvitationCancelledEmailData( "buzz.lightyear@toystory.com", "Buzz", "Netflix" , "Woody").getSubject();
        Assertions.assertEquals( "Companies House: authorisation to file online for Netflix cancelled", message );
    }
}
