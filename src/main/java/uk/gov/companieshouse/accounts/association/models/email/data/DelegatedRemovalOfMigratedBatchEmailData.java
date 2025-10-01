package uk.gov.companieshouse.accounts.association.models.email.data;

import java.util.Objects;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class DelegatedRemovalOfMigratedBatchEmailData extends EmailData {

    private String removedBy;

    private String removedUser;

    private String companyName;


    public DelegatedRemovalOfMigratedBatchEmailData(){}

    public DelegatedRemovalOfMigratedBatchEmailData( String removedBy, String removedUser, String companyName ) {
        this.removedBy = removedBy;
        this.removedUser = removedUser;
        this.companyName = companyName;
    }

    public void setRemovedBy(String removedBy) { this.removedBy = removedBy; }

    public String getRemovedBy() { return removedBy; }

    public void setRemovedUser(String removedUser) { this.removedUser = removedUser; }

    public String getRemovedUser() { return removedUser; }

    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCompanyName() { return companyName; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        DelegatedRemovalOfMigratedBatchEmailData that = (DelegatedRemovalOfMigratedBatchEmailData) o;
        return Objects.equals(removedBy, that.removedBy) && Objects.equals(removedUser, that.removedUser) && Objects.equals(companyName, that.companyName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(removedBy, removedUser, companyName);
    }

    @Override
    public String toString() {
        return "DelegatedRemovalOfMigratedBatchEmailData{" +
                "removedBy='" + removedBy + '\'' +
                ", removedUser='" + removedUser + '\'' +
                ", companyName='" + companyName + '\'' +
                '}';
    }
}
