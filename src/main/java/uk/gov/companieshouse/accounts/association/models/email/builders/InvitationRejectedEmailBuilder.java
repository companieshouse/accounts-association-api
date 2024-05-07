package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.accounts.association.models.email.data.InvitationRejectedEmailData;

import java.util.Objects;

public class InvitationRejectedEmailBuilder extends EmailBuilder<InvitationRejectedEmailBuilder, InvitationRejectedEmailData> {

    private String inviteeDisplayName;

    public InvitationRejectedEmailBuilder setInviteeDisplayName( final String inviteeDisplayName ) {
        this.inviteeDisplayName = inviteeDisplayName;
        return this;
    }

    @Override
    protected InvitationRejectedEmailBuilder self(){
        return this;
    }

    @Override
    public InvitationRejectedEmailData build() {
        if (Objects.isNull(recipientEmail) || Objects.isNull(inviteeDisplayName) || Objects.isNull(companyName)) {
            throw new NullPointerException("recipientEmail, inviteeDisplayName, and companyName cannot be null");
        }

        final var subject = String.format("Companies House: %s has declined to be digitally authorised to file online for %s", inviteeDisplayName, companyName);

        final var emailData = new InvitationRejectedEmailData();
        emailData.setTo( recipientEmail );
        emailData.setSubject( subject );
        emailData.setPersonWhoDeclined( inviteeDisplayName );
        emailData.setCompanyName( companyName );

        return emailData;
    }

}
