package uk.gov.companieshouse.accounts.association.service.email;

import org.springframework.context.ApplicationEvent;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;

public class EmailEvent extends ApplicationEvent {

    private final transient AssociationDao targetAssociation;
    private final transient User targetUser;
    private final StatusEnum newStatus;
    private final transient RequestContextData requestContextData;

    public EmailEvent(final Object source, final AssociationDao targetAssociation, final User targetUser,
            final StatusEnum newStatus, final RequestContextData requestContextData) {
        super(source);
        this.targetAssociation = targetAssociation;
        this.targetUser = targetUser;
        this.newStatus = newStatus;
        this.requestContextData = requestContextData;
    }

    public AssociationDao getTargetAssociation() {
        return targetAssociation;
    }

    public User getTargetUser() {
        return targetUser;
    }

    public StatusEnum getNewStatus() {
        return newStatus;
    }

    public RequestContextData getRequestContextData() {
        return requestContextData;
    }
}
