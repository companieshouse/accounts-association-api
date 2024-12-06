package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
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
        final var xRequestId = getXRequestId();
        return () -> {
            try {
                LOG.debugContext( xRequestId, String.format( "Sending request to accounts-user-api: GET /users/{user_id}. Attempting to fetch user details for user %s", userId ), null );
                return request.execute().getData();
            } catch( ApiErrorResponseException exception ){
                if( exception.getStatusCode() == 404 ) {
                    LOG.errorContext( xRequestId, new Exception( String.format( "Could not find user details for user with id: %s", userId ) ), null );
                    throw new NotFoundRuntimeException( "accounts-association-api", "Failed to find user" );
                } else {
                    LOG.errorContext( xRequestId, new Exception( String.format( "Failed to retrieve user details for user with id: %s", userId ) ), null );
                    throw new InternalServerErrorRuntimeException("Failed to retrieve user details");
                }
            } catch( URIValidationException exception ){
                LOG.errorContext( xRequestId, new Exception( String.format( "Failed to fetch user details for user %s, because uri was incorrectly formatted", userId ) ), null );
                throw new InternalServerErrorRuntimeException( "Invalid uri for accounts-user-api service" );
            } catch ( Exception exception ){
                LOG.errorContext( xRequestId, new Exception( String.format( "Failed to retrieve user details for user with id: %s", userId ) ), null );
                throw new InternalServerErrorRuntimeException("Failed to retrieve user details");
            }
        };

    }

    public User fetchUserDetails( final String userId ) {
        return createFetchUserDetailsRequest(userId).get();
    }

    public UsersList searchUserDetails( final List<String> emails ){

        try {
            LOG.debugContext( getXRequestId(), String.format( "Sending request to accounts-user-api: GET /users/search. Attempting to retrieve users: %s", String.join( ", ", emails ) ), null );
            return accountsUserEndpoint.searchUserDetails(emails)
                    .getData();
        } catch( URIValidationException exception ){
            LOG.errorContext( getXRequestId(), new Exception( String.format( "Search failed to fetch user details for users (%s), because uri was incorrectly formatted", String.join(", ", emails) ) ), null );
            throw new InternalServerErrorRuntimeException( "Invalid uri for accounts-user-api service" );
        } catch ( Exception exception ){
            LOG.errorContext( getXRequestId(), new Exception( String.format( "Search failed to retrieve user details for: %s", String.join(", ", emails) ) ), null );
            throw new InternalServerErrorRuntimeException("Search failed to retrieve user details");
        }

    }

    public Map<String, User> fetchUserDetails( final Stream<AssociationDao> associationDaos ){
        final Map<String, User> users = new ConcurrentHashMap<>();
        associationDaos.map( AssociationDao::getUserId )
                .distinct()
                .filter( Objects::nonNull )
                .map( this::createFetchUserDetailsRequest )
                .parallel()
                .map( Supplier::get )
                .forEach( user -> users.put( user.getUserId(), user ) );
        return users;
    }

}
