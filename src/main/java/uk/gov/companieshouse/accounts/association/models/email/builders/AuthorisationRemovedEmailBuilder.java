package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.accounts.association.models.email.data.AuthorisationRemovedEmailData;

import java.util.Objects;

public class AuthorisationRemovedEmailBuilder extends EmailBuilder<AuthorisationRemovedEmailBuilder, AuthorisationRemovedEmailData> {

    private String removedByDisplayName;
    private String removedUserDisplayName;

    public AuthorisationRemovedEmailBuilder setRemovedByDisplayName( final String removedByDisplayName ) {
        this.removedByDisplayName = removedByDisplayName;
        return this;
    }

    public AuthorisationRemovedEmailBuilder setRemovedUserDisplayName( final String removedUserDisplayName ) {
        this.removedUserDisplayName = removedUserDisplayName;
        return this;
    }

    @Override
    protected AuthorisationRemovedEmailBuilder self(){
        return this;
    }

    @Override
    public AuthorisationRemovedEmailData build() {
        if (Objects.isNull(recipientEmail) || Objects.isNull(removedByDisplayName) || Objects.isNull(removedUserDisplayName) || Objects.isNull(companyName)) {
            throw new NullPointerException("recipientEmail, removedByDisplayName, removedUserDisplayName, and companyName cannot be null");
        }

        final var subject = String.format("Companies House: %s's authorisation removed to file online for %s", removedUserDisplayName, companyName);

        final var emailData = new AuthorisationRemovedEmailData();
        emailData.setTo(recipientEmail);
        emailData.setSubject(subject);
        emailData.setPersonWhoRemovedAuthorisation(removedByDisplayName);
        emailData.setPersonWhoWasRemoved(removedUserDisplayName);
        emailData.setCompanyName(companyName);

        return emailData;
    }


}
