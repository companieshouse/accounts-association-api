package uk.gov.companieshouse.accounts.association.service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListCompanyDaoToDtoMapper;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;

@Transactional
@Service
public class AssociationsService {

    private final AssociationsRepository associationsRepository;
    private final AssociationsListCompanyDaoToDtoMapper associationsListCompanyDaoToDtoMapper;

    public AssociationsService(AssociationsRepository associationsRepository, AssociationsListCompanyDaoToDtoMapper associationsListCompanyDaoToDtoMapper) {
        this.associationsRepository = associationsRepository;
        this.associationsListCompanyDaoToDtoMapper = associationsListCompanyDaoToDtoMapper;
    }

    public AssociationsList fetchAssociatedUsers( final String companyNumber, final boolean includeRemoved, final int itemsPerPage, final int pageIndex ){
        final Pageable pageable = PageRequest.of( pageIndex, itemsPerPage );

        final var statuses = new HashSet<>( Set.of( StatusEnum.CONFIRMED, StatusEnum.AWAITING_APPROVAL ) );
        if ( includeRemoved )
            statuses.add( StatusEnum.REMOVED );

        final var associations = associationsRepository.fetchAssociatedUsers( companyNumber, statuses, pageable );

        final var endpointUri = String.format( "/associations/companies/%s", companyNumber );
        final var companyNumberAndEndpointUri = Map.of( "companyNumber", companyNumber, "endpointUri", endpointUri );

        return associationsListCompanyDaoToDtoMapper.daoToDto( associations, companyNumberAndEndpointUri );
    }

}