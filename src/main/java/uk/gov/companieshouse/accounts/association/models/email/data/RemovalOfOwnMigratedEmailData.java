package uk.gov.companieshouse.accounts.association.models.email.data;

import java.util.Objects;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class RemovalOfOwnMigratedEmailData extends EmailData {

    private String companyName;

    public RemovalOfOwnMigratedEmailData() {}

    public RemovalOfOwnMigratedEmailData(String companyName) {
        this.companyName = companyName;
    }

    public void setCompanyName(String companyName) { this.companyName = companyName; }

    public String getCompanyName() { return companyName; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        RemovalOfOwnMigratedEmailData that = (RemovalOfOwnMigratedEmailData) o;
        return Objects.equals(companyName, that.companyName);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(companyName);
    }

    @Override
    public String toString() {
        return "RemovalOfOwnMigratedEmailData{" +
                "companyName='" + companyName + '\'' +
                '}';
    }
}
