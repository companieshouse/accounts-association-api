package uk.gov.companieshouse.accounts.association.controller;

import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.api.accounts.associations.api.AssociationsListForCompanyInterface;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;

@RestController
public class AssociationsListForCompanyController implements AssociationsListForCompanyInterface {

    private final CompanyService companyService;
    private final AssociationsService associationsService;

    public AssociationsListForCompanyController( final CompanyService companyService, AssociationsService associationsService ) {
        this.companyService = companyService;
        this.associationsService = associationsService;
    }

    @Override
    public ResponseEntity<AssociationsList> getAssociationsForCompany( final String companyNumber, final Boolean includeRemoved, final Integer pageIndex, final Integer itemsPerPage ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with company_number=%s, includeRemoved=%b, itemsPerPage=%d, pageIndex=%d.", companyNumber, includeRemoved, itemsPerPage, pageIndex ),null );

        if ( pageIndex < 0 ){
            LOGGER.errorContext( getXRequestId(), new Exception( "pageIndex was less than 0" ), null );
            throw new BadRequestRuntimeException( "Please check the request and try again" );
        }

        if ( itemsPerPage <= 0 ){
            LOGGER.errorContext( getXRequestId(), new Exception( "itemsPerPage was less than or equal to 0" ), null);
            throw new BadRequestRuntimeException( "Please check the request and try again" );
        }

        final var companyProfile = companyService.fetchCompanyProfile( companyNumber );

        LOGGER.debugContext( getXRequestId(), "Attempting to fetch associated users", null );
        final var associationsList = associationsService.fetchAssociatedUsers( companyNumber, companyProfile, includeRemoved, itemsPerPage, pageIndex );
        final var associationsListIsEmpty = associationsList.getItems().isEmpty();

        LOGGER.infoContext( getXRequestId(), associationsListIsEmpty ? "Could not find any associations" : String.format( "Successfully fetched %d associations", associationsList.getItems().size() ),null );

        return new ResponseEntity<>( associationsList, HttpStatus.OK );
    }

}