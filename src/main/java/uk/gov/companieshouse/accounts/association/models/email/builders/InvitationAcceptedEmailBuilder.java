package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.accounts.association.models.email.data.InvitationAcceptedEmailData;

import java.util.Objects;

public class InvitationAcceptedEmailBuilder extends EmailBuilder<InvitationAcceptedEmailBuilder, InvitationAcceptedEmailData> {

    private String inviterDisplayName;

    private String inviteeDisplayName;

    public InvitationAcceptedEmailBuilder setInviterDisplayName( final String inviterDisplayName ) {
        this.inviterDisplayName = inviterDisplayName;
        return this;
    }

    public InvitationAcceptedEmailBuilder setInviteeDisplayName( final String inviteeDisplayName ) {
        this.inviteeDisplayName = inviteeDisplayName;
        return this;
    }

    @Override
    protected InvitationAcceptedEmailBuilder self(){
        return this;
    }

    @Override
    public InvitationAcceptedEmailData build() {
        if (Objects.isNull(recipientEmail) || Objects.isNull(inviterDisplayName) || Objects.isNull(inviteeDisplayName) || Objects.isNull(companyName)) {
            throw new NullPointerException("recipientEmail, inviterDisplayName, inviteeDisplayName, and companyName cannot be null");
        }

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
