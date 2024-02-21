package uk.gov.companieshouse.accounts.association.repositories;

import jakarta.validation.constraints.NotNull;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.api.accounts.associations.model.Association.StatusEnum;

@Repository
public interface AssociationsRepository extends MongoRepository<Association, String>{

    Page<Association> findByUserIdAndCompanyNumberLike(final String userId, @NotNull final String companyNumber, final Pageable pageable);

    @Query( "{ 'company_number': ?0, 'status': { $in: ?1 } }" )
    Page<Association> fetchAssociatedUsers( final String companyNumber, final Set<StatusEnum> statuses, final Pageable pageable );

}