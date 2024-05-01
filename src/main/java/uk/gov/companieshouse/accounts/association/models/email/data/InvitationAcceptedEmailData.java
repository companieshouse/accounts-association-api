package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class InvitationAcceptedEmailData extends EmailData {

    private String authorisedPerson;

    private String companyName;

    private String personWhoCreatedInvite;

    public InvitationAcceptedEmailData(){}

    public InvitationAcceptedEmailData(String authorisedPerson, String companyName, String personWhoCreatedInvite) {
        this.authorisedPerson = authorisedPerson;
        this.companyName = companyName;
        this.personWhoCreatedInvite = personWhoCreatedInvite;
    }

    public void setAuthorisedPerson(String authorisedPerson) {
        this.authorisedPerson = authorisedPerson;
    }

    public String getAuthorisedPerson() {
        return authorisedPerson;
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
