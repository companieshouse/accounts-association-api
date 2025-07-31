package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class AuthCodeConfirmationEmailData extends EmailData {

    private String authorisedPerson;
    private String companyName;

    public AuthCodeConfirmationEmailData(){}

    public AuthCodeConfirmationEmailData( final String to, final String authorisedPerson, final String companyName) {
        setTo( to );
        setAuthorisedPerson( authorisedPerson );
        setCompanyName( companyName );
        setSubject();
    }

    public AuthCodeConfirmationEmailData to( final String to ){
        setTo( to );
        return this;
    }

    public void setAuthorisedPerson( final String authorisedPerson) {
        this.authorisedPerson = authorisedPerson;
    }

    public AuthCodeConfirmationEmailData authorisedPerson( final String authorisedPerson ){
        setAuthorisedPerson( authorisedPerson );
        return this;
    }

    public String getAuthorisedPerson() {
        return authorisedPerson;
    }

    public void setCompanyName( final String companyName) {
        this.companyName = companyName;
    }

    public AuthCodeConfirmationEmailData companyName( final String companyName ){
        setCompanyName( companyName );
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setSubject(){
        setSubject( String.format("Companies House: %s is now authorised to file online for %s", authorisedPerson, companyName) );
    }

    public AuthCodeConfirmationEmailData subject(){
        setSubject();
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AuthCodeConfirmationEmailData that)) {
            return false;
        }

        return new EqualsBuilder().append(authorisedPerson,
                that.authorisedPerson).append(companyName, that.companyName).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(authorisedPerson).append(companyName)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "AuthCodeConfirmationEmailData{" +
                "authorisedPerson='" + authorisedPerson + '\'' +
                ", companyName='" + companyName + '\'' +
                '}';
    }

}
