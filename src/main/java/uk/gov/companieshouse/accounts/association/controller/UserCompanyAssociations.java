package uk.gov.companieshouse.accounts.association.controller;

import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.companieshouse.accounts.association.models.Constants.COMPANIES_HOUSE;
import static uk.gov.companieshouse.accounts.association.models.Constants.PAGINATION_IS_MALFORMED;
import static uk.gov.companieshouse.accounts.association.models.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.fetchAllStatusesWithout;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToAuthCodeConfirmedUpdated;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.mapToDisplayValue;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.CONFIRMED;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyAssociationsInterface;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPost;
import uk.gov.companieshouse.api.accounts.associations.model.ResponseBodyPost;

@RestController
public class UserCompanyAssociations implements UserCompanyAssociationsInterface {

    private final CompanyService companyService;
    private final AssociationsService associationsService;
    private final UsersService usersService;
    private final EmailService emailService;

    @Autowired
    public UserCompanyAssociations( final CompanyService companyService, final AssociationsService associationsService, final UsersService usersService, final EmailService emailService ) {
        this.companyService = companyService;
        this.associationsService = associationsService;
        this.usersService = usersService;
        this.emailService = emailService;
    }

     /**
     This endpoint is called when a user uses the auth_code to add themselves to a company.
     For auditing purposes, when a user removes themselves from a company, the API changes the user's
     association status to "removed" instead of actually deleting it. Thus, even though the association
     does not exist conceptually, it might actually exist in a removed state. The frontend should not need
     to be aware of this nuance and so should call this endpoint when a user uses the auth_code to add themselves.
     To that end, this endpoint implements an upsert.

     Using upsert also allows us to differentiate between various nuanced scenarios. For example, suppose that
     a user has an association in an awaiting-approval state i.e. they have an invitation. If they change the state
     to confirmed via PATCH /associations/{id}, then this would be interpreted as accepting an invitation.
     On the other hand if they change their status to confirmed via this endpoint, then this will be interpreted as
     using the auth_code to add themselves. This allows us to accurately account for edge cases where the user might
     have been invited to a company, but decided to use the auth_code to add themselves, instead of accepting the
     invitation.
     */
    @Override
    public ResponseEntity<ResponseBodyPost> addAssociation( final RequestBodyPost requestBody ) {
        final var userId = requestBody.getUserId();
        final var companyNumber = requestBody.getCompanyNumber();

        LOGGER.infoContext( getXRequestId(), String.format( "Received request with user_id=%s, company_number=%s.", userId, companyNumber ),null );

        final var targetUser = usersService.fetchUserDetails( userId, getXRequestId() );
        final var companyDetails = companyService.fetchCompanyProfile( companyNumber );

        final var targetAssociationId = Optional.of( associationsService.fetchAssociationsForUserAndPartialCompanyNumber( targetUser, companyNumber, 0, 15 ) )
                .filter( Page::hasContent )
                .map( Page::getContent )
                .map( List::getFirst )
                .map( targetAssociation -> {
                    if ( CONFIRMED.getValue().equals( targetAssociation.getStatus() ) ){
                        throw new BadRequestRuntimeException( getXRequestId(), "Association already exists.", new Exception( String.format( "Association between user_id %s and company_number %s already exists.", userId, companyNumber ) ) );
                    }
                    associationsService.updateAssociation( targetAssociation.getId(), mapToAuthCodeConfirmedUpdated( targetAssociation, targetUser, COMPANIES_HOUSE ) );
                    return targetAssociation.getId();
                } )
                .orElseGet( () -> associationsService.createAssociationWithAuthCodeApprovalRoute( companyNumber, userId ).getId() );
        final var reaEmailMono = Optional.ofNullable(
                emailService.sendReaDigitalAuthorisationAddedEmail(getXRequestId(), companyDetails.getCompanyNumber(), Mono.just(companyDetails.getCompanyName()))).orElse(Mono.empty());
        Mono.just( companyNumber )
                .flatMapMany( associationsService::fetchConfirmedUserIds )
                .flatMap( emailService.sendAuthCodeConfirmationEmailToAssociatedUser( getXRequestId(), companyDetails.getCompanyNumber(), Mono.just( companyDetails.getCompanyName() ), mapToDisplayValue( targetUser, targetUser.getEmail() ) ) )
                .then( reaEmailMono )
                .subscribe();

        return new ResponseEntity<>( new ResponseBodyPost().associationLink( String.format( "/associations/%s", targetAssociationId ) ), CREATED );
    }

    @Override
    public ResponseEntity<AssociationsList> fetchAssociationsBy( final List<String> status, final Integer pageIndex, final Integer itemsPerPage, final String companyNumber ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with user_id=%s, status=%s, page_index=%d, items_per_page=%d, company_number=%s.", getEricIdentity(), String.join( ",", status ), pageIndex, itemsPerPage, companyNumber ),null );

        final var allStatuses = fetchAllStatusesWithout( Set.of() ).stream().map( Association.StatusEnum::toString ).collect( Collectors.toSet() );
        if ( !allStatuses.containsAll(status) ) {
            throw new BadRequestRuntimeException( getXRequestId(), PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "Status is invalid" ) );
        }

        if ( pageIndex < 0 || itemsPerPage <= 0 ){
            throw new BadRequestRuntimeException( getXRequestId(), PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( PAGINATION_IS_MALFORMED ) );
        }

        final var associationsList = associationsService.fetchAssociationsForUserAndPartialCompanyNumberAndStatuses( getUser(), companyNumber, new HashSet<>( status ), pageIndex, itemsPerPage );

        return new ResponseEntity<>( associationsList, OK );
    }
}
