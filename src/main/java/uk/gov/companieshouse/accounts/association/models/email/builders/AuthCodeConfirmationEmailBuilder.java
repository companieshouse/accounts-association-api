package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.accounts.association.models.email.data.AuthCodeConfirmationEmailData;

import java.util.Objects;

public class AuthCodeConfirmationEmailBuilder extends EmailBuilder<AuthCodeConfirmationEmailBuilder, AuthCodeConfirmationEmailData> {

    private String displayName;

    public AuthCodeConfirmationEmailBuilder setDisplayName( final String displayName ){
        this.displayName = displayName;
        return this;
    }

    @Override
    protected AuthCodeConfirmationEmailBuilder self(){
        return this;
    }

    @Override
    public AuthCodeConfirmationEmailData build(){
        if (Objects.isNull(recipientEmail) || Objects.isNull(displayName) || Objects.isNull(companyName)) {
            throw new NullPointerException("recipientEmail, displayName, and companyName cannot be null");
        }

        final var subject = String.format("Companies House: %s is now authorised to file online for %s", displayName, companyName);

        final var emailData = new AuthCodeConfirmationEmailData();
        emailData.setTo(recipientEmail);
        emailData.setSubject(subject);
        emailData.setAuthorisedPerson(displayName);
        emailData.setCompanyName(companyName);

        return emailData;
    }


}
