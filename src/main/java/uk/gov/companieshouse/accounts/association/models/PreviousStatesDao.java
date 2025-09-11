package uk.gov.companieshouse.accounts.association.models;

import java.time.LocalDateTime;
import org.springframework.data.mongodb.core.mapping.Field;

public class PreviousStatesDao {

    private String status;

    @Field("changed_by")
    private String changedBy;

    @Field("changed_at")
    private LocalDateTime changedAt;

    public void setStatus(final String status) {
        this.status = status;
    }

    public PreviousStatesDao status(final String status){
        setStatus(status);
        return this;
    }

    public String getStatus() {
        return status;
    }

    public void setChangedBy(final String changedBy) {
        this.changedBy = changedBy;
    }

    public PreviousStatesDao changedBy(final String changedBy){
        setChangedBy(changedBy);
        return this;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedAt(final LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public PreviousStatesDao changedAt(final LocalDateTime changedAt){
        setChangedAt(changedAt);
        return this;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }


    @Override
    public String toString() {
        return "PreviousStatesDao{" +
                "status='" + status + '\'' +
                ", changedBy='" + changedBy + '\'' +
                ", changedAt=" + changedAt +
                '}';
    }

}
