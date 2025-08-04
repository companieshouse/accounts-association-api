package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

import java.util.Objects;

public class InviteEmailData extends EmailData {

    private String inviterDisplayName;

    private String companyName;

    private String invitationExpiryTimestamp;

    private String invitationLink;

    public InviteEmailData(){}

    public InviteEmailData( final String to, final String inviterDisplayName, final String companyName, final String invitationExpiryTimestamp, final String invitationLink) {
        setTo( to );
        setInviterDisplayName( inviterDisplayName );
        setCompanyName( companyName );
        setInvitationExpiryTimestamp( invitationExpiryTimestamp );
        setInvitationLink( invitationLink );
        setSubject();
    }

    public InviteEmailData to( final String to ){
        setTo( to );
        return this;
    }

    public void setInviterDisplayName( final String inviterDisplayName ) {
        this.inviterDisplayName = inviterDisplayName;
    }

    public InviteEmailData inviterDisplayName( final String inviterDisplayName ){
        setInviterDisplayName( inviterDisplayName );
        return this;
    }

    public String getInviterDisplayName() {
        return inviterDisplayName;
    }

    public void setCompanyName( final String companyName ) {
        this.companyName = companyName;
    }

    public InviteEmailData companyName( final String companyName ){
        if ( Objects.isNull(companyName) ) {
            throw new NullPointerException("companyName cannot be null");
        }
        setCompanyName( companyName );
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setInvitationExpiryTimestamp( final String invitationExpiryTimestamp ) {
        this.invitationExpiryTimestamp = invitationExpiryTimestamp;
    }

    public InviteEmailData invitationExpiryTimestamp( final String invitationExpiryTimestamp ){
        setInvitationExpiryTimestamp( invitationExpiryTimestamp );
        return this;
    }

    public String getInvitationExpiryTimestamp() {
        return invitationExpiryTimestamp;
    }

    public void setInvitationLink( final String invitationLink ) {
        this.invitationLink = invitationLink;
    }

    public InviteEmailData invitationLink( final String invitationLink ){
        setInvitationLink( invitationLink );
        return this;
    }

    public String getInvitationLink() {
        return invitationLink;
    }

    public void setSubject(){
        setSubject( String.format("Companies House: %s invited to be authorised to file online for %s", inviterDisplayName, companyName) );
    }

    public InviteEmailData subject(){
        setSubject();
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof InviteEmailData that)) {
            return false;
        }

        return new EqualsBuilder().append(inviterDisplayName,
                        that.inviterDisplayName).append(companyName, that.companyName)
                .append(invitationExpiryTimestamp, that.invitationExpiryTimestamp)
                .append(invitationLink, that.invitationLink).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(inviterDisplayName).append(companyName)
                .append(invitationExpiryTimestamp).append(invitationLink).toHashCode();
    }

    @Override
    public String toString() {
        return "InviteEmailData{" +
                "inviterDisplayName='" + inviterDisplayName + '\'' +
                ", companyName='" + companyName + '\'' +
                ", invitationExpiryTimestamp='" + invitationExpiryTimestamp + '\'' +
                ", invitationLink='" + invitationLink + '\'' +
                '}';
    }

}
