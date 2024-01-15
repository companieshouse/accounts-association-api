package uk.gov.companieshouse.accounts.association.models;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document("user_company_associations")
@CompoundIndex(name = "company_user_idx", def = "{'company_number': 1, 'user_id': 1}", unique = true)
public class Association {


    @Id
    private String id;
    @NotNull
    @Field("company_number")
    private String companyNumber;

    @NotNull
    @Field("user_id")
    private String userId;

    @NotNull
    private String status;

    @Field("creation_time")
    @CreatedDate
    private LocalDateTime creationTime;

    @Field("confirmation_expiration_time")
    private String confirmationExpirationTime;
    @Field("confirmation_approval_time")
    private String confirmationApprovalTime;

    @Field("deletion_time")
    private String deletionTime;

    @Version
    private Integer version;

    private boolean temporary;

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setConfirmationExpirationTime(String confirmationExpirationTime) {
        this.confirmationExpirationTime = confirmationExpirationTime;
    }

    public void setConfirmationApprovalTime(String confirmationApprovalTime) {
        this.confirmationApprovalTime = confirmationApprovalTime;
    }

    public void setDeletionTime(String deletionTime) {
        this.deletionTime = deletionTime;
    }

    public String getId() {
        return id;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public String getUserId() {
        return userId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreationTime() {
        return creationTime;
    }

    public String getConfirmationExpirationTime() {
        return confirmationExpirationTime;
    }

    public String getConfirmationApprovalTime() {
        return confirmationApprovalTime;
    }

    public String getDeletionTime() {
        return deletionTime;
    }


    public int getVersion() {
        return version;
    }

    public boolean isTemporary() {
        return temporary;
    }

    public void setTemporary(boolean temporary) {
        this.temporary = temporary;
    }


    public Association(final String companyNumber, final String userId, final String status, final String confirmationExpirationTime, final String confirmationApprovalTime, final String deletionTime, boolean temporary) {
        this.companyNumber = companyNumber;
        this.userId = userId;
        this.status = status;
        this.confirmationExpirationTime = confirmationExpirationTime;
        this.confirmationApprovalTime = confirmationApprovalTime;
        this.deletionTime = deletionTime;
        this.temporary = temporary;
    }

    public Association() {
    }

    @Override
    public String toString() {
        return "Associations{" +
                "id='" + id + '\'' +
                ", companyNumber='" + companyNumber + '\'' +
                ", userId='" + userId + '\'' +
                ", status='" + status + '\'' +
                ", creationTime=" + creationTime +
                ", confirmationExpirationTime='" + confirmationExpirationTime + '\'' +
                ", confirmationApprovalTime='" + confirmationApprovalTime + '\'' +
                ", deletionTime='" + deletionTime + '\'' +
                ", temporary='" + temporary + '\'' +
                ", version=" + version +
                '}';
    }
}