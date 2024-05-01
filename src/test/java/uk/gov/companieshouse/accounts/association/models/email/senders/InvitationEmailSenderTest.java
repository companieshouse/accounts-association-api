package uk.gov.companieshouse.accounts.association.models.email.senders;

import static org.mockito.ArgumentMatchers.any;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_MESSAGE_TYPE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationEmailData;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class InvitationEmailSenderTest {

    @Mock
    EmailProducer emailProducer;

    InvitationEmailSender invitationEmailSender;

    @BeforeEach
    public void setup(){
        invitationEmailSender = new InvitationEmailSender( emailProducer );
    }

    @Test
    void sendInvitationEmailWithNullInputsThrowsNullPointerException(){
        Assertions.assertThrows( NullPointerException.class, () -> invitationEmailSender.sendEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( null ) );
        Assertions.assertThrows( NullPointerException.class, () -> invitationEmailSender.sendEmail( null, "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> invitationEmailSender.sendEmail( "Krishna Patel", null, "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
        Assertions.assertThrows( NullPointerException.class, () -> invitationEmailSender.sendEmail( "Krishna Patel", "Elon Musk", null ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendInvitationEmailWithUnexpectedIssueThrowsBadRequestRuntimeException(){
        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> invitationEmailSender.sendEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" ) );
    }

    @Test
    void sendInvitationEmailThrowsMessageOnKafkaQueue(){
        final var emailData = new InvitationEmailData();
        emailData.setTo( "kpatel@companieshouse.gov.uk" );
        emailData.setSubject( "Companies House: Elon Musk invited to be authorised to file online for Tesla" );
        emailData.setPersonWhoCreatedInvite( "Krishna Patel" );
        emailData.setInvitee( "Elon Musk" );
        emailData.setCompanyName( "Tesla" );

        invitationEmailSender.sendEmail( "Krishna Patel", "Elon Musk", "Tesla" ).accept( "kpatel@companieshouse.gov.uk" );
        Mockito.verify( emailProducer ).sendEmail( emailData, INVITATION_MESSAGE_TYPE );
    }

}
