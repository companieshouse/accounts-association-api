package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class InviteEmailData extends EmailData {

    private String inviterDisplayName;

    private String companyName;

    private String invitationExpiryTimestamp;

    private String invitationLink;

    public InviteEmailData(){}

    public InviteEmailData(String inviterDisplayName, String companyName, String invitationExpiryTimestamp, String invitationLink) {
        this.inviterDisplayName = inviterDisplayName;
        this.companyName = companyName;
        this.invitationExpiryTimestamp = invitationExpiryTimestamp;
        this.invitationLink = invitationLink;
    }

    public void setInviterDisplayName(String inviterDisplayName) {
        this.inviterDisplayName = inviterDisplayName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public void setInvitationExpiryTimestamp(String invitationExpiryTimestamp) {
        this.invitationExpiryTimestamp = invitationExpiryTimestamp;
    }

    public void setInvitationLink(String invitationLink) {
        this.invitationLink = invitationLink;
    }

    public String getInviterDisplayName() {
        return inviterDisplayName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public String getInvitationExpiryTimestamp() {
        return invitationExpiryTimestamp;
    }

    public String getInvitationLink() {
        return invitationLink;
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
