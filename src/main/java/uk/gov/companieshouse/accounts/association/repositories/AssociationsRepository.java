package uk.gov.companieshouse.accounts.association.repositories;

import jakarta.validation.constraints.NotNull;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.models.InvitationDao;
import uk.gov.companieshouse.api.accounts.associations.model.Invitation;

@Repository
public interface AssociationsRepository extends MongoRepository<AssociationDao, String>{

    Page<AssociationDao> findByUserIdAndCompanyNumberLike(final String userId, @NotNull final String companyNumber, final Pageable pageable);
    Page<AssociationDao> findAllByUserId(final String userId, final Pageable pageable);
    @Query( "{ 'id': ?0 }" )
    int updateUser( String userId, Update update );

    @Query("{ $or:[ {'user_id': ?0 }, {'user_email': ?1 } ], 'status': { $in: ?2 }, 'company_number': { $regex: ?3 } }")
    Page<AssociationDao> findAllByUserIdOrUserEmailAndStatusIsInAndCompanyNumberLike(final String userId , final String userEmail, final List<String> status , final String companyNumber, final Pageable pageable);
    @Query( "{ 'company_number': ?0, 'status': { $in: ?1 } }" )
    Page<AssociationDao> fetchAssociatedUsers( final String companyNumber, final Set<String> statuses, final Pageable pageable );

    @Query( value = "{ 'company_number': ?0, 'user_id': ?1 }", exists = true )
    boolean associationExists( String companyNumber, String userId );

    @Query( value = "{ 'company_number': ?0, 'user_email': ?1 }")
    Optional<AssociationDao> fetchAssociationForCompanyNumberAndUserEmail(final String companyNumber,final String userEmail );

    @Query( value = "{ 'company_number': ?0, 'user_id': ?1 }")
    Optional<AssociationDao> fetchAssociationForCompanyNumberAndUserId(final String companyNumber, final String userId);

    @Query( "{ 'company_number': ?0, 'user_email': ?1, 'status': { $in: ?2 } } }" )
    Page<AssociationDao> fetchAssociationForCompanyNumberUserEmailAndStatus( final String companyNumber, final String userEmail, final Set<String> statuses, final Pageable pageable );

    // this doesnt work.
    @Aggregation(pipeline = {
            "{ $addFields: { latest_invitation: { $arrayElemAt: [ { $filter: { input: '$invitations', as: 'invitation', cond: { $lt: ['$$invitation.invited_at', '$approval_expiry_at'] } } }, -1 ] } } }",
            "{ $match: { 'latest_invitation': { $ne: null }, 'user_id': ?0, 'status': 'awaiting-approval' } }",
            "{ $project: { 'associationId': '$_id', 'isActive': { $literal: true }, 'invitedBy': '$latest_invitation.invited_by', 'invitedAt': '$latest_invitation.invited_at', 'li': '$latest_invitation' } }"
    })
    List<LinkedHashMap> findUserWithLatestInvitation(String userId);

    @Query( " { '_id': ?0 } " )
    int updateAssociation( String associationId, Update update );
}