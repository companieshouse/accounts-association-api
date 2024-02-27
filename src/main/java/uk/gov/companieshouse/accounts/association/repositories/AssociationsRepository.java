package uk.gov.companieshouse.accounts.association.repositories;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import uk.gov.companieshouse.accounts.association.models.Association;

@Repository
public interface AssociationsRepository extends MongoRepository<Association, String>{

    Page<Association> findByUserIdAndCompanyNumberLike(final String userId, @NotNull final String companyNumber, final Pageable pageable);

}