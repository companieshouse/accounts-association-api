package uk.gov.companieshouse.accounts.association.service;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListUserMapper;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.user.model.User;

import java.util.List;
import java.util.Objects;

@Service
@RequestScope
public class AssociationsService {


    private final AssociationsRepository associationsRepository;


    private final AssociationsListUserMapper associationsListUserMapper;

    @Autowired
    public AssociationsService(AssociationsRepository associationsRepository, AssociationsListUserMapper associationsListUserMapper) {
        this.associationsRepository = associationsRepository;
        this.associationsListUserMapper = associationsListUserMapper;
    }

    @Transactional(readOnly = true)
    public AssociationsList fetchAssociationsForUserStatusAndCompany(
            @NotNull final User user, final List<String> status, final Integer pageIndex, final Integer itemsPerPage,
            final String companyNumber) {
        Page<AssociationDao> results = null;
        if (Objects.isNull(status) || status.isEmpty()) {
            results = associationsRepository.findByUserIdAndCompanyNumberLike(
                    user.getUserId(), companyNumber, PageRequest.of(pageIndex, itemsPerPage));

        } else {
            associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike(
                    user.getUserId(), status, companyNumber, PageRequest.of(pageIndex, itemsPerPage));
        }

        assert results != null;
        return associationsListUserMapper.daoToDto(results, user);
    }
}