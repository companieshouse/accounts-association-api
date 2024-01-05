package uk.gov.companieshouse.accounts.association.controller;

import static uk.gov.companieshouse.accounts.association.utils.Date.isBeforeNow;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.AccountsAssociationServiceApplication;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyAssociationStatusInterface;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
@RequestMapping( "/associations" )
public class UserCompanyAssociationStatusApi implements UserCompanyAssociationStatusInterface {

    private final AssociationsService associationsService;

    private static final Logger LOG = LoggerFactory.getLogger(AccountsAssociationServiceApplication.APPLICATION_NAME_SPACE);

    public UserCompanyAssociationStatusApi(AssociationsService associationsService) {
        this.associationsService = associationsService;
    }

    @Override
    public ResponseEntity<Void> updateAssociationStatusForUserAndCompany(
            @Pattern(regexp = "^[a-zA-Z0-9]*$") String userId,
            @Pattern(regexp = "^[0-9]{0,64}$") String companyNumber,
            String status, @NotNull String xRequestId) {


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


    // TODO: add enum for status

}
