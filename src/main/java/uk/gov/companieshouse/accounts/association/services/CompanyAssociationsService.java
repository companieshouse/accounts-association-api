package uk.gov.companieshouse.accounts.association.services;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.models.Associations;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.repositories.UsersRepository;
import uk.gov.companieshouse.api.accounts.associations.model.UserInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class CompanyAssociationsService {

    private final AssociationsRepository associationsRepository;

    private final UsersRepository usersRepository;
    public CompanyAssociationsService(AssociationsRepository associationsRepository, UsersRepository usersRepository){
        this.associationsRepository = associationsRepository;
        this.usersRepository = usersRepository;
    }

    public List<UserInfo> getUsersByCompanyNumber(final String companyNumber){
        List<String> associatedUserIds= Optional.ofNullable(associationsRepository
                .findAllByCompanyNumber(companyNumber)).orElse(new ArrayList<>())
                .stream()
                .map(Associations::getUserId)
                .toList();

       /* usersRepository.findAllById(associatedUserIds)
                .stream()
                .reduce();*/



        return null;
    }
}
