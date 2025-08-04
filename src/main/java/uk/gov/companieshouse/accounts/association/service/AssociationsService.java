package uk.gov.companieshouse.accounts.association.service;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListCompanyMapper;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListUserMapper;
import uk.gov.companieshouse.accounts.association.mapper.InvitationsCollectionMappers;
import uk.gov.companieshouse.accounts.association.mapper.PreviousStatesCollectionMappers;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.InvitationsList;
import uk.gov.companieshouse.api.accounts.associations.model.PreviousStatesList;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;
import static uk.gov.companieshouse.accounts.association.utils.AssociationsUtil.fetchAllStatusesWithout;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.DAYS_SINCE_INVITE_TILL_EXPIRES;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum.AUTH_CODE;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum.INVITATION;
import static uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum.CONFIRMED;
import static uk.gov.companieshouse.api.accounts.associations.model.PreviousState.StatusEnum.AWAITING_APPROVAL;

@Service
public class AssociationsService {

    private final AssociationsRepository associationsRepository;

    private final AssociationsListUserMapper associationsListUserMapper;

    private final AssociationsListCompanyMapper associationsListCompanyMapper;

    private final PreviousStatesCollectionMappers previousStatesCollectionMapper;

    private final InvitationsCollectionMappers invitationsCollectionMappers;

    @Autowired
    public AssociationsService( final AssociationsRepository associationsRepository, final AssociationsListUserMapper associationsListUserMapper, final AssociationsListCompanyMapper associationsListCompanyMapper, final PreviousStatesCollectionMappers previousStatesCollectionMapper, final InvitationsCollectionMappers invitationsCollectionMappers ) {
        this.associationsRepository = associationsRepository;
        this.associationsListUserMapper = associationsListUserMapper;
        this.associationsListCompanyMapper = associationsListCompanyMapper;
        this.previousStatesCollectionMapper = previousStatesCollectionMapper;
        this.invitationsCollectionMappers = invitationsCollectionMappers;
    }

    @Transactional( readOnly = true )
    public Optional<AssociationDao> fetchAssociationDao( final String associationId ) {
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch association with id: %s", associationId ), null );
        final var association = associationsRepository.findById( associationId );
        LOGGER.debugContext( getXRequestId(), String.format( "Successfully to fetched association with id: %s", associationId ), null );
        return association;
    }

    @Transactional( readOnly = true )
    public Optional<Association> fetchAssociationDto( final String associationId ) {
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to retrieve association with id: %s", associationId ), null );
        final var association = associationsRepository.findById( associationId ).map( associationDao -> associationsListCompanyMapper.daoToDto( associationDao, null, null ) );
        LOGGER.debugContext( getXRequestId(), String.format( "Successfully retrieved association with id: %s", associationId ), null );
        return association;
    }

    @Transactional( readOnly = true )
    public Optional<AssociationDao> fetchAssociationDao( final String companyNumber, final String userId, final String userEmail ) {
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch association for user_id=%s and company_number=%s. user_email was provided: %b.", userId, companyNumber, Objects.nonNull( userEmail ) ), null );
        final var association = associationsRepository.fetchAssociation( companyNumber, userId, userEmail );
        LOGGER.debugContext( getXRequestId(), String.format( "Successfully fetched association for user_id=%s and company_number=%s. user_email was provided: %b.", userId, companyNumber, Objects.nonNull( userEmail ) ), null );
        return association;
    }

    @Transactional( readOnly = true )
    public boolean confirmedAssociationExists( final String companyNumber, final String userId ) {
        return associationsRepository.confirmedAssociationExists( companyNumber, userId );
    }

    @Transactional( readOnly = true )
    public Flux<String> fetchConfirmedUserIds( final String companyNumber ) {
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch user_id's for confirmed associations at company %s", companyNumber ), null );
        return Flux.fromStream( associationsRepository.fetchConfirmedAssociations( companyNumber ).map( AssociationDao::getUserId ) );
    }

