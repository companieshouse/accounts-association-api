package uk.gov.companieshouse.accounts.association.repositories;

import java.util.Set;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;

import java.util.List;

@Repository
public interface AssociationsRepository extends MongoRepository<AssociationDao, String>{

    Page<AssociationDao> findByUserIdAndCompanyNumberLike(final String userId, @NotNull final String companyNumber, final Pageable pageable);
    Page<AssociationDao> findAllByUserId(final String userId, final Pageable pageable);
    @Query( "{ 'id': ?0 }" )
    int updateUser( String userId, Update update );

    Page<AssociationDao> findAllByUserIdAndStatusIsInAndCompanyNumberLike(final String userId , final List<String> status , final String companyNumber, final Pageable pageable);

    @Query( "{ 'company_number': ?0, 'status': { $in: ?1 } }" )
    Page<AssociationDao> fetchAssociatedUsers( final String companyNumber, final Set<String> statuses, final Pageable pageable );
}