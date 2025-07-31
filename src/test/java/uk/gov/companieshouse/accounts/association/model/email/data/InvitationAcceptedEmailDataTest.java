package uk.gov.companieshouse.accounts.association.model.email.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationAcceptedEmailData;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class InvitationAcceptedEmailDataTest {

    @Test
    void toNotificationSentLoggingMessageComputesCorrectMessage(){
        final var message = new InvitationAcceptedEmailData( "buzz.lightyear@toystory.com", "Woody", "Netflix", "Buzz" ).getSubject();
        Assertions.assertEquals( "Companies House: Buzz is now authorised to file online for Netflix", message );
    }

}
