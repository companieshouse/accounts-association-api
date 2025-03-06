package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.APPLICATION_NAMESPACE;

import java.util.Objects;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.api.chs_notification_sender.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_notification_sender.model.SenderDetails;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public abstract class BaseEmailService {

    private final WebClient emailWebClient;

    protected static final Logger LOG = LoggerFactory.getLogger( APPLICATION_NAMESPACE );

    protected BaseEmailService( final WebClient emailWebClient ){
        this.emailWebClient = emailWebClient;
    }

    private String mapToLogMessage( final GovUkEmailDetailsRequest email ){
        final var emailDetails = email.getEmailDetails();
        final var senderDetails = email.getSenderDetails();

        final var template = String.format( "%s:%s", emailDetails.getTemplateId(), emailDetails.getTemplateVersion() );
        final var sender = Objects.isNull( senderDetails.getUserId() ) ? APPLICATION_NAMESPACE : senderDetails.getUserId();
        final var recipient = email.getRecipientDetails().getEmailAddress();
        final var sentAt = email.getCreatedAt();
        final var content = email.getEmailDetails().getPersonalisationDetails();

        return String.format( "[%s] email from [%s] to [%s] at [%s]. Email content: [%s]", template, sender, recipient, sentAt, content );
    }

    protected Mono<String> sendEmail( final GovUkEmailDetailsRequest email ){
        final var xRequestId = getXRequestId();
        return emailWebClient.post()
                .uri( "/notification-sender/email" )
                .bodyValue( email )
                .retrieve()
                .bodyToMono( Void.class )
                .onErrorMap( throwable -> {
                    LOG.errorContext( xRequestId, String.format( "Failed to send %s", mapToLogMessage( email ) ), (Exception) throwable, null );
                    throw new InternalServerErrorRuntimeException( "Failed to send email" );
                } )
                .doOnSuccess( onSuccess -> LOG.infoContext( xRequestId, String.format( "Successfully sent %s", mapToLogMessage( email ) ), null ) )
                .doOnSubscribe( onSubscribe -> LOG.infoContext( xRequestId, "Sending request to chs-notification-sender-api: POST /notification-sender/email.", null ) )
                .doFinally( signalType -> LOG.infoContext( xRequestId, "Finished request to chs-notification-sender-api", null ) )
                .then( Mono.just( email ).map( GovUkEmailDetailsRequest::getSenderDetails ).map( SenderDetails::getReference ) );
    }

}
