package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Email payload for notifying the Registered Email Address (REA) of a change in
 * digital authorisation status for a company.
 */
public class ReaDigitalAuthChangedEmailData extends EmailData {

    private String companyName;
    private String companyNumber;

    public ReaDigitalAuthChangedEmailData(){}

    public ReaDigitalAuthChangedEmailData(String companyName, String companyNumber) {
        this.companyName = companyName;
        this.companyNumber = companyNumber;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReaDigitalAuthChangedEmailData that)) return false;
        return new EqualsBuilder()
                .append(companyName, that.companyName)
                .append(companyNumber, that.companyNumber)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(companyName)
                .append(companyNumber)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "ReaDigitalAuthChangedEmailData{" +
                "companyName='" + companyName + '\'' +
                ", companyNumber='" + companyNumber + '\'' +
                '}';
    }
}
