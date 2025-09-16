package uk.gov.companieshouse.accounts.association.models;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Document("user_company_associations")
@CompoundIndex(name = "company_user_idx", def = "{'company_number': 1, 'user_id': 1, 'user_email': 1}", unique = true)
public class AssociationDao {

    @Id
    private String id;

    @Indexed
    @NotNull
    @Field("company_number")
    private String companyNumber;

    @Indexed
    @Field("user_id")
    private String userId;

    @Indexed
    @Field("user_email")
    private String userEmail;

    @NotNull
    private String status;

    @NotNull
    @Field("approval_route")
    private String approvalRoute;

    private final List<InvitationDao> invitations = new ArrayList<>();

    @Field("approval_expiry_at")
    private LocalDateTime approvalExpiryAt;

    @Field("approved_at")
    private LocalDateTime approvedAt;

    @Field("removed_at")
    private LocalDateTime removedAt;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("migrated_at")
    private LocalDateTime migratedAt;

    @Field("unauthorised_at")
    private LocalDateTime unauthorisedAt;

    @Field("unauthorised_by")
    private String unauthorisedBy;

    @Field("previous_states")
    private final List<PreviousStatesDao> previousStates = new ArrayList<>();

    @NotNull
    private String etag;

    @Version
    private Integer version;

    public void setId(final String id){
        this.id = id;
    }

    public AssociationDao id(final String id){
        setId(id);
        return this;
    }

    public String getId() {
        return id;
    }

    public void setCompanyNumber(final String companyNumber){
        this.companyNumber = companyNumber;
    }

    public AssociationDao companyNumber(final String companyNumber){
        setCompanyNumber(companyNumber);
        return this;
    }

    public String getCompanyNumber() {
        return companyNumber;
    }

    public void setUserId(final String userId){
        this.userId = userId;
    }

    public AssociationDao userId(final String userId){
        setUserId(userId);
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserEmail(final String userEmail){
        this.userEmail = userEmail;
    }

    public AssociationDao userEmail(final String userEmail){
        setUserEmail(userEmail);
        return this;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setStatus(final String status){
        this.status = status;
    }

    public AssociationDao status(final String status){
        setStatus(status);
        return this;
    }

    public String getStatus() {
        return status;
    }

    public void setApprovalRoute(final String approvalRoute){
        this.approvalRoute = approvalRoute;
    }

    public AssociationDao approvalRoute(final String approvalRoute){
        setApprovalRoute(approvalRoute);
        return this;
    }


    public String getApprovalRoute() {
        return approvalRoute;
    }

    public void setInvitations(final List<InvitationDao> invitations){
        this.invitations.clear();
        this.invitations.addAll(invitations);
    }

    public AssociationDao invitations(final List<InvitationDao> invitations){
        setInvitations(invitations);
        return this;
    }

    public List<InvitationDao> getInvitations() {
        return invitations;
    }

    public void setApprovalExpiryAt(final LocalDateTime approvalExpiryAt){
        this.approvalExpiryAt = approvalExpiryAt;
    }

    public AssociationDao approvalExpiryAt(final LocalDateTime approvalExpiryAt){
        setApprovalExpiryAt(approvalExpiryAt);
        return this;
    }

    public LocalDateTime getApprovalExpiryAt() {
        return approvalExpiryAt;
    }

    public void setApprovedAt(final LocalDateTime approvedAt){
        this.approvedAt = approvedAt;
    }

    public AssociationDao approvedAt(final LocalDateTime approvedAt){
        setApprovedAt(approvedAt);
        return this;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setRemovedAt(final LocalDateTime removedAt){
        this.removedAt = removedAt;
    }

    public AssociationDao removedAt(final LocalDateTime removedAt){
        setRemovedAt(removedAt);
        return this;
    }

    public LocalDateTime getRemovedAt() {
        return removedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setMigratedAt(final LocalDateTime migratedAt){
        this.migratedAt = migratedAt;
    }

    public AssociationDao migratedAt(final LocalDateTime migratedAt){
        setMigratedAt(migratedAt);
        return this;
    }

    public LocalDateTime getMigratedAt() {
        return migratedAt;
    }

    public void setUnauthorisedAt(final LocalDateTime unauthorisedAt){
        this.unauthorisedAt = unauthorisedAt;
    }

    public AssociationDao unauthorisedAt(final LocalDateTime unauthorisedAt){
        setUnauthorisedAt(unauthorisedAt);
        return this;
    }

    public LocalDateTime getUnauthorisedAt() {
        return unauthorisedAt;
    }

    public void setUnauthorisedBy(final String unauthorisedBy){
        this.unauthorisedBy = unauthorisedBy;
    }

    public AssociationDao unauthorisedBy(final String unauthorisedBy){
        setUnauthorisedBy(unauthorisedBy);
        return this;
    }

    public String getUnauthorisedBy() {
        return unauthorisedBy;
    }

    public void setPreviousStates(final List<PreviousStatesDao> previousStates){
        this.previousStates.clear();
        this.previousStates.addAll(previousStates);
    }

    public AssociationDao previousStates(final List<PreviousStatesDao> previousStates){
        setPreviousStates(previousStates);
        return this;
    }

    public List<PreviousStatesDao> getPreviousStates() {
        return previousStates;
    }

    public void setEtag(final String etag){
        this.etag = etag;
    }

    public AssociationDao etag(final String etag){
        setEtag(etag);
        return this;
    }

    public String getEtag() {
        return etag;
    }

    public Integer getVersion() {
        return version;
    }

    @Override
    public String toString() {
        return "AssociationDao{" +
                "id='" + id + '\'' +
                ", companyNumber='" + companyNumber + '\'' +
                ", userId='" + userId + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", status='" + status + '\'' +
                ", approvalRoute='" + approvalRoute + '\'' +
                ", invitations=" + invitations +
                ", approvalExpiryAt=" + approvalExpiryAt +
                ", approvedAt=" + approvedAt +
                ", removedAt=" + removedAt +
                ", createdAt=" + createdAt +
                ", migratedAt=" + migratedAt +
                ", unauthorisedAt=" + unauthorisedAt +
                ", unauthorisedBy='" + unauthorisedBy + '\'' +
                ", previousStates=" + previousStates +
                ", etag='" + etag + '\'' +
                ", version=" + version +
                '}';
    }
}