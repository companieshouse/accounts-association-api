package uk.gov.companieshouse.accounts.association.repositories;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.models.Invitation;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssociationsRepository extends MongoRepository<Association, String>{

    Page<Association> findByUserIdAndCompanyNumberLike(final String userId, @NotNull final String companyNumber, final Pageable pageable);
    Page<Association> findAllByUserId(final String userId, final Pageable pageable);
    //Association findById(final String id);
    Optional<Association> findAssociationById(final String id);
    List<Invitation> findAllInvitationsById(final String id);
    @Query( "{ 'id': ?0 }" )
    int updateUser( String userId, Update update );
}