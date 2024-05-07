package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.accounts.association.models.email.data.InvitationCancelledEmailData;
import uk.gov.companieshouse.email_producer.model.EmailData;

import java.util.Objects;

public class InvitationCancelledEmailBuilder extends EmailBuilder {

    private String cancelledByDisplayName;

    private String cancelledUserDisplayName;

    public void setCancelledByDisplayName(String cancelledByDisplayName) {
        this.cancelledByDisplayName = cancelledByDisplayName;
    }

    public void setCancelledUserDisplayName(String cancelledUserDisplayName) {
        this.cancelledUserDisplayName = cancelledUserDisplayName;
    }

    public EmailData buildEmailData() {

        if (Objects.isNull(recipientEmail) || Objects.isNull(cancelledByDisplayName) || Objects.isNull(cancelledUserDisplayName) || Objects.isNull(companyName)) {
            throw new NullPointerException("recipientEmail, cancelledByDisplayName, cancelledUserDisplayName, and companyName cannot be null");
        }

        return getInvitationCancelledEmailData(cancelledByDisplayName, cancelledUserDisplayName, companyName);
    }

    private InvitationCancelledEmailData getInvitationCancelledEmailData(String cancelledByDisplayName, String cancelledUserDisplayName, String companyName) {
        final var subject = String.format("Companies House: Invitation cancelled for %s to be authorised to file online for %s", cancelledUserDisplayName, companyName);

        final var emailData = new InvitationCancelledEmailData();
        emailData.setTo(recipientEmail);
        emailData.setSubject(subject);
        emailData.setPersonWhoCancelledInvite(cancelledByDisplayName);
        emailData.setPersonWhoWasCancelled(cancelledUserDisplayName);
        emailData.setCompanyName(companyName);
        return emailData;
    }

}
