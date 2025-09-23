package uk.gov.companieshouse.accounts.association.service.email;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.service.EmailService;

@Component
public class EmailEventListener implements ApplicationListener<EmailEvent> {

    private final EmailService emailService;

    @Autowired
    public EmailEventListener(EmailService emailService) {
        this.emailService = emailService;
    }

    @Override
    public void onApplicationEvent(EmailEvent event) {
        emailService.sendStatusUpdateEmails(event.getTargetAssociation(), event.getTargetUser(), event.getNewStatus(), event.getRequestContextData());
    }
}
