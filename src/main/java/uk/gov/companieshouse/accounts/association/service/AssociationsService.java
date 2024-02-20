package uk.gov.companieshouse.accounts.association.service;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.enums.StatusEnum;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Optional;

@Service
public class AssociationsService {

    private final AssociationsRepository associationsRepository;

    public AssociationsService(AssociationsRepository associationsRepository) {
        this.associationsRepository = associationsRepository;
    }

    public Optional<Association> getByUserIdAndCompanyNumber(@NotNull final String userId, @NotNull final String companyNumber) {
        return associationsRepository.findByUserIdAndCompanyNumber(userId, companyNumber);
    }

    public void softDeleteAssociation( final String userId, final String companyNumber, final boolean userInfoExists ){
        var update = new Update()
                .set( "status", StatusEnum.REMOVED.getValue() )
                .set( "deletionTime", LocalDateTime.now() );

        if ( userInfoExists )
            update = update.set( "temporary", false );

        associationsRepository.updateAssociation(userId, companyNumber, update);
    }

    public void confirmAssociation(final String userId, final String companyNumber) {
        final var update = new Update()
                .set( "temporary", false )
                .set( "status", StatusEnum.CONFIRMED.getValue() )
                .set( "confirmationApprovalTime", LocalDateTime.now() );

        associationsRepository.updateAssociation(userId, companyNumber, update);
    }

    public Page<Association> findByUserIdAndCompanyNumberLike(final String userId, final String companyNumber, final boolean includeRemoved, final int pageNumber, final int pageSize ){
        final Pageable pageable = PageRequest.of( pageNumber, pageSize );
        String statusRemove = StatusEnum.CONFIRMED.getValue();

        if (includeRemoved) {
            // If include_removed parameter is true, return associations with all statuses
            return associationsRepository.findByUserIdAndCompanyNumberLike( userId, companyNumber, pageable );
        } else {
            // If include_removed parameter is false, return associations with statuses Confirmed and Awaiting Approval
            return associationsRepository.findByUserIdAndCompanyNumberLikeAndStatusNotIn(userId, companyNumber, statusRemove, pageable);
        }

    }

}