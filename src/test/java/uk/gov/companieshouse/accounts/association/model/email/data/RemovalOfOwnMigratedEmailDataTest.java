package uk.gov.companieshouse.accounts.association.model.email.data;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.RemovalOfOwnMigratedEmailData;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class RemovalOfOwnMigratedEmailDataTest {

    @Test
    void toNotificationSentLoggingMessageComputesCorrectMessage(){
        final var message = new RemovalOfOwnMigratedEmailData("buzz.lightyear@toystory.com", "Netflix"  ).getSubject();
        Assertions.assertEquals( "Companies House: authorisation to file online for Netflix not restored", message );
    }

}
