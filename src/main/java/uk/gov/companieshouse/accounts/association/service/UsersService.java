package uk.gov.companieshouse.accounts.association.service;

import org.springframework.stereotype.Service;


import uk.gov.companieshouse.accounts.association.rest.UserInfoMockEndpoint;
import uk.gov.companieshouse.api.accounts.associations.model.UserInfo;

import java.util.Optional;

@Service
public class UsersService {

    private final UserInfoMockEndpoint userInfoMockEndpoint;

    public UsersService(UserInfoMockEndpoint userInfoMockEndpoint) {
        this.userInfoMockEndpoint = userInfoMockEndpoint;
    }

    public Optional<UserInfo> fetchUserInfo(final String userEmail) {
        return userInfoMockEndpoint.fetchUserInfo(userEmail);
    }

}
