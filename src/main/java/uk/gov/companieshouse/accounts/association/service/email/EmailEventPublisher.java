package uk.gov.companieshouse.accounts.association.service.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;

@Component
public class EmailEventPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    public EmailEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    public void publishEmailEvent(final AssociationDao targetAssociation, final User targetUser,
            final StatusEnum newStatus, final RequestContextData requestContextData) {
        EmailEvent emailEvent = new EmailEvent(this, targetAssociation, targetUser, newStatus, requestContextData);

        applicationEventPublisher.publishEvent(emailEvent);
    }
}
