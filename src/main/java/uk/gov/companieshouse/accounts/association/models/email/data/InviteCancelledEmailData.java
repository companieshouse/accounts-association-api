package uk.gov.companieshouse.accounts.association.models.email.data;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import uk.gov.companieshouse.email_producer.model.EmailData;

public class InviteCancelledEmailData extends EmailData {

    private String companyName;

    private String cancelledBy;

    public InviteCancelledEmailData(){}

    public InviteCancelledEmailData( final String to, final String companyName, final String cancelledBy) {
        setTo( to );
        setCompanyName( companyName );
        setCancelledBy( cancelledBy );
        setSubject();
    }

    public InviteCancelledEmailData to( final String to ){
        setTo( to );
        return this;
    }

    public String getCompanyName() {
        return companyName;
    }

    public InviteCancelledEmailData companyName( final String companyName ){
        setCompanyName( companyName );
        return this;
    }

    public void setCompanyName(final String companyName ) {
        this.companyName = companyName;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public InviteCancelledEmailData cancelledBy( final String cancelledBy ){
        setCancelledBy( cancelledBy );
        return this;
    }

    public void setCancelledBy(final String cancelledBy ) {
        this.cancelledBy = cancelledBy;
    }

    public void setSubject(){
        setSubject( String.format("Companies House: Invitation cancelled for %s to be authorised to file online for %s", cancelledBy, companyName) );
    }

    public InviteCancelledEmailData subject(){
        setSubject();
        return this;
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
