package uk.gov.companieshouse.accounts.association.models.email.data;

import uk.gov.companieshouse.email_producer.model.EmailData;

import java.util.Objects;

public class RemovalOfOwnMigratedEmailData extends EmailData {

    private String companyName;

    public RemovalOfOwnMigratedEmailData(){}

    public RemovalOfOwnMigratedEmailData( final String to, final String companyName ) {
        setTo( to );
        setCompanyName( companyName );
        setSubject();
    }

    public RemovalOfOwnMigratedEmailData to( final String to ){
        setTo( to );
        return this;
    }

    public void setCompanyName( final String companyName ) { this.companyName = companyName; }

    public RemovalOfOwnMigratedEmailData companyName( final String companyName ){
        setCompanyName( companyName );
        return this;
    }

    public String getCompanyName() { return companyName; }

    public void setSubject(){
        setSubject( String.format("Companies House: authorisation to file online for %s not restored", companyName) );
    }

    public RemovalOfOwnMigratedEmailData subject(){
        setSubject();
        return this;
    }

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
