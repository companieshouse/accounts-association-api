package uk.gov.companieshouse.accounts.association.models;

import static uk.gov.companieshouse.accounts.association.utils.Constants.AUTHORISATION_REMOVED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.AUTH_CODE_CONFIRMATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_ACCEPTED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_CANCELLED_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_MESSAGE_TYPE;
import static uk.gov.companieshouse.accounts.association.utils.Constants.INVITATION_REJECTED_MESSAGE_TYPE;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import uk.gov.companieshouse.accounts.association.models.email.data.AuthCodeConfirmationEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.AuthorisationRemovedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationAcceptedEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationCancelledEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationEmailData;
import uk.gov.companieshouse.accounts.association.models.email.data.InvitationRejectedEmailData;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.email_producer.model.EmailData;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class NotificationLog {

    private String notificationType;
    private String sentTo;
    private String sentTimestamp;
    private String companyName;

    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    private final Map<String, Consumer<EmailData>> fromEmailData = Map.of(
            AUTH_CODE_CONFIRMATION_MESSAGE_TYPE, emailData -> companyName = ((AuthCodeConfirmationEmailData) emailData).getCompanyName(),
            AUTHORISATION_REMOVED_MESSAGE_TYPE, emailData -> companyName = ((AuthorisationRemovedEmailData) emailData).getCompanyName(),
            INVITATION_CANCELLED_MESSAGE_TYPE, emailData -> companyName = ((InvitationCancelledEmailData) emailData).getCompanyName(),
            INVITATION_MESSAGE_TYPE, emailData -> companyName = ((InvitationEmailData) emailData).getCompanyName(),
            INVITATION_ACCEPTED_MESSAGE_TYPE, emailData -> companyName = ((InvitationAcceptedEmailData) emailData).getCompanyName(),
            INVITATION_REJECTED_MESSAGE_TYPE, emailData -> companyName = ((InvitationRejectedEmailData) emailData).getCompanyName()
    );

    public NotificationLog( EmailData emailData, String messageType ){
        notificationType = messageType;
        sentTo = emailData.getTo();
        sentTimestamp = LocalDateTime.now().toString();
        fromEmailData.getOrDefault( messageType, theEmailData -> {} ).accept( emailData );
    }

    public void print(){
        var logMessage = String.format( "%s notification sent to %s at %s.", notificationType, sentTo, sentTimestamp );
        logMessage += Objects.isNull( companyName ) ? "" : String.format( " This was in relation to company %s.", companyName );
        LOG.debug( logMessage );
    }

}
