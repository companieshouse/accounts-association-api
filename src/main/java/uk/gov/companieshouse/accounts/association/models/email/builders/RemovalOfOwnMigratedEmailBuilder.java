package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.accounts.association.models.email.data.RemovalOfOwnMigratedEmailData;

import java.util.Objects;

public class RemovalOfOwnMigratedEmailBuilder extends EmailBuilder<RemovalOfOwnMigratedEmailBuilder, RemovalOfOwnMigratedEmailData> {

    @Override
    protected RemovalOfOwnMigratedEmailBuilder self() { return this; }

    @Override
    public RemovalOfOwnMigratedEmailData build() {
        if (Objects.isNull(recipientEmail) || Objects.isNull(companyName)) {
            throw new NullPointerException("recipientEmail and companyName cannot be null");
        }

        final var subject = String.format("Companies House: authorisation to file online for %s not restored", companyName);

        final var emailData = new RemovalOfOwnMigratedEmailData();
        emailData.setTo(recipientEmail);
        emailData.setSubject(subject);
        emailData.setCompanyName(companyName);

        return emailData;
    }
}
