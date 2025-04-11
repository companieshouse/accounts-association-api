package uk.gov.companieshouse.accounts.association.controller;

import static java.time.LocalDateTime.now;
import static uk.gov.companieshouse.accounts.association.models.Constants.ITEMS_PER_PAGE_WAS_LESS_THAN_OR_EQUAL_TO_0;
import static uk.gov.companieshouse.accounts.association.models.Constants.PAGE_INDEX_WAS_LESS_THAN_0;
import static uk.gov.companieshouse.accounts.association.models.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.mapToInvitationUpdate;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.DAYS_SINCE_INVITE_TILL_EXPIRES;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyInvitationsInterface;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationRequestBodyPost;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.associations.model.ResponseBodyPost;
import uk.gov.companieshouse.api.accounts.user.model.UsersList;
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

        if (pageIndex < 0) {
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( PAGE_INDEX_WAS_LESS_THAN_0 ) );
        }

        if (itemsPerPage <= 0) {
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( ITEMS_PER_PAGE_WAS_LESS_THAN_OR_EQUAL_TO_0 ) );
        }

        final var user = getUser();

        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to retrieve active invitations for user %s", getEricIdentity() ), null );
        final var invitations = associationsService.fetchActiveInvitations( user, pageIndex, itemsPerPage );
        LOGGER.infoContext( getXRequestId(), String.format( "Successfully retrieved active invitations for user %s", getEricIdentity() ), null );

        return new ResponseEntity<>( invitations, HttpStatus.OK );
    }

    @Override
    public ResponseEntity<ResponseBodyPost> inviteUser(final InvitationRequestBodyPost requestBody) {
        final var companyNumber = requestBody.getCompanyNumber();
        final var inviteeEmail = requestBody.getInviteeEmailId();

        LOGGER.infoContext( getXRequestId(), String.format( "Received request with user_id=%s, company_number=%s, invitee_email_id=%s.", getEricIdentity(), companyNumber, inviteeEmail  ),null );

        if (Objects.isNull(inviteeEmail)) {
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( "invitee_email_id is null." ) );
        }

        final var inviterUserDetails= Objects.requireNonNull(getUser());
        CompanyDetails companyDetails;
        try {
            companyDetails= companyService.fetchCompanyProfile( companyNumber );
        } catch( NotFoundRuntimeException notFoundRuntimeException ){
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN, new Exception( notFoundRuntimeException.getMessage() ) );
        }
        if (!associationsService.confirmedAssociationExists(companyNumber,getEricIdentity())) {
            throw new BadRequestRuntimeException("requesting user does not have access", new Exception( String.format( "Requesting user %s does not have a confirmed association at company %s", getEricIdentity(), companyNumber ) ));
        }

        final var inviterDisplayName = Optional.ofNullable( inviterUserDetails.getDisplayName() ).orElse( inviterUserDetails.getEmail() );
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to create requests for users associated with company %s", companyNumber ), null );
        final var cachedAssociatedUsers = Mono.just( companyNumber )
                .flatMapMany( associationsService::fetchConfirmedUserIds)
                .cache();

        final var inviteeUserDetails = usersService.searchUserDetails( List.of( inviteeEmail ) );

        final var inviteeUserFound = Objects.nonNull(inviteeUserDetails) && !inviteeUserDetails.isEmpty();

        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch association for user %s and company %s", inviteeEmail, companyNumber ), null );
        final var associationWithUserEmail = associationsService.fetchAssociationDao(companyNumber, null, inviteeEmail);
        AssociationDao association;

        if (associationWithUserEmail.isPresent()) {
            association = associationWithUserEmail.get();
            if (inviteeUserFound) {
                association.setUserEmail(null);
                association.setUserId(inviteeUserDetails.getFirst().getUserId());
            }
            LOGGER.debugContext( getXRequestId(), String.format( "Attempting to create new invitation. Association's id: %s", association.getId() ), null );
            final var now = now();
            associationsService.updateAssociation(association.getId(), mapToInvitationUpdate( association, Optional.ofNullable( inviteeUserDetails ).orElse( new UsersList() ).isEmpty() ? null : inviteeUserDetails.getFirst(), getEricIdentity(), now ) );
            LOGGER.infoContext( getXRequestId(), String.format( "Created new invitation. Association's id: %s", association.getId() ), null );
            emailService.sendInviteEmail( getXRequestId(), companyDetails, inviterDisplayName, now.plusDays( DAYS_SINCE_INVITE_TILL_EXPIRES ).toString(), inviteeEmail ).subscribe();
            cachedAssociatedUsers.flatMap( emailService.sendInvitationEmailToAssociatedUser( getXRequestId(), companyDetails, inviterDisplayName, inviteeEmail ) ).subscribe();
            return new ResponseEntity<>( new ResponseBodyPost().associationLink( String.format( "/associations/%s", association.getId() ) ), HttpStatus.CREATED );
        }

        //if association with email not found and user found
        if (inviteeUserFound) {
            final var userDetails = inviteeUserDetails.getFirst();
            final var inviteeUserId = userDetails.getUserId();
            LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch association for user %s and company %s", inviteeUserId, companyNumber ), null );
            Optional<AssociationDao> associationWithUserID = associationsService.fetchAssociationDao( companyNumber, inviteeUserId, null );
            final var inviteeDisplayName = Optional.ofNullable( userDetails.getDisplayName() ).orElse( userDetails.getEmail() );

            if(associationWithUserID.isEmpty()){
                LOGGER.debugContext( getXRequestId(), String.format( "Attempting to create new invitation. Association's for user %s and company", inviteeUserId, companyNumber ), null );
                association = associationsService.createAssociationWithInvitationApprovalRoute( companyNumber, inviteeUserId, null, getEricIdentity() );
                LOGGER.infoContext( getXRequestId(), String.format( "Created new invitation. Association's id: %s", association.getId() ), null );
                emailService.sendInviteEmail( getXRequestId(), companyDetails, inviterDisplayName, association.getApprovalExpiryAt().toString(), inviteeEmail ).subscribe();
                cachedAssociatedUsers.flatMap( emailService.sendInvitationEmailToAssociatedUser( getXRequestId(), companyDetails, inviterDisplayName, inviteeDisplayName ) ).subscribe();
                return new ResponseEntity<>( new ResponseBodyPost().associationLink( String.format( "/associations/%s", association.getId() ) ), HttpStatus.CREATED );
            } else if(associationWithUserID.get().getStatus().equals("confirmed")) {
                throw new BadRequestRuntimeException(String.format("There is an existing association with Confirmed status for the user %s", inviteeEmail), new Exception( String.format( "%s already has a confirmed association at company %s", inviteeEmail, companyNumber ) ));
            }
            LOGGER.debugContext( getXRequestId(), String.format( "Attempting to create new invitation. Association's id: %s", associationWithUserID.get().getId() ), null );
            final var now = now();
            associationsService.updateAssociation( associationWithUserID.get().getId(), mapToInvitationUpdate( associationWithUserID.get(), inviteeUserDetails.isEmpty() ? null : inviteeUserDetails.getFirst(), getEricIdentity(), now ) );
            LOGGER.infoContext( getXRequestId(), String.format( "Created new invitation. Association's id: %s", associationWithUserID.get().getId() ), null );
            emailService.sendInviteEmail( getXRequestId(), companyDetails, inviterDisplayName, now.plusDays( DAYS_SINCE_INVITE_TILL_EXPIRES ).toString(), inviteeEmail ).subscribe();
            cachedAssociatedUsers.flatMap( emailService.sendInvitationEmailToAssociatedUser( getXRequestId(), companyDetails, inviterDisplayName, inviteeDisplayName ) ).subscribe();
            return new ResponseEntity<>( new ResponseBodyPost().associationLink( String.format( "/associations/%s", associationWithUserID.get().getId() ) ), HttpStatus.CREATED );
        }
        //if association with email not found, user not found
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to create new invitation for user %s and company %s", inviteeEmail, companyNumber ), null );
        association = associationsService.createAssociationWithInvitationApprovalRoute( companyNumber, null, inviteeEmail, getEricIdentity() );
        LOGGER.infoContext( getXRequestId(), String.format( "Created new invitation. Association's id: %s", association.getId() ), null );
        emailService.sendInviteEmail( getXRequestId(), companyDetails, inviterDisplayName, association.getApprovalExpiryAt().toString(), inviteeEmail ).subscribe();
        cachedAssociatedUsers.flatMap( emailService.sendInvitationEmailToAssociatedUser( getXRequestId(), companyDetails, inviterDisplayName, inviteeEmail ) ).subscribe();
        return new ResponseEntity<>( new ResponseBodyPost().associationLink( String.format( "/associations/%s", association.getId() ) ), HttpStatus.CREATED );
    }

}
