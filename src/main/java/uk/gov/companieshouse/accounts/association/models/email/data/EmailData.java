package uk.gov.companieshouse.accounts.association.models.email.data;

public class EmailData {
    private String to;
    private String subject;

    public EmailData() {
    }

    public String getTo() {
        return this.to;
    }

    public String getSubject() {
        return this.subject;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }
}