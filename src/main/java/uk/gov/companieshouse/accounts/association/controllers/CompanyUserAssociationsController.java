package uk.gov.companieshouse.accounts.association.controllers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.companieshouse.accounts.association.AccountsAssociationServiceApplication;
import uk.gov.companieshouse.accounts.association.models.Associations;
import uk.gov.companieshouse.accounts.association.services.AssociationsCompanyService;
import uk.gov.companieshouse.api.accounts.associations.api.CompanyUserAssociationsListInterface;
import uk.gov.companieshouse.api.accounts.associations.model.UserCompanyAssociationsResponse;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;

@RestController
public class CompanyUserAssociationsController implements CompanyUserAssociationsListInterface {

    private static final Logger LOG = LoggerFactory.getLogger(AccountsAssociationServiceApplication.APPLICATION_NAME_SPACE);

    private final AssociationsCompanyService associationsCompanyService;

    public CompanyUserAssociationsController(@Autowired AssociationsCompanyService associationsCompanyService) {
        this.associationsCompanyService = associationsCompanyService;
    }

    @Override
    public ResponseEntity<UserCompanyAssociationsResponse>
    getAssociatedUsersForCompany(@Pattern(regexp = "^\\d{0,64}$") final String companyNumber,
                                 @NotNull final String xRequestId,
                                 @Valid final String includeUnauthorised) {

        LOG.infoContext(xRequestId, "Find Users By company_number:" + companyNumber, null);

        return ResponseEntity.ok(null);
    }
}
