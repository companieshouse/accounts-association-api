package uk.gov.companieshouse.accounts.association.service;

import static uk.gov.companieshouse.accounts.association.utils.Date.now;

import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.models.Associations;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;

@Service
public class AssociationsService {

    private final AssociationsRepository associationsRepository;

    public AssociationsService(AssociationsRepository associationsRepository) {
        this.associationsRepository = associationsRepository;
    }

    public Associations fetchConfirmationExpirationTime( final String userId, final String companyNumber ){
        return associationsRepository.fetchConfirmationExpirationTime( userId, companyNumber );
    }

    public void softDeleteAssociation( final String userId, final String companyNumber ){
        final var update = new Update()
                .set( "status", "Deleted" )
                .set( "deletionTime", now() );

        associationsRepository.updateAssociation( userId, companyNumber, update );
    }

    public void confirmAssociation( final String userId, final String companyNumber ){
        final var update = new Update()
                .set( "status", "Confirmed" )
                .set( "confirmationApprovalTime", now() );

        associationsRepository.updateAssociation( userId, companyNumber, update );
    }

}
