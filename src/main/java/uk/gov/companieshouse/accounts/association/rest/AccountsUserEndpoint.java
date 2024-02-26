package uk.gov.companieshouse.accounts.association.rest;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.service.ApiClientService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

@Service
public class AccountsUserEndpoint {

    private final ApiClientService apiClientService;

    public AccountsUserEndpoint(ApiClientService apiClientService) {
        this.apiClientService = apiClientService;
    }

    public ApiResponse<UsersList> searchUserDetails( final List<String> emails ) throws ApiErrorResponseException, URIValidationException {
        final var searchUserDetailsUrl = "/users/search";
        return apiClientService.getInternalApiClient()
                               .privateAccountsUserResourceHandler()
                               .searchUserDetails( searchUserDetailsUrl, emails )
                               .execute();
    }

    public ApiResponse<User> getUserDetails( final String userId ) throws ApiErrorResponseException, URIValidationException {
        final var getUserDetailsUrl = String.format( "/users/%s", userId );
        return apiClientService.getInternalApiClient()
                               .privateAccountsUserResourceHandler()
                               .getUserDetails( getUserDetailsUrl )
                               .execute();
    }

}
