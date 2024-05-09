package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class InvitationRejectedEmailData extends EmailData {

    private String personWhoDeclined;

    private String companyName;

    public InvitationRejectedEmailData(){}

    public InvitationRejectedEmailData(String personWhoDeclined, String companyName) {
        this.personWhoDeclined = personWhoDeclined;
        this.companyName = companyName;
    }


    public void setPersonWhoDeclined(String personWhoDeclined) {
        this.personWhoDeclined = personWhoDeclined;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getPersonWhoDeclined() {
        return personWhoDeclined;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof InvitationRejectedEmailData that)) {
            return false;
        }

        return new EqualsBuilder().append(personWhoDeclined,
                that.personWhoDeclined).append(companyName, that.companyName).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(personWhoDeclined).append(companyName)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "InvitationRejectedEmailData{" +
                "personWhoDeclined='" + personWhoDeclined + '\'' +
                ", companyName='" + companyName + '\'' +
                '}';
    }

}
