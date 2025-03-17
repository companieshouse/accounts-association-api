package uk.gov.companieshouse.accounts.association.controller;

import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.accounts.association.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.PreviousStatesDao;
import uk.gov.companieshouse.accounts.association.service.AssociationsService;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.EmailService;
import uk.gov.companieshouse.api.accounts.associations.api.UserCompanyAssociationsInterface;
import uk.gov.companieshouse.api.accounts.associations.model.*;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;
import static uk.gov.companieshouse.accounts.association.models.Constants.ITEMS_PER_PAGE_WAS_LESS_THAN_OR_EQUAL_TO_0;
import static uk.gov.companieshouse.accounts.association.models.Constants.PAGE_INDEX_WAS_LESS_THAN_0;
import static uk.gov.companieshouse.accounts.association.models.Constants.PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.AWAITING_APPROVAL;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.MIGRATED;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut.StatusEnum.REMOVED;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;

@RestController
public class UserCompanyAssociations implements UserCompanyAssociationsInterface {

    private final CompanyService companyService;
    private final AssociationsService associationsService;
    private final EmailService emailService;

    @Autowired
    public UserCompanyAssociations( final CompanyService companyService, final AssociationsService associationsService, final EmailService emailService ) {
        this.companyService = companyService;
        this.associationsService = associationsService;
        this.emailService = emailService;
    }

    @Override
    public ResponseEntity<ResponseBodyPost> addAssociation(final RequestBodyPost requestBody) {
        final var companyNumber = requestBody.getCompanyNumber();

        LOGGER.infoContext( getXRequestId(), String.format( "Received request with user_id=%s, company_number=%s.", getEricIdentity(), companyNumber ),null );

        final var userDetails = Objects.requireNonNull(getUser());
        final var displayName = Optional.ofNullable(userDetails.getDisplayName()).orElse(userDetails.getEmail());

        final var companyDetails = companyService.fetchCompanyProfile(companyNumber);

        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch association between user %s and company %s", getEricIdentity(), companyNumber ), null );
        var existingAssociation = associationsService.fetchAssociationsDaoForUserStatusAndCompany(userDetails, List.of(AWAITING_APPROVAL.getValue(), CONFIRMED.getValue(), REMOVED.getValue(), MIGRATED.getValue()),0,15,companyNumber);

        AssociationDao association;

        if(!existingAssociation.isEmpty()){
            association = existingAssociation.get().iterator().next();
            if(association.getStatus().equals(CONFIRMED.getValue())){
                LOGGER.errorContext( getXRequestId(), new Exception( String.format( "Association between user_id %s and company_number %s already exists.", getEricIdentity(), companyNumber ) ),null );
                throw new BadRequestRuntimeException("Association already exists.");
            }
            association.getPreviousStates().add( new PreviousStatesDao().status( association.getStatus() ).changedAt( LocalDateTime.now() ).changedBy( getEricIdentity() ) );
            association.setStatus(Association.StatusEnum.CONFIRMED.getValue());
            association.setUserId(userDetails.getUserId());
            association.setApprovalRoute(ApprovalRouteEnum.AUTH_CODE.getValue());
            association.setUserEmail(null);
            association.setEtag( generateEtag() );
            LOGGER.debugContext( getXRequestId(), String.format( "Attempting to update association %s", association.getId() ), null );
            association = associationsService.upsertAssociation(association);
            LOGGER.infoContext( getXRequestId(), String.format("Successfully updated association for company_number %s and user_id %s.", companyNumber, getEricIdentity() ), null );
        } else{
            LOGGER.debugContext( getXRequestId(), String.format( "Attempting to create association for company_number %s and user_id %s.", companyNumber, getEricIdentity() ), null );
            association = associationsService.createAssociation(companyNumber, getEricIdentity(), null, ApprovalRouteEnum.AUTH_CODE, null);
            LOGGER.infoContext( getXRequestId(), String.format("Successfully created association for company_number %s and user_id %s.", companyNumber, getEricIdentity() ), null );
        }

        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to create requests for users associated with company %s", companyNumber ), null );

        Mono.just( companyNumber )
                .map( associationsService::fetchAssociatedUsers )
                .flatMapMany( Flux::fromIterable )
                .flatMap( emailService.sendAuthCodeConfirmationEmailToAssociatedUser( getXRequestId(), companyDetails, displayName ) )
                .subscribe();

        return new ResponseEntity<>(new ResponseBodyPost().associationLink( String.format( "/associations/%s", association.getId())), HttpStatus.CREATED);
    }

    @Override
    public ResponseEntity<AssociationsList> fetchAssociationsBy( final List<String> status, final Integer pageIndex, final Integer itemsPerPage, final String companyNumber ) {
        LOGGER.infoContext( getXRequestId(), String.format( "Received request with user_id=%s, status=%s, page_index=%d, items_per_page=%d, company_number=%s.", getEricIdentity(), String.join( ",", status ), pageIndex, itemsPerPage, companyNumber ),null );

        if ( pageIndex < 0 ) {
            LOGGER.errorContext( getXRequestId(), new Exception( PAGE_INDEX_WAS_LESS_THAN_0 ), null );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        if ( itemsPerPage <= 0 ) {
            LOGGER.errorContext( getXRequestId(), new Exception( ITEMS_PER_PAGE_WAS_LESS_THAN_OR_EQUAL_TO_0 ), null );
            throw new BadRequestRuntimeException( PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN );
        }

        final var user = getUser();

        LOGGER.debugContext( getXRequestId(), "Attempting to fetch associations", null );
        final var associationsList = associationsService.fetchAssociationsForUserStatusAndCompany( user, status, pageIndex, itemsPerPage, companyNumber );
        LOGGER.infoContext( getXRequestId(), String.format( "Successfully fetched %d associations", associationsList.getItems().size() ), null );

        return new ResponseEntity<>( associationsList, HttpStatus.OK );
    }

}

