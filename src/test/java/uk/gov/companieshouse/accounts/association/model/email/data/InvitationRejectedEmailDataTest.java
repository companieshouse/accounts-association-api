package uk.gov.companieshouse.accounts.association.model.email.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationRejectedEmailData;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class InvitationRejectedEmailDataTest {

    @Test
    void toNotificationSentLoggingMessageComputesCorrectMessage(){
        final var message = new InvitationRejectedEmailData( "buzz.lightyear@toystory.com", "Woody", "Netflix" ).getSubject();
        Assertions.assertEquals( "Companies House: Woody has declined to be digitally authorised to file online for Netflix", message );
    }

}
