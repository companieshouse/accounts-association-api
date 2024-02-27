package uk.gov.companieshouse.accounts.association.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;

@Service
public class AssociationsService {

    private final AssociationsRepository associationsRepository;

    public AssociationsService(AssociationsRepository associationsRepository) {
        this.associationsRepository = associationsRepository;
    }

}