package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.accounts.association.models.email.data.AuthCodeConfirmationEmailData;
import uk.gov.companieshouse.email_producer.model.EmailData;

import java.util.Objects;

public class AuthCodeConfirmationEmailBuilder extends EmailBuilder {

    private String displayName;


    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public EmailData buildEmailData() {

        if (Objects.isNull(recipientEmail) || Objects.isNull(displayName) || Objects.isNull(companyName)) {
            throw new NullPointerException("recipientEmail, displayName, and companyName cannot be null");
        }

        return getAuthCodeConfirmationEmailData();

    }

    private AuthCodeConfirmationEmailData getAuthCodeConfirmationEmailData() {
        final var subject = String.format("Companies House: %s is now authorised to file online for %s", displayName, companyName);

        final var emailData = new AuthCodeConfirmationEmailData();
        emailData.setTo(recipientEmail);
        emailData.setSubject(subject);
        emailData.setAuthorisedPerson(displayName);
        emailData.setCompanyName(companyName);

        return emailData;
    }

    ;


}
