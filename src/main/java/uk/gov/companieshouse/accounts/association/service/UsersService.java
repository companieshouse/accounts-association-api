package uk.gov.companieshouse.accounts.association.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.rest.AccountsUserEndpoint;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;

@Service
public class UsersService {

    private final AccountsUserEndpoint accountsUserEndpoint;

    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    public UsersService(AccountsUserEndpoint accountsUserEndpoint) {
        this.accountsUserEndpoint = accountsUserEndpoint;
    }

    public User fetchUserDetails(final String userId) {

        try {
            LOG.debug(String.format("Attempting to fetch user details for user %s", userId));
            return accountsUserEndpoint.getUserDetails(userId)
                    .getData();
        } catch (ApiErrorResponseException exception) {
            if (exception.getStatusCode() == 404) {
                LOG.errorContext(String.format("Could not find user details for user %s", userId), exception, null);
                throw new NotFoundRuntimeException("accounts-association-api", "Failed to find user");
            } else {
                LOG.errorContext(String.format("Failed to retrieve user details for user %s", userId), exception, null);
                throw new InternalServerErrorRuntimeException("Failed to retrieve user details");
            }
        } catch (URIValidationException exception) {
            LOG.errorContext(String.format("Failed to fetch user details for user %s, because uri was incorrectly formatted", userId), exception, null);
            throw new InternalServerErrorRuntimeException("Invalid uri for accounts-user-api service");
        } catch (Exception exception) {
            LOG.errorContext(String.format("Failed to retrieve user details for user %s", userId), exception, null);
            throw new InternalServerErrorRuntimeException("Failed to retrieve user details");
        }

    }

    public List<User> searchUserDetails(final List<String> emails) {

        try {
            LOG.debug(String.format("Attempting to fetch user details for users %s", String.join(",", emails)));
            return accountsUserEndpoint.searchUserDetails(emails)
                    .getData();
        } catch (URIValidationException | ApiErrorResponseException | Exception exception) {
            LOG.errorContext(String.format("Failed to retrieve user details for users %s", String.join(",", emails)), exception, null);
            throw new InternalServerErrorRuntimeException("Failed to retrieve user details");
        }

    }

}
