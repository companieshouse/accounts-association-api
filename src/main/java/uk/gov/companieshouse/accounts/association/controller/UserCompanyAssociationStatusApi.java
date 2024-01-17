package uk.gov.companieshouse.accounts.association.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.AccountsAssociationServiceApplication;
import uk.gov.companieshouse.accounts.association.enums.StatusEnum;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyAssociationStatusInterface;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
public class UserCompanyAssociationStatusApi implements UserCompanyAssociationStatusInterface {

    private final AssociationsService associationsService;
    private final UsersService usersService;

    private static final Logger LOG = LoggerFactory.getLogger(AccountsAssociationServiceApplication.applicationNameSpace);

    public UserCompanyAssociationStatusApi(AssociationsService associationsService,
            UsersService usersService) {
        this.associationsService = associationsService;
        this.usersService = usersService;
    }

    @Override
    public ResponseEntity<Void> updateAssociationStatusForUserAndCompany(
           final String userEmail, final String companyNumber, final String status, final String xRequestId) {

        LOG.debug( String.format( "%s: Attempting to update association status between user (%s) and company (%s)...", xRequestId, userEmail, companyNumber ) );

        if ( !( status.equals( StatusEnum.CONFIRMED.getValue() ) || status.equals( StatusEnum.REMOVED.getValue() ) ) ){
            LOG.error( String.format( "%s: Invalid status provided (%s)", xRequestId, status ) );
            throw new BadRequestRuntimeException( "Please check the request and try again" );
        }

        final var userInfoOptional = usersService.fetchUserInfo( userEmail );
        final var userInfoExists = userInfoOptional.isPresent();
        final var userId = !userInfoExists ? userEmail : userInfoOptional.get().getUserId();

        if (associationsService.getByUserIdAndCompanyNumber(userId,companyNumber).isEmpty()) {
            LOG.error( String.format( "%s: Unable to find association where companyNumber is %s, and userEmail is %s", xRequestId, companyNumber, userEmail ) );
            throw new BadRequestRuntimeException( "Please check the request and try again" );
        }

        if ( status.equals( StatusEnum.CONFIRMED.getValue() ) ){
            if ( !userInfoExists ) {
                LOG.error( String.format( "%s: User with email address (%s) does not exist.", xRequestId, userEmail ) );
                throw new BadRequestRuntimeException("Please check the request and try again");
            }

            associationsService.confirmAssociation( userId, companyNumber );
        }

        if ( status.equals( StatusEnum.REMOVED.getValue() ) ){
            associationsService.softDeleteAssociation( userId, companyNumber, userInfoExists );
        }

        LOG.debug( String.format( "%s: Updated association status between user (%s) and company (%s)...", xRequestId, userEmail, companyNumber ) );

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
