package uk.gov.companieshouse.accounts.association.models.email.senders;

import static org.mockito.ArgumentMatchers.any;
import static uk.gov.companieshouse.accounts.association.utils.Constants.AUTHORISATION_REMOVED_MESSAGE_TYPE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.AuthorisationRemovedEmailData;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class AuthorisationRemovedEmailSenderTest {

    @Mock
    EmailProducer emailProducer;

    AuthorisationRemovedEmailSender authorisationRemovedEmailSender;

    @BeforeEach
    public void setup(){
        authorisationRemovedEmailSender = new AuthorisationRemovedEmailSender( emailProducer );
    }

    @Test
    void sendAuthorisationRemovedEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> authorisationRemovedEmailSender.sendEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( null) );
        Assertions.assertThrows( NullPointerException.class, () -> authorisationRemovedEmailSender.sendEmail( null, "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> authorisationRemovedEmailSender.sendEmail( "Krishna Patel", null, "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> authorisationRemovedEmailSender.sendEmail(  "Krishna Patel", "Elon Musk", null ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendAuthorisationRemovedEmailWithUnexpectedIssueThrowsBadRequestRuntimeException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> authorisationRemovedEmailSender.sendEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendAuthorisationRemovedEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new AuthorisationRemovedEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk's authorisation removed to file online for Tesla" );
        emailData.setPersonWhoRemovedAuthorisation( "Krishna Patel" );
        emailData.setPersonWhoWasRemoved( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        authorisationRemovedEmailSender.sendEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" );
        Mockito.verify( emailProducer ).sendEmail( emailData, AUTHORISATION_REMOVED_MESSAGE_TYPE );
    }

}
