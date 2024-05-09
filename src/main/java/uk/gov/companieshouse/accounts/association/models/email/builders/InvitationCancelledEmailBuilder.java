package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.accounts.association.models.email.data.InvitationCancelledEmailData;

import java.util.Objects;

public class InvitationCancelledEmailBuilder extends EmailBuilder<InvitationCancelledEmailBuilder, InvitationCancelledEmailData> {

    private String cancelledByDisplayName;

    private String cancelledUserDisplayName;

    public InvitationCancelledEmailBuilder setCancelledByDisplayName( final String cancelledByDisplayName ) {
        this.cancelledByDisplayName = cancelledByDisplayName;
        return this;
    }

    public InvitationCancelledEmailBuilder setCancelledUserDisplayName( final String cancelledUserDisplayName ) {
        this.cancelledUserDisplayName = cancelledUserDisplayName;
        return this;
    }

    @Override
    protected InvitationCancelledEmailBuilder self(){
        return this;
    }

    @Override
    public InvitationCancelledEmailData build() {
        if (Objects.isNull(recipientEmail) || Objects.isNull(cancelledByDisplayName) || Objects.isNull(cancelledUserDisplayName) || Objects.isNull(companyName)) {
            throw new NullPointerException("recipientEmail, cancelledByDisplayName, cancelledUserDisplayName, and companyName cannot be null");
        }

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
