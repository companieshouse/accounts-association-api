package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonTo;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.isOAuth2Request;

import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException.BadRequest;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.companieshouse.accounts.association.client.UserClient;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
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
//        if (StringUtils.isBlank(userId)) {
//            NotFoundRuntimeException exception = new NotFoundRuntimeException(BLANK_USER_ID, new Exception(BLANK_USER_ID));
//            LOGGER.errorContext(xRequestId, BLANK_USER_ID, exception, null);
//            throw exception;
//        }

        return userClient.requestUserDetails(userId, xRequestId);
    }

    public Map<String, User> fetchUsersDetails(final Stream<AssociationDao> associations) {
        final var xRequestId = getXRequestId();
        final var userDetailsMap = new ConcurrentHashMap<String, User>();

        associations.parallel()
                .map(AssociationDao::getUserId)
                .forEach(userId -> {
                    final var userDetails = fetchUserDetails(userId, xRequestId);
                    userDetailsMap.put(userId, userDetails);
                });

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
        return userClient.requestUserDetailsByEmail(email, xRequestId);
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

    public User fetchUserDetails(final AssociationDao association) {
        return retrieveUserDetails(association.getUserId(), association.getUserEmail());
    }
}
