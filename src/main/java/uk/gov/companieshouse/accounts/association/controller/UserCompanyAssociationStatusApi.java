package uk.gov.companieshouse.accounts.association.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.AccountsAssociationServiceApplication;
import uk.gov.companieshouse.accounts.association.enums.StatusEnum;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyAssociationStatusInterface;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Optional;

@RestController
public class UserCompanyAssociationStatusApi implements UserCompanyAssociationStatusInterface {

    private final AssociationsService associationsService;
    private final UsersService usersService;

    private static final Logger LOG = LoggerFactory.getLogger(AccountsAssociationServiceApplication.APPLICATION_NAME_SPACE);

    public UserCompanyAssociationStatusApi(AssociationsService associationsService,
            UsersService usersService) {
        this.associationsService = associationsService;
        this.usersService = usersService;
    }

    @Override
    public ResponseEntity<Void> updateAssociationStatusForUserAndCompany(
           String userEmail, String companyNumber, String status, String xRequestId) {

        LOG.debug( String.format( "%s: Attempting to update association status between user (%s) and company (%s)...", xRequestId, userEmail, companyNumber ) );

        if ( !StatusEnum.contains( status ) ){
            LOG.error( String.format( "%s: Invalid status provided (%s)", xRequestId, status ) );
            throw new BadRequestRuntimeException( "Status must be either 'Confirmed' or 'Deleted'" );
        }

        final var userOptional = usersService.fetchUserId( userEmail );
        if ( userOptional.isEmpty() ) {
            LOG.error( String.format( "%s: Unable to find user with the email address: %s.", xRequestId, userEmail ) );
            throw new NotFoundRuntimeException( "user_email", String.format( "Could not find user with email address: %s.", userEmail)  );
        }
        final var userId = userOptional.get().getId();

        if (associationsService.getByUserIdAndCompanyNumber(userId,companyNumber).isEmpty()) {
            LOG.error( String.format( "%s: Unable to find association where companyNumber is %s, and userId is %s", xRequestId, companyNumber, userId ) );
            throw new NotFoundRuntimeException( "association", String.format( "Could not find association where companyNumber is %s, and userId is %s.", companyNumber, userId )  );
        }

        if ( status.equals( StatusEnum.CONFIRMED.getValue() ) )
            associationsService.confirmAssociation( userId, companyNumber );
        else if ( status.equals( StatusEnum.REMOVED.getValue() ) )
            associationsService.softDeleteAssociation( userId, companyNumber );
        else
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);

        LOG.debug( String.format( "%s: Updated association status between user (%s) and company (%s)...", xRequestId, userEmail, companyNumber ) );

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
