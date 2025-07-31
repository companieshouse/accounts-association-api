package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class InvitationEmailData extends EmailData {

    private String invitee;

    private String companyName;

    private String personWhoCreatedInvite;

    public InvitationEmailData(){}

    public InvitationEmailData( final String to, final String invitee, final String companyName, final String personWhoCreatedInvite) {
        setTo( to );
        setInvitee( invitee );
        setCompanyName( companyName );
        setPersonWhoCreatedInvite( personWhoCreatedInvite);
        setSubject();
    }

    public InvitationEmailData to( final String to ){
        setTo( to );
        return this;
    }

    public void setInvitee(final String invitee ) {
        this.invitee = invitee;
    }

    public InvitationEmailData invitee( final String invitee ){
        setInvitee( invitee );
        return this;
    }

    public String getInvitee() {
        return invitee;
    }

    public void setCompanyName( final String companyName ) {
        this.companyName = companyName;
    }

    public InvitationEmailData companyName( final String companyName ){
        setCompanyName( companyName );
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setPersonWhoCreatedInvite(String personWhoCreatedInvite) {
        this.personWhoCreatedInvite = personWhoCreatedInvite;
    }

    public InvitationEmailData personWhoCreatedInvite( final String personWhoCreatedInvite ){
        setPersonWhoCreatedInvite( personWhoCreatedInvite );
        return this;
    }

    public String getPersonWhoCreatedInvite() {
        return personWhoCreatedInvite;
    }

    public void setSubject(){
        setSubject( String.format( "Companies House: invitation to be authorised to file online for %s", companyName ) );
    }

    public InvitationEmailData subject(){
        setSubject();
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof InvitationEmailData that)) {
            return false;
        }

        return new EqualsBuilder().append(invitee, that.invitee)
                .append(companyName, that.companyName)
                .append(personWhoCreatedInvite, that.personWhoCreatedInvite).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(invitee).append(companyName)
                .append(personWhoCreatedInvite).toHashCode();
    }

    @Override
    public String toString() {
        return "InvitationEmailData{" +
                "invitee='" + invitee + '\'' +
                ", companyName='" + companyName + '\'' +
                ", personWhoCreatedInvite='" + personWhoCreatedInvite + '\'' +
                '}';
    }

}
