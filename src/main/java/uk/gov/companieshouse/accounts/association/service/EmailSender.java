package uk.gov.companieshouse.accounts.association.service;

import org.springframework.context.annotation.ComponentScan;
import uk.gov.companieshouse.accounts.association.utils.MessageType;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;
import uk.gov.companieshouse.email_producer.model.EmailData;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@ComponentScan("uk.gov.companieshouse.email_producer")
public abstract class EmailSender {

    private final EmailProducer emailProducer;

    protected static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    public EmailSender( EmailProducer emailProducer ){
        this.emailProducer = emailProducer;
    }


    protected void sendEmail(final EmailData emailData, final MessageType messageType ) throws EmailSendingException {
        try {
            emailProducer.sendEmail(emailData, messageType.getMessageType());
        } catch (EmailSendingException exception) {
            LOG.errorContext(messageType.getMessageType(), String.format("Failed to send email due to %s", exception.getMessage()), exception, null);
            throw exception;
        }
    }

}
