package uk.gov.companieshouse.accounts.association.model.email.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.InviteCancelledEmailData;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class InviteCancelledEmailDataTest {

    @Test
    void toNotificationSentLoggingMessageComputesCorrectMessage(){
        final var message = new InviteCancelledEmailData( "buzz.lightyear@toystory.com", "Netflix", "Woody" ).getSubject();
        Assertions.assertEquals( "Companies House: Invitation cancelled for Woody to be authorised to file online for Netflix", message );
    }
}
