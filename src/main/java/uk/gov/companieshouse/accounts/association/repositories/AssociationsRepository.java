package uk.gov.companieshouse.accounts.association.repositories;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.Association;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public interface AssociationsRepository extends MongoRepository<Association, String>{

    Iterable<Association> findAllByCompanyNumber(final String companyNumber );

    Page<Association> findByUserIdAndCompanyNumberLike(final String userId, @NotNull final String companyNumber, final Pageable pageable);

    Page<Association> findByUserIdAndCompanyNumberLikeAndStatusNotIn(final String userId, @NotNull final String companyNumber, final String includeRemoval, final Pageable pageable);

    Iterable<Association> findAllByUserId(final String userId );

    @Query( value = "{ 'userId': ?0, 'companyNumber': ?1 }")
    Optional<Association> findByUserIdAndCompanyNumber(final String userId, final String companyNumber );

    @Query( "{ 'userId': ?0, 'companyNumber': ?1 }" )
    void updateAssociation( final String userId, final String companyNumber, Update update );

}