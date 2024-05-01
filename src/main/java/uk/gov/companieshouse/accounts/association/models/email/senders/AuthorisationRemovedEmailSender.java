package uk.gov.companieshouse.accounts.association.models.email.senders;

import static uk.gov.companieshouse.accounts.association.utils.Constants.AUTHORISATION_REMOVED_MESSAGE_TYPE;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import uk.gov.companieshouse.accounts.association.models.email.data.AuthorisationRemovedEmailData;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;

public class AuthorisationRemovedEmailSender extends EmailSender {

    public AuthorisationRemovedEmailSender( EmailProducer emailProducer ) {
        super( emailProducer );
    }

    protected Consumer<String> sendEmail( final String removedByDisplayName, final String removedUserDisplayName, final String companyName ){
        return recipientEmail -> {
            if ( Objects.isNull( recipientEmail ) || Objects.isNull( removedByDisplayName ) || Objects.isNull( removedUserDisplayName ) || Objects.isNull( companyName ) ){
                LOG.error( "Attempted to send email where recipientEmail, removedByDisplayName, removedUserDisplayName, or companyName was null" );
                throw new NullPointerException( "recipientEmail, removedByDisplayName, removedUserDisplayName, and companyName cannot be null" );
            }

            final var subject = String.format( "Companies House: %s's authorisation removed to file online for %s", removedUserDisplayName, companyName );

            final var emailData = new AuthorisationRemovedEmailData();
            emailData.setTo( recipientEmail );
            emailData.setSubject( subject );
            emailData.setPersonWhoRemovedAuthorisation( removedByDisplayName );
            emailData.setPersonWhoWasRemoved( removedUserDisplayName );
            emailData.setCompanyName( companyName );

            LOG.debug( String.format( "Sending email entitled '%s' to '%s'", subject, recipientEmail ) );
            sendEmail( emailData, AUTHORISATION_REMOVED_MESSAGE_TYPE );
        };
    }

    public void sendEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String removedByDisplayName, final String removedUserDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers ) {
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendEmail( removedByDisplayName, removedUserDisplayName, companyDetails.getCompanyName() ), requestsToFetchAssociatedUsers );
    }

}
