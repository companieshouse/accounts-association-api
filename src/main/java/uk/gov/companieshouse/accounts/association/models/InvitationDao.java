package uk.gov.companieshouse.accounts.association.models;

import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.DAYS_SINCE_INVITE_TILL_EXPIRES;

public class InvitationDao {
    @Field("invited_by")
    private String invitedBy;
    @Field("invited_at")
    private LocalDateTime invitedAt;

    public String getInvitedBy() {
        return invitedBy;
    }

    public void setInvitedBy(String invitedBy) {
        this.invitedBy = invitedBy;
    }

    public LocalDateTime getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(LocalDateTime invitedAt) {
        this.invitedAt = invitedAt;
    }

    public LocalDateTime getExpiredAt() {
        return invitedAt.plusDays(DAYS_SINCE_INVITE_TILL_EXPIRES);
    }

    @Override
    public String toString() {
        return "InvitationDao{" +
                "invitedBy='" + invitedBy + '\'' +
                ", invitedAt=" + invitedAt +
                '}';
    }
}
