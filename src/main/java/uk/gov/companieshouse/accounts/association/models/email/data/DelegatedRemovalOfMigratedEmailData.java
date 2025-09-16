package uk.gov.companieshouse.accounts.association.models.email.data;

import uk.gov.companieshouse.email_producer.model.EmailData;

import java.util.Objects;

public class DelegatedRemovalOfMigratedEmailData extends EmailData {

    private String removedBy;

    private String companyName;


    public DelegatedRemovalOfMigratedEmailData() {}

    public DelegatedRemovalOfMigratedEmailData(String removedBy, String companyName) {
        this.removedBy = removedBy;
        this.companyName = companyName;
    }

    public void setRemovedBy(String removedBy) { this.removedBy = removedBy; }

    public String getRemovedBy() { return removedBy; }

    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCompanyName() { return companyName; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DelegatedRemovalOfMigratedEmailData that = (DelegatedRemovalOfMigratedEmailData) o;
        return Objects.equals(removedBy, that.removedBy) && Objects.equals(companyName, that.companyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(removedBy, companyName);
    }

    @Override
    public String toString() {
        return "DelegatedRemovalOfMigratedEmailData{" +
                "removedBy='" + removedBy + '\'' +
                ", companyName='" + companyName + '\'' +
                '}';
    }
}
