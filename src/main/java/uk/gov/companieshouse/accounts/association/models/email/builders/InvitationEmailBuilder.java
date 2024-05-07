package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.accounts.association.models.email.data.InvitationEmailData;
import uk.gov.companieshouse.email_producer.model.EmailData;

import java.util.Objects;

public class InvitationEmailBuilder extends EmailBuilder {
    private String inviterDisplayName;

    private String inviteeDisplayName;

    public void setInviterDisplayName(String inviterDisplayName) {
        this.inviterDisplayName = inviterDisplayName;
    }

    public void setInviteeDisplayName(String inviteeDisplayName) {
        this.inviteeDisplayName = inviteeDisplayName;
    }

    public EmailData buildEmailData() {
        {
            if (Objects.isNull(recipientEmail) || Objects.isNull(inviterDisplayName) || Objects.isNull(inviteeDisplayName) || Objects.isNull(companyName)) {
                throw new NullPointerException("recipientEmail, inviterDisplayName, inviteeDisplayName, and companyName cannot be null");
            }

            return getInvitationEmailData();
        }
    }

    private InvitationEmailData getInvitationEmailData() {
        final var subject = String.format("Companies House: %s invited to be authorised to file online for %s", inviteeDisplayName, companyName);

        final var emailData = new InvitationEmailData();
        emailData.setTo(recipientEmail);
        emailData.setSubject(subject);
        emailData.setPersonWhoCreatedInvite(inviterDisplayName);
        emailData.setInvitee(inviteeDisplayName);
        emailData.setCompanyName(companyName);

        return emailData;
    }

}
