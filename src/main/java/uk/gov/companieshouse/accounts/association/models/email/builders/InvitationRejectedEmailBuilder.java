package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.accounts.association.models.email.data.InvitationRejectedEmailData;
import uk.gov.companieshouse.email_producer.model.EmailData;

import java.util.Objects;

public class InvitationRejectedEmailBuilder extends EmailBuilder {

    private String inviteeDisplayName;

    public void setInviteeDisplayName(String inviteeDisplayName) {
        this.inviteeDisplayName = inviteeDisplayName;
    }

    public EmailData buildEmailData() {

        if (Objects.isNull(recipientEmail) || Objects.isNull(inviteeDisplayName) || Objects.isNull(companyName)) {
            throw new NullPointerException("recipientEmail, inviteeDisplayName, and companyName cannot be null");
        }

        return getInvitationRejectedEmailData();
    }

    private InvitationRejectedEmailData getInvitationRejectedEmailData() {
        final var subject = String.format("Companies House: %s has declined to be digitally authorised to file online for %s", inviteeDisplayName, companyName);

        final var emailData = new InvitationRejectedEmailData();
        emailData.setTo(recipientEmail);
        emailData.setSubject(subject);
        emailData.setPersonWhoDeclined(inviteeDisplayName);
        emailData.setCompanyName(companyName);
        return emailData;
    }


}
