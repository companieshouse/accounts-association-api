package uk.gov.companieshouse.accounts.association.mapper.abstracts;

import java.util.Objects;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationLinks;

@Mapper( componentModel = "spring" )
public abstract class AssociationDaoToDtoMapper {

    private static final String DEFAULT_KIND = "association";

    @AfterMapping
    public void enrichAssociationWithDerivedValues( @MappingTarget Association association ){
        final var associationId = association.getId();
        final var self = String.format( "/%s", associationId );
        final var links = new AssociationLinks().self( self );

        association.setLinks( links );

        if ( Objects.isNull( association.getKind() ) ) {
            association.setKind( DEFAULT_KIND );
        }
    }

}
