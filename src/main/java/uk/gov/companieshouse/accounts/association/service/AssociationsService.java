package uk.gov.companieshouse.accounts.association.service;

import java.util.HashSet;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;


@Transactional
@Service
public class AssociationsService {

    private final AssociationsRepository associationsRepository;

    public AssociationsService(AssociationsRepository associationsRepository) {
        this.associationsRepository = associationsRepository;
    }

    public Page<Association> fetchAssociatedUsers( final String companyNumber, final boolean includeRemoved, final int itemsPerPage, final int pageIndex ){
        final Pageable pageable = PageRequest.of( pageIndex, itemsPerPage );

        final var statuses = new HashSet<>( Set.of( StatusEnum.CONFIRMED, StatusEnum.AWAITING_APPROVAL ) );
        if ( includeRemoved )
            statuses.add( StatusEnum.REMOVED );

        return associationsRepository.fetchAssociatedUsers( companyNumber, statuses, pageable );
    }

}