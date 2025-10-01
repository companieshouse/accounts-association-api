package uk.gov.companieshouse.accounts.association.models;

import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.DAYS_SINCE_INVITE_TILL_EXPIRES;

import java.time.LocalDateTime;
import org.springframework.data.mongodb.core.mapping.Field;

public class InvitationDao {

    @Field( "invited_by" )
    private String invitedBy;

    @Field( "invited_at" )
    private LocalDateTime invitedAt;

    public void setInvitedBy( final String invitedBy ){
        this.invitedBy = invitedBy;
    }

    public InvitationDao invitedBy( final String invitedBy ){
        setInvitedBy( invitedBy );
        return this;
    }

    public String getInvitedBy(){
        return invitedBy;
    }

    public void setInvitedAt( final LocalDateTime invitedAt ){
        this.invitedAt = invitedAt;
    }

    public InvitationDao invitedAt( final LocalDateTime invitedAt ){
        setInvitedAt( invitedAt );
        return this;
    }

    public LocalDateTime getInvitedAt(){
        return invitedAt;
    }

    public LocalDateTime getExpiredAt() {
        return invitedAt.plusDays( DAYS_SINCE_INVITE_TILL_EXPIRES );
    }

    @Override
    public String toString() {
        return "InvitationDao{" +
                "invitedBy='" + invitedBy + '\'' +
                ", invitedAt=" + invitedAt +
                '}';
    }

}
