package uk.gov.companieshouse.accounts.association.controller;

import static org.springframework.http.HttpStatus.OK;
import static uk.gov.companieshouse.accounts.association.models.Constants.PAGINATION_IS_MALFORMED;
import static uk.gov.companieshouse.accounts.association.models.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.fetchAllStatusesWithout;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.ForbiddenRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.api.AssociationDataForCompanyInterface;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.FetchRequestBodyPost;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.*;

@RestController
public class AssociationsListForCompanyController implements AssociationDataForCompanyInterface {

    private final CompanyService companyService;
    private final AssociationsService associationsService;
    private final UsersService usersService;

    public AssociationsListForCompanyController(final CompanyService companyService, final AssociationsService associationsService, final UsersService usersService) {
        this.companyService = companyService;
        this.associationsService = associationsService;
        this.usersService = usersService;
    }

    @Override
    public ResponseEntity<AssociationsList> getAssociationsForCompany( final String companyNumber, final Boolean includeRemoved, final Integer pageIndex, final Integer itemsPerPage ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with company_number=%s, includeRemoved=%b, itemsPerPage=%d, pageIndex=%d.", companyNumber, includeRemoved, itemsPerPage, pageIndex ),null );

        if ( pageIndex < 0 || itemsPerPage <= 0 ){
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( PAGINATION_IS_MALFORMED ) );
        }

        if ( !associationsService.confirmedAssociationExists( companyNumber, getEricIdentity() ) ){
            throw new ForbiddenRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "Requesting user is not permitted to retrieve data." ) );
        }

        final var companyProfile = companyService.fetchCompanyProfile( companyNumber );
        final var statuses = includeRemoved ? fetchAllStatusesWithout( Set.of() ) : fetchAllStatusesWithout( Set.of( StatusEnum.REMOVED ) );
        final var associationsList = associationsService.fetchUnexpiredAssociationsForCompanyAndStatuses( companyProfile, statuses, null, null, pageIndex, itemsPerPage );

        return new ResponseEntity<>( associationsList, OK );
    }

    @Override
    public ResponseEntity<Association> getAssociationsForCompanyUserAndStatus( final String companyNumber, final FetchRequestBodyPost requestBodyPost ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with company_number=%s, user_email=%s, user_id=%s .", companyNumber, requestBodyPost.getUserEmail(), requestBodyPost.getUserId() ),null );

        final var userId = requestBodyPost.getUserId();
        final var userEmail = requestBodyPost.getUserEmail();
        final var statuses =  Optional.ofNullable( requestBodyPost.getStatus() )
                .filter( statusEnums -> !statusEnums.isEmpty() )
                .orElse( List.of( FetchRequestBodyPost.StatusEnum.CONFIRMED ) )
                .stream()
                .map( FetchRequestBodyPost.StatusEnum::getValue )
                .map( StatusEnum::fromValue )
                .collect(Collectors.toSet());

        if ( Objects.nonNull( userId ) == Objects.nonNull( userEmail ) ){
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN,  new Exception( "Only one of user_id or user_email must be present" ) );
        }

        final var companyProfile = companyService.fetchCompanyProfile( companyNumber );
        final var user = usersService.retrieveUserDetails( userId, userEmail );
        final var targetUserEmail =  Objects.nonNull( user ) ? user.getEmail() : userEmail;

        return associationsService.fetchUnexpiredAssociationsForCompanyUserAndStatuses( companyProfile, statuses, user, targetUserEmail )
                .map( association -> new ResponseEntity<>( association, OK ) )
                .orElseThrow( () -> new NotFoundRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "Association not found") ) );

    }

}