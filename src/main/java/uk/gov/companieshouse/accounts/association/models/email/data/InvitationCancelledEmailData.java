package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class InvitationCancelledEmailData extends EmailData {

    private String personWhoWasCancelled;

    private String companyName;

    private String personWhoCancelledInvite;

    public InvitationCancelledEmailData(){}

    public InvitationCancelledEmailData( final String to, final String personWhoWasCancelled, final String companyName, final String personWhoCancelledInvite) {
        setTo( to );
        setPersonWhoWasCancelled( personWhoWasCancelled );
        setCompanyName( companyName );
        setPersonWhoCancelledInvite( personWhoCancelledInvite );
        setSubject();
    }

    public InvitationCancelledEmailData to( final String to ){
        setTo( to );
        return this;
    }

    public void setPersonWhoWasCancelled( final String personWhoWasCancelled ) {
        this.personWhoWasCancelled = personWhoWasCancelled;
    }

    public InvitationCancelledEmailData personWhoWasCancelled( final String personWhoWasCancelled ){
        setPersonWhoWasCancelled( personWhoWasCancelled );
        return this;
    }

    public String getPersonWhoWasCancelled() {
        return personWhoWasCancelled;
    }

    public void setCompanyName( final String companyName ) {
        this.companyName = companyName;
    }

    public InvitationCancelledEmailData companyName( final String companyName ){
        setCompanyName( companyName );
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setPersonWhoCancelledInvite( final String personWhoCancelledInvite ) {
        this.personWhoCancelledInvite = personWhoCancelledInvite;
    }

    public InvitationCancelledEmailData personWhoCancelledInvite( final String personWhoCancelledInvite ){
        setPersonWhoCancelledInvite( personWhoCancelledInvite );
        return this;
    }

    public String getPersonWhoCancelledInvite() {
        return personWhoCancelledInvite;
    }

    public void setSubject(){
        setSubject( String.format( "Companies House: authorisation to file online for %s cancelled", companyName ) );
    }

    public InvitationCancelledEmailData subject(){
        setSubject();
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof InvitationCancelledEmailData that)) {
            return false;
        }

        return new EqualsBuilder().append(personWhoWasCancelled,
                        that.personWhoWasCancelled).append(companyName, that.companyName)
                .append(personWhoCancelledInvite, that.personWhoCancelledInvite).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(personWhoWasCancelled).append(companyName)
                .append(personWhoCancelledInvite).toHashCode();
    }

    @Override
    public String toString() {
        return "InvitationCancelledEmailData{" +
                "personWhoWasCancelled='" + personWhoWasCancelled + '\'' +
                ", companyName='" + companyName + '\'' +
                ", personWhoCancelledInvite='" + personWhoCancelledInvite + '\'' +
                '}';
    }

}
