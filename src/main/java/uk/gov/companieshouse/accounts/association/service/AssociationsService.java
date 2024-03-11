package uk.gov.companieshouse.accounts.association.service;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.RequestScope;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListUserMapper;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.user.model.User;

import java.util.List;

@Service
@RequestScope
public class AssociationsService {

    @Autowired
    private AssociationsRepository associationsRepository;

    @Autowired
    private AssociationsListUserMapper associationsListUserMapper;


    public AssociationsList fetchAssociationsForUserStatusAndCompany(final String requestId,
                                                                     @NotNull final User user, final List<String> status, final Integer pageIndex, final Integer itemsPerPage,
                                                                     final String companyNumber) {
        final var results = associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike(
                user.getUserId(), status, companyNumber, PageRequest.of(pageIndex, itemsPerPage));

        return associationsListUserMapper.daoToDto(results, user);
    }
}