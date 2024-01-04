package uk.gov.companieshouse.accounts.association.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.companieshouse.accounts.association.AccountsAssociationServiceApplication;
import uk.gov.companieshouse.accounts.association.models.Associations;
import uk.gov.companieshouse.accounts.association.services.AssociationsCompanyService;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;

import static uk.gov.companieshouse.accounts.association.utils.Constants.APPLICATION_CONTEXT_PATH;

@RestController
@RequestMapping(APPLICATION_CONTEXT_PATH + "/companies")
public class AssociationsCompanyController {

    private static final Logger LOG = LoggerFactory.getLogger(AccountsAssociationServiceApplication.APPLICATION_NAME_SPACE);
    @Autowired
    private AssociationsCompanyService associationsCompanyService;

    @GetMapping("/{company_number}/users")
    public ResponseEntity<List<Associations>> findUsersByCompanyNumber(
            @PathVariable("company_number") String company_number) {
        LOG.debug("Find Users By company_number:" + company_number);
        List<Associations> usersList = associationsCompanyService.findUsersByCompanyNumber(company_number);
        return ResponseEntity.ok().body(usersList);
    }

}
