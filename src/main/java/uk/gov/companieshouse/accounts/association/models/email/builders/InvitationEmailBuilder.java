package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.accounts.association.models.email.data.InvitationEmailData;

import java.util.Objects;

public class InvitationEmailBuilder extends EmailBuilder<InvitationEmailBuilder, InvitationEmailData> {
    private String inviterDisplayName;

    private String inviteeDisplayName;

    public InvitationEmailBuilder setInviterDisplayName( final String inviterDisplayName ) {
        this.inviterDisplayName = inviterDisplayName;
        return this;
    }

    public InvitationEmailBuilder setInviteeDisplayName( final String inviteeDisplayName ) {
        this.inviteeDisplayName = inviteeDisplayName;
        return this;
    }

    @Override
    protected InvitationEmailBuilder self(){
        return this;
    }

    @Override
    public InvitationEmailData build() {
        if (Objects.isNull(recipientEmail) || Objects.isNull(inviterDisplayName) || Objects.isNull(inviteeDisplayName) || Objects.isNull(companyName)) {
            throw new NullPointerException("recipientEmail, inviterDisplayName, inviteeDisplayName, and companyName cannot be null");
        }

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
