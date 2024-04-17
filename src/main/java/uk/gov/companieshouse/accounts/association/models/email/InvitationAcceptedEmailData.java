package uk.gov.companieshouse.accounts.association.models.email;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class InvitationAcceptedEmailData extends EmailData {

    private String authorisedPerson;

    private String companyName;

    private String personInvitationFrom;

    public InvitationAcceptedEmailData(){}

    public InvitationAcceptedEmailData(String authorisedPerson, String companyName, String personInvitationFrom) {
        this.authorisedPerson = authorisedPerson;
        this.companyName = companyName;
        this.personInvitationFrom = personInvitationFrom;
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

    public void setPersonInvitationFrom(String personInvitationFrom) {
        this.personInvitationFrom = personInvitationFrom;
    }

    public String getPersonInvitationFrom() {
        return personInvitationFrom;
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
                .append(personInvitationFrom, that.personInvitationFrom).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(authorisedPerson).append(companyName)
                .append(personInvitationFrom).toHashCode();
    }

    @Override
    public String toString() {
        return "InvitationAcceptedEmailData{" +
                "authorisedPerson='" + authorisedPerson + '\'' +
                ", companyName='" + companyName + '\'' +
                ", personInvitationFrom='" + personInvitationFrom + '\'' +
                '}';
    }
}
