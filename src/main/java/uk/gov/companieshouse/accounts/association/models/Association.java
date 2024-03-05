package uk.gov.companieshouse.accounts.association.models;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;

@Document("user_company_associations")
public class Association {
    @Id
    private String id;
    @Indexed
    @NotNull
    @Field("company_number")
    private String companyNumber;

    @Indexed
    @Field("user_id")
    private String userId;
    @NotNull
    private StatusEnum status;
    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;
    @Field("approved_at")
    @FutureOrPresent
    private LocalDateTime approvedAt;
    @Field("removed_at")
    @FutureOrPresent
    private LocalDateTime removedAt;
    @NotNull
    @Field("approval_route")
    private ApprovalRouteEnum approvalRoute;
    @Indexed
    @Field("user_email")
    private String userEmail;
    @Field("approval_expiry_at")
    @FutureOrPresent
    private LocalDateTime approvalExpiryAt;

    @Field("invitations")
    private List<Invitation> invitations ;
    @NotNull
    private String etag;
    @Version
    private Integer version;

    public Association() {
        invitations = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(StatusEnum status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public ApprovalRouteEnum getApprovalRoute() {
        return approvalRoute;
    }

    public void setApprovalRoute(ApprovalRouteEnum approvalRoute) {
        this.approvalRoute = approvalRoute;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public List<Invitation> getInvitations() {
        return invitations;
    }

    public void setInvitations(List<Invitation> invitations) {

        this.invitations.addAll(invitations);
    }

    public String getEtag() {
        return etag;
    }


    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }

    public LocalDateTime getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(LocalDateTime removedAt) {
        this.removedAt = removedAt;
    }

    public LocalDateTime getApprovalExpiryAt() {
        return approvalExpiryAt;
    }

    public void setApprovalExpiryAt(LocalDateTime approvalExpiryAt) {
        this.approvalExpiryAt = approvalExpiryAt;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public Integer getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "Association{" +
                "id='" + id + '\'' +
                ", companyNumber='" + companyNumber + '\'' +
                ", userId='" + userId + '\'' +
                ", status='" + status + '\'' +
                ", createdAt=" + createdAt +
                ", approvedAt=" + approvedAt +
                ", removedAt=" + removedAt +
                ", approvalRoute='" + approvalRoute + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", approvalExpiryAt=" + approvalExpiryAt +
                ", invitations=" + invitations +
                ", etag='" + etag + '\'' +
                ", version=" + version +
                '}';
    }
}