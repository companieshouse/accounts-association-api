package uk.gov.companieshouse.accounts.association.service;

import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.accounts.association.mapper.AssociationMapper;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListCompanyMapper;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListUserMapper;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.user.model.User;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import uk.gov.companieshouse.api.company.CompanyDetails;

@Transactional
@Service
public class AssociationsService {


    private final AssociationsRepository associationsRepository;


    private final AssociationsListUserMapper associationsListUserMapper;

    private final AssociationsListCompanyMapper associationsListCompanyMapper;

    private final AssociationMapper associationMapper;


    @Autowired
    public AssociationsService(AssociationsRepository associationsRepository,
                               AssociationsListUserMapper associationsListUserMapper,
                               AssociationsListCompanyMapper associationsListCompanyMapper,
                               AssociationMapper associationMapper) {
        this.associationsRepository = associationsRepository;
        this.associationsListUserMapper = associationsListUserMapper;
        this.associationsListCompanyMapper = associationsListCompanyMapper;
        this.associationMapper = associationMapper;
    }

    @Transactional(readOnly = true)
    public AssociationsList fetchAssociationsForUserStatusAndCompany(
            @NotNull final User user, List<String> status, final Integer pageIndex, final Integer itemsPerPage,
            final String companyNumber) {

        if (Objects.isNull(status) || status.isEmpty()) {
            status = Collections.singletonList(Association.StatusEnum.CONFIRMED.getValue());

        }

        Page<AssociationDao> results = associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike(
                user.getUserId(), status, Optional.ofNullable(companyNumber).orElse(""), PageRequest.of(pageIndex, itemsPerPage));


        return associationsListUserMapper.daoToDto(results, user);
    }

    public AssociationsList fetchAssociatedUsers( final String companyNumber, final CompanyDetails companyDetails, final boolean includeRemoved, final int itemsPerPage, final int pageIndex ){
        final Pageable pageable = PageRequest.of( pageIndex, itemsPerPage );

        final var statuses = new HashSet<>( Set.of( StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue() ) );
        if ( includeRemoved )
            statuses.add( StatusEnum.REMOVED.getValue() );

        final var associations = associationsRepository.fetchAssociatedUsers( companyNumber, statuses, pageable );

        return associationsListCompanyMapper.daoToDto( associations, companyDetails );
    }

}