package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class InviteCancelledEmailData extends EmailData {

    private String companyName;

    private String cancelledBy;

    public InviteCancelledEmailData() {}

    public InviteCancelledEmailData(String companyName, String cancelledBy) {
        this.companyName = companyName;
        this.cancelledBy = cancelledBy;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof InviteCancelledEmailData that)) {
            return false;
        }

        return new EqualsBuilder().append(companyName, that.companyName)
                .append(cancelledBy, that.cancelledBy).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(companyName).append(cancelledBy).toHashCode();
    }

    @Override
    public String toString() {
        return "InviteCancelledEmailData{" +
                "companyName='" + companyName + '\'' +
                ", cancelledBy='" + cancelledBy + '\'' +
                '}';
    }
}
