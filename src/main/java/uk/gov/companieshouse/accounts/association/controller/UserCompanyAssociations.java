package uk.gov.companieshouse.accounts.association.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.models.UserContext;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyAssociationsInterface;
import uk.gov.companieshouse.api.accounts.associations.model.*;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.REMOVED;

@RestController
public class UserCompanyAssociations implements UserCompanyAssociationsInterface {


    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);
    private static final String PAGE_INDEX_WAS_LESS_THEN_0 = "pageIndex was less then 0";
    private static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN = "Please check the request and try again";
    private static final String ITEMS_PER_PAGE_WAS_LESS_THEN_0 = "itemsPerPage was less then 0";


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

        LOG.debugContext(xRequestId, String.format("Attempting to create association for company_number %s and user_id %s", companyNumber, ericIdentity), null);

        final var userDetails = Objects.requireNonNull(UserContext.getLoggedUser());
        final var displayName = Optional.ofNullable(userDetails.getDisplayName()).orElse(userDetails.getEmail());

        final var companyDetails = companyService.fetchCompanyProfile(companyNumber);
        var existingAssociation = associationsService.fetchAssociationsDaoForUserStatusAndCompany(userDetails, List.of(AWAITING_APPROVAL.getValue(), CONFIRMED.getValue(), REMOVED.getValue()),0,15,companyNumber);

        AssociationDao association;

        if(!existingAssociation.isEmpty()){
            association = existingAssociation.get().iterator().next();
            if(association.getStatus().equals(CONFIRMED.getValue())){
                LOG.error(String.format("%s: Association between user_id %s and company_number %s already exists.", xRequestId, ericIdentity, companyNumber));
                throw new BadRequestRuntimeException("Association already exists.");
            }
            association.setStatus(Association.StatusEnum.CONFIRMED.getValue());
            association.setUserId(userDetails.getUserId());
            association.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
            association.setUserEmail(null);
            association = associationsService.upsertAssociation(association);

        } else{
            association = associationsService.createAssociation(companyNumber, ericIdentity, null, ApprovalRouteEnum.AUTH_CODE, null);
        }

        LOG.debugContext(xRequestId, String.format("Successfully created/updated association for company_number %s and user_id %s in user_company_associations.", companyNumber, ericIdentity), null);

        final var associatedUsers = emailService.createRequestsToFetchAssociatedUsers( companyNumber );
        emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( xRequestId, companyDetails, displayName, associatedUsers );

        return new ResponseEntity<>(new ResponseBodyPost().associationId(association.getId()), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<InvitationsList> fetchActiveInvitationsForUser( final String xRequestId, final String ericIdentity, final Integer pageIndex, final Integer itemsPerPage ) {
        LOG.debugContext( xRequestId, String.format( "Attempting to fetch active invitations for user %s", ericIdentity ), null );

        if (pageIndex < 0) {
            LOG.error(PAGE_INDEX_WAS_LESS_THEN_0);
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }

        if (itemsPerPage <= 0) {
            LOG.error(ITEMS_PER_PAGE_WAS_LESS_THEN_0);
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }

        final var user = UserContext.getLoggedUser();

        final var invitations = associationsService.fetchActiveInvitations( user, pageIndex, itemsPerPage );
        LOG.debugContext( xRequestId, String.format( "Successfully retrieved active invitations for user %s", ericIdentity ), null );

       return new ResponseEntity<>( invitations, HttpStatus.OK );
    }

    @Override
    public ResponseEntity<AssociationsList> fetchAssociationsBy(
            final String xRequestId,
            final String ericIdentity,
            final List<String> status,
            final Integer pageIndex,
            final Integer itemsPerPage,
            final String companyNumber) {

        LOG.debugContext(xRequestId, "Trying to fetch associations data for user in session :".concat(ericIdentity), null);

        if (pageIndex < 0) {
            LOG.error(PAGE_INDEX_WAS_LESS_THEN_0);
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }

        if (itemsPerPage <= 0) {
            LOG.error(ITEMS_PER_PAGE_WAS_LESS_THEN_0);
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }

        final var user = UserContext.getLoggedUser();
        final AssociationsList associationsList = associationsService
                .fetchAssociationsForUserStatusAndCompany(
                        user, status, pageIndex, itemsPerPage, companyNumber);

        return new ResponseEntity<>(associationsList, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Association> getAssociationForId(final String xRequestId, final String id) {
        LOG.debugContext(xRequestId, String.format("Attempting to get the Association details : %s", id), null);
        final var association = associationsService.findAssociationById(id);
        if (association.isEmpty()) {
            var errorMessage = String.format("Cannot find Association for the Id: %s", id);
            LOG.error(errorMessage);
            throw new NotFoundRuntimeException(StaticPropertyUtil.APPLICATION_NAMESPACE, errorMessage);
        }
        return new ResponseEntity<>(association.get(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<InvitationsList> getInvitationsForAssociation(final String xRequestId, final String associationId, final Integer pageIndex, final Integer itemsPerPage) {
        if (pageIndex < 0) {
            LOG.error(PAGE_INDEX_WAS_LESS_THEN_0);
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }

        if (itemsPerPage <= 0) {
            LOG.error(ITEMS_PER_PAGE_WAS_LESS_THEN_0);
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }

        final Optional<AssociationDao> associationDaoOptional = associationsService.findAssociationDaoById(associationId);
        if (associationDaoOptional.isEmpty()) {
            LOG.error(String.format("%s: Could not find association %s in user_company_associations.", xRequestId, associationId));
            throw new NotFoundRuntimeException("accounts-association-api", String.format("Association %s was not found.", associationId));
        }

        final AssociationDao associationDao = associationDaoOptional.get();
        final InvitationsList invitations = associationsService.fetchInvitations(associationDao, pageIndex, itemsPerPage);

        return new ResponseEntity<>(invitations, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ResponseBodyPost> inviteUser(final String xRequestId, final String ericIdentity, final InvitationRequestBodyPost requestBody) {
        final var companyNumber = requestBody.getCompanyNumber();
        final var inviteeEmail = requestBody.getInviteeEmailId();

        LOG.infoContext(xRequestId, String.format("%s is attempting to invite a new user to company %s.", ericIdentity, companyNumber), null);
        if (Objects.isNull(inviteeEmail)) {
            LOG.error(String.format("%s: inviteeEmail is null.", xRequestId));
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }

        final var inviterUserDetails= Objects.requireNonNull(UserContext.getLoggedUser());
        CompanyDetails companyDetails;
        try {
            LOG.debugContext( xRequestId, String.format( "Attempting to fetch %s from company-profile-api.", companyNumber ), null );
            companyDetails= companyService.fetchCompanyProfile( companyNumber );
        } catch( NotFoundRuntimeException notFoundRuntimeException ){
            LOG.error( notFoundRuntimeException.getMessage() );
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }
        if (!associationsService.confirmedAssociationExists(companyNumber,ericIdentity)) {
            LOG.error(String.format("%s: requesting user %s does not have access to invite", xRequestId, ericIdentity));
            throw new BadRequestRuntimeException("requesting user does not have access");
        }

        final var inviterDisplayName = Optional.ofNullable( inviterUserDetails.getDisplayName() ).orElse( inviterUserDetails.getEmail() );
        final var associatedUsers = emailService.createRequestsToFetchAssociatedUsers( companyNumber );

        LOG.debugContext( xRequestId, String.format( "Attempting to search for %s in accounts-user-api.", inviteeEmail ), null );
        final var inviteeUserDetails = usersService.searchUserDetails( List.of( inviteeEmail ) );

        final var inviteeUserFound = Objects.nonNull(inviteeUserDetails) && !inviteeUserDetails.isEmpty();

        final var associationWithUserEmail = associationsService.fetchAssociationForCompanyNumberAndUserEmail(companyNumber, inviteeEmail);
        AssociationDao association;

        if (associationWithUserEmail.isPresent()) {
            LOG.debugContext(xRequestId, String.format("Association for company %s and user email %s was found.", companyNumber, inviteeEmail), null);
            association = associationWithUserEmail.get();
            if (inviteeUserFound) {
                association.setUserEmail(null);
                association.setUserId(inviteeUserDetails.getFirst().getUserId());
            }
            final var invitationAssociation = associationsService.sendNewInvitation(ericIdentity, association);
            emailService.sendInviteEmail( xRequestId, companyDetails, inviterDisplayName, invitationAssociation.getApprovalExpiryAt().toString(), inviteeEmail );
            emailService.sendInvitationEmailToAssociatedUsers( xRequestId, companyDetails, inviterDisplayName,inviteeEmail, associatedUsers );
            return new ResponseEntity<>( new ResponseBodyPost().associationId( invitationAssociation.getId() ), HttpStatus.CREATED );
        }

        //if association with email not found and user found
        if (inviteeUserFound) {
            final var userDetails = inviteeUserDetails.getFirst();
            final var inviteeUserId = userDetails.getUserId();
            LOG.debugContext( xRequestId, String.format( "Attempting to fetch association for company %s and user id %s", companyNumber, inviteeUserId ), null );
            Optional<AssociationDao> associationWithUserID = associationsService.fetchAssociationForCompanyNumberAndUserId( companyNumber, inviteeUserId );
            final var inviteeDisplayName = Optional.ofNullable( userDetails.getDisplayName() ).orElse( userDetails.getEmail() );

            if(associationWithUserID.isEmpty()){
                LOG.infoContext( xRequestId, String.format( "Creating association and invitation for company %s and user id %s", companyNumber, inviteeUserDetails.getFirst().getUserId() ), null );

                association = associationsService.createAssociation(companyNumber,inviteeUserId,null,ApprovalRouteEnum.INVITATION,ericIdentity);
                emailService.sendInviteEmail( xRequestId, companyDetails, inviterDisplayName, association.getApprovalExpiryAt().toString(), inviteeEmail );
                emailService.sendInvitationEmailToAssociatedUsers( xRequestId, companyDetails, inviterDisplayName,inviteeDisplayName, associatedUsers );
                return new ResponseEntity<>( new ResponseBodyPost().associationId( association.getId() ), HttpStatus.CREATED );
            } else if(associationWithUserID.get().getStatus().equals("confirmed")) {
                throw new BadRequestRuntimeException(String.format("There is an existing association with Confirmed status for the user %s", inviteeEmail));
            }
            LOG.infoContext(xRequestId, String.format("Association for company %s and user id %s found, association id: %s with status %s", companyNumber, inviteeUserDetails.getFirst().getUserId(), associationWithUserID.get().getId(), associationWithUserID.get().getStatus()), null);
            association = associationsService.sendNewInvitation(ericIdentity, associationWithUserID.get());
            emailService.sendInviteEmail( xRequestId, companyDetails, inviterDisplayName, association.getApprovalExpiryAt().toString(), inviteeEmail );
            emailService.sendInvitationEmailToAssociatedUsers( xRequestId, companyDetails, inviterDisplayName,inviteeDisplayName, associatedUsers );
            return new ResponseEntity<>( new ResponseBodyPost().associationId( association.getId() ), HttpStatus.CREATED );

        }
        //if association with email not found, user not found
        association = associationsService.createAssociation(companyNumber, null ,inviteeEmail,ApprovalRouteEnum.INVITATION,ericIdentity);
        emailService.sendInviteEmail( xRequestId, companyDetails, inviterDisplayName, association.getApprovalExpiryAt().toString(), inviteeEmail );
        emailService.sendInvitationEmailToAssociatedUsers( xRequestId, companyDetails, inviterDisplayName,inviteeEmail, associatedUsers );

        return new ResponseEntity<>( new ResponseBodyPost().associationId( association.getId() ), HttpStatus.CREATED );
    }


    @Override
    public ResponseEntity<Void> updateAssociationStatusForId(final String xRequestId, final String associationId, final String requestingUserId, final RequestBodyPut requestBody) {

        final var newStatus = requestBody.getStatus();

        LOG.infoContext(xRequestId, String.format("Attempting to update association status for association %s to %s.", associationId, newStatus.getValue()), null);

        final var requestingUserDetails = Objects.requireNonNull(UserContext.getLoggedUser());
        final var requestingUserDisplayValue = Optional.ofNullable(requestingUserDetails.getDisplayName()).orElse(requestingUserDetails.getEmail());


        final var associationOptional = associationsService.findAssociationDaoById(associationId);
        if (associationOptional.isEmpty()) {
            LOG.error(String.format("%s: Could not find association %s in user_company_associations.", xRequestId, associationId));
            throw new NotFoundRuntimeException("accounts-association-api", String.format("Association %s was not found.", associationId));
        }

        final AssociationDao associationDao = associationOptional.get();


        final var oldStatus = associationDao.getStatus();


        final var timestampKey = newStatus.equals(CONFIRMED) ? "approved_at" : "removed_at";
        var update = new Update()
                .set("status", newStatus.getValue())
                .set(timestampKey, LocalDateTime.now().toString());

        final boolean requestingAndTargetUserEmailMatches = Objects.nonNull(associationDao.getUserEmail()) && associationDao.getUserEmail().equals(requestingUserDetails.getEmail());
        final boolean requestingAndTargetUserIdMatches = Objects.nonNull(associationDao.getUserId()) && associationDao.getUserId().equals(requestingUserId);
        final boolean requestingAndTargetUserMatches = requestingAndTargetUserIdMatches || requestingAndTargetUserEmailMatches;
        final boolean associationWithUserEmailExists = Objects.nonNull(associationDao.getUserEmail());


        String targetUserDisplayValue = requestingUserDisplayValue;

        if( !requestingAndTargetUserMatches ){
            //if requesting user and target user is different, requesting user is only allowed to remove and should have active association with the company.
            if (newStatus.equals(CONFIRMED) || !associationsService.confirmedAssociationExists(associationDao.getCompanyNumber(), requestingUserId)) {
                LOG.error(String.format("%s: requesting %s  user does not have access to perform the action", xRequestId, requestingUserId));
                throw new BadRequestRuntimeException("requesting user does not have access to perform the action");
            }
        }

        if (associationWithUserEmailExists) {
            var targetUserEmail = associationDao.getUserEmail();

            Optional<User> targetUserOptional =
                    Optional.ofNullable(usersService.searchUserDetails(List.of(targetUserEmail)))
                            .flatMap(list -> list.stream().findFirst());
            // do not allow user to accept invitation until one login account is created.
            if (newStatus.equals(CONFIRMED) && targetUserOptional.isEmpty()) {
                LOG.error(String.format("%s: Could not find user %s, via the accounts-user-api", xRequestId, targetUserEmail));
                throw new BadRequestRuntimeException(String.format("Could not find data for user %s", targetUserEmail));
            }

            //if user found, swap out the temporary email id with the user id.
            if (targetUserOptional.isPresent()) {
                targetUserDisplayValue = Optional.ofNullable(targetUserOptional.get().getDisplayName()).orElse(targetUserOptional.get().getEmail());
                LOG.debugContext(xRequestId, "Successfully fetched data from accounts-user-api for user.", null);
                update.set("user_email", null).set("user_id", targetUserOptional.get().getUserId());
            } else {
                targetUserDisplayValue = targetUserEmail;
            }
        } else if(!requestingAndTargetUserMatches){
                var tempUser = usersService.fetchUserDetails(associationDao.getUserId());
                targetUserDisplayValue=Optional.ofNullable(tempUser.getDisplayName()).orElse(tempUser.getEmail());
        }



        associationsService.updateAssociation(associationId, update);


        sendEmailNotificationForStatusUpdate(xRequestId,
                requestingUserDisplayValue,
                targetUserDisplayValue,
                requestingAndTargetUserMatches,
                newStatus,
                oldStatus,
                associationOptional.get()
        );

        return new ResponseEntity<>(HttpStatus.OK);
    }

    private void sendEmailNotificationForStatusUpdate(String xRequestId, String requestingUserDisplayValue, String targetUserDisplayValue, boolean requestingAndTargetUserMatches, RequestBodyPut.StatusEnum newStatus, String oldStatus, AssociationDao associationDao) {
        final String companyNumber = associationDao.getCompanyNumber();
        final List<InvitationDao> invitations = associationDao.getInvitations();

        final var companyDetails = companyService.fetchCompanyProfile(companyNumber);
        final var requestsToFetchAssociatedUsers = emailService.createRequestsToFetchAssociatedUsers(companyNumber);

        final var authorisedUserRemoved = oldStatus.equals(CONFIRMED.getValue()) && newStatus.equals(REMOVED);
        final var userAcceptedInvitation = requestingAndTargetUserMatches && oldStatus.equals(AWAITING_APPROVAL.getValue()) && newStatus.equals(CONFIRMED);
        final var userCancelledInvitation = !requestingAndTargetUserMatches && oldStatus.equals(AWAITING_APPROVAL.getValue()) && newStatus.equals(REMOVED);
        final var userRejectedInvitation = requestingAndTargetUserMatches && oldStatus.equals(AWAITING_APPROVAL.getValue()) && newStatus.equals(REMOVED);

        if (userRejectedInvitation) {
            emailService.sendInvitationRejectedEmailToAssociatedUsers(xRequestId, companyDetails, requestingUserDisplayValue, requestsToFetchAssociatedUsers);
        } else if (authorisedUserRemoved) {
            final Supplier<User> requestToGetRemovedUserEmail = usersService.createFetchUserDetailsRequest(associationDao.getUserId());
            emailService.sendAuthorisationRemovedEmailToRemovedUser(xRequestId, companyDetails, requestingUserDisplayValue, requestToGetRemovedUserEmail);
            emailService.sendAuthorisationRemovedEmailToAssociatedUsers(xRequestId, companyDetails, requestingUserDisplayValue, targetUserDisplayValue, requestsToFetchAssociatedUsers);
        } else if (userAcceptedInvitation) {
            final var invitedByDisplayName = invitations.stream()
                    .max(Comparator.comparing(InvitationDao::getInvitedAt))
                    .map(InvitationDao::getInvitedBy)
                    .map(usersService::fetchUserDetails)
                    .map(user -> Optional.ofNullable(user.getDisplayName()).orElse(user.getEmail()))
                    .orElseThrow(() -> new NullPointerException("Inviter does not exist."));

            emailService.sendInvitationAcceptedEmailToAssociatedUsers(xRequestId, companyDetails, invitedByDisplayName, requestingUserDisplayValue, requestsToFetchAssociatedUsers);
        } else if (userCancelledInvitation) {
            emailService.sendInvitationCancelledEmailToAssociatedUsers(xRequestId, companyDetails, requestingUserDisplayValue, targetUserDisplayValue, requestsToFetchAssociatedUsers);
        }
    }


}

