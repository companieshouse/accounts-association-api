package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class AuthorisationRemovedEmailData extends EmailData {

    private String personWhoWasRemoved;

    private String companyName;

    private String personWhoRemovedAuthorisation;

    public AuthorisationRemovedEmailData(){}

    public AuthorisationRemovedEmailData(String personWhoWasRemoved, String companyName, String personWhoRemovedAuthorisation) {
        this.personWhoWasRemoved = personWhoWasRemoved;
        this.companyName = companyName;
        this.personWhoRemovedAuthorisation = personWhoRemovedAuthorisation;
    }

    public void setPersonWhoWasRemoved(String personWhoWasRemoved) {
        this.personWhoWasRemoved = personWhoWasRemoved;
    }

    public String getPersonWhoWasRemoved() {
        return personWhoWasRemoved;
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

        if (!(o instanceof AuthorisationRemovedEmailData that)) {
            return false;
        }

        return new EqualsBuilder().append(personWhoWasRemoved,
                        that.personWhoWasRemoved).append(companyName, that.companyName)
                .append(personWhoRemovedAuthorisation, that.personWhoRemovedAuthorisation)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(personWhoWasRemoved).append(companyName)
                .append(personWhoRemovedAuthorisation).toHashCode();
    }

    @Override
    public String toString() {
        return "AuthorisationRemovedEmailData{" +
                "personWhoWasRemoved='" + personWhoWasRemoved + '\'' +
                ", companyName='" + companyName + '\'' +
                ", personWhoRemovedAuthorisation='" + personWhoRemovedAuthorisation + '\'' +
                '}';
    }
}
