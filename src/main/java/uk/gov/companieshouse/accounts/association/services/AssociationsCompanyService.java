package uk.gov.companieshouse.accounts.association.services;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.models.Associations;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;

import java.util.List;

@Service
public class AssociationsCompanyService {

    private final AssociationsRepository associationsRepository;

    public AssociationsCompanyService(AssociationsRepository associationsRepository){
        this.associationsRepository = associationsRepository;
    }

    public List<Associations> findUsersByCompanyNumber(String companyNumber){
        return associationsRepository.findAllByCompanyNumber(companyNumber);
    }
}
