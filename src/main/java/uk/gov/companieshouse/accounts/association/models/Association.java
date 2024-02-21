package uk.gov.companieshouse.accounts.association.models;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;

import java.time.LocalDateTime;

@Document("user_company_associations")
@CompoundIndex(name = "company_user_idx", def = "{'company_number': 1, 'user_id': 1}", unique = true)
public class Association {

    @Id
    private String id;

    @Field("etag")
    private String etag;

    @NotNull
    @Field("user_id")
    private String userId;

    @Field("user_email")
    private String userEmail;

    @Field("display_name")
    private String displayName;

    @NotNull
    @Field("company_number")
    private String companyNumber;

    @Field("company_name")
    private String companyName;

    @NotNull
    private StatusEnum status;

    @Field("created_at")
    @CreatedDate
    private LocalDateTime createdAt;

    @Field("approved_at")
    private LocalDateTime approvedAt;

    @Field("removed_at")
    private LocalDateTime removedAt;

    @Field("kind")
    private String kind;

    // TODO: add approval_route, approval_expiry_at, invitations, notifications, links

    @Version
    private Integer version;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public StatusEnum getStatus() {
        return status;
    }

    public void setStatus(
            StatusEnum status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
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

    public String getKind() {
        return kind;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Association(String id, String etag, String userId, String userEmail, String displayName,
            String companyNumber, String companyName, StatusEnum status, LocalDateTime createdAt,
            LocalDateTime approvedAt, LocalDateTime removedAt, String kind, Integer version) {
        this.id = id;
        this.etag = etag;
        this.userId = userId;
        this.userEmail = userEmail;
        this.displayName = displayName;
        this.companyNumber = companyNumber;
        this.companyName = companyName;
        this.status = status;
        this.createdAt = createdAt;
        this.approvedAt = approvedAt;
        this.removedAt = removedAt;
        this.kind = kind;
        this.version = version;
    }

    public Association() {
    }

    @Override
    public String toString() {
        return "Association{" +
                "id='" + id + '\'' +
                ", etag='" + etag + '\'' +
                ", userId='" + userId + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", displayName='" + displayName + '\'' +
                ", companyNumber='" + companyNumber + '\'' +
                ", companyName='" + companyName + '\'' +
                ", status=" + status +
                ", createdAt=" + createdAt +
                ", approvedAt=" + approvedAt +
                ", removedAt=" + removedAt +
                ", kind='" + kind + '\'' +
                ", version=" + version +
                '}';
    }
}