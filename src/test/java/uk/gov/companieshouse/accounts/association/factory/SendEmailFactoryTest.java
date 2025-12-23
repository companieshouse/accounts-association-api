package uk.gov.companieshouse.accounts.association.factory;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.accounts.association.models.email.data.EmailData;
import uk.gov.companieshouse.api.chskafka.SendEmail;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Tag("unit-test")
class SendEmailFactoryTest {

    private SendEmailFactory sendEmailFactory;
    private static final String APP_ID = "test-app-id";

    @BeforeEach
    void setUp() {
        sendEmailFactory = new SendEmailFactory(APP_ID);
    }

    @Test
    void createSendEmailCreatesSendEmailCorrectly() {
        // Arrange
        String emailAddress = "test@example.com";
        String messageType = "test type";

        EmailData emailData = new EmailData();
        emailData.setTo(emailAddress);

        // Act
        SendEmail sendEmail = sendEmailFactory.createSendEmail(emailData, messageType);

        // Assert
        assertNotNull(sendEmail);
        assertEquals(APP_ID, sendEmail.getAppId());
        assertEquals(emailAddress, sendEmail.getEmailAddress());
        assertEquals(messageType, sendEmail.getMessageType());
        assertNotNull(sendEmail.getMessageId());
        assertNotNull(sendEmail.getJsonData());
    }
}