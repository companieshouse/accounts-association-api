package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.accounts.association.models.email.data.YourAuthorisationRemovedEmailData;

import java.util.Objects;

public class YourAuthorisationRemovedEmailBuilder extends EmailBuilder<YourAuthorisationRemovedEmailBuilder, YourAuthorisationRemovedEmailData> {

    private String removedByDisplayName;

    public YourAuthorisationRemovedEmailBuilder setRemovedByDisplayName(final String removedByDisplayName ) {
        this.removedByDisplayName = removedByDisplayName;
        return this;
    }

    @Override
    protected YourAuthorisationRemovedEmailBuilder self(){
        return this;
    }

    @Override
    public YourAuthorisationRemovedEmailData build() {
        if (Objects.isNull(recipientEmail) || Objects.isNull(removedByDisplayName) || Objects.isNull(companyName)) {
            throw new NullPointerException("recipientEmail, removedByDisplayName, removedUserDisplayName, and companyName cannot be null");
        }

        final var subject = String.format("Companies House: Authorisation removed to file online for %s", companyName);

        final var emailData = new YourAuthorisationRemovedEmailData();
        emailData.setTo(recipientEmail);
        emailData.setSubject(subject);
        emailData.setPersonWhoRemovedAuthorisation(removedByDisplayName);
        emailData.setCompanyName(companyName);

        return emailData;
    }


}
