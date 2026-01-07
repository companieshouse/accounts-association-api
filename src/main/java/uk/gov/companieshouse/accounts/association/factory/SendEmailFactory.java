package uk.gov.companieshouse.accounts.association.factory;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.models.email.data.EmailData;
import uk.gov.companieshouse.api.chskafka.SendEmail;

import java.util.UUID;

import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonFrom;

@Component
public class SendEmailFactory {
    private final String appId;

    SendEmailFactory(@Value("${email.appId}") String appId) {
        this.appId = appId;
    }

    public SendEmail createSendEmail(EmailData emailData, String messageType) {
        var sendEmail = new SendEmail();
        sendEmail.jsonData(parseJsonFrom(emailData, ""));
        sendEmail.setEmailAddress(emailData.getTo());
        sendEmail.setAppId(appId);
        sendEmail.setMessageId(UUID.randomUUID().toString());
        sendEmail.setMessageType(messageType);
        return sendEmail;
    }

}