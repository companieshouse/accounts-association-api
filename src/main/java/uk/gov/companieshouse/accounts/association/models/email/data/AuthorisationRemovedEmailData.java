package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class AuthorisationRemovedEmailData extends EmailData {

    private String personWhoWasRemoved;

    private String companyName;

    private String personWhoRemovedAuthorisation;

    public AuthorisationRemovedEmailData(){}

    public AuthorisationRemovedEmailData(final String to, final String personWhoWasRemoved, final String companyName, final String personWhoRemovedAuthorisation) {
        setTo( to );
        setPersonWhoWasRemoved( personWhoWasRemoved );
        setCompanyName( companyName );
        setPersonWhoRemovedAuthorisation( personWhoRemovedAuthorisation );
        setSubject();
    }

    public AuthorisationRemovedEmailData to( final String to ){
        setTo( to );
        return this;
    }

    public void setPersonWhoWasRemoved( final String personWhoWasRemoved ) {
        this.personWhoWasRemoved = personWhoWasRemoved;
    }

    public AuthorisationRemovedEmailData personWhoWasRemoved( final String personWhoWasRemoved ){
        setPersonWhoWasRemoved( personWhoWasRemoved );
        return this;
    }

    public String getPersonWhoWasRemoved() {
        return personWhoWasRemoved;
    }

    public void setCompanyName( final String companyName ) {
        this.companyName = companyName;
    }

    public AuthorisationRemovedEmailData companyName( final String companyName ){
        setCompanyName( companyName );
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setPersonWhoRemovedAuthorisation( final String personWhoRemovedAuthorisation ) {
        this.personWhoRemovedAuthorisation = personWhoRemovedAuthorisation;
    }

    public AuthorisationRemovedEmailData personWhoRemovedAuthorisation( final String personWhoRemovedAuthorisation ){
        setPersonWhoRemovedAuthorisation( personWhoRemovedAuthorisation );
        return this;
    }

    public String getPersonWhoRemovedAuthorisation() {
        return personWhoRemovedAuthorisation;
    }

    public void setSubject(){
        setSubject( String.format("Companies House: %s's authorisation removed to file online for %s", personWhoWasRemoved, companyName) );
    }

    public AuthorisationRemovedEmailData subject(){
        setSubject();
        return this;
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
