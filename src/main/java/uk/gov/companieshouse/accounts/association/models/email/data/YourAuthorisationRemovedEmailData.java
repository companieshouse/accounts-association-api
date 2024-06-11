package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

import java.util.Objects;

public class YourAuthorisationRemovedEmailData extends EmailData {

    private String companyName;

    private String personWhoRemovedAuthorisation;

    public YourAuthorisationRemovedEmailData(){}

    public YourAuthorisationRemovedEmailData(String companyName, String personWhoRemovedAuthorisation) {
        this.companyName = companyName;
        this.personWhoRemovedAuthorisation = personWhoRemovedAuthorisation;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setPersonWhoRemovedAuthorisation(String personWhoRemovedAuthorisation) {
        this.personWhoRemovedAuthorisation = personWhoRemovedAuthorisation;
    }

    public String getPersonWhoRemovedAuthorisation() {
        return personWhoRemovedAuthorisation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof YourAuthorisationRemovedEmailData that)) {
            return false;
        }

        return new EqualsBuilder().append(companyName, that.companyName)
                .append(personWhoRemovedAuthorisation, that.personWhoRemovedAuthorisation)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return Objects.hash(companyName, personWhoRemovedAuthorisation);
    }

    @Override
    public String toString() {
        return "YourAuthorisationRemovedEmailData{" +
                "companyName='" + companyName + '\'' +
                ", personWhoRemovedAuthorisation='" + personWhoRemovedAuthorisation + '\'' +
                '}';
    }
}
