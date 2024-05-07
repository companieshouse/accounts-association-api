package uk.gov.companieshouse.accounts.association.models.email.builders;

import java.util.Objects;

import uk.gov.companieshouse.accounts.association.models.email.data.InvitationAcceptedEmailData;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class InvitationAcceptedEmailBuilder extends EmailBuilder {


    private String inviterDisplayName;


    private String inviteeDisplayName;

    public void setInviterDisplayName(String inviterDisplayName) {
        this.inviterDisplayName = inviterDisplayName;
    }

    public void setInviteeDisplayName(String inviteeDisplayName) {
        this.inviteeDisplayName = inviteeDisplayName;
    }

    @Override
    public EmailData buildEmailData() {

        if (Objects.isNull(recipientEmail) || Objects.isNull(inviterDisplayName) || Objects.isNull(inviteeDisplayName) || Objects.isNull(companyName)) {
            throw new NullPointerException("recipientEmail, inviterDisplayName, inviteeDisplayName, and companyName cannot be null");
        }

        return getInvitationAcceptedEmailData();
    }

    private InvitationAcceptedEmailData getInvitationAcceptedEmailData() {
        final var subject = String.format("Companies House: %s is now authorised to file online for %s", inviteeDisplayName, companyName);

        final var emailData = new InvitationAcceptedEmailData();
        emailData.setTo(recipientEmail);
        emailData.setSubject(subject);
        emailData.setPersonWhoCreatedInvite(inviterDisplayName);
        emailData.setAuthorisedPerson(inviteeDisplayName);
        emailData.setCompanyName(companyName);

        return emailData;
    }


}
