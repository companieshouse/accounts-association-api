package uk.gov.companieshouse.accounts.association.models.email.senders;

import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_CANCELLED_MESSAGE_TYPE;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationCancelledEmailData;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;

public class InvitationCancelledEmailSender extends EmailSender {

    public InvitationCancelledEmailSender( EmailProducer emailProducer ) {
        super( emailProducer );
    }

    protected Consumer<String> sendEmail( final String cancelledByDisplayName, final String cancelledUserDisplayName, final String companyName ) {
        return recipientEmail -> {
            if ( Objects.isNull( recipientEmail ) || Objects.isNull( cancelledByDisplayName ) || Objects.isNull( cancelledUserDisplayName ) || Objects.isNull( companyName ) ){
                LOG.error( "Attempted to send email where recipientEmail, cancelledByDisplayName, cancelledUserDisplayName, or companyName was null" );
                throw new NullPointerException( "recipientEmail, cancelledByDisplayName, cancelledUserDisplayName, and companyName cannot be null" );
            }

            final var subject = String.format( "Companies House: Invitation cancelled for %s to be authorised to file online for %s", cancelledUserDisplayName, companyName );

            final var emailData = new InvitationCancelledEmailData();
            emailData.setTo( recipientEmail );
            emailData.setSubject( subject );
            emailData.setPersonWhoCancelledInvite( cancelledByDisplayName );
            emailData.setPersonWhoWasCancelled( cancelledUserDisplayName );
            emailData.setCompanyName( companyName );

            LOG.debug( String.format( "Sending email entitled '%s' to '%s'", subject, recipientEmail ) );
            sendEmail( emailData, INVITATION_CANCELLED_MESSAGE_TYPE );
        };
    }

    public void sendEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String cancelledByDisplayName, final String cancelledUserDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers ){
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendEmail( cancelledByDisplayName, cancelledUserDisplayName, companyDetails.getCompanyName() ), requestsToFetchAssociatedUsers );
    }

}
