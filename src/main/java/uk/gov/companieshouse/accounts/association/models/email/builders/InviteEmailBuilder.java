package uk.gov.companieshouse.accounts.association.models.email.builders;

import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.CHS_URL;

import java.util.Objects;
import uk.gov.companieshouse.accounts.association.models.email.data.InviteEmailData;

public class InviteEmailBuilder extends EmailBuilder<InviteEmailBuilder, InviteEmailData> {

    private String inviterDisplayName;

    private String invitationExpiryTimestamp;

    private final String invitationLink = CHS_URL + "/your-companies/company-invitations?mtm_campaign=associations_invite";

    public InviteEmailBuilder setInviterDisplayName( final String inviterDisplayName ){
        this.inviterDisplayName = inviterDisplayName;
        return this;
    }

    public InviteEmailBuilder setInvitationExpiryTimestamp( final String invitationExpiryTimestamp ){
        this.invitationExpiryTimestamp = invitationExpiryTimestamp;
        return this;
    }

    @Override
    protected InviteEmailBuilder self(){
        return this;
    }

    @Override
    public InviteEmailData build(){
        if ( Objects.isNull( recipientEmail ) || Objects.isNull( inviterDisplayName ) || Objects.isNull( companyName ) || Objects.isNull( invitationExpiryTimestamp ) || Objects.isNull( invitationLink ) ) {
            throw new NullPointerException("recipientEmail, inviterDisplayName, companyName, invitationExpiryTimestamp, and invitationLink cannot be null");
        }

        final var subject = String.format( "Companies House: invitation to be authorised to file online for %s", companyName );

        final var emailData = new InviteEmailData();
        emailData.setTo( recipientEmail );
        emailData.setSubject( subject );
        emailData.setInviterDisplayName( inviterDisplayName );
        emailData.setCompanyName( companyName );
        emailData.setInvitationExpiryTimestamp( invitationExpiryTimestamp );
        emailData.setInvitationLink( invitationLink );

        return emailData;
    }


}