    @Transactional( readOnly = true )
    public AssociationsList fetchUnexpiredAssociationsForCompanyAndStatuses( final CompanyDetails companyDetails, final Set<StatusEnum> statuses, final String userId, final String userEmail,  final int pageIndex, final int itemsPerPage ) {
        LOGGER.debugContext( getXRequestId(), "Attempting to fetch unexpired associations for company and statuses", null );
        final var parsedStatuses = statuses.stream().map( StatusEnum::getValue ).collect( Collectors.toSet() );
        final var associationDaos = Objects.nonNull( userEmail ) || Objects.nonNull( userId )
                ? associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatusesAndUser( companyDetails.getCompanyNumber(), parsedStatuses, userId, userEmail, LocalDateTime.now(), PageRequest.of( pageIndex, itemsPerPage ) )
                : associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatuses( companyDetails.getCompanyNumber(), parsedStatuses, LocalDateTime.now(), PageRequest.of( pageIndex, itemsPerPage ) );
        final var associations = associationsListCompanyMapper.daoToDto( associationDaos, companyDetails );
        LOGGER.debugContext( getXRequestId(), "Successfully fetched unexpired associations for company and statuses" ,null );
        return associations;
    }

    @Transactional( readOnly = true )
    public Optional<Association> fetchUnexpiredAssociationsForCompanyUserAndStatuses(final CompanyDetails companyDetails, final Set<StatusEnum> statuses, final User user, final String userEmail ) {
        LOGGER.debugContext( getXRequestId(), "Attempting to fetch unexpired associations for company, user and statuses", null );
        final var userId = Optional.ofNullable( user ).map( User::getUserId ).orElse( null );
        final var parsedStatuses = statuses.stream().map( StatusEnum::getValue ).collect( Collectors.toSet() );
        final var association = Optional.of( associationsRepository.fetchUnexpiredAssociationsForCompanyAndStatusesAndUser( companyDetails.getCompanyNumber(), parsedStatuses, userId, userEmail, LocalDateTime.now(),  null ) )
                .filter( Slice::hasContent )
                .map( Slice::getContent )
                .map( List::getFirst )
                .map( associationDao -> associationsListCompanyMapper.daoToDto( associationDao, user, companyDetails ) );
        LOGGER.debugContext( getXRequestId(), "Successfully fetched unexpired associations for company, user and statuses" ,null );
        return association;
    }

    @Transactional( readOnly = true )
    public Page<AssociationDao> fetchAssociationsForUserAndPartialCompanyNumber( final User user, final String partialCompanyNumber, final int pageIndex, final int itemsPerPage ){
        final var loggingString = Objects.nonNull( partialCompanyNumber ) ? String.format( " and company %s", partialCompanyNumber ) : "";
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch associations for user %s", user.getUserId() ) + loggingString, null );
        final var coalescedPartialCompanyNumber = Optional.ofNullable( partialCompanyNumber ).orElse( "" );
        final var allStatuses = fetchAllStatusesWithout( Set.of() ).stream().map( StatusEnum::getValue ).collect( Collectors.toSet() );
        final var associations = associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber( user.getUserId(), user.getEmail(), allStatuses, coalescedPartialCompanyNumber, PageRequest.of( pageIndex, itemsPerPage ) );
        LOGGER.debugContext( getXRequestId(), String.format( "Successfully fetched associations for user %s", user.getUserId() ) + loggingString, null );
        return associations;
    }

    @Transactional( readOnly = true )
    public AssociationsList fetchAssociationsForUserAndPartialCompanyNumberAndStatuses( final User user, final String partialCompanyNumber, final Set<String> statuses, final int pageIndex, final int itemsPerPage ) {
        LOGGER.debugContext( getXRequestId(), "Attempting to fetch associations for user, partial company number, and statuses", null );
        final var coalescedPartialCompanyNumber = Optional.ofNullable( partialCompanyNumber ).orElse( "" );
        final var coalescedStatuses = Optional.ofNullable( statuses )
                .filter( parsedStatuses -> !parsedStatuses.isEmpty() )
                .orElse( Set.of( CONFIRMED.getValue() ) );
        final var results = associationsRepository.fetchAssociationsForUserAndStatusesAndPartialCompanyNumber( user.getUserId(), user.getEmail(), coalescedStatuses, coalescedPartialCompanyNumber, PageRequest.of( pageIndex, itemsPerPage ) );
        final var associations =  associationsListUserMapper.daoToDto( results, user );
        LOGGER.infoContext( getXRequestId(), "Successfully fetched associations for user, partial company number, and statuses", null );
        return associations;
    }

    @Transactional( readOnly = true )
    public Optional<InvitationsList> fetchInvitations( final String associationId, final int pageIndex, final int itemsPerPage ) {
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch invitations for association %s", associationId ), null );
        final var invitations = associationsRepository.findById( associationId ).map( association -> invitationsCollectionMappers.daoToDto( association, pageIndex, itemsPerPage ) );
        LOGGER.debugContext( getXRequestId(), String.format( "Successfully fetched invitations for association %s", associationId ), null );
        return invitations;
    }

