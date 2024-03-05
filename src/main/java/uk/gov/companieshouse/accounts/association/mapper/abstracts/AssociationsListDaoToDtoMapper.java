package uk.gov.companieshouse.accounts.association.mapper.abstracts;

import java.util.Map;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsListLinks;

@Mapper( componentModel = "spring" )
public abstract class AssociationsListDaoToDtoMapper {

    @Value("${internal.api.url}")
    private String internalApiUrl;

    @AfterMapping
    public void enrichWithMetadata( Page<Association> page, @MappingTarget AssociationsList list, @Context Map<String,String> context ){
        final var endpointUri = context.get( "endpointUri" );

        final var pageIndex = page.getNumber();
        final var itemsPerPage = page.getSize();
        final var totalPages = page.getTotalPages();
        final var totalResults = page.getTotalElements();
        final var isLastPage = page.isLast();

        final var self = String.format( "%s/associations", internalApiUrl );
        final var next = isLastPage ? "" : String.format( "%s%s?page_index=%d&items_per_page=%d", internalApiUrl, endpointUri, pageIndex + 1, itemsPerPage );
        final var links = new AssociationsListLinks().self( self ).next( next );

        list.links( links )
            .pageNumber( pageIndex )
            .itemsPerPage( itemsPerPage )
            .totalResults( (int) totalResults )
            .totalPages( totalPages );
    }

}
