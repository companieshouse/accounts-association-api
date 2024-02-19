package uk.gov.companieshouse.accounts.association.rest;

import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.accounts.user.model.RolesList;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

@Service
public class AccountsUserEndpoint {

    private final InternalApiClient internalApiClient;

    public AccountsUserEndpoint(InternalApiClient internalApiClient) {
        this.internalApiClient = internalApiClient;
    }

    public ApiResponse<UsersList> searchUserDetails( final List<String> emails ) throws ApiErrorResponseException, URIValidationException {
        final var queryParams =
        emails.stream()
              .map( email -> String.format( "user_email=%s", email ) )
              .collect( Collectors.joining( "&" ) );

        final var searchUserDetailsUrl = String.format( "/users/search?%s", queryParams );

        return internalApiClient.privateAccountsUserResourceHandler()
                                .searchUserDetails( searchUserDetailsUrl )
                                .execute();
    }

    public ApiResponse<User> getUserDetails( final String userId ) throws ApiErrorResponseException, URIValidationException {
        final var getUserDetailsUrl = String.format( "/users/%s", userId );
        return internalApiClient.privateAccountsUserResourceHandler()
                                .getUserDetails( getUserDetailsUrl )
                                .execute();
    }

    public ApiResponse<RolesList> getUserRoles( final String userId ) throws ApiErrorResponseException, URIValidationException {
        final var getUserRolesUrl = String.format( "/users/%s/roles", userId );
        return internalApiClient.privateAccountsUserResourceHandler()
                                .getUserRoles( getUserRolesUrl )
                                .execute();
    }

    public ApiResponse<Void> setUserRoles( final String userId, final RolesList roles ) throws ApiErrorResponseException, URIValidationException {
        final var putUserRolesUrl = String.format( "/users/%s/roles", userId );
        return internalApiClient.privateAccountsUserResourceHandler()
                                .putUserRoles( putUserRolesUrl, roles )
                                .execute();
    }

}
