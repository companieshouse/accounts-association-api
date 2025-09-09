package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonTo;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.isOAuth2Request;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;

@Service
public class UsersService {

    private final RestClient usersRestClient;

    private static final String REST_CLIENT_EXCEPTION = "Encountered rest client exception when fetching user details for: %s";

    @Autowired
    private UsersService(@Qualifier("usersRestClient") final RestClient usersRestClient) {
        this.usersRestClient = usersRestClient;
    }

    public User fetchUserDetails(final String userId, final String xRequestId) {
        if (userId == null) {
            IllegalArgumentException exception = new IllegalArgumentException("UserID cannot be null");
            LOGGER.errorContext(xRequestId, "UserID cannot be null", exception, null);
            throw exception;
        }

        final var uri = UriComponentsBuilder.newInstance()
                .path("/users/{user}")
                .buildAndExpand(userId)
                .toUri();

        final var response = usersRestClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        return parseJsonTo(User.class).apply(response);
    }

    public Map<String, User> fetchUserDetails(final Stream<AssociationDao> associations) {
        final var xRequestId = getXRequestId();
        final var userDetailsMap = new ConcurrentHashMap<String, User>();

        associations.parallel()
                .map(AssociationDao::getUserId)
                .forEach(userId -> {
                    try {
                        final var userDetails = fetchUserDetails(userId, xRequestId);
                        userDetailsMap.put(userId, userDetails);
                    } catch (RestClientException exception) {
                        LOGGER.errorContext(xRequestId, String.format(REST_CLIENT_EXCEPTION, userId), exception, null);
                    }
                });

        return userDetailsMap;
    }

    public UsersList searchUserDetailsbByEmail(final List<String> emails) {
        final var xRequestId = getXRequestId();
        if (emails == null) {
            IllegalArgumentException exception = new IllegalArgumentException("Emails cannot be null");
            LOGGER.errorContext(xRequestId, "Emails cannot be null", exception, null);
            throw exception;
        }

        final var userList = Collections.synchronizedList(new UsersList());

        emails.stream()
                .parallel()
                .forEach(email -> {
                    try {
                        var userDetails = fetchUserDetailsByEmail(email);
                        userList.add(userDetails);
                    } catch (RestClientException exception) {
                        LOGGER.errorContext(xRequestId, String.format(REST_CLIENT_EXCEPTION, email), exception, null);
                    }
                });

        return (UsersList) userList;
    }

    public User fetchUserDetailsByEmail(final String email) {
        final var uri = UriComponentsBuilder.newInstance()
                .path("/users/search")
                .queryParam("user_email", "{email}")
                .buildAndExpand(email)
                .encode()
                .toUri();

        final var response = usersRestClient.get()
                .uri(uri)
                .retrieve()
                .body(String.class);

        return parseJsonTo(User.class).apply(response);
    }

    public User retrieveUserDetails(final String targetUserId, final String targetUserEmail) {
        final var fetchedByUserId = Optional.ofNullable(targetUserId).map(userId -> {
            if (isOAuth2Request() && userId.equals(getEricIdentity())) {
                return getUser();
            }
            return fetchUserDetails(userId, getXRequestId());
        }).orElse(null);

        if (Objects.nonNull(fetchedByUserId)) {
            return fetchedByUserId;
        }

        return Optional.ofNullable(targetUserEmail).map(userEmail -> {
            if (isOAuth2Request() && userEmail.equals(getUser().getEmail())) {
                return getUser();
            }
            return Optional.of(userEmail).map(List::of).map(this::searchUserDetailsbByEmail)
                    .filter(list -> !list.isEmpty()).map(List::getFirst).orElse(null);
        }).orElse(null);
    }

    public User fetchUserDetails(final AssociationDao association) {
        return retrieveUserDetails(association.getUserId(), association.getUserEmail());
    }
}
