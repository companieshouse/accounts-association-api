package uk.gov.companieshouse.accounts.association.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.models.UserInfo;
import uk.gov.companieshouse.accounts.association.models.Users;
import uk.gov.companieshouse.accounts.association.repositories.UsersRepository;
import uk.gov.companieshouse.accounts.association.rest.UserInfoMockEndpoint;

@Service
public class UsersService {

    private final UsersRepository usersRepository;
    private final UserInfoMockEndpoint userInfoMockEndpoint;

    public UsersService(UsersRepository usersRepository, UserInfoMockEndpoint userInfoMockEndpoint) {
        this.usersRepository = usersRepository;
        this.userInfoMockEndpoint = userInfoMockEndpoint;
    }

    public Optional<Users> fetchUserId( final String email ){
        return usersRepository.fetchUserId( email );
    }

    public Optional<UserInfo> fetchUserInfo( final String userEmail ){
        return userInfoMockEndpoint.fetchUserInfo( userEmail );
    }

}
