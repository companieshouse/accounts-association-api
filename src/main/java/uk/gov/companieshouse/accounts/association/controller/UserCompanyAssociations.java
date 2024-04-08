package uk.gov.companieshouse.accounts.association.controller;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyAssociationsInterface;
import uk.gov.companieshouse.api.accounts.associations.model.*;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.List;
import java.util.Optional;

@RestController
public class UserCompanyAssociations implements UserCompanyAssociationsInterface {


    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);


    private final UsersService usersService;


    private final AssociationsService associationsService;

    private final CompanyService companyService;

    @Autowired
    public UserCompanyAssociations(UsersService usersService, AssociationsService associationsService, CompanyService companyService) {
        this.usersService = usersService;
        this.associationsService = associationsService;
        this.companyService = companyService;
    }

    @Override
    public ResponseEntity<ResponseBodyPost> addAssociation(final String xRequestId, final String ericIdentity, final RequestBodyPost requestBody) {
        final var companyNumber = requestBody.getCompanyNumber();

        LOG.infoContext(xRequestId, String.format("Attempting to create association for company_number %s and user_id %s", companyNumber, ericIdentity), null);

        LOG.infoContext(xRequestId, String.format("Attempting to fetch company for company_number %s from company profile cache.", companyNumber), null);
        companyService.fetchCompanyProfile(companyNumber);
        LOG.infoContext(xRequestId, String.format("Successfully fetched company for company_number %s from company profile cache.", companyNumber), null);

        LOG.infoContext(xRequestId, String.format("Attempting to check if association between company_number %s and user_id %s exists in user_company_associations.", companyNumber, ericIdentity), null);
        if (associationsService.associationExists(companyNumber, ericIdentity)) {
            LOG.error(String.format("%s: Association between user_id %s and company_number %s already exists.", xRequestId, ericIdentity, companyNumber));
            throw new BadRequestRuntimeException("Association already exists.");
        }
        LOG.infoContext(xRequestId, String.format("Could not find association for company_number %s and user_id %s in user_company_associations.", companyNumber, ericIdentity), null);

        LOG.infoContext(xRequestId, String.format("Attempting to create association for company_number %s and user_id %s in user_company_associations.", companyNumber, ericIdentity), null);
        final var association = associationsService.createAssociation(companyNumber, ericIdentity, null, ApprovalRouteEnum.AUTH_CODE);
        LOG.infoContext(xRequestId, String.format("Successfully created association for company_number %s and user_id %s in user_company_associations.", companyNumber, ericIdentity), null);

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

        LOG.infoContext(xRequestId, "Trying to fetch associations data for user in session :".concat(ericIdentity), null);

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
        LOG.debug(String.format("%s: Attempting to get the Association details : %s", xRequestId, id));

        final var association = associationsService.findAssociationById(id);
        if (association.isEmpty()) {
            var errorMessage = String.format("Cannot find Association for the Id: %s", id);
            LOG.error(errorMessage);
            throw new NotFoundRuntimeException(StaticPropertyUtil.APPLICATION_NAMESPACE, errorMessage);
        }
        return new ResponseEntity<>(association.get(), HttpStatus.OK);
    }

    // TODO: test this method
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
            LOG.error( String.format( "%s: Was either unable to fetch %s from accounts-user-api, or %s from company-profile-api.", xRequestId, ericIdentity, companyNumber ) );
            throw new BadRequestRuntimeException( "Please check the request and try again" );
        }

        LOG.debugContext( xRequestId, String.format( "Attempting to search for %s in accounts-user-api.", inviteeEmail ), null );
        final var usersList = usersService.searchUserDetails( List.of( inviteeEmail ) );
        final var userFound = !( Objects.isNull( usersList ) || usersList.isEmpty() );

        LOG.debugContext( xRequestId, String.format( "Attempting to fetch association for company %s and user email %s.", companyNumber, inviteeEmail ), null );
        final var associationEmailOptional = associationsService.fetchAssociationForCompanyNumberAndUserEmail( companyNumber, inviteeEmail );
        AssociationDao association = null;
        if ( associationEmailOptional.isPresent() ){
            // TODO: swap operation
            LOG.debugContext( xRequestId, String.format( "Association for company %s and user email %s was found.", companyNumber, inviteeEmail), null );
            association = associationEmailOptional.get();
        }

        if ( associationEmailOptional.isEmpty() && userFound ){
            final var userDetails = usersList.getFirst();
            final var inviteeUserId = userDetails.getUserId();
            LOG.debugContext( xRequestId, String.format( "Association for company %s and user email %s was not found, but user was found. Attempting to fetch association for company %s and user id %s", companyNumber, inviteeEmail, companyNumber, inviteeUserId ), null );
            final var associationUserOptional = associationsService.fetchAssociationForCompanyNumberAndUserId( companyNumber, inviteeUserId );
            association = associationUserOptional.orElseGet(
                    () -> associationsService.createAssociation(companyNumber, inviteeUserId, null,
                            ApprovalRouteEnum.INVITATION));
        }

        if ( associationEmailOptional.isEmpty() && !userFound ){
            LOG.debugContext( xRequestId, String.format( "Association for company %s and user email %s was not found, and user was not found.", companyNumber, inviteeEmail), null );
            association = associationsService.createAssociation( companyNumber, null, inviteeEmail, ApprovalRouteEnum.INVITATION );
        }

        if ( Association.StatusEnum.AWAITING_APPROVAL.getValue().equals( association.getStatus() ) ){
            LOG.debugContext( xRequestId, "Attempting to send invitation", null );
            associationsService.inviteUser( ericIdentity, association );
        }

        final var associationId = association.getId();
        return new ResponseEntity<>( new ResponseBodyPost().associationId( associationId ), HttpStatus.CREATED );
    }

    @Override
    public ResponseEntity<Void> updateAssociationStatusForId(final String xRequestId, final String associationId, final RequestBodyPut requestBody) {
        final var status = requestBody.getStatus();

        LOG.infoContext(xRequestId, String.format("Attempting to update association status for association %s to %s.", associationId, status.getValue()), null);

        LOG.debugContext(xRequestId, String.format("Attempting to fetch association %s from user_company_associations.", associationId), null);
        final var associationOptional = associationsService.findAssociationDaoById(associationId);
        if (associationOptional.isEmpty()) {
            LOG.error(String.format("%s: Could not find association %s in user_company_associations.", xRequestId, associationId));
            throw new NotFoundRuntimeException("accounts-association-api", String.format("Association %s was not found.", associationId));
        }
        LOG.debugContext(xRequestId, String.format("Successfully fetched association %s from user_company_associations.", associationId), null);

        final var association = associationOptional.get();

        association.setStatus( status.getValue() );
        if ( status.equals(RequestBodyPut.StatusEnum.CONFIRMED) ){
            association.setApprovedAt( LocalDateTime.now() );
        }

        if ( status.equals(StatusEnum.REMOVED) ){
            association.setRemovedAt( LocalDateTime.now() );
        }


        if (Objects.isNull(association.getUserId())) {
            LOG.debugContext(xRequestId, String.format("Association %s does not have a userId. Attempting to fetch data from accounts-user-api.", associationId), null);
            final var userEmail = association.getUserEmail();
            final var usersList = usersService.searchUserDetails(List.of(userEmail));
            final var userNotFound = Objects.isNull(usersList) || usersList.isEmpty();

            if (status.equals(StatusEnum.CONFIRMED) && userNotFound) {
                LOG.error(String.format("%s: Could not find user %s, via the accounts-user-api", xRequestId, userEmail));
                throw new BadRequestRuntimeException(String.format("Could not find data for user %s", userEmail));

            }
            //if user found, swap out the temporary email id with the user id.
            if (!userNotFound) {
                LOG.debugContext(xRequestId, "Successfully fetched data from accounts-user-api for user.", null);
                association.setUserEmail( null );
                association.setUserId( usersList.getFirst().getUserId() );
            }


        }

        LOG.debugContext(xRequestId, String.format("Attempting to update the status of association %s to %s", associationId, status.getValue()), null);
        associationsService.updateAssociation(association);
        LOG.infoContext(xRequestId, "Successfully updated association status for association %s to %s.", null);

        return new ResponseEntity<>(HttpStatus.OK);
    }

}
