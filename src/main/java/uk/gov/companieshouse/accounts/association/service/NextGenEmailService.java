package uk.gov.companieshouse.accounts.association.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import uk.gov.companieshouse.accounts.association.models.emails.EmailBuilder;
import uk.gov.companieshouse.api.accounts.user.model.User;

@Service
public class NextGenEmailService extends BaseEmailService {

    protected NextGenEmailService( @Qualifier( "emailWebClient" ) final WebClient emailWebClient ) {
        super( emailWebClient );
    }

    // Example usage:
    public void sendGreetingEmail( final User recipient, final String content ){
        final var email = new EmailBuilder<String>()
                .templateId( "greeting_email_test" )
                .templateVersion( 1 )
                .addTemplateContent( content )
                .systemIsASender( true )
                .addRecipient( recipient )
                .build();
        sendEmails( email ).subscribe();
    }

}
