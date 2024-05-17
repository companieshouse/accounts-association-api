package uk.gov.companieshouse.accounts.association.models;

import java.time.LocalDateTime;
import org.springframework.data.mongodb.core.mapping.Field;

public class InvitationDao {
    @Field("invited_by")
    private String invitedBy;
    @Field("invited_at")
    private LocalDateTime invitedAt;

    //Default constructor
    public InvitationDao() {
    }

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

    @Override
    public String toString() {
        return "InvitationDao{" +
                "invitedBy='" + invitedBy + '\'' +
                ", invitedAt=" + invitedAt +
                '}';
    }
}
