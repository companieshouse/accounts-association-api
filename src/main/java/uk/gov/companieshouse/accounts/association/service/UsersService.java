package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
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
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.service.client.UserClient;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.utils.AssociationDaoStreamSplitter;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;

@Service
public class UsersService {

    private final UserClient userClient;

    private static final String BLANK_USER_ID = "UserID cannot be blank";
    private static final String EMAIL_IN_LIST_CANNOT_BE_NULL = "Email in list cannot be null";

    @Autowired
    private UsersService(final UserClient userClient) {
        this.userClient = userClient;
    }

    public User fetchUserDetails(final String userId, final String xRequestId) {
        if (StringUtils.isBlank(userId)) {
            NotFoundRuntimeException exception = new NotFoundRuntimeException(BLANK_USER_ID, new Exception(BLANK_USER_ID));
            LOGGER.errorContext(xRequestId, BLANK_USER_ID, exception, null);
            throw exception;
        }
        LOGGER.debugContext(xRequestId, "Searching for users by userId: " + userId, null);

        return userClient.requestUserDetails(userId, xRequestId);
    }

    public Map<String, User> fetchUsersDetails(final Stream<AssociationDao> associations) {
        final var xRequestId = getXRequestId();
        final var userDetailsMap = new ConcurrentHashMap<String, User>();

        final var splitStreams = associations.parallel()
                .collect(Collector.of(AssociationDaoStreamSplitter::new, AssociationDaoStreamSplitter::accept, AssociationDaoStreamSplitter::merge));

        if (!splitStreams.userIds.isEmpty()) {
            splitStreams.userIds.parallelStream()
                    .forEach(userId -> {
                        final var userDetails = fetchUserDetails(userId, xRequestId);
                        userDetailsMap.put(userId, userDetails);
                    });
        }

        if (!splitStreams.userEmails.isEmpty()) {
            splitStreams.userEmails.parallelStream()
                    .forEach(userEmail -> fetchUserDetailsByEmail(userEmail, xRequestId)
                    .forEach( user -> userDetailsMap.put(user.getUserId(), user)));
        }

        return userDetailsMap;
    }

    public UsersList searchUsersDetailsByEmail(final List<String> emails) {
        final var xRequestId = getXRequestId();
        if (emails == null) {
            IllegalArgumentException exception = new IllegalArgumentException("Emails cannot be null");
            LOGGER.errorContext(xRequestId, "Emails cannot be null", exception, null);
            throw exception;
        }
        if (emails.stream().anyMatch(Objects::isNull)) {
            InternalServerErrorRuntimeException exception = new InternalServerErrorRuntimeException(EMAIL_IN_LIST_CANNOT_BE_NULL,
                    new Exception(EMAIL_IN_LIST_CANNOT_BE_NULL));
            LOGGER.errorContext(xRequestId, EMAIL_IN_LIST_CANNOT_BE_NULL, exception, null);
            throw exception;
        }

        final var synchronizedList = Collections.synchronizedList(new UsersList());

        emails.stream()
                .parallel()
                .forEach(email -> {
                    var userDetails = fetchUserDetailsByEmail(email, xRequestId);
                    if (Objects.nonNull(userDetails)) {
                        synchronizedList.addAll(userDetails);
                    }
                });

        UsersList usersList = null;
        if (!synchronizedList.isEmpty()) {
            usersList = new UsersList();
            usersList.addAll(synchronizedList);
        }
        return usersList;
    }

    public UsersList fetchUserDetailsByEmail(final String email, final String xRequestId) {
        LOGGER.debugContext(xRequestId, "Searching for users by email: " + String.join(",", email), null);
        return userClient.requestUserDetailsByEmail(email, xRequestId);
    }

    public User retrieveUserDetails(final String xRequestId, final String targetUserId, final String targetUserEmail) {
        LOGGER.traceContext(xRequestId, "Attempting to fetch user by id: " + targetUserId, null);
        final var fetchedByUserId = Optional.ofNullable(targetUserId).map(userId -> {
            if (isOAuth2Request() && userId.equals(getEricIdentity())) {
                return getUser();
            }
            return fetchUserDetails(userId, getXRequestId());
        }).orElse(null);

        if (Objects.nonNull(fetchedByUserId)) {
            return fetchedByUserId;
        }
        LOGGER.traceContext(xRequestId, "Fetching user by id: " + targetUserId + " failed", null);

        LOGGER.traceContext(xRequestId, "Attempting to fetch user by email: " + targetUserEmail, null);
        return Optional.ofNullable(targetUserEmail)
                .map(userEmail -> {
                    if (isOAuth2Request() && userEmail.equals(getUser().getEmail())) {
                        return getUser();
                    }
                    return Optional.of(userEmail)
                            .map(List::of)
                            .map(this::searchUsersDetailsByEmail)
                            .filter(list -> !list.isEmpty())
                            .map(List::getFirst)
                            .orElse(null);
                }).orElse(null);

    }

    public User fetchUserDetails(final String xRequestId, final AssociationDao association) {
        return retrieveUserDetails(xRequestId, association.getUserId(), association.getUserEmail());
    }
}
