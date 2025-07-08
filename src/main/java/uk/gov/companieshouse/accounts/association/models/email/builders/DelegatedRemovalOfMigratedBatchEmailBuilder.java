package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.accounts.association.models.email.data.DelegatedRemovalOfMigratedBatchEmailData;

import java.util.Objects;

public class DelegatedRemovalOfMigratedBatchEmailBuilder extends EmailBuilder<DelegatedRemovalOfMigratedBatchEmailBuilder, DelegatedRemovalOfMigratedBatchEmailData> {

    private String removedBy;
    private String removedUser;

    public DelegatedRemovalOfMigratedBatchEmailBuilder setRemovedBy( final String removedBy ) {
        this.removedBy = removedBy;
        return this;
    }

    public DelegatedRemovalOfMigratedBatchEmailBuilder setRemovedUser( final String removedUser ) {
        this.removedUser = removedUser;
        return this;
    }

    @Override
    protected DelegatedRemovalOfMigratedBatchEmailBuilder self(){ return this;}

    @Override
    public DelegatedRemovalOfMigratedBatchEmailData build() {
        if (Objects.isNull(recipientEmail) || Objects.isNull(removedBy) || Objects.isNull(removedUser) || Objects.isNull(companyName)) {
            throw new NullPointerException("recipientEmail, removedBy, removedUser, and companyName cannot be null");
        }

        final var subject = String.format("Companies House: %s's digital authorisation not restored for %s", removedUser, companyName);

        final var emailData = new DelegatedRemovalOfMigratedBatchEmailData();
        emailData.setTo(recipientEmail);
        emailData.setSubject(subject);
        emailData.setRemovedBy(removedBy);
        emailData.setRemovedUser(removedUser);
        emailData.setCompanyName(companyName);

        return emailData;
    }
}
