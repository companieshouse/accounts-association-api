package uk.gov.companieshouse.accounts.association.service;

import static org.mockito.ArgumentMatchers.any;
import static uk.gov.companieshouse.accounts.association.utils.MessageType.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;
import uk.gov.companieshouse.email_producer.model.EmailData;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class EmailSenderTest {

    @Mock
    EmailProducer emailProducer;

    @InjectMocks
    EmailService emailService;

    @Test
    void sendEmailThrowsDataOnKafkaQueue(){
        final var email = new EmailData();
        email.setTo( "Jordan Peterson" );
        email.setSubject( "Hello!" );

        emailService.sendEmail( email, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE );
        Mockito.verify( emailProducer ).sendEmail( email, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE.getMessageType() );
    }

    @Test
    void sendEmailWithUnexpectedIssueThrowsEmailSendingException(){
        final var email = new EmailData();
        email.setTo( "Jordan Peterson" );
        email.setSubject( "Hello!" );

        Mockito.doThrow( new EmailSendingException( "Failed to send email", new Exception() ) ).when( emailProducer ).sendEmail( any(), any() );
        Assertions.assertThrows( EmailSendingException.class, () -> emailService.sendEmail( email, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE ) );
    }

}