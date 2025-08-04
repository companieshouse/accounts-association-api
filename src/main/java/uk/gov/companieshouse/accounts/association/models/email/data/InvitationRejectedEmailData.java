package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class InvitationRejectedEmailData extends EmailData {

    private String personWhoDeclined;

    private String companyName;

    public InvitationRejectedEmailData(){}

    public InvitationRejectedEmailData( final String to, final String personWhoDeclined, final String companyName) {
        setTo( to );
        setPersonWhoDeclined( personWhoDeclined );
        setCompanyName( companyName );
        setSubject();
    }

    public InvitationRejectedEmailData to( final String to ){
        setTo( to );
        return this;
    }

    public void setPersonWhoDeclined( final String personWhoDeclined ) {
        this.personWhoDeclined = personWhoDeclined;
    }

    public InvitationRejectedEmailData personWhoDeclined( final String personWhoDeclined ){
        setPersonWhoDeclined( personWhoDeclined );
        return this;
    }

    public String getPersonWhoDeclined() {
        return personWhoDeclined;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public InvitationRejectedEmailData companyName( final String companyName ){
        setCompanyName( companyName );
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setSubject(){
        setSubject( String.format("Companies House: %s has declined to be digitally authorised to file online for %s", personWhoDeclined, companyName) );
    }

    public InvitationRejectedEmailData subject(){
        setSubject();
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof InvitationRejectedEmailData that)) {
            return false;
        }

        return new EqualsBuilder().append(personWhoDeclined,
                that.personWhoDeclined).append(companyName, that.companyName).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(personWhoDeclined).append(companyName)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "InvitationRejectedEmailData{" +
                "personWhoDeclined='" + personWhoDeclined + '\'' +
                ", companyName='" + companyName + '\'' +
                '}';
    }

}
