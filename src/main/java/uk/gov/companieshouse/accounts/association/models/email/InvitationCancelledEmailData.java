package uk.gov.companieshouse.accounts.association.models.email;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class InvitationCancelledEmailData extends EmailData {

    private String inviteCancelledPerson;

    private String companyName;

    private String personWhoCancelledInvite;

    public InvitationCancelledEmailData(){}

    public InvitationCancelledEmailData(String inviteCancelledPerson, String companyName, String personWhoCancelledInvite) {
        this.inviteCancelledPerson = inviteCancelledPerson;
        this.companyName = companyName;
        this.personWhoCancelledInvite = personWhoCancelledInvite;
    }

    public void setInviteCancelledPerson(String inviteCancelledPerson) {
        this.inviteCancelledPerson = inviteCancelledPerson;
    }

    public String getInviteCancelledPerson() {
        return inviteCancelledPerson;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setPersonWhoCancelledInvite(String personWhoCancelledInvite) {
        this.personWhoCancelledInvite = personWhoCancelledInvite;
    }

    public String getPersonWhoCancelledInvite() {
        return personWhoCancelledInvite;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof InvitationCancelledEmailData that)) {
            return false;
        }

        return new EqualsBuilder().append(inviteCancelledPerson,
                        that.inviteCancelledPerson).append(companyName, that.companyName)
                .append(personWhoCancelledInvite, that.personWhoCancelledInvite).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(inviteCancelledPerson).append(companyName)
                .append(personWhoCancelledInvite).toHashCode();
    }

    @Override
    public String toString() {
        return "InvitationCancelledEmailData{" +
                "inviteCancelledPerson='" + inviteCancelledPerson + '\'' +
                ", companyName='" + companyName + '\'' +
                ", personWhoCancelledInvite='" + personWhoCancelledInvite + '\'' +
                '}';
    }

}
