package uk.gov.companieshouse.accounts.association.utils;

import org.springframework.data.domain.Page;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.Links;

public final class MapperUtil {

    private MapperUtil(){}

    public static AssociationsList enrichWithMetadata( final Page<Association> page, final String endpointUrl ) {
        final var pageIndex = page.getNumber();
        final var itemsPerPage = page.getSize();
        final var self = String.format( "/associations%s?page_index=%d&items_per_page=%d", endpointUrl, pageIndex, itemsPerPage );
        final var next = page.isLast() ? "" : String.format( "/associations%s?page_index=%d&items_per_page=%d", endpointUrl, pageIndex + 1, itemsPerPage );
        final var links = new Links().self( self ).next( next );

        return new AssociationsList()
                .items( page.getContent() )
                .pageNumber( pageIndex )
                .itemsPerPage( itemsPerPage )
                .totalResults( (int) page.getTotalElements() )
                .totalPages( page.getTotalPages() )
                .links( links );
    }

}
