package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.accounts.association.models.email.data.AuthorisationRemovedEmailData;
import uk.gov.companieshouse.email_producer.model.EmailData;

import java.util.Objects;

public class AuthorisationRemovedEmailBuilder extends EmailBuilder {

    private String removedByDisplayName;
    private String removedUserDisplayName;

    public void setRemovedByDisplayName(String removedByDisplayName) {
        this.removedByDisplayName = removedByDisplayName;
    }

    public void setRemovedUserDisplayName(String removedUserDisplayName) {
        this.removedUserDisplayName = removedUserDisplayName;
    }

    @Override
    public EmailData buildEmailData() {

        if (Objects.isNull(recipientEmail) || Objects.isNull(removedByDisplayName) || Objects.isNull(removedUserDisplayName) || Objects.isNull(companyName)) {
            throw new NullPointerException("recipientEmail, removedByDisplayName, removedUserDisplayName, and companyName cannot be null");
        }

        return getAuthorisationRemovedEmailData();
    }

    private AuthorisationRemovedEmailData getAuthorisationRemovedEmailData() {
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
