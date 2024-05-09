package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.email_producer.model.EmailData;

public abstract class EmailBuilder<T extends EmailBuilder<T, U>, U extends EmailData> {

    protected String companyName;

    protected String recipientEmail;

    public T setCompanyName( final String companyName ) {
        this.companyName = companyName;
        return self();
    }

    public T setRecipientEmail( final String recipientEmail ) {
        this.recipientEmail = recipientEmail;
        return self();
    }

    protected abstract T self();

    public abstract U build();

}
