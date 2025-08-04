package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class InvitationAcceptedEmailData extends EmailData {

    private String authorisedPerson;

    private String companyName;

    private String personWhoCreatedInvite;

    public InvitationAcceptedEmailData(){}

    public InvitationAcceptedEmailData( final String to, final String inviteeDisplayName, final String companyName, final String personWhoCreatedInvite) {
        setTo( to );
        setAuthorisedPerson( inviteeDisplayName );
        setCompanyName( companyName );
        setPersonWhoCreatedInvite( personWhoCreatedInvite );
        setSubject();
    }

    public InvitationAcceptedEmailData to( final String inviteeEmail ){
        setTo( inviteeEmail );
        return this;
    }

    public void setAuthorisedPerson( final String authorisedPerson ) {
        this.authorisedPerson = authorisedPerson;
    }

    public InvitationAcceptedEmailData authorisedPerson( final String authorisedPerson ){
        setAuthorisedPerson( authorisedPerson );
        return this;
    }

    public String getAuthorisedPerson() {
        return authorisedPerson;
    }

    public void setCompanyName( final String companyName ) {
        this.companyName = companyName;
    }

    public InvitationAcceptedEmailData companyName( final String companyName ){
        setCompanyName( companyName );
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setPersonWhoCreatedInvite( final String personWhoCreatedInvite ) {
        this.personWhoCreatedInvite = personWhoCreatedInvite;
    }

    public InvitationAcceptedEmailData personWhoCreatedInvite( final String personWhoCreatedInvite ){
        setPersonWhoCreatedInvite( personWhoCreatedInvite );
        return this;
    }

    public String getPersonWhoCreatedInvite() {
        return personWhoCreatedInvite;
    }

    public void setSubject(){
        setSubject( String.format("Companies House: %s is now authorised to file online for %s", personWhoCreatedInvite, companyName) );
    }

    public InvitationAcceptedEmailData subject(){
        setSubject();
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof InvitationAcceptedEmailData that)) {
            return false;
        }

        return new EqualsBuilder().append(authorisedPerson,
                        that.authorisedPerson).append(companyName, that.companyName)
                .append(personWhoCreatedInvite, that.personWhoCreatedInvite).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(authorisedPerson).append(companyName)
                .append(personWhoCreatedInvite).toHashCode();
    }

    @Override
    public String toString() {
        return "InvitationAcceptedEmailData{" +
                "authorisedPerson='" + authorisedPerson + '\'' +
                ", companyName='" + companyName + '\'' +
                ", personWhoCreatedInvite='" + personWhoCreatedInvite + '\'' +
                '}';
    }
}
