package uk.gov.companieshouse.accounts.association.models;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class UserInfo {

    private String userId;

    private String userEmail;

    private String displayName;

    private String authorisationStatus;

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

    public String getAuthorisationStatus() {
        return authorisationStatus;
    }

    public void setAuthorisationStatus(String authorisationStatus) {
        this.authorisationStatus = authorisationStatus;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof UserInfo userInfo)) {
            return false;
        }

        return new EqualsBuilder().append(getUserId(),
                        userInfo.getUserId()).append(getUserEmail(), userInfo.getUserEmail())
                .append(getDisplayName(), userInfo.getDisplayName())
                .append(getAuthorisationStatus(), userInfo.getAuthorisationStatus()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).append(getUserId()).append(getUserEmail())
                .append(getDisplayName()).append(getAuthorisationStatus()).toHashCode();
    }

    @Override
    public String toString() {
        return "UserInfo{" +
                "userId='" + userId + '\'' +
                ", userEmail='" + userEmail + '\'' +
                ", displayName='" + displayName + '\'' +
                ", authorisationStatus='" + authorisationStatus + '\'' +
                '}';
    }

}
