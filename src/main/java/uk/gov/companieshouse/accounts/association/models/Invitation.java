package uk.gov.companieshouse.accounts.association.models;

import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

public class Invitation {
    @Field("invited_by")
    private String invitedBy;
    @Field("invited_at")
    private LocalDateTime invitedAt;

    public Invitation() {
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

//    @Override
//    public String toString() {
//        return "Invitation{" +
//                "invitedBy='" + invitedBy + '\'' +
//                ", invitedAt=" + invitedAt +
//                '}';
//    }
}