    @Transactional( readOnly = true )
    public InvitationsList fetchActiveInvitations( final User user, final int pageIndex, final int itemsPerPage ) {
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to retrieve active invitations for user %s", user.getUserId() ), null );
        final var associationsWithActiveInvitations = associationsRepository.fetchAssociationsWithActiveInvitations( user.getUserId(), user.getEmail(), LocalDateTime.now() );
        final var invitations = invitationsCollectionMappers.daoToDto( associationsWithActiveInvitations, pageIndex, itemsPerPage );
        LOGGER.debugContext( getXRequestId(), String.format( "Successfully retrieved active invitations for user %s", user.getUserId() ), null );
        return invitations;
    }

    @Transactional( readOnly = true )
    public Optional<PreviousStatesList> fetchPreviousStates( final String associationId, final int pageIndex, final int itemsPerPage ){
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to fetch previous states for association %s", associationId ), null );
        return associationsRepository.findById( associationId ).map( association -> previousStatesCollectionMapper.daoToDto( association, pageIndex, itemsPerPage ) );
    }

    @Transactional
    public AssociationDao createAssociationWithAuthCodeApprovalRoute( final String companyNumber, final String userId ){
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to create association for company_number %s and user_id %s.", companyNumber, userId ), null );
        if ( Objects.isNull( companyNumber ) || Objects.isNull( userId ) ) {
            LOGGER.errorContext( getXRequestId(), new Exception( "companyNumber or userId is null" ), null );
            throw new NullPointerException( "companyNumber and userId must not be null" );
        }

        final var proposedAssociation = new AssociationDao()
                .companyNumber( companyNumber )
                .userId( userId )
                .status( CONFIRMED.getValue() )
                .approvalRoute( AUTH_CODE.getValue() )
                .etag( generateEtag() );

        final var createdAssociation = associationsRepository.insert( proposedAssociation );
        LOGGER.debugContext( getXRequestId(), String.format( "Successfully created association for company_number %s and user_id %s.", companyNumber, userId ), null );
        return createdAssociation;
    }

    @Transactional
    public AssociationDao createAssociationWithInvitationApprovalRoute( final String companyNumber, final String userId, final String userEmail, final String invitedByUserId ){
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to create new invitation for user_id=%s and company_number=%s. user_email was provided: %b.", userId, companyNumber, Objects.nonNull( userEmail ) ), null );
        if ( Objects.isNull( companyNumber ) || ( Objects.isNull( userId ) && Objects.isNull( userEmail ) ) || Objects.isNull( invitedByUserId ) ) {
            LOGGER.errorContext( getXRequestId(), new Exception( "companyNumber, user, or invitedByUserId is null" ), null );
            throw new NullPointerException( "companyNumber, user, and invitedByUserId must not be null" );
        }

        final var proposedAssociation = new AssociationDao()
                .companyNumber( companyNumber )
                .userId( userId )
                .userEmail( userEmail )
                .status( AWAITING_APPROVAL.getValue() )
                .approvalRoute( INVITATION.getValue() )
                .approvalExpiryAt( LocalDateTime.now().plusDays( DAYS_SINCE_INVITE_TILL_EXPIRES ) )
                .invitations( List.of( new InvitationDao().invitedBy( invitedByUserId ).invitedAt( LocalDateTime.now() ) ) )
                .etag( generateEtag() );

        LOGGER.debugContext( getXRequestId(), "Insert Association", null );
        final var createdAssociation = associationsRepository.insert( proposedAssociation );
        LOGGER.debugContext( getXRequestId(), String.format( "Successfully created new invitation for user_id=%s and company_number=%s. user_email was provided: %b.", userId, companyNumber,  Objects.nonNull( userEmail ) ), null );
        return createdAssociation;
    }

    @Transactional
    public void updateAssociation( final String associationId, final Update update ) {
        LOGGER.debugContext( getXRequestId(), String.format( "Attempting to update association with id: %s", associationId ), null );
        Optional.ofNullable( associationId )
                .map( id -> associationsRepository.updateAssociation( id, update ) )
                .filter( numRecordsUpdated -> numRecordsUpdated > 0 )
                .orElseThrow( () -> new InternalServerErrorRuntimeException( "Failed to update association", new Exception( String.format( "Failed to update association with id: %s", associationId ) ) ) );
        LOGGER.debugContext( getXRequestId(), String.format( "Updated association %s", associationId ), null );
    }

}