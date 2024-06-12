package uk.gov.companieshouse.accounts.association.models.email.builders;

import uk.gov.companieshouse.accounts.association.models.email.data.InviteEmailData;

import java.util.Objects;

public class InviteEmailBuilder extends EmailBuilder<InviteEmailBuilder, InviteEmailData> {

    private String inviterDisplayName;

    private String invitationExpiryTimestamp;

    private String invitationLink;

    public InviteEmailBuilder setInviterDisplayName( final String inviterDisplayName ){
        this.inviterDisplayName = inviterDisplayName;
        return this;
    }

    public InviteEmailBuilder setInvitationExpiryTimestamp( final String invitationExpiryTimestamp ){
        this.invitationExpiryTimestamp = invitationExpiryTimestamp;
        return this;
    }

    public InviteEmailBuilder setInvitationLink( final String invitationLink ){
        this.invitationLink = invitationLink;
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
