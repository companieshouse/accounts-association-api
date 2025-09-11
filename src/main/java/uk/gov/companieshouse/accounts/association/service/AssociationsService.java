package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToAuthCodeConfirmedUpdated;

import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;

@Service
public class AssociationsService {

    private final TransactionalService transactionalService;

    @Autowired
    public AssociationsService(TransactionalService transactionalService) {
        this.transactionalService = transactionalService;
    }

    public AssociationDao fetchAssociationDao(User targetUser, CompanyDetails targetCompany) {
        final var companyNumber = targetCompany.getCompanyNumber();
        final var userId = targetUser.getUserId();

        return Optional.of(transactionalService.fetchAssociationsForUserAndPartialCompanyNumber(targetUser, companyNumber, 0, 15))
                .filter(Page::hasContent)
                .map(Page::getContent)
                .map(List::getFirst)
                .map(targetAssociation -> {
                    if (RequestBodyPut.StatusEnum.CONFIRMED.getValue().equals(targetAssociation.getStatus())){
                        throw new BadRequestRuntimeException("Association already exists.", new Exception(String.format("Association between user_id %s and company_number %s already exists.", userId, companyNumber)));
                    }
                    transactionalService.updateAssociation(targetAssociation.getId(), mapToAuthCodeConfirmedUpdated(targetAssociation, targetUser, COMPANIES_HOUSE));
                    return targetAssociation;
                })
                .orElseGet(() -> transactionalService.createAssociationWithAuthCodeApprovalRoute(companyNumber, userId));
    }

}
