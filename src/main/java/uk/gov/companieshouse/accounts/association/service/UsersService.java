package uk.gov.companieshouse.accounts.association.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.rest.AccountsUserEndpoint;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.accountsuser.request.PrivateAccountsUserUserGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

@Service
public class UsersService {

    private final AccountsUserEndpoint accountsUserEndpoint;

    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    public UsersService(AccountsUserEndpoint accountsUserEndpoint) {
        this.accountsUserEndpoint = accountsUserEndpoint;
    }

    public Supplier<User> createFetchUserDetailsRequest( final String userId ) {
        PrivateAccountsUserUserGet request = accountsUserEndpoint.createGetUserDetailsRequest(userId);
        return () -> {
            try {
                LOG.debug( String.format( "Attempting to fetch user details for user %s", userId ) );
                return request.execute().getData();
            } catch( ApiErrorResponseException exception ){
                if( exception.getStatusCode() == 404 ) {
                    LOG.error( String.format( "Could not find user details for user %s", userId ) );
                    throw new NotFoundRuntimeException( "accounts-association-api", "Failed to find user" );
                } else {
                    LOG.error( String.format( "Failed to retrieve user details for user %s", userId ) );
                    throw new InternalServerErrorRuntimeException("Failed to retrieve user details");
                }
            } catch( URIValidationException exception ){
                LOG.error( String.format( "Failed to fetch user details for user %s, because uri was incorrectly formatted", userId ) );
                throw new InternalServerErrorRuntimeException( "Invalid uri for accounts-user-api service" );
            } catch ( Exception exception ){
                LOG.error( String.format( "Failed to retrieve user details for user %s", userId ) );
                throw new InternalServerErrorRuntimeException("Failed to retrieve user details");
            }
        };

    }

    public User fetchUserDetails( final String userId ) {
        return createFetchUserDetailsRequest(userId).get();
    }

    public UsersList searchUserDetails( final List<String> emails ){

        try {
            LOG.debug( String.format( "Attempting to search for user details for: %s", String.join(", ", emails) ) );
            return accountsUserEndpoint.searchUserDetails(emails)
                    .getData();
        } catch( URIValidationException exception ){
            LOG.error( String.format( "Search failed to fetch user details for users (%s), because uri was incorrectly formatted", String.join(", ", emails) ) );
            throw new InternalServerErrorRuntimeException( "Invalid uri for accounts-user-api service" );
        } catch ( Exception exception ){
            LOG.error( String.format( "Search failed to retrieve user details for: %s", String.join(", ", emails) ) );
            throw new InternalServerErrorRuntimeException("Search failed to retrieve user details");
        }

    }

}
