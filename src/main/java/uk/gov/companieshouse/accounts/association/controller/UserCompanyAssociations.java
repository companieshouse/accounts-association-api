package uk.gov.companieshouse.accounts.association.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyAssociationsInterface;
import uk.gov.companieshouse.api.accounts.associations.model.*;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.REMOVED;

@RestController
public class UserCompanyAssociations implements UserCompanyAssociationsInterface {


    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);
    private static final String PAGE_INDEX_WAS_LESS_THAN_0 = "pageIndex was less than 0";
    private static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN = "Please check the request and try again";
    private static final String ITEMS_PER_PAGE_WAS_LESS_THAN_OR_EQUAL_TO_0 = "itemsPerPage was less than or equal to 0";


    private final UsersService usersService;


    private final AssociationsService associationsService;

    private final CompanyService companyService;
    private final EmailService emailService;

    @Autowired
    public UserCompanyAssociations(UsersService usersService, AssociationsService associationsService, CompanyService companyService, EmailService emailService) {
        this.usersService = usersService;
        this.associationsService = associationsService;
        this.companyService = companyService;
        this.emailService = emailService;
    }

    @Override
    public ResponseEntity<ResponseBodyPost> addAssociation(final String xRequestId, final String ericIdentity, final RequestBodyPost requestBody) {
        final var companyNumber = requestBody.getCompanyNumber();

        LOG.infoContext( xRequestId, String.format( "Received request with user_id=%s, company_number=%s.", ericIdentity, companyNumber ),null );

        final var userDetails = Objects.requireNonNull(getUser());
        final var displayName = Optional.ofNullable(userDetails.getDisplayName()).orElse(userDetails.getEmail());

        final var companyDetails = companyService.fetchCompanyProfile(companyNumber);

        LOG.debugContext( xRequestId, String.format( "Attempting to fetch association between user %s and company %s", ericIdentity, companyNumber ), null );
        var existingAssociation = associationsService.fetchAssociationsDaoForUserStatusAndCompany(userDetails, List.of(AWAITING_APPROVAL.getValue(), CONFIRMED.getValue(), REMOVED.getValue()),0,15,companyNumber);

        AssociationDao association;

        if(!existingAssociation.isEmpty()){
            association = existingAssociation.get().iterator().next();
            if(association.getStatus().equals(CONFIRMED.getValue())){
                LOG.errorContext( xRequestId, new Exception( String.format( "Association between user_id %s and company_number %s already exists.", ericIdentity, companyNumber ) ),null );
                throw new BadRequestRuntimeException("Association already exists.");
            }
            association.setStatus(Association.StatusEnum.CONFIRMED.getValue());
            association.setUserId(userDetails.getUserId());
            association.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
            association.setUserEmail(null);
            LOG.debugContext( xRequestId, String.format( "Attempting to update association %s", association.getId() ), null );
            association = associationsService.upsertAssociation(association);
            LOG.infoContext( xRequestId, String.format("Successfully updated association for company_number %s and user_id %s.", companyNumber, ericIdentity ), null );
        } else{
            LOG.debugContext( xRequestId, String.format( "Attempting to create association for company_number %s and user_id %s.", companyNumber, ericIdentity ), null );
            association = associationsService.createAssociation(companyNumber, ericIdentity, null, ApprovalRouteEnum.AUTH_CODE, null);
            LOG.infoContext( xRequestId, String.format("Successfully created association for company_number %s and user_id %s.", companyNumber, ericIdentity ), null );
        }

        LOG.debugContext( xRequestId, String.format( "Attempting to create requests for users associated with company %s", companyNumber ), null );

        Mono.just( companyNumber )
                .map( associationsService::fetchAssociatedUsers )
                .flatMapMany( Flux::fromIterable )
                .flatMap( emailService.sendAuthCodeConfirmationEmailToAssociatedUser( xRequestId, companyDetails, displayName ) )
                .subscribe();

        return new ResponseEntity<>(new ResponseBodyPost().associationId(association.getId()), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<InvitationsList> fetchActiveInvitationsForUser( final String xRequestId, final String ericIdentity, final Integer pageIndex, final Integer itemsPerPage ) {
        LOG.infoContext( xRequestId, String.format( "Received request with user_id=%s, itemsPerPage=%d, pageIndex=%d.", ericIdentity, itemsPerPage, pageIndex ),null );

        if (pageIndex < 0) {
            LOG.errorContext( xRequestId, new Exception(PAGE_INDEX_WAS_LESS_THAN_0), null );
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }

        if (itemsPerPage <= 0) {
            LOG.errorContext( xRequestId, new Exception(ITEMS_PER_PAGE_WAS_LESS_THAN_OR_EQUAL_TO_0), null );
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }

        final var user = getUser();

        LOG.debugContext( xRequestId, String.format( "Attempting to retrieve active invitations for user %s", ericIdentity ), null );
        final var invitations = associationsService.fetchActiveInvitations( user, pageIndex, itemsPerPage );
        LOG.infoContext( xRequestId, String.format( "Successfully retrieved active invitations for user %s", ericIdentity ), null );

       return new ResponseEntity<>( invitations, HttpStatus.OK );
    }

    @Override
    public ResponseEntity<AssociationsList> fetchAssociationsBy( final String xRequestId, final String ericIdentity, final List<String> status, final Integer pageIndex, final Integer itemsPerPage, final String companyNumber ) {
        LOG.infoContext( xRequestId, String.format( "Received request with user_id=%s, status=%s, page_index=%d, items_per_page=%d, company_number=%s.", ericIdentity, String.join( ",", status ), pageIndex, itemsPerPage, companyNumber ),null );

        if ( pageIndex < 0 ) {
            LOG.errorContext( xRequestId, new Exception( PAGE_INDEX_WAS_LESS_THAN_0 ), null );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        if ( itemsPerPage <= 0 ) {
            LOG.errorContext( xRequestId, new Exception( ITEMS_PER_PAGE_WAS_LESS_THAN_OR_EQUAL_TO_0 ), null );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        final var user = getUser();

        LOG.debugContext( xRequestId, "Attempting to fetch associations", null );
        final var associationsList = associationsService.fetchAssociationsForUserStatusAndCompany( user, status, pageIndex, itemsPerPage, companyNumber );
        LOG.infoContext( xRequestId, String.format( "Successfully fetched %d associations", associationsList.getItems().size() ), null );

        return new ResponseEntity<>( associationsList, HttpStatus.OK );
    }

    @Override
    public ResponseEntity<Association> getAssociationForId(final String xRequestId, final String id) {
        LOG.infoContext( xRequestId, String.format( "Received request with id=%s.", id ),null );

        LOG.debugContext( xRequestId, String.format( "Attempting to retrieve association with id: %s", id ), null );
        final var association = associationsService.findAssociationById(id);
        if (association.isEmpty()) {
            var errorMessage = String.format("Cannot find Association for the id: %s", id);
            LOG.errorContext( xRequestId, new Exception( errorMessage ), null );
            throw new NotFoundRuntimeException(StaticPropertyUtil.APPLICATION_NAMESPACE, errorMessage);
        }

        LOG.infoContext( xRequestId, String.format( "Successfully fetched association %s", id ), null );

        return new ResponseEntity<>(association.get(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<InvitationsList> getInvitationsForAssociation( final String xRequestId, final String associationId, final Integer pageIndex, final Integer itemsPerPage ) {
        LOG.infoContext( xRequestId, String.format( "Received request with id=%s, page_index=%d, items_per_page=%d.", associationId, pageIndex, itemsPerPage ),null );

        if ( pageIndex < 0 ) {
            LOG.errorContext( xRequestId, new Exception( PAGE_INDEX_WAS_LESS_THAN_0 ), null );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        if ( itemsPerPage <= 0 ) {
            LOG.errorContext( xRequestId, new Exception( ITEMS_PER_PAGE_WAS_LESS_THAN_OR_EQUAL_TO_0 ), null );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        LOG.debugContext( xRequestId, String.format( "Attempting to fetch association %s", associationId ), null );
        final var associationDaoOptional = associationsService.findAssociationDaoById( associationId );
        if ( associationDaoOptional.isEmpty() ) {
            LOG.errorContext( xRequestId, new Exception( String.format( "Could not find association %s.", associationId ) ), null );
            throw new NotFoundRuntimeException( "accounts-association-api", String.format( "Association %s was not found.", associationId ) );
        }

        final var associationDao = associationDaoOptional.get();
        LOG.debugContext( xRequestId, String.format( "Attempting to fetch invitations for association %s", associationId ), null );
        final var invitations = associationsService.fetchInvitations( associationDao, pageIndex, itemsPerPage );

        LOG.infoContext( xRequestId, String.format( "Successfully fetched invitations for association %s", associationId ), null );

        return new ResponseEntity<>( invitations, HttpStatus.OK );
    }

    @Override
    public ResponseEntity<ResponseBodyPost> inviteUser(final String xRequestId, final String ericIdentity, final InvitationRequestBodyPost requestBody) {
        final var companyNumber = requestBody.getCompanyNumber();
        final var inviteeEmail = requestBody.getInviteeEmailId();

        LOG.infoContext( xRequestId, String.format( "Received request with user_id=%s, company_number=%s, invitee_email_id=%s.", ericIdentity, companyNumber, inviteeEmail  ),null );

        if (Objects.isNull(inviteeEmail)) {
            LOG.errorContext( xRequestId, new Exception( "invitee_email_id is null." ), null );
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }

        final var inviterUserDetails= Objects.requireNonNull(getUser());
        CompanyDetails companyDetails;
        try {
            companyDetails= companyService.fetchCompanyProfile( companyNumber );
        } catch( NotFoundRuntimeException notFoundRuntimeException ){
            LOG.errorContext( xRequestId, new Exception( notFoundRuntimeException.getMessage() ), null );
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }
        if (!associationsService.confirmedAssociationExists(companyNumber,ericIdentity)) {
            LOG.errorContext( xRequestId, new Exception( String.format( "Requesting user %s does not have a confirmed association at company %s", ericIdentity, companyNumber ) ), null );
            throw new BadRequestRuntimeException("requesting user does not have access");
        }

        final var inviterDisplayName = Optional.ofNullable( inviterUserDetails.getDisplayName() ).orElse( inviterUserDetails.getEmail() );
        LOG.debugContext( xRequestId, String.format( "Attempting to create requests for users associated with company %s", companyNumber ), null );
        final var cachedAssociatedUsers = Mono.just( companyNumber )
                .map( associationsService::fetchAssociatedUsers )
                .flatMapMany( Flux::fromIterable )
                .cache();

        final var inviteeUserDetails = usersService.searchUserDetails( List.of( inviteeEmail ) );

        final var inviteeUserFound = Objects.nonNull(inviteeUserDetails) && !inviteeUserDetails.isEmpty();

        LOG.debugContext( xRequestId, String.format( "Attempting to fetch association for user %s and company %s", inviteeEmail, companyNumber ), null );
        final var associationWithUserEmail = associationsService.fetchAssociationForCompanyNumberAndUserEmail(companyNumber, inviteeEmail);
        AssociationDao association;

        if (associationWithUserEmail.isPresent()) {
            association = associationWithUserEmail.get();
            if (inviteeUserFound) {
                association.setUserEmail(null);
                association.setUserId(inviteeUserDetails.getFirst().getUserId());
            }
            LOG.debugContext( xRequestId, String.format( "Attempting to create new invitation. Association's id: %s", association.getId() ), null );
            final var invitationAssociation = associationsService.sendNewInvitation(ericIdentity, association);
            LOG.infoContext( xRequestId, String.format( "Created new invitation. Association's id: %s", invitationAssociation.getId() ), null );
            emailService.sendInviteEmail( xRequestId, companyDetails, inviterDisplayName, invitationAssociation.getApprovalExpiryAt().toString(), inviteeEmail ).subscribe();
            cachedAssociatedUsers.flatMap( emailService.sendInvitationEmailToAssociatedUser( xRequestId, companyDetails, inviterDisplayName, inviteeEmail ) ).subscribe();
            return new ResponseEntity<>( new ResponseBodyPost().associationId( invitationAssociation.getId() ), HttpStatus.CREATED );
        }

        //if association with email not found and user found
        if (inviteeUserFound) {
            final var userDetails = inviteeUserDetails.getFirst();
            final var inviteeUserId = userDetails.getUserId();
            LOG.debugContext( xRequestId, String.format( "Attempting to fetch association for user %s and company %s", inviteeUserId, companyNumber ), null );
            Optional<AssociationDao> associationWithUserID = associationsService.fetchAssociationForCompanyNumberAndUserId( companyNumber, inviteeUserId );
            final var inviteeDisplayName = Optional.ofNullable( userDetails.getDisplayName() ).orElse( userDetails.getEmail() );

            if(associationWithUserID.isEmpty()){
                LOG.debugContext( xRequestId, String.format( "Attempting to create new invitation. Association's for user %s and company", inviteeUserId, companyNumber ), null );
                association = associationsService.createAssociation(companyNumber,inviteeUserId,null,ApprovalRouteEnum.INVITATION,ericIdentity);
                LOG.infoContext( xRequestId, String.format( "Created new invitation. Association's id: %s", association.getId() ), null );
                emailService.sendInviteEmail( xRequestId, companyDetails, inviterDisplayName, association.getApprovalExpiryAt().toString(), inviteeEmail ).subscribe();
                cachedAssociatedUsers.flatMap( emailService.sendInvitationEmailToAssociatedUser( xRequestId, companyDetails, inviterDisplayName, inviteeDisplayName ) ).subscribe();
                return new ResponseEntity<>( new ResponseBodyPost().associationId( association.getId() ), HttpStatus.CREATED );
            } else if(associationWithUserID.get().getStatus().equals("confirmed")) {
                LOG.errorContext( xRequestId, new Exception( String.format( "%s already has a confirmed association at company %s", inviteeEmail, companyNumber ) ), null );
                throw new BadRequestRuntimeException(String.format("There is an existing association with Confirmed status for the user %s", inviteeEmail));
            }
            LOG.debugContext( xRequestId, String.format( "Attempting to create new invitation. Association's id: %s", associationWithUserID.get().getId() ), null );
            association = associationsService.sendNewInvitation(ericIdentity, associationWithUserID.get());
            LOG.infoContext( xRequestId, String.format( "Created new invitation. Association's id: %s", association.getId() ), null );
            emailService.sendInviteEmail( xRequestId, companyDetails, inviterDisplayName, association.getApprovalExpiryAt().toString(), inviteeEmail ).subscribe();
            cachedAssociatedUsers.flatMap( emailService.sendInvitationEmailToAssociatedUser( xRequestId, companyDetails, inviterDisplayName, inviteeDisplayName ) ).subscribe();
            return new ResponseEntity<>( new ResponseBodyPost().associationId( association.getId() ), HttpStatus.CREATED );
        }
        //if association with email not found, user not found
        LOG.debugContext( xRequestId, String.format( "Attempting to create new invitation for user %s and company %s", inviteeEmail, companyNumber ), null );
        association = associationsService.createAssociation(companyNumber, null ,inviteeEmail,ApprovalRouteEnum.INVITATION,ericIdentity);
        LOG.infoContext( xRequestId, String.format( "Created new invitation. Association's id: %s", association.getId() ), null );
        emailService.sendInviteEmail( xRequestId, companyDetails, inviterDisplayName, association.getApprovalExpiryAt().toString(), inviteeEmail ).subscribe();
        cachedAssociatedUsers.flatMap( emailService.sendInvitationEmailToAssociatedUser( xRequestId, companyDetails, inviterDisplayName, inviteeEmail ) ).subscribe();
        return new ResponseEntity<>( new ResponseBodyPost().associationId( association.getId() ), HttpStatus.CREATED );
    }

    private void throwBadRequestWhenUserIsNotPermittedToPerformAction( final String xRequestId, final String requestingUserId, final AssociationDao associationDao, final RequestBodyPut.StatusEnum newStatus, final boolean requestingAndTargetUserMatches ){
        if ( !requestingAndTargetUserMatches && ( newStatus.equals( CONFIRMED ) || !associationsService.confirmedAssociationExists( associationDao.getCompanyNumber(), requestingUserId ) ) ) {
            LOG.errorContext( xRequestId, new Exception( String.format( "Requesting %s user cannot change another user to confirmed or the requesting user is not associated with company %s", requestingUserId, associationDao.getCompanyNumber() ) ), null );
            throw new BadRequestRuntimeException( "requesting user does not have access to perform the action" );
        }
    }

    private String updateAssociationWithUserEmail( final String xRequestId, final RequestBodyPut.StatusEnum newStatus, final AssociationDao associationDao ){
        final var targetUserEmail = associationDao.getUserEmail();
        final var timestampKey = newStatus.equals( CONFIRMED ) ? "approved_at" : "removed_at";
        final var update = new Update().set( "status", newStatus.getValue() ).set( timestampKey, LocalDateTime.now().toString() );

        final var targetUserDisplayValue =
                Optional.ofNullable( usersService.searchUserDetails( List.of( targetUserEmail ) ) )
                        .flatMap( list -> list.stream().findFirst() )
                        .map( user -> {
                            update.set( "user_email", null ).set( "user_id", user.getUserId() );
                            return Optional.ofNullable( user.getDisplayName() ).orElse( user.getEmail() );
                        } )
                        .orElseGet( () -> {
                            if ( newStatus.equals( CONFIRMED ) ) {
                                LOG.errorContext( xRequestId, new Exception( String.format( "Could not find user %s, so cannot change status to confirmed.", targetUserEmail ) ), null );
                                throw new BadRequestRuntimeException( String.format( "Could not find data for user %s", targetUserEmail ) );
                            }
                            return targetUserEmail;
                        } );

        LOG.debugContext( xRequestId, String.format( "Attempting to update association with id: %s", associationDao.getId() ), null );
        associationsService.updateAssociation( associationDao.getId(), update );

        return targetUserDisplayValue;
    }

    private String updateAssociationWithUserId( final RequestBodyPut.StatusEnum newStatus, final AssociationDao associationDao, final String requestingUserDisplayValue, final boolean requestingAndTargetUserMatches ){
        final var timestampKey = newStatus.equals( CONFIRMED ) ? "approved_at" : "removed_at";
        final var update = new Update().set( "status", newStatus.getValue() ).set( timestampKey, LocalDateTime.now().toString() );

        var targetUserDisplayValue = requestingUserDisplayValue;
        if( !requestingAndTargetUserMatches ){
            final var tempUser = usersService.fetchUserDetails( associationDao.getUserId() );
            targetUserDisplayValue = Optional.ofNullable( tempUser.getDisplayName() ).orElse( tempUser.getEmail() );
        }

        LOG.debugContext( getXRequestId(), String.format( "Attempting to update association with id: %s", associationDao.getId() ), null );
        associationsService.updateAssociation(associationDao.getId(), update);

        return targetUserDisplayValue;
    }

    @Override
    public ResponseEntity<Void> updateAssociationStatusForId( final String xRequestId, final String associationId, final String requestingUserId, final RequestBodyPut requestBody ) {
        final var newStatus = requestBody.getStatus();

        LOG.infoContext( xRequestId, String.format( "Received request with id=%s, user_id=%s, status=%s.", associationId, requestingUserId, newStatus.getValue() ),null );

        final var requestingUserDetails = Objects.requireNonNull( getUser() );
        final var requestingUserDisplayValue = Optional.ofNullable( requestingUserDetails.getDisplayName() ).orElse( requestingUserDetails.getEmail() );

        LOG.debugContext( xRequestId, String.format( "Attempting to fetch association with id %s", associationId ), null );
        final var associationDao =
        associationsService.findAssociationDaoById( associationId )
                .orElseThrow( () -> {
                    LOG.errorContext( xRequestId, new Exception( String.format( "Could not find association %s.", associationId ) ), null );
                    return new NotFoundRuntimeException( "accounts-association-api", String.format( "Association %s was not found.", associationId ) );
                } );
        
        final boolean requestingAndTargetUserEmailMatches = Objects.nonNull( associationDao.getUserEmail() ) && associationDao.getUserEmail().equals( requestingUserDetails.getEmail() );
        final boolean requestingAndTargetUserIdMatches = Objects.nonNull( associationDao.getUserId() ) && associationDao.getUserId().equals( requestingUserId );
        final boolean requestingAndTargetUserMatches = requestingAndTargetUserIdMatches || requestingAndTargetUserEmailMatches;
        final boolean associationWithUserEmailExists = Objects.nonNull( associationDao.getUserEmail() );

        throwBadRequestWhenUserIsNotPermittedToPerformAction( xRequestId, requestingUserId, associationDao, newStatus, requestingAndTargetUserMatches );

        var targetUserDisplayValue = requestingUserDisplayValue;
        if ( associationWithUserEmailExists ) {
            targetUserDisplayValue = updateAssociationWithUserEmail( xRequestId, newStatus, associationDao );
        } else {
            targetUserDisplayValue = updateAssociationWithUserId( newStatus, associationDao, requestingUserDisplayValue, requestingAndTargetUserMatches );
        }
        LOG.infoContext( xRequestId, String.format( "Updated association %s", associationDao.getId() ), null );

        sendEmailNotificationForStatusUpdate( xRequestId, requestingUserDisplayValue, targetUserDisplayValue, requestingAndTargetUserMatches, newStatus, associationDao.getStatus(), associationDao );

        return new ResponseEntity<>( HttpStatus.OK );
    }

    private void sendEmailNotificationForStatusUpdate( final String xRequestId, final String requestingUserDisplayValue, final String targetUserDisplayValue, final boolean requestingAndTargetUserMatches, final RequestBodyPut.StatusEnum newStatus, final String oldStatus, final AssociationDao associationDao ) {
        final var cachedCompanyName = Mono.just( associationDao.getCompanyNumber() )
                .map( companyService::fetchCompanyProfile )
                .map( CompanyDetails::getCompanyName )
                .cache();

        final var cachedAssociatedUsers =
        Mono.just( associationDao.getCompanyNumber() )
                .map( associationsService::fetchAssociatedUsers )
                .flatMapMany( Flux::fromIterable )
                .cache();

        final var cachedInvitedByDisplayName = Mono.just( associationDao )
                .map( AssociationDao::getInvitations )
                .flatMapMany( Flux::fromIterable )
                .reduce( (firstInvitation, secondInvitation) -> firstInvitation.getInvitedAt().isAfter( secondInvitation.getInvitedAt() ) ? firstInvitation : secondInvitation )
                .map( InvitationDao::getInvitedBy )
                .map( usersService::fetchUserDetails )
                .map( user -> Optional.ofNullable( user.getDisplayName() ).orElse( user.getEmail() ) )
                .cache();

        var emails = Flux.empty();
        if ( requestingAndTargetUserMatches && oldStatus.equals( AWAITING_APPROVAL.getValue() ) && newStatus.equals( REMOVED ) ) {
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendInvitationRejectedEmailToAssociatedUser( xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue ) ) );
        } else if ( oldStatus.equals( CONFIRMED.getValue() ) && newStatus.equals( REMOVED ) ) {
            emails = emails.concatWith( emailService.sendAuthorisationRemovedEmailToRemovedUser( xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, associationDao.getUserId() ) );
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendAuthorisationRemovedEmailToAssociatedUser( xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ) );
        } else if ( requestingAndTargetUserMatches && oldStatus.equals( AWAITING_APPROVAL.getValue() ) && newStatus.equals( CONFIRMED ) ) {
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendInvitationAcceptedEmailToAssociatedUser( xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, cachedInvitedByDisplayName, requestingUserDisplayValue ) ) );
        } else if ( !requestingAndTargetUserMatches && oldStatus.equals( AWAITING_APPROVAL.getValue() ) && newStatus.equals( REMOVED ) ) {
            emails = emails.concatWith( emailService.sendInviteCancelledEmail(xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, associationDao ) );
            emails = emails.concatWith( cachedAssociatedUsers.flatMap( emailService.sendInvitationCancelledEmailToAssociatedUser( xRequestId, associationDao.getCompanyNumber(), cachedCompanyName, requestingUserDisplayValue, targetUserDisplayValue ) ) );
        }
        emails.subscribe();
    }

}

