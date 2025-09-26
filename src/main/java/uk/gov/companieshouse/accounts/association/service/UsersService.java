package uk.gov.companieshouse.accounts.association.service;

import java.net.URI;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.NOT_FOUND;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonTo;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.*;

@Service
public class UsersService {

    private final WebClient usersWebClient;

    private UsersService( @Qualifier( "usersWebClient" ) final WebClient usersWebClient ){
        this.usersWebClient = usersWebClient;
    }

    public Mono<User> toFetchUserDetailsRequest( final String userId, final String xRequestId ) {
        return usersWebClient.get()
                .uri(uriBuilder -> UriComponentsBuilder.fromUri(uriBuilder.build())
                        .path("/users/{user}")
                        .buildAndExpand(userId)
                        .toUri())
                .header( "X-Request-Id", xRequestId )
                .retrieve()
                .bodyToMono( String.class )
                .map( parseJsonTo( User.class ) )
                .onErrorMap( throwable -> {
                    if ( throwable instanceof WebClientResponseException exception && NOT_FOUND.equals( exception.getStatusCode() ) ){
                        return new NotFoundRuntimeException( "Failed to find user", exception );
                    }
                    throw new InternalServerErrorRuntimeException( "Failed to retrieve user details", (Exception) throwable );
                } )
                .doOnSubscribe( onSubscribe -> LOGGER.infoContext( xRequestId, String.format( "Sending request to accounts-user-api: GET /users/{user_id}. Attempting to retrieve user: %s", userId ), null ) )
                .doFinally( signalType -> LOGGER.infoContext( xRequestId, String.format( "Finished request to accounts-user-api for user: %s", userId ), null ) );
    }

    public User fetchUserDetails(final String userId, final String xRequestId ){
        return toFetchUserDetailsRequest( userId, xRequestId ).block( Duration.ofSeconds( 20L ) );
    }

    public Map<String, User> fetchUserDetails( final Stream<AssociationDao> associations ){
        final var xRequestId = getXRequestId();
        return Flux.fromStream( associations )
                .filter( association -> Objects.nonNull( association.getUserId() ) )
                .map( AssociationDao::getUserId )
                .distinct()
                .flatMap( userId -> toFetchUserDetailsRequest( userId, xRequestId ) )
                .collectMap( User::getUserId )
                .block( Duration.ofSeconds( 20L ) );
    }

    public UsersList searchUserDetails( final List<String> emails ) {
        final var xRequestId = getXRequestId();
        return usersWebClient.get()
                .uri(uriBuilder -> UriComponentsBuilder.fromUri(uriBuilder.build())
                        .path("/users/search")
                        .queryParam("user_email", "{emails}")
                        .encode()
                        .buildAndExpand(String.join(",", emails))
                        .toUri())
                .retrieve()
                .bodyToMono( String.class )
                .map( parseJsonTo( UsersList.class ) )
                .onErrorMap( throwable -> {
                    throw new InternalServerErrorRuntimeException( "Failed to retrieve user details", (Exception) throwable );
                } )
                .doOnSubscribe( onSubscribe -> LOGGER.infoContext( xRequestId,  "Sending request to accounts-user-api: GET /users/search. Attempting to retrieve users" , null ) )
                .doFinally( signalType -> LOGGER.infoContext( xRequestId, "Finished request to accounts-user-api for users",  null ) )
                .block( Duration.ofSeconds( 20L ) );
    }

    public User retrieveUserDetails( final String targetUserId, final String targetUserEmail ){
        final var fetchedByUserId = Optional.ofNullable( targetUserId )
                .map( userId -> {
                    if ( isOAuth2Request() && userId.equals( getEricIdentity() ) ){
                        return getUser();
                    }
                    return fetchUserDetails( userId, getXRequestId() );
                } )
                .orElse( null );

        if ( Objects.nonNull( fetchedByUserId ) ){
            return fetchedByUserId;
        }

        return Optional.ofNullable( targetUserEmail )
                .map( userEmail -> {
                    if ( isOAuth2Request() && userEmail.equals( getUser().getEmail() ) ){
                        return getUser();
                    }
                    return Optional.of( userEmail )
                            .map( List::of )
                            .map( this::searchUserDetails )
                            .filter( list -> !list.isEmpty() )
                            .map( List::getFirst )
                            .orElse( null );
                } )
                .orElse( null );
    }

    public User fetchUserDetails( final AssociationDao association ){
        return retrieveUserDetails( association.getUserId(), association.getUserEmail() );
    }

}
