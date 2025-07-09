package uk.gov.companieshouse.accounts.association.controller;

import static java.time.LocalDateTime.now;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.OK;
import static uk.gov.companieshouse.accounts.association.models.Constants.PAGINATION_IS_MALFORMED;
import static uk.gov.companieshouse.accounts.association.models.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToInvitationUpdate;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.DAYS_SINCE_INVITE_TILL_EXPIRES;
import static uk.gov.companieshouse.accounts.association.utils.UserUtil.mapToDisplayValue;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.CONFIRMED;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyInvitationsInterface;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationRequestBodyPost;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.associations.model.ResponseBodyPost;
import uk.gov.companieshouse.api.company.CompanyDetails;

@RestController
public class UserCompanyInvitations implements UserCompanyInvitationsInterface {

    private final UsersService usersService;
    private final CompanyService companyService;
    private final AssociationsService associationsService;
    private final EmailService emailService;

    public UserCompanyInvitations( final UsersService usersService, final CompanyService companyService, final AssociationsService associationsService, final EmailService emailService ) {
        this.usersService = usersService;
        this.companyService = companyService;
        this.associationsService = associationsService;
        this.emailService = emailService;
    }

    @Override
    public ResponseEntity<InvitationsList> fetchActiveInvitationsForUser( final Integer pageIndex, final Integer itemsPerPage ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with user_id=%s, itemsPerPage=%d, pageIndex=%d.", getEricIdentity(), itemsPerPage, pageIndex ),null );

        if ( pageIndex < 0 || itemsPerPage <= 0 ){
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( PAGINATION_IS_MALFORMED ) );
        }

        final var invitations = associationsService.fetchActiveInvitations( getUser(), pageIndex, itemsPerPage );

        return new ResponseEntity<>( invitations, OK );
    }

    @Override
    public ResponseEntity<ResponseBodyPost> inviteUser( final InvitationRequestBodyPost requestBody ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with user_id=%s, company_number=%s, invitee_email_id=%s.", getEricIdentity(), requestBody.getCompanyNumber(), requestBody.getInviteeEmailId()  ),null );

        final var inviteeEmail = Optional.of( requestBody )
                .map( InvitationRequestBodyPost::getInviteeEmailId )
                .orElseThrow( () -> new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "invitee_email_id is null." ) ) );

        final var companyNumber = requestBody.getCompanyNumber();
        final CompanyDetails companyDetails;
        try {
            companyDetails = companyService.fetchCompanyProfile( companyNumber );
        } catch( NotFoundRuntimeException exception ){
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( exception.getMessage() ) );
        }

        if ( !associationsService.confirmedAssociationExists( companyNumber, getEricIdentity() ) ){
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "Requesting user %s does not have a confirmed association at company %s", getEricIdentity(), companyNumber ) ) );
        }

        final var inviteeUserDetails = Optional
                .ofNullable( usersService.searchUserDetails( List.of( inviteeEmail ) ) )
                .filter( list -> !list.isEmpty() )
                .map( List::getFirst )
                .orElse( null );

        final var targetAssociation = associationsService.fetchAssociationDao( companyNumber, Objects.nonNull( inviteeUserDetails ) ? inviteeUserDetails.getUserId() : null, inviteeEmail )
                .map( association -> {
                    if( CONFIRMED.getValue().equals( association.getStatus() ) ) {
                        throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( String.format( "%s already has a confirmed association at company %s", inviteeEmail, companyNumber ) ) );
                    }
                    associationsService.updateAssociation( association.getId(), mapToInvitationUpdate( association, inviteeUserDetails, getEricIdentity(), now() ) );
                    return association.approvalExpiryAt( now().plusDays( DAYS_SINCE_INVITE_TILL_EXPIRES ) );
                } )
                .orElseGet( () -> {
                    final var userId = Objects.nonNull( inviteeUserDetails ) ? inviteeUserDetails.getUserId() : null;
                    final var userEmail = Objects.nonNull( inviteeUserDetails ) ? null : inviteeEmail;
                    return associationsService.createAssociationWithInvitationApprovalRoute( companyNumber, userId, userEmail, getEricIdentity() );
                } );

        emailService.sendInviteEmail( getXRequestId(), companyDetails.getCompanyNumber(), Mono.just( companyDetails.getCompanyName() ), mapToDisplayValue( getUser(), getUser().getEmail() ), targetAssociation.getApprovalExpiryAt().toString(), inviteeEmail ).subscribe();
        Mono.just( companyNumber )
                .flatMapMany( associationsService::fetchConfirmedUserIds )
                .flatMap( emailService.sendInvitationEmailToAssociatedUser( getXRequestId(), companyDetails.getCompanyNumber(), Mono.just( companyDetails.getCompanyName() ), mapToDisplayValue( getUser(), getUser().getEmail() ), mapToDisplayValue( inviteeUserDetails, inviteeEmail ) ) )
                .subscribe();

        return new ResponseEntity<>( new ResponseBodyPost().associationLink( String.format( "/associations/%s", targetAssociation.getId() ) ), CREATED );
    }

}
