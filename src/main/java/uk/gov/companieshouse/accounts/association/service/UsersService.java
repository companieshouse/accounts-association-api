package uk.gov.companieshouse.accounts.association.service;

import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.models.Users;
import uk.gov.companieshouse.accounts.association.repositories.UsersRepository;

@Service
public class UsersService {

    private final UsersRepository usersRepository;

    public UsersService(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    public Optional<Users> fetchUserId( final String email ){
        return usersRepository.fetchUserId( email );
    }

}
