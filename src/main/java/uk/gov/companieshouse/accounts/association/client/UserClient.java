package uk.gov.companieshouse.accounts.association.client;

import static uk.gov.companieshouse.accounts.association.models.Constants.REST_CLIENT_EXCEPTION;
import static uk.gov.companieshouse.accounts.association.models.Constants.REST_CLIENT_FINISH;
import static uk.gov.companieshouse.accounts.association.models.Constants.REST_CLIENT_START;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonTo;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException.BadRequest;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;

@Component
public class UserClient {
    private final RestClient usersRestClient;

    @Autowired
    public UserClient(@Qualifier("usersRestClient") RestClient usersRestClient) {
        this.usersRestClient = usersRestClient;
    }

    public UsersList requestUserDetailsByEmail(final String email, String xRequestId) {
        final var uri = UriComponentsBuilder.newInstance()
                .path("/users/search")
                .queryParam("user_email", "{email}")
                .encode()
                .buildAndExpand(email)
                .toUri();

        return parseJsonTo(sendRequest(uri, xRequestId), UsersList.class);
    }

    public User requestUserDetails(final String userId, final String xRequestId) {
        final var uri = UriComponentsBuilder.newInstance()
                .path("/users/{user}")
                .buildAndExpand(userId)
                .toUri();

        return parseJsonTo(sendRequest(uri, xRequestId), User.class);
    }

    private String sendRequest(URI uri, String xRequestId) {
        try {
            LOGGER.infoContext(xRequestId, String.format(REST_CLIENT_START, uri), null);
            var response = usersRestClient.get().uri(uri).retrieve().body(String.class);
            LOGGER.infoContext(xRequestId, String.format(REST_CLIENT_FINISH, uri), null);
            return response;
        } catch (NotFound exception) {
            LOGGER.infoContext(getXRequestId(), String.format("No user found: %s", uri), null);
            throw new NotFoundRuntimeException(exception.getMessage(), exception);
        } catch (BadRequest exception) {
            LOGGER.errorContext(getXRequestId(), String.format("Bad request made: %s", uri), exception, null);
            throw new InternalServerErrorRuntimeException(exception.getMessage(), exception);
        } catch (RestClientException exception) {
            LOGGER.errorContext(getXRequestId(), String.format(REST_CLIENT_EXCEPTION, uri), exception, null);
            throw new InternalServerErrorRuntimeException(exception.getMessage(), exception);
        }
    }

}
