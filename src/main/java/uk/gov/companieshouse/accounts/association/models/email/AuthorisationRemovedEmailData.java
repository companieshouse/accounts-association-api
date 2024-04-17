package uk.gov.companieshouse.accounts.association.models.email;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class AuthorisationRemovedEmailData extends EmailData {

    private String authenticationRemovedPerson;

    private String companyName;

    private String personWhoRemovedAuthorisation;

    public AuthorisationRemovedEmailData(){}

    public AuthorisationRemovedEmailData(String authenticationRemovedPerson, String companyName, String personWhoRemovedAuthorisation) {
        this.authenticationRemovedPerson = authenticationRemovedPerson;
        this.companyName = companyName;
        this.personWhoRemovedAuthorisation = personWhoRemovedAuthorisation;
    }

    public void setAuthenticationRemovedPerson(String authenticationRemovedPerson) {
        this.authenticationRemovedPerson = authenticationRemovedPerson;
    }

    public String getAuthenticationRemovedPerson() {
        return authenticationRemovedPerson;
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

        return new EqualsBuilder().append(authenticationRemovedPerson,
                        that.authenticationRemovedPerson).append(companyName, that.companyName)
                .append(personWhoRemovedAuthorisation, that.personWhoRemovedAuthorisation)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(authenticationRemovedPerson).append(companyName)
                .append(personWhoRemovedAuthorisation).toHashCode();
    }

    @Override
    public String toString() {
        return "AuthorisationRemovedEmailData{" +
                "authenticationRemovedPerson='" + authenticationRemovedPerson + '\'' +
                ", companyName='" + companyName + '\'' +
                ", personWhoRemovedAuthorisation='" + personWhoRemovedAuthorisation + '\'' +
                '}';
    }
}
