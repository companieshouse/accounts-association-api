package uk.gov.companieshouse.accounts.association.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.api.AssociationsListForCompanyInterface;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
public class AssociationsListForCompanyController implements AssociationsListForCompanyInterface {

    private final AssociationsService associationsService;
    private final CompanyService companyService;

    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    public AssociationsListForCompanyController(AssociationsService associationsService, CompanyService companyService) {
        this.associationsService = associationsService;
        this.companyService = companyService;
    }

    @Override
    public ResponseEntity<AssociationsList> getAssociationsForCompany( final String companyNumber, final String xRequestId, final Boolean includeRemoved, final Integer pageIndex, final Integer itemsPerPage) {

        LOG.debugContext(xRequestId, String.format( "Attempting to fetch users that are associated with company %s. includeRemoved=%b, itemsPerPage=%d, and pageIndex=%d.", companyNumber, includeRemoved, itemsPerPage, pageIndex ),null );

        if ( pageIndex < 0 ){
            LOG.error("pageIndex was less then 0" );
            throw new BadRequestRuntimeException( "Please check the request and try again" );
        }

        if ( itemsPerPage <= 0 ){
            LOG.error( "itemsPerPage was less then 0" );
            throw new BadRequestRuntimeException( "Please check the request and try again" );
        }

        final var companyProfile = companyService.fetchCompanyProfile( companyNumber );

        final var associationsList = associationsService.fetchAssociatedUsers( companyNumber, companyProfile, includeRemoved, itemsPerPage, pageIndex );
        final var associationsListIsEmpty = associationsList.getItems().isEmpty();

        LOG.debugContext(xRequestId, associationsListIsEmpty ? "Could not find any associations" : "Successfully fetched associations",null) ;

        return new ResponseEntity<>( associationsList, HttpStatus.OK );
    }

}