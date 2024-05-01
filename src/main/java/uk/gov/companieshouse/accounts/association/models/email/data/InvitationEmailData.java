package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class InvitationEmailData extends EmailData {

    private String invitee;

    private String companyName;

    private String personWhoCreatedInvite;

    public InvitationEmailData(){}

    public InvitationEmailData(String invitee, String companyName, String personWhoCreatedInvite) {
        this.invitee = invitee;
        this.companyName = companyName;
        this.personWhoCreatedInvite = personWhoCreatedInvite;
    }

    public void setInvitee(String invitee) {
        this.invitee = invitee;
    }

    public String getInvitee() {
        return invitee;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setPersonWhoCreatedInvite(String personWhoCreatedInvite) {
        this.personWhoCreatedInvite = personWhoCreatedInvite;
    }

    public String getPersonWhoCreatedInvite() {
        return personWhoCreatedInvite;
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
