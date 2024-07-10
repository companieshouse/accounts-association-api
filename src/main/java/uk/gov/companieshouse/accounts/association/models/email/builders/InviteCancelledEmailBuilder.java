package uk.gov.companieshouse.accounts.association.models.email.builders;

import java.util.Objects;
import uk.gov.companieshouse.accounts.association.models.email.data.InviteCancelledEmailData;

public class InviteCancelledEmailBuilder extends EmailBuilder<InviteCancelledEmailBuilder, InviteCancelledEmailData> {

    private String cancelledBy;

    public InviteCancelledEmailBuilder setCancelledBy( final String cancelledBy ){
        this.cancelledBy = cancelledBy;
        return this;
    }

    @Override
    protected InviteCancelledEmailBuilder self(){
        return this;
    }

    @Override
    public InviteCancelledEmailData build(){
        if ( Objects.isNull( recipientEmail ) || Objects.isNull( companyName ) || Objects.isNull( cancelledBy ) ) {
            throw new NullPointerException( "recipientEmail, companyName, and cancelledBy cannot be null");
        }

        final var subject = String.format( "Companies House: authorisation to file online for %s cancelled", companyName );

        final var emailData = new InviteCancelledEmailData();
        emailData.setTo( recipientEmail );
        emailData.setSubject( subject );
        emailData.setCompanyName( companyName );
        emailData.setCancelledBy( cancelledBy );

        return emailData;
    }

}
