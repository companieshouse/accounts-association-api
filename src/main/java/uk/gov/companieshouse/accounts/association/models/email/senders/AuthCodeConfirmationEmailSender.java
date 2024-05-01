package uk.gov.companieshouse.accounts.association.models.email.senders;

import static uk.gov.companieshouse.accounts.association.utils.Constants.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import uk.gov.companieshouse.accounts.association.models.email.data.AuthCodeConfirmationEmailData;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;

public class AuthCodeConfirmationEmailSender extends EmailSender {

    public AuthCodeConfirmationEmailSender( EmailProducer emailProducer ){
        super( emailProducer );
    }

    protected Consumer<String> sendEmail( final String displayName, final String companyName ){
        return recipientEmail -> {
            if ( Objects.isNull( recipientEmail ) || Objects.isNull( displayName ) || Objects.isNull( companyName ) ){
                LOG.error( "Attempted to send email where recipientEmail, displayName, or companyName was null" );
                throw new NullPointerException( "recipientEmail, displayName, and companyName cannot be null" );
            }

            final var subject = String.format( "Companies House: %s is now authorised to file online for %s", displayName, companyName );

            final var emailData = new AuthCodeConfirmationEmailData();
            emailData.setTo( recipientEmail );
            emailData.setSubject( subject );
            emailData.setAuthorisedPerson( displayName );
            emailData.setCompanyName( companyName );

            LOG.debug( String.format( "Sending email entitled '%s' to '%s'", subject, recipientEmail ) );
            sendEmail( emailData, AUTH_CODE_CONFIRMATION_MESSAGE_TYPE);
        };
    }

    public void sendEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String displayName, final List<Supplier<User>> requestsToFetchAssociatedUsers ){
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendEmail( displayName, companyDetails.getCompanyName() ), requestsToFetchAssociatedUsers );
    }

}
