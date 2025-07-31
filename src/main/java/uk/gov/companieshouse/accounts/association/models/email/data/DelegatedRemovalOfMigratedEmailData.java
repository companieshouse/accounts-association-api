package uk.gov.companieshouse.accounts.association.models.email.data;

import uk.gov.companieshouse.email_producer.model.EmailData;

import java.util.Objects;

public class DelegatedRemovalOfMigratedEmailData extends EmailData {

    private String removedBy;

    private String companyName;


    public DelegatedRemovalOfMigratedEmailData(){}

    public DelegatedRemovalOfMigratedEmailData( final String to, final String removedBy, final String companyName) {
        setTo( to );
        setRemovedBy( removedBy );
        setCompanyName( companyName );
        setSubject();
    }

    public DelegatedRemovalOfMigratedEmailData to( final String to ){
        setTo( to );
        return this;
    }

    public void setRemovedBy( final String removedBy ) { this.removedBy = removedBy; }

    public DelegatedRemovalOfMigratedEmailData removedBy( final String removedBy ){
        setRemovedBy( removedBy );
        return this;
    }

    public String getRemovedBy() { return removedBy; }

    public void setCompanyName( final String companyName ) { this.companyName = companyName; }

    public DelegatedRemovalOfMigratedEmailData companyName( final String companyName ){
        setCompanyName( companyName );
        return this;
    }

    public String getCompanyName() { return companyName; }

    public void setSubject(){
        setSubject( String.format("Companies House: authorisation to file online for %s not restored", companyName) );
    }

    public DelegatedRemovalOfMigratedEmailData subject(){
        setSubject();
        return this;
    }

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
