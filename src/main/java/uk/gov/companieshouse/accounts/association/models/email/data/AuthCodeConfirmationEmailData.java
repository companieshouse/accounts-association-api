package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class AuthCodeConfirmationEmailData extends EmailData {

    private String authorisedPerson;
    private String companyName;

    public AuthCodeConfirmationEmailData(){}

    public AuthCodeConfirmationEmailData(String authorisedPerson, String companyName) {
        this.authorisedPerson = authorisedPerson;
        this.companyName = companyName;
    }

    public void setAuthorisedPerson(String authorisedPerson) {
        this.authorisedPerson = authorisedPerson;
    }

    public String getAuthorisedPerson() {
        return authorisedPerson;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyName() {
        return companyName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AuthCodeConfirmationEmailData that)) {
            return false;
        }

        return new EqualsBuilder().append(authorisedPerson,
                that.authorisedPerson).append(companyName, that.companyName).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(authorisedPerson).append(companyName)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "AuthCodeConfirmationEmailData{" +
                "authorisedPerson='" + authorisedPerson + '\'' +
                ", companyName='" + companyName + '\'' +
                '}';
    }

}
