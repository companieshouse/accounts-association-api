package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.email_producer.model.EmailData;

public abstract class EmailBuilder {

    protected   String companyName;

    protected   String recipientEmail;

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void setRecipientEmail(String recipientEmail) {
        this.recipientEmail = recipientEmail;
    }

    public abstract EmailData buildEmailData();

}
