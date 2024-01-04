package uk.gov.companieshouse.accounts.association.controller;

import static uk.gov.companieshouse.accounts.association.utils.Date.isBeforeNow;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;

@RestController
@RequestMapping( "/associations" )
public class UserCompanyAssociationStatusApi {

    private final AssociationsService associationsService;

    public UserCompanyAssociationStatusApi(AssociationsService associationsService) {
        this.associationsService = associationsService;
    }

    @PutMapping( "/companies/{company_number}/users/{user_id}/{status}" )
    public ResponseEntity<?> updateAssociationStatusForUserAndCompany(
            @PathVariable("user_id") final String userId, @PathVariable("company_number") final String companyNumber,
            @PathVariable("status") final String status, @RequestHeader( "X-Request-Id" ) String xRequestId ){

        try {
            final var association = associationsService.fetchConfirmationExpirationTime( userId, companyNumber );

            if ( association == null || association.getConfirmationExpirationTime() == null )
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);

            if ( isBeforeNow( association.getConfirmationExpirationTime() ) )
                associationsService.softDeleteAssociation( userId, companyNumber );
            else
                associationsService.confirmAssociation( userId, companyNumber );

            return new ResponseEntity<>(HttpStatus.OK);
        } catch ( Exception e ){
            return new ResponseEntity<>( HttpStatus.INTERNAL_SERVER_ERROR );
        }

    }

}
