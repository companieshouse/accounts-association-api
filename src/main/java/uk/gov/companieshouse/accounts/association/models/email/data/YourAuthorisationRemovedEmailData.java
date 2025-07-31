package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

import java.util.Objects;

public class YourAuthorisationRemovedEmailData extends EmailData {

    private String companyName;

    private String personWhoRemovedAuthorisation;

    public YourAuthorisationRemovedEmailData(){}

    public YourAuthorisationRemovedEmailData( final String to, final String companyName, final String personWhoRemovedAuthorisation) {
        setTo( to );
        setCompanyName( companyName );
        setPersonWhoRemovedAuthorisation( personWhoRemovedAuthorisation );
        setSubject();
    }

    public YourAuthorisationRemovedEmailData to( final String to ){
        setTo( to );
        return this;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public YourAuthorisationRemovedEmailData companyName( final String companyName ){
        setCompanyName( companyName );
        return this;
    }
    public String getCompanyName() {
        return companyName;
    }

    public void setPersonWhoRemovedAuthorisation(String personWhoRemovedAuthorisation) {
        this.personWhoRemovedAuthorisation = personWhoRemovedAuthorisation;
    }

    public YourAuthorisationRemovedEmailData personWhoRemovedAuthorisation( final String personWhoRemovedAuthorisation ){
        setPersonWhoRemovedAuthorisation( personWhoRemovedAuthorisation );
        return this;
    }

    public String getPersonWhoRemovedAuthorisation() {
        return personWhoRemovedAuthorisation;
    }

    public void setSubject(){
        setSubject( String.format("Companies House: Authorisation removed to file online for %s", companyName ) );
    }

    public YourAuthorisationRemovedEmailData subject(){
        setSubject();
        return this;
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
