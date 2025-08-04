package uk.gov.companieshouse.accounts.association.model.email.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.InviteEmailData;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class InviteEmailDataTest {

    @Test
    void toNotificationSentLoggingMessageComputesCorrectMessage(){
        final var message = new InviteEmailData( "buzz.lightyear@toystory.com", "Woody", "Netflix", "", "" ).getSubject(); //TODO: what data should i fill in here
        Assertions.assertEquals( "Companies House: Woody invited to be authorised to file online for Netflix", message );
    }

}
