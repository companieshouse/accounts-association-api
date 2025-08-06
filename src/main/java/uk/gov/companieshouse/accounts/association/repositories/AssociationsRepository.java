package uk.gov.companieshouse.accounts.association.repositories;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.Association;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

@Repository
public interface AssociationsRepository extends MongoRepository<Association, String>{

    @Query( "{ 'company_number': ?0, '$or': [ { 'user_id': { '$ne': null, '$eq': ?1 } }, { 'user_email': { '$ne': null, '$eq': ?2 } } ] }" )
    Optional<Association> fetchAssociation(final String companyNumber, final String userId, final String userEmail );

    @Query( value = "{ 'company_number': ?0, 'user_id': ?1, 'status': 'confirmed' }", exists = true )
    boolean confirmedAssociationExists( final String companyNumber, final String userId );

    @Query( "{ 'company_number': ?0, 'status': 'confirmed' }" )
    Stream<Association> fetchConfirmedAssociations(final String companyNumber );

    @Query( "{ 'company_number': ?0, 'status': { $in: ?1 }, '$or': [ { 'status': { '$ne': 'awaiting-approval' } }, { '$and': [ { 'status': 'awaiting-approval' }, { 'approval_expiry_at': { $gt: ?2 } } ] } ] }" )
    Page<Association> fetchUnexpiredAssociationsForCompanyAndStatuses(final String companyNumber, final Set<String> statuses, final LocalDateTime now, final Pageable pageable );

    @Query( "{ 'company_number': ?0, 'status': { $in: ?1 }, '$and': [ { '$or': [ { 'user_id': { '$ne': null, '$eq': ?2 } }, { 'user_email': { '$ne': null, '$eq': ?3 } } ] }, { '$or': [ { 'status': { '$ne': 'awaiting-approval' } }, { '$and': [ { 'status': 'awaiting-approval' }, { 'approval_expiry_at': { $gt: ?4 } } ] } ] } ] }" )
    Page<Association> fetchUnexpiredAssociationsForCompanyAndStatusesAndUser(final String companyNumber, final Set<String> statuses, final String userId, final String userEmail, final LocalDateTime now, final Pageable pageable );

    @Query( "{ $or:[ {'user_id': ?0 }, {'user_email': ?1 } ], 'status': { $in: ?2 }, 'company_number': { $regex: ?3 } }" )
    Page<Association> fetchAssociationsForUserAndStatusesAndPartialCompanyNumber(final String userId, final String userEmail, final Set<String> statuses, final String partialCompanyNumber, final Pageable pageable );

    @Query( "{ '$or': [ { 'user_id': { '$ne': null, '$eq': ?0 } }, { 'user_email': { '$ne': null, '$eq': ?1 } } ], 'status': 'awaiting-approval', 'approval_expiry_at': { $gt: ?2 } }" )
    List<Association> fetchAssociationsWithActiveInvitations(final String userId, final String userEmail, final LocalDateTime now );

    @Query( "{ '_id': ?0 }" )
    int updateAssociation( final String associationId, final Update update );

}