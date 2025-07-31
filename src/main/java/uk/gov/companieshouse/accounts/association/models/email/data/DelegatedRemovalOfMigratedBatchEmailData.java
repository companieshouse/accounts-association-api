package uk.gov.companieshouse.accounts.association.models.email.data;

import uk.gov.companieshouse.email_producer.model.EmailData;

import java.util.Objects;

public class DelegatedRemovalOfMigratedBatchEmailData extends EmailData {

    private String removedBy;

    private String removedUser;

    private String companyName;


    public DelegatedRemovalOfMigratedBatchEmailData(){}

    public DelegatedRemovalOfMigratedBatchEmailData( final String to, final String removedBy, final String removedUser, final String companyName ) {
        setTo( to );
        setRemovedBy( removedBy );
        setRemovedUser( removedUser );
        setCompanyName( companyName );
        setSubject();
    }


    public DelegatedRemovalOfMigratedBatchEmailData to( final String to ){
        setTo( to );
        return this;
    }

    public void setRemovedBy( final String removedBy ) { this.removedBy = removedBy; }

    public DelegatedRemovalOfMigratedBatchEmailData removedBy( final String removedBy ){
        setRemovedBy( removedBy );
        return this;
    }

    public String getRemovedBy() { return removedBy; }

    public void setRemovedUser(final String removedUser ) { this.removedUser = removedUser; }

    public DelegatedRemovalOfMigratedBatchEmailData removedUser( final String removedUser ){
        setRemovedUser( removedUser );
        return this;
    }

    public String getRemovedUser() { return removedUser; }

    public void setCompanyName( final String companyName ) { this.companyName = companyName; }

    public DelegatedRemovalOfMigratedBatchEmailData companyName( final String companyName ){
        setCompanyName( companyName );
        return this;
    }

    public void setSubject(){
        setSubject( String.format("Companies House: %s's digital authorisation not restored for %s", removedUser, companyName) );
    }

    public DelegatedRemovalOfMigratedBatchEmailData subject(){
        setSubject();
        return this;
    }

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
