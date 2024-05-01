package uk.gov.companieshouse.accounts.association.models.email.senders;

import static org.mockito.ArgumentMatchers.any;
import static uk.gov.companieshouse.accounts.association.utils.Constants.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.AuthCodeConfirmationEmailData;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class AuthCodeConfirmationEmailSenderTest {

    @Mock
    EmailProducer emailProducer;

    AuthCodeConfirmationEmailSender authCodeConfirmationEmailSender;

    @BeforeEach
    public void setup(){
        authCodeConfirmationEmailSender = new AuthCodeConfirmationEmailSender( emailProducer );
    }

    @Test
    void sendAuthCodeConfirmationEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> authCodeConfirmationEmailSender.sendEmail( "Krishna Patel", "Tesla" ).accept( null ) );
        Assertions.assertThrows( NullPointerException.class, () -> authCodeConfirmationEmailSender.sendEmail( (String) null, "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> authCodeConfirmationEmailSender.sendEmail( "Krishna Patel", null ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendAuthCodeConfirmationEmailWithUnexpectedIssueThrowsBadRequestRuntimeException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> authCodeConfirmationEmailSender.sendEmail( "Krishna Patel", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendAuthCodeConfirmationEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new AuthCodeConfirmationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Krishna Patel is now authorised to file online for Tesla" );
        emailData.setAuthorisedPerson( "Krishna Patel" );
        emailData.setCompanyName( "Tesla" );

        authCodeConfirmationEmailSender.sendEmail( "Krishna Patel", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" );
        Mockito.verify( emailProducer ).sendEmail( emailData, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE );
    }

}
