package uk.gov.companieshouse.accounts.association.controller;

import static org.springframework.http.HttpStatus.OK;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_READ_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.PAGINATION_IS_MALFORMED;
import static uk.gov.companieshouse.accounts.association.models.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.fetchAllStatusesWithout;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.ForbiddenRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.api.AssociationsListForCompanyInterface;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.user.model.User;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.hasAdminPrivilege;

@RestController
public class AssociationsListForCompanyController implements AssociationsListForCompanyInterface {

    private final CompanyService companyService;
    private final AssociationsService associationsService;
    private final UsersService usersService;

    public AssociationsListForCompanyController(final CompanyService companyService, final AssociationsService associationsService, final UsersService usersService) {
        this.companyService = companyService;
        this.associationsService = associationsService;
        this.usersService = usersService;
    }

    @Override
    public ResponseEntity<AssociationsList> getAssociationsForCompany( final String companyNumber, final Boolean includeRemoved, final String userEmail, final Integer pageIndex, final Integer itemsPerPage ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with company_number=%s, includeRemoved=%b, itemsPerPage=%d, pageIndex=%d.", companyNumber, includeRemoved, itemsPerPage, pageIndex ),null );

        if ( pageIndex < 0 || itemsPerPage <= 0 ){
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( PAGINATION_IS_MALFORMED ) );
        }

        if ( !associationsService.confirmedAssociationExists( companyNumber, getEricIdentity() ) && !hasAdminPrivilege( ADMIN_READ_PERMISSION ) ){
            throw new ForbiddenRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "Requesting user is not permitted to retrieve data." ) );
        }

        final var userId = Optional.ofNullable( userEmail )
                .map( List :: of )
                .map( usersService :: searchUserDetails )
                .filter( users -> !users.isEmpty() )
                .map( List :: getFirst )
                .map( User :: getUserId )
                .orElse( null );

        final var companyProfile = companyService.fetchCompanyProfile( companyNumber );
        final var statuses = includeRemoved ? fetchAllStatusesWithout( Set.of() ) : fetchAllStatusesWithout( Set.of( StatusEnum.REMOVED ) );
        final var associationsList = associationsService.fetchUnexpiredAssociationsForCompanyAndStatuses( companyProfile, statuses, userId, userEmail, pageIndex, itemsPerPage );

        return new ResponseEntity<>( associationsList, OK );
    }

}