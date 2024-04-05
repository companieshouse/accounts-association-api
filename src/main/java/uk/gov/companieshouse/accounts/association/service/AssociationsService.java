package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.mapper.AssociationMapper;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListCompanyMapper;
import uk.gov.companieshouse.accounts.association.mapper.AssociationsListUserMapper;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.RequestBodyPut;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.*;

@Service
public class AssociationsService {


    private final AssociationsRepository associationsRepository;


    private final AssociationsListUserMapper associationsListUserMapper;

    private final AssociationsListCompanyMapper associationsListCompanyMapper;

    private final AssociationMapper associationMapper;

    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    @Autowired
    public AssociationsService(AssociationsRepository associationsRepository,
                               AssociationsListUserMapper associationsListUserMapper,
                               AssociationsListCompanyMapper associationsListCompanyMapper,
                               AssociationMapper associationMapper) {
        this.associationsRepository = associationsRepository;
        this.associationsListUserMapper = associationsListUserMapper;
        this.associationsListCompanyMapper = associationsListCompanyMapper;
        this.associationMapper = associationMapper;
    }

    @Transactional(readOnly = true)
    public AssociationsList fetchAssociationsForUserStatusAndCompany(
            @NotNull final User user, List<String> status, final Integer pageIndex, final Integer itemsPerPage,
            final String companyNumber) {

        if (Objects.isNull(status) || status.isEmpty()) {
            status = Collections.singletonList(Association.StatusEnum.CONFIRMED.getValue());

        }

        Page<AssociationDao> results = associationsRepository.findAllByUserIdAndStatusIsInAndCompanyNumberLike(
                user.getUserId(), status, Optional.ofNullable(companyNumber).orElse(""), PageRequest.of(pageIndex, itemsPerPage));


        return associationsListUserMapper.daoToDto(results, user);
    }

    @Transactional(readOnly = true)
    public AssociationsList fetchAssociatedUsers(final String companyNumber, final CompanyDetails companyDetails, final boolean includeRemoved, final int itemsPerPage, final int pageIndex, final String userEmail) {
        final Pageable pageable = PageRequest.of(pageIndex, itemsPerPage);

        final var statuses = new HashSet<>(Set.of(StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue()));
        if (includeRemoved) {
            statuses.add(StatusEnum.REMOVED.getValue());
        }

        final Page<AssociationDao> associations;
        if ( Objects.isNull( userEmail ) ){
            associations = associationsRepository.fetchAssociatedUsers(companyNumber, statuses, pageable);
        } else {
            associations = associationsRepository.fetchAssociationForCompanyNumberUserEmailAndStatus( companyNumber, userEmail, statuses, pageable );
        }

        return associationsListCompanyMapper.daoToDto(associations, companyDetails);
    }

    @Transactional(readOnly = true)
    public Optional<Association> findAssociationById(final String id) {

        return associationsRepository.findById(id).map(associationMapper::daoToDto);
    }

    @Transactional(readOnly = true)
    public boolean associationExists( final String companyNumber, final String userId ){
        return associationsRepository.associationExists( companyNumber, userId );
    }

    @Transactional
    public AssociationDao createAssociation( final String companyNumber, final String userId, final ApprovalRouteEnum approvalRoute ){
        if ( Objects.isNull( companyNumber ) || Objects.isNull( userId ) ) {
            LOG.error( "Attempted to create association with null company_number or null user_id" );
            throw new NullPointerException("companyNumber and userId must not be null");
        }

        final var association = new AssociationDao();
        association.setCompanyNumber( companyNumber );
        association.setUserId( userId );
        association.setApprovalRoute( approvalRoute.getValue() );
        association.setEtag( generateEtag() );
        association.setStatus( StatusEnum.CONFIRMED.getValue() );
        return associationsRepository.insert( association );
    }

    @Transactional
    public void updateAssociationStatus( final String associationId, final String userId, final RequestBodyPut.StatusEnum status, boolean swapUserEmailForUserId ){
        if ( Objects.isNull( associationId ) ) {
            LOG.error( "Attempted to update association with null association id" );
            throw new NullPointerException( "associationId must not be null" );
        }

        final var timestampKey = status.equals( RequestBodyPut.StatusEnum.CONFIRMED ) ? "approved_at" : "removed_at";

        var update = new Update()
                .set( "status", status.getValue() )
                .set( timestampKey, LocalDateTime.now().toString() )
                .set( "etag", generateEtag() );

        if ( swapUserEmailForUserId ) {
            if ( Objects.isNull( userId ) ){
                LOG.error( "Attempted to update association with null user_id" );
                throw new NullPointerException( "userId must not be null" );
            }
            update = update.set( "user_email", null ).set( "user_id", userId );
        }

        final var numRecordsUpdated = associationsRepository.updateAssociation( associationId, update );

        if ( numRecordsUpdated == 0 ){
            LOG.error( String.format( "Failed to update status of association %s to %s", associationId, status.getValue() ) );
            throw new InternalServerErrorRuntimeException( "Failed to update association status" );
        }

    }

}