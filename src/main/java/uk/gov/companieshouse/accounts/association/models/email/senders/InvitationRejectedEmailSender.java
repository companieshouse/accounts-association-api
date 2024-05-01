package uk.gov.companieshouse.accounts.association.models.email.senders;

import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_REJECTED_MESSAGE_TYPE;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationRejectedEmailData;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.email_producer.EmailProducer;

public class InvitationRejectedEmailSender extends EmailSender {

    public InvitationRejectedEmailSender( EmailProducer emailProducer ) {
        super( emailProducer );
    }

    protected Consumer<String> sendEmail( final String inviteeDisplayName, final String companyName ){
        return recipientEmail -> {
            if ( Objects.isNull( recipientEmail ) || Objects.isNull( inviteeDisplayName ) || Objects.isNull( companyName ) ){
                LOG.error( "Attempted to send email where recipientEmail, inviteeDisplayName, or companyName was null" );
                throw new NullPointerException( "recipientEmail, inviteeDisplayName, and companyName cannot be null" );
            }

            final var subject = String.format( "Companies House: %s has declined to be digitally authorised to file online for %s", inviteeDisplayName, companyName );

            final var emailData = new InvitationRejectedEmailData();
            emailData.setTo( recipientEmail );
            emailData.setSubject( subject );
            emailData.setPersonWhoDeclined( inviteeDisplayName );
            emailData.setCompanyName( companyName );

            LOG.debug( String.format( "Sending email entitled '%s' to '%s'", subject, recipientEmail ) );
            sendEmail( emailData, INVITATION_REJECTED_MESSAGE_TYPE );
        };
    }

    public void sendEmailToAssociatedUsers( final String xRequestId, final CompanyDetails companyDetails, final String inviteeDisplayName, final List<Supplier<User>> requestsToFetchAssociatedUsers ){
        sendEmailToUsersAssociatedWithCompany( xRequestId, companyDetails, sendEmail( inviteeDisplayName, companyDetails.getCompanyName() ), requestsToFetchAssociatedUsers );
    }

}
