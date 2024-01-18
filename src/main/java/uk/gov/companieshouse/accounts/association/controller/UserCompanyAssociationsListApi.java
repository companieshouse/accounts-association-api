package uk.gov.companieshouse.accounts.association.controller;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.AccountsAssociationServiceApplication;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompaniesService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyAssociationsListInterface;
import uk.gov.companieshouse.api.accounts.associations.model.UserCompanyAssociationsResponse;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
public class UserCompanyAssociationsListApi implements UserCompanyAssociationsListInterface {

    private final UsersService usersService;
    private final AssociationsService associationsService;
    private final CompaniesService companiesService;

    private static final Logger LOG = LoggerFactory.getLogger(AccountsAssociationServiceApplication.applicationNameSpace);

    public UserCompanyAssociationsListApi(UsersService usersService, AssociationsService associationsService,
            CompaniesService companiesService) {
        this.usersService = usersService;
        this.associationsService = associationsService;
        this.companiesService = companiesService;
    }

    @Override
    public ResponseEntity<UserCompanyAssociationsResponse> getCompaniesAssociatedWithUser(
            final String userEmail, final String xRequestId, final Boolean includeUnauthorised) {

        LOG.debug( String.format( "%s: Attempting to retrieve companies associated with %s.", xRequestId, userEmail ) );

        final var userInfoOptional = usersService.fetchUserInfo( userEmail );
        if ( userInfoOptional.isEmpty() ){
            LOG.error( String.format( "%s: UserInfo for %s does not exist.", xRequestId, userEmail ) );
            throw new BadRequestRuntimeException( "Please check the request and try again" );
        }

        final var userInfo = userInfoOptional.get();
        final var userId = userInfo.getUserId();
        final var associations = associationsService.findAllByUserId( userId, includeUnauthorised );

        if ( associations.isEmpty() ){
            LOG.debug( String.format( "%s: No associations found for %s (includeUnauthorised=%B).", xRequestId, userEmail, includeUnauthorised ) );
            return new ResponseEntity<>( new UserCompanyAssociationsResponse().successBody( List.of() ), HttpStatus.OK );
        }

        final var companyNumbers =
        associations.stream()
                    .map( Association::getCompanyNumber )
                    .toList();

        final var companies = companiesService.fetchCompanies( companyNumbers );

        LOG.debug( String.format( "%s: Retrieve companies associated with %s.", xRequestId, userEmail ) );
        return new ResponseEntity<>( new UserCompanyAssociationsResponse().successBody( companies ), HttpStatus.OK );
    }

}
