package uk.gov.companieshouse.accounts.association.models.email;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class InvitationRejectedEmailData extends EmailData {

    private String declinedAuthorisationPerson;

    private String companyName;

    public InvitationRejectedEmailData(){}

    public InvitationRejectedEmailData(String declinedAuthorisationPerson, String companyName) {
        this.declinedAuthorisationPerson = declinedAuthorisationPerson;
        this.companyName = companyName;
    }


    public void setDeclinedAuthorisationPerson(String declinedAuthorisationPerson) {
        this.declinedAuthorisationPerson = declinedAuthorisationPerson;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getDeclinedAuthorisationPerson() {
        return declinedAuthorisationPerson;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof InvitationRejectedEmailData that)) {
            return false;
        }

        return new EqualsBuilder().append(declinedAuthorisationPerson,
                that.declinedAuthorisationPerson).append(companyName, that.companyName).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(declinedAuthorisationPerson).append(companyName)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "InvitationRejectedEmailData{" +
                "declinedAuthorisationPerson='" + declinedAuthorisationPerson + '\'' +
                ", companyName='" + companyName + '\'' +
                '}';
    }

}
