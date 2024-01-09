package uk.gov.companieshouse.accounts.association.service;

import org.springframework.stereotype.Service;
import uk.gov.companieshouse.accounts.association.repositories.AssociationsRepository;
import org.springframework.data.mongodb.core.query.Update;
import java.time.LocalDateTime;

@Service
public class AssociationsService {

    private final AssociationsRepository associationsRepository;

    public AssociationsService(AssociationsRepository associationsRepository){
        this.associationsRepository = associationsRepository;
    }

    public boolean associationExists( String userId, String companyNumber ){
        return associationsRepository.associationExists( userId, companyNumber );
    }

    public void softDeleteAssociation( final String userId, final String companyNumber ){
        final var update = new Update()
                .set( "status", "Deleted" )
                .set( "deletionTime", LocalDateTime.now() );

        associationsRepository.updateAssociation( userId, companyNumber, update );
    }

    public void confirmAssociation( final String userId, final String companyNumber ){
        final var update = new Update()
                .set( "status", "Confirmed" )
                .set( "confirmationApprovalTime", LocalDateTime.now() );

        associationsRepository.updateAssociation( userId, companyNumber, update );
    }

}
