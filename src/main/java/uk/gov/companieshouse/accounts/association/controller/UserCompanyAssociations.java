package uk.gov.companieshouse.accounts.association.controller;

import static uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum.INVITATION;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.REMOVED;

import java.util.Comparator;
import java.util.LinkedList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
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
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@RestController
public class UserCompanyAssociations implements UserCompanyAssociationsInterface {


    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);


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

        LOG.debugContext( xRequestId, String.format("Attempting to fetch user for user_id %s from accounts-user-api.", ericIdentity), null );
        final var userDetails = usersService.fetchUserDetails( ericIdentity );
        final var displayName = Optional.ofNullable( userDetails.getDisplayName() ).orElse( userDetails.getEmail() );
        LOG.debugContext( xRequestId, String.format("Successfully fetched user for user_id %s from accounts-user-api.", ericIdentity), null );

        LOG.debugContext(xRequestId, String.format("Attempting to fetch company for company_number %s from company profile cache.", companyNumber), null);
        final var companyDetails = companyService.fetchCompanyProfile(companyNumber);
        LOG.debugContext(xRequestId, String.format("Successfully fetched company for company_number %s from company profile cache.", companyNumber), null);

        LOG.debugContext(xRequestId, String.format("Attempting to check if association between company_number %s and user_id %s exists in user_company_associations.", companyNumber, ericIdentity), null);
        if (associationsService.associationExists(companyNumber, ericIdentity)) {
            LOG.error(String.format("%s: Association between user_id %s and company_number %s already exists.", xRequestId, ericIdentity, companyNumber));
            throw new BadRequestRuntimeException("Association already exists.");
        }
        LOG.debugContext(xRequestId, String.format("Could not find association for company_number %s and user_id %s in user_company_associations.", companyNumber, ericIdentity), null);

        final var associatedUsers = emailService.createRequestsToFetchAssociatedUsers( companyNumber, List.of() );
        LOG.debugContext(xRequestId, String.format("Attempting to create association for company_number %s and user_id %s in user_company_associations.", companyNumber, ericIdentity), null);
        final var association = associationsService.createAssociation(companyNumber, ericIdentity, null, ApprovalRouteEnum.AUTH_CODE, null);
        LOG.debugContext(xRequestId, String.format("Successfully created association for company_number %s and user_id %s in user_company_associations.", companyNumber, ericIdentity), null);
        emailService.sendAuthCodeConfirmationEmailToAssociatedUsers( xRequestId, companyDetails, displayName, associatedUsers );

        return new ResponseEntity<>(new ResponseBodyPost().associationId(association.getId()), HttpStatus.CREATED);
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
            LOG.error("pageIndex was less then 0");
            throw new BadRequestRuntimeException("Please check the request and try again");
        }

        if (itemsPerPage <= 0) {
            LOG.error("itemsPerPage was less then 0");
            throw new BadRequestRuntimeException("Please check the request and try again");
        }

        final var user = usersService.fetchUserDetails(ericIdentity);
        Optional.ofNullable(user).orElseThrow(() -> new BadRequestRuntimeException("Eric id is not valid"));//NOSONAR or else throw will be caught by controller advice
        final AssociationsList associationsList = associationsService
                .fetchAssociationsForUserStatusAndCompany(
                        user, status, pageIndex, itemsPerPage, companyNumber);

        return new ResponseEntity<>(associationsList, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<Association> getAssociationForId(final String xRequestId, final String id) {
        LOG.debugContext(xRequestId, String.format("Attempting to get the Association details : %s",id), null);

        final var association = associationsService.findAssociationById(id);
        if (association.isEmpty()) {
            var errorMessage = String.format("Cannot find Association for the Id: %s", id);
            LOG.error(errorMessage);
            throw new NotFoundRuntimeException(StaticPropertyUtil.APPLICATION_NAMESPACE, errorMessage);
        }
        return new ResponseEntity<>(association.get(), HttpStatus.OK);
    }

    @Override
    public ResponseEntity<ResponseBodyPost> inviteUser( final String xRequestId, final String ericIdentity, final InvitationRequestBodyPost requestBody ) {
        final var companyNumber = requestBody.getCompanyNumber();
        final var inviteeEmail = requestBody.getInviteeEmailId();

        LOG.infoContext( xRequestId, String.format("%s is attempting to invite a new user to company %s.", ericIdentity, companyNumber ), null );
        if ( Objects.isNull( inviteeEmail ) ) {
            LOG.error( String.format( "%s: inviteeEmail is null.", xRequestId ) );
            throw new BadRequestRuntimeException( "Please check the request and try again" );
        }

        try {
            LOG.debugContext( xRequestId, String.format( "Attempting to fetch %s from accounts-user-api.", ericIdentity ), null );
            usersService.fetchUserDetails(ericIdentity);
            LOG.debugContext( xRequestId, String.format( "Attempting to fetch %s from company-profile-api.", companyNumber ), null );
            companyService.fetchCompanyProfile( companyNumber );
        } catch( NotFoundRuntimeException notFoundRuntimeException ){
            LOG.error( notFoundRuntimeException.getMessage() );
            throw new BadRequestRuntimeException( "Please check the request and try again" );
        }

        LOG.debugContext( xRequestId, String.format( "Attempting to search for %s in accounts-user-api.", inviteeEmail ), null );
        final var inviteeUserDetails = usersService.searchUserDetails( List.of( inviteeEmail ) );
        final var inviteeUserFound = !( Objects.isNull( inviteeUserDetails ) || inviteeUserDetails.isEmpty() );

        LOG.debugContext( xRequestId, String.format( "Attempting to fetch association for company %s and user email %s.", companyNumber, inviteeEmail ), null );
        final var associationWithUserEmail = associationsService.fetchAssociationForCompanyNumberAndUserEmail( companyNumber, inviteeEmail );
        AssociationDao association = null;

        if ( associationWithUserEmail.isPresent()){
            LOG.debugContext( xRequestId, String.format( "Association for company %s and user email %s was found.", companyNumber, inviteeEmail), null );
            association = associationWithUserEmail.get();
            if( inviteeUserFound ) {
                association.setUserEmail(null);
                association.setUserId(inviteeUserDetails.getFirst().getUserId());
            }
            var associationId = associationsService.sendNewInvitation(ericIdentity, association).getId();
            return new ResponseEntity<>( new ResponseBodyPost().associationId( associationId ), HttpStatus.CREATED );
        }

        //if association with email not found and user found
        if ( inviteeUserFound ){
            final var userDetails = inviteeUserDetails.getFirst();
            final var inviteeUserId = userDetails.getUserId();
            LOG.debugContext( xRequestId, String.format( "Attempting to fetch association for company %s and user id %s", companyNumber, inviteeUserId ), null );
            Optional<AssociationDao> associationWithUserID = associationsService.fetchAssociationForCompanyNumberAndUserId( companyNumber, inviteeUserId );

            if(associationWithUserID.isEmpty()){
                LOG.infoContext( xRequestId, String.format( "Creating association and invitation for company %s and user id %s", companyNumber, inviteeUserDetails.getFirst().getUserId() ), null );
                association = associationsService.createAssociation(companyNumber,inviteeUserId,null,ApprovalRouteEnum.INVITATION,ericIdentity);
                return new ResponseEntity<>( new ResponseBodyPost().associationId( association.getId() ), HttpStatus.CREATED );
            } else if(associationWithUserID.get().getStatus().equals("confirmed")) {
                throw new BadRequestRuntimeException(String.format("There is an existing association with Confirmed status for the user %s", inviteeEmail));
            }
            LOG.infoContext( xRequestId, String.format( "Association for company %s and user id %s found, association id: %s with status %s",  companyNumber, inviteeUserDetails.getFirst().getUserId(), associationWithUserID.get().getId(), associationWithUserID.get().getStatus() ), null );
            LOG.debugContext( xRequestId, String.format( "Attempting to send new invitation for company %s and user id %s for association id: %s", companyNumber, inviteeUserId, associationWithUserID.get().getId() ), null );
            association = associationsService.sendNewInvitation(ericIdentity, associationWithUserID.get());
            return new ResponseEntity<>( new ResponseBodyPost().associationId( association.getId() ), HttpStatus.CREATED );

        }
        //if association with email not found, user not found
        association = associationsService.createAssociation(companyNumber, null ,inviteeEmail,ApprovalRouteEnum.INVITATION,ericIdentity);
        return new ResponseEntity<>( new ResponseBodyPost().associationId( association.getId() ), HttpStatus.CREATED );
    }

    @Override
    public ResponseEntity<Void> updateAssociationStatusForId(final String xRequestId, final String associationId, final String requestingUserId, final RequestBodyPut requestBody) {
        final var newStatus = requestBody.getStatus();

        LOG.debugContext(xRequestId, String.format("Attempting to update association status for association %s to %s.", associationId, newStatus.getValue()), null);

        LOG.debugContext( xRequestId, String.format("Attempting to fetch user for user_id %s from accounts-user-api.", requestingUserId), null );
        final var requestingUserDetails = usersService.fetchUserDetails( requestingUserId );
        final var requestingUserEmail = requestingUserDetails.getEmail();
        final var requestingUserDisplayName = Optional.ofNullable( requestingUserDetails.getDisplayName() ).orElse( requestingUserDetails.getEmail() );
        LOG.debugContext( xRequestId, String.format("Successfully fetched user for user_id %s from accounts-user-api.", requestingUserId), null );

        LOG.debugContext(xRequestId, String.format("Attempting to fetch association %s from user_company_associations.", associationId), null);
        final var associationOptional = associationsService.findAssociationDaoById(associationId);
        if (associationOptional.isEmpty()) {
            LOG.error(String.format("%s: Could not find association %s in user_company_associations.", xRequestId, associationId));
            throw new NotFoundRuntimeException("accounts-association-api", String.format("Association %s was not found.", associationId));
        }
        LOG.debugContext(xRequestId, String.format("Successfully fetched association %s from user_company_associations.", associationId), null);

        final var association = associationOptional.get();
        final var targetUserId = association.getUserId();
        final var targetUserEmail = association.getUserEmail();
        final var companyNumber = association.getCompanyNumber();
        final var oldStatus = association.getStatus();
        final var approvalRoute = association.getApprovalRoute();
        final var invitations = association.getInvitations();
        final var timestampKey = newStatus.equals(CONFIRMED) ? "approved_at" : "removed_at";

        var update = new Update()
                .set("status", newStatus.getValue())
                .set(timestampKey, LocalDateTime.now().toString());

        Optional<User> targetUserOptional = Optional.empty();
        if ( Objects.isNull( targetUserId ) ) {
            LOG.debugContext(xRequestId, String.format("Association %s does not have a userId. Attempting to fetch data from accounts-user-api.", associationId), null);

            targetUserOptional =
            Optional.ofNullable( usersService.searchUserDetails( List.of( targetUserEmail ) ) )
                    .flatMap( list -> list.stream().findFirst() );

            if ( newStatus.equals(CONFIRMED) && targetUserOptional.isEmpty() ) {
                LOG.error(String.format("%s: Could not find user %s, via the accounts-user-api", xRequestId, targetUserEmail));
                throw new BadRequestRuntimeException(String.format("Could not find data for user %s", targetUserEmail));
            }

            //if user found, swap out the temporary email id with the user id.
            if ( targetUserOptional.isPresent() ) {
                LOG.debugContext(xRequestId, "Successfully fetched data from accounts-user-api for user.", null);
                update.set("user_email", null).set("user_id", targetUserOptional.get().getUserId());
            }
        }

        LOG.debugContext(xRequestId, String.format("Attempting to update the status of association %s to %s", associationId, newStatus.getValue()), null);
        associationsService.updateAssociation(associationId, update);
        LOG.debugContext(xRequestId, String.format( "Successfully updated association status for association %s to %s.", associationId, newStatus.getValue() ), null);

        final var userIdsMatch = !Objects.isNull( targetUserId ) && targetUserId.equals( requestingUserId );
        final var userEmailsMatch = !Objects.isNull( targetUserEmail ) && targetUserEmail.equals( requestingUserEmail );
        final var usersMatch = userIdsMatch || userEmailsMatch;
        final var authorisedUserRemoved = !usersMatch && oldStatus.equals( CONFIRMED.getValue() ) && newStatus.equals( REMOVED );
        final var userAcceptedInvitation = usersMatch && INVITATION.getValue().equals( approvalRoute ) && oldStatus.equals( AWAITING_APPROVAL.getValue() ) && newStatus.equals( CONFIRMED );
        final var userCancelledInvitation = !usersMatch && INVITATION.getValue().equals( approvalRoute ) && oldStatus.equals( AWAITING_APPROVAL.getValue() ) && newStatus.equals( REMOVED );
        final var userRejectedInvitation = usersMatch && INVITATION.getValue().equals( approvalRoute ) && oldStatus.equals( AWAITING_APPROVAL.getValue() ) && newStatus.equals( REMOVED );
        final var notificationMustBeSent = authorisedUserRemoved || userAcceptedInvitation || userCancelledInvitation || userRejectedInvitation;
        if ( notificationMustBeSent ){
            LOG.debugContext(xRequestId, String.format("Attempting to fetch company for company_number %s from company profile cache.", companyNumber), null);
            final var companyDetails = companyService.fetchCompanyProfile(companyNumber);
            LOG.debugContext(xRequestId, String.format("Successfully fetched company for company_number %s from company profile cache.", companyNumber), null);

            var targetUser = requestingUserDetails;
            if ( !usersMatch ){
                targetUser = targetUserOptional.orElseGet( () -> !Objects.isNull( targetUserId ) ? usersService.fetchUserDetails( targetUserId ) : null );
            }
            final String targetUserDisplayName =
            Optional.ofNullable( targetUser )
                    .map( user -> Optional.ofNullable( user.getDisplayName() ).orElse( user.getEmail() ) )
                    .orElse( targetUserEmail );

            final var excludedUserIds = new LinkedList<>( List.of( requestingUserId ) );
            if ( !Objects.isNull( targetUser ) ) {
                excludedUserIds.add( targetUser.getUserId() );
            }
            final var requestsToFetchAssociatedUsers = emailService.createRequestsToFetchAssociatedUsers( companyNumber, excludedUserIds );

            if ( authorisedUserRemoved ) {
                emailService.sendAuthorisationRemovedEmailToAssociatedUsers( xRequestId, companyDetails, requestingUserDisplayName, targetUserDisplayName, requestsToFetchAssociatedUsers );
            }

            if ( userAcceptedInvitation ) {
                final var invitedByDisplayName =
                invitations.stream()
                           .max( Comparator.comparing( InvitationDao::getInvitedAt ) )
                           .map( InvitationDao::getInvitedBy )
                           .map( usersService::fetchUserDetails )
                           .map( user -> Optional.ofNullable( user.getDisplayName() ).orElse( user.getEmail() ) )
                           .orElseThrow( () -> new NullPointerException( "Inviter does not exist." ) );

                emailService.sendInvitationAcceptedEmailToAssociatedUsers( xRequestId, companyDetails, invitedByDisplayName, requestingUserDisplayName, requestsToFetchAssociatedUsers );
            }

            if ( userCancelledInvitation ){
                emailService.sendInvitationCancelledEmailToAssociatedUsers( xRequestId, companyDetails, requestingUserDisplayName, targetUserDisplayName, requestsToFetchAssociatedUsers );
            }

            if ( userRejectedInvitation ){
                emailService.sendInvitationRejectedEmailToAssociatedUsers( xRequestId, companyDetails, requestingUserDisplayName, requestsToFetchAssociatedUsers );
            }
        }

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
