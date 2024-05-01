package uk.gov.companieshouse.accounts.association.models.email.senders;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;
import uk.gov.companieshouse.email_producer.model.EmailData;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public abstract class EmailSender {

    private final EmailProducer emailProducer;

    protected static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    public EmailSender( EmailProducer emailProducer ){
        this.emailProducer = emailProducer;
    }

    protected void sendEmail(final EmailData emailData, final String messageType ) throws EmailSendingException {
        try {
            emailProducer.sendEmail(emailData, messageType);
            LOG.debug(String.format("Submitted %s email to Kafka", messageType));
        } catch (EmailSendingException exception) {
            LOG.error("Error sending email", exception);
            throw exception;
        }
    }

    protected void sendEmailToUsersAssociatedWithCompany( final String xRequestId, final CompanyDetails companyDetails, final Consumer<String> sendEmailForEmailAddress, List<Supplier<User>> requestsToFetchAssociatedUsers ){
        final var companyNumber = companyDetails.getCompanyNumber();
        LOG.debugContext( xRequestId, String.format( "Attempting to send notifications to users from company %s", companyNumber ), null );
        requestsToFetchAssociatedUsers.stream()
                .map( Supplier::get )
                .map( User::getEmail )
                .forEach( sendEmailForEmailAddress );
        LOG.debugContext( xRequestId, String.format( "Successfully sent notifications to users from company %s", companyNumber ), null );
    }


}
