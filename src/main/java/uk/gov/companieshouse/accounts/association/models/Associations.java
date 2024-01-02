package uk.gov.companieshouse.accounts.association.models;

import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
@CompoundIndex( name = "company_user_idx", def = "{'companyNumber': 1, 'userId': 1}", unique = true )
public class Associations {

    @Id
    private String id;
    private String companyNumber;

    @NotNull
    private String userId;

    @NotNull
    private String status;

    private String creationTime;

    private String confirmationExpirationTime;

    private String confirmationApprovalTime;

    private String deletionTime;

    public Associations(){}

    public Associations(String userId, String companyNumber, String status, String creationTime,
            String confirmationExpirationTime, String confirmationApprovalTime,
            String deletionTime) {
        this.userId = userId;
        this.companyNumber = companyNumber;
        this.status = status;
        this.creationTime = creationTime;
        this.confirmationExpirationTime = confirmationExpirationTime;
        this.confirmationApprovalTime = confirmationApprovalTime;
        this.deletionTime = deletionTime;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof Associations that)) {
            return false;
        }

        return new EqualsBuilder().append(getUserId(), that.getUserId())
                .append(getCompanyNumber(), that.getCompanyNumber())
                .append(getStatus(), that.getStatus())
                .append(getCreationTime(), that.getCreationTime())
                .append(getConfirmationExpirationTime(), that.getConfirmationExpirationTime())
                .append(getConfirmationApprovalTime(), that.getConfirmationApprovalTime())
                .append(getDeletionTime(), that.getDeletionTime()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getUserId()).append(getCompanyNumber())
                .append(getStatus()).append(getCreationTime())
                .append(getConfirmationExpirationTime())
                .append(getConfirmationApprovalTime()).append(getDeletionTime()).toHashCode();
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setCompanyNumber(String companyNumber) {
        this.companyNumber = companyNumber;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreationTime(String creationTime) {
        this.creationTime = creationTime;
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

    public String getUserId() {
        return userId;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public String getStatus() {
        return status;
    }

    public String getCreationTime() {
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
}
