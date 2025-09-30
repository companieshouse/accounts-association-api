package uk.gov.companieshouse.accounts.association.models.email.builders;

import java.util.Objects;
import uk.gov.companieshouse.accounts.association.models.email.data.DelegatedRemovalOfMigratedEmailData;

public class DelegatedRemovalOfMigratedEmailBuilder extends EmailBuilder<DelegatedRemovalOfMigratedEmailBuilder, DelegatedRemovalOfMigratedEmailData > {

    private String removedBy;

    public DelegatedRemovalOfMigratedEmailBuilder setRemovedBy( final String removedBy ){
        this.removedBy = removedBy;
        return this;
    }

    @Override
    protected DelegatedRemovalOfMigratedEmailBuilder self() { return this; }

    @Override
    public DelegatedRemovalOfMigratedEmailData build() {
        if (Objects.isNull(recipientEmail) || Objects.isNull(removedBy) || Objects.isNull(companyName)) {
            throw new NullPointerException("recipientEmail, removedBy, and companyName cannot be null");
        }

        final var subject = String.format("Companies House: authorisation to file online for %s not restored", companyName);

        final var emailData = new DelegatedRemovalOfMigratedEmailData();
        emailData.setTo(recipientEmail);
        emailData.setSubject(subject);
        emailData.setRemovedBy(removedBy);
        emailData.setCompanyName(companyName);

        return emailData;
    }

}
