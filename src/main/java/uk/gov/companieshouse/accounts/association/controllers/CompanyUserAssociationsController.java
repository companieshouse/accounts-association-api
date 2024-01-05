package uk.gov.companieshouse.accounts.association.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.companieshouse.accounts.association.AccountsAssociationServiceApplication;
import uk.gov.companieshouse.accounts.association.services.CompanyAssociationsService;
import uk.gov.companieshouse.api.accounts.associations.api.CompanyUserAssociationsListInterface;
import uk.gov.companieshouse.api.accounts.associations.model.UserCompanyAssociationsResponse;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
public class CompanyUserAssociationsController implements CompanyUserAssociationsListInterface {

    private static final Logger LOG = LoggerFactory.getLogger(AccountsAssociationServiceApplication.APPLICATION_NAME_SPACE);

    private final CompanyAssociationsService associationsCompanyService;

    public CompanyUserAssociationsController(@Autowired CompanyAssociationsService associationsCompanyService) {
        this.associationsCompanyService = associationsCompanyService;
    }

    @Override
    public ResponseEntity<UserCompanyAssociationsResponse>
    getAssociatedUsersForCompany(final String companyNumber,
                                  final String xRequestId,
                                  final String includeUnauthorised) {

        LOG.infoContext(xRequestId, "Find Users By company_number:" + companyNumber, null);

        return ResponseEntity.ok(null);
    }
}
