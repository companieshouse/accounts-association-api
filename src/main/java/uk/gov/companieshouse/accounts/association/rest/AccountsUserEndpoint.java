package uk.gov.companieshouse.accounts.association.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.utils.ApiClientUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.accountsuser.request.PrivateAccountsUserUserGet;
import uk.gov.companieshouse.api.handler.exception.URIValidationException;
import uk.gov.companieshouse.api.model.ApiResponse;

import java.util.List;

@Service
public class AccountsUserEndpoint {

    @Value("${internal.api.url}")
    private String internalApiUrl;

    private final ApiClientUtil apiClientUtil;

    @Autowired
    public AccountsUserEndpoint(ApiClientUtil apiClientUtil) {
        this.apiClientUtil = apiClientUtil;
    }

    public ApiResponse<UsersList> searchUserDetails(final List<String> emails) throws ApiErrorResponseException, URIValidationException {
        final var searchUserDetailsUrl = "/users/search";
        return apiClientUtil.getInternalApiClient(internalApiUrl)
                .privateAccountsUserResourceHandler()
                .searchUserDetails(searchUserDetailsUrl, emails)
                .execute();
    }

    public PrivateAccountsUserUserGet createGetUserDetailsRequest(final String userId) {
        final var getUserDetailsUrl = String.format("/users/%s", userId);
        return apiClientUtil.getInternalApiClient(internalApiUrl)
                            .privateAccountsUserResourceHandler()
                            .getUserDetails(getUserDetailsUrl);
    }

    public ApiResponse<User> getUserDetails(final String userId) throws ApiErrorResponseException, URIValidationException {
        return createGetUserDetailsRequest(userId).execute();
    }

}
