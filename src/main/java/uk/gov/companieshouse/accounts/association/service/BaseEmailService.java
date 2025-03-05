package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.APPLICATION_NAMESPACE;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.api.chs_notification_sender.model.EmailDetails;
import uk.gov.companieshouse.api.chs_notification_sender.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_notification_sender.model.RecipientDetailsEmail;
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
        final var templateIds = email.getEmailDetails()
                .stream()
                .map( emailDetails -> String.format( "%s:%s", emailDetails.getTemplateId(), emailDetails.getTemplateVersion() ) )
                .collect( Collectors.joining( ", " ) );

        final var senders = email.getSenderDetails()
                .stream()
                .map( SenderDetails::getUserId )
                .map( userId -> Objects.isNull( userId ) ? APPLICATION_NAMESPACE : userId )
                .collect( Collectors.joining( ", " ) );

        final var recipients = email.getRecipientDetails()
                .stream()
                .map( RecipientDetailsEmail::getEmailAddress )
                .collect( Collectors.joining( ", " ) );

        final var sentAt = email.getCreatedAt();

        final var content = email.getEmailDetails()
                .stream()
                .map( EmailDetails::getPersonalisationDetails )
                .collect( Collectors.joining( ", " ) );

        return String.format( "[%s] emails from [%s] to [%s] at [%s]. Email content: [%s]", templateIds, senders, recipients, sentAt, content );
    }

    protected Mono<List<String>> sendEmails( final GovUkEmailDetailsRequest email ){

        final var references = Mono.just( email )
                .map( GovUkEmailDetailsRequest::getSenderDetails )
                .flatMapMany( Flux::fromIterable )
                .map( SenderDetails::getReference )
                .collectList();

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
                .then( references );
    }

}
