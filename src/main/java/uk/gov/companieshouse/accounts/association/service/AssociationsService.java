package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.GenerateEtagUtil.generateEtag;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
import uk.gov.companieshouse.accounts.association.mapper.InvitationsMapper;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import uk.gov.companieshouse.accounts.association.repositories.InvitationsRepository;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.ApprovalRouteEnum;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class AssociationsService {


    private final AssociationsRepository associationsRepository;
    private final InvitationsRepository invitationsRepository;
    private final AssociationsListUserMapper associationsListUserMapper;

    private final AssociationsListCompanyMapper associationsListCompanyMapper;

    private final AssociationMapper associationMapper;

    private final InvitationsMapper invitationMapper;

    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    @Autowired
    public AssociationsService(AssociationsRepository associationsRepository,
                               InvitationsRepository invitationsRepository,
                               AssociationsListUserMapper associationsListUserMapper,
                               AssociationsListCompanyMapper associationsListCompanyMapper,
                               AssociationMapper associationMapper,
                               InvitationsMapper invitationMapper) {
        this.associationsRepository = associationsRepository;
        this.associationsListUserMapper = associationsListUserMapper;
        this.associationsListCompanyMapper = associationsListCompanyMapper;
        this.associationMapper = associationMapper;
        this.invitationsRepository = invitationsRepository;
        this.invitationMapper = invitationMapper;
    }

    @Transactional(readOnly = true)
    public AssociationsList fetchAssociationsForUserStatusAndCompany(
            @NotNull final User user, List<String> status, final Integer pageIndex, final Integer itemsPerPage,
            final String companyNumber) {

        if (Objects.isNull(status) || status.isEmpty()) {
            status = Collections.singletonList(Association.StatusEnum.CONFIRMED.getValue());

        }

        Page<AssociationDao> results = associationsRepository.findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike(
                user.getUserId(),user.getEmail(), status, Optional.ofNullable(companyNumber).orElse(""), PageRequest.of(pageIndex, itemsPerPage))
                ;


        return associationsListUserMapper.daoToDto(results, user);
    }

    @Transactional(readOnly = true)
    public AssociationsList fetchAssociatedUsers(final String companyNumber,
                                                 final CompanyDetails companyDetails,
                                                 final boolean includeRemoved,
                                                 final int itemsPerPage,
                                                 final int pageIndex) {
        final Pageable pageable = PageRequest.of(pageIndex, itemsPerPage);

        final var statuses = new HashSet<>(Set.of(StatusEnum.CONFIRMED.getValue(), StatusEnum.AWAITING_APPROVAL.getValue()));
        if (includeRemoved) {
            statuses.add(StatusEnum.REMOVED.getValue());
        }

        final Page<AssociationDao> associations = associationsRepository.fetchAssociatedUsers(companyNumber, statuses, pageable);


        return associationsListCompanyMapper.daoToDto(associations, companyDetails);
    }

    @Transactional(readOnly = true)
    public Optional<Association> findAssociationById(final String id) {

        return associationsRepository.findById(id).map(associationMapper::daoToDto);
    }

    @Transactional(readOnly = true)
    public Optional<AssociationDao> findAssociationDaoById(final String id) {

        return associationsRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public boolean associationExists(final String companyNumber, final String userId) {
        return associationsRepository.associationExists(companyNumber, userId);
    }

    @Transactional(readOnly = true)
    public Optional<AssociationDao> fetchAssociationForCompanyNumberAndUserEmail(final String companyNumber, final String userEmail) {
        return associationsRepository.fetchAssociationForCompanyNumberAndUserEmail(companyNumber, userEmail);
    }

    @Transactional(readOnly = true)
    public Optional<AssociationDao> fetchAssociationForCompanyNumberAndUserId(final String companyNumber, final String userId) {
        return associationsRepository.fetchAssociationForCompanyNumberAndUserId(companyNumber, userId);
    }

    @Transactional
    public AssociationDao createAssociation(final String companyNumber,
                                            final String userId,
                                            final String userEmail,
                                            final ApprovalRouteEnum approvalRoute,
                                            final String invitedByUserId) {
        if (Objects.isNull(companyNumber) || companyNumber.isEmpty()) {
            throw new NullPointerException("companyNumber must not be null");
        }

        if (Objects.isNull(userId) && Objects.isNull(userEmail)) {
            throw new NullPointerException("UserId or UserEmail should be provided");
        }

        final var association = new AssociationDao();
        association.setCompanyNumber(companyNumber);
        association.setUserId(userId);
        association.setUserEmail(userEmail);
        association.setApprovalRoute(approvalRoute.getValue());
        association.setEtag(generateEtag());


        if (ApprovalRouteEnum.INVITATION.equals(approvalRoute)) {
            addInvitation(invitedByUserId, association);

        } else {
            association.setStatus(StatusEnum.CONFIRMED.getValue());
        }
        return associationsRepository.save(association);
    }

    private static void addInvitation(String invitedByUserId, AssociationDao association) {
        if ( Objects.isNull( invitedByUserId ) ){
            throw new NullPointerException( "invitedByUserId cannot be null." );
        }

        InvitationDao invitationDao = new InvitationDao();
        invitationDao.setInvitedAt(LocalDateTime.now());
        invitationDao.setInvitedBy(invitedByUserId);
        association.setStatus(StatusEnum.AWAITING_APPROVAL.getValue());
        association.setApprovalExpiryAt(LocalDateTime.now().plusDays(7));
        association.getInvitations().add(invitationDao);
    }

    @Transactional
    public AssociationDao sendNewInvitation(final String invitedByUserId, final AssociationDao association) {
        association.setEtag( generateEtag() );
        addInvitation(invitedByUserId, association);
        return associationsRepository.save(association);
    }

    @Transactional
    public void updateAssociation(final String associationId, final Update update) {
        if (Objects.isNull(associationId)) {
            LOG.error("Attempted to update association with null association id");
            throw new NullPointerException("associationId must not be null");
        }
        update.set("etag", generateEtag());
        final var numRecordsUpdated = associationsRepository.updateAssociation(associationId, update);

        if (numRecordsUpdated == 0) {
            LOG.error(String.format("Failed to update association with id: %s", associationId));
            throw new InternalServerErrorRuntimeException("Failed to update association");
        }

    }




    // TODO: test this
    @Transactional( readOnly = true )
    public List<Invitation> fetchActiveInvitations( final String userId ){
        return invitationsRepository.fetchActiveInvitations( userId )
                                    .stream()
                                    .flatMap( invitationMapper::daoToDto )
                                    .toList();
    }

}