package uk.gov.companieshouse.accounts.association.mapper;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.associations.model.Links;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;

@Component
@Mapper( componentModel = "spring" )
public abstract class AssociationsListMappers extends AssociationMapper {

    private String computeEndpointUrl( final CompanyDetails companyDetails ){
        return Optional.ofNullable( companyDetails )
                .map( company -> String.format( "/companies/%s", company.getCompanyNumber() ) )
                .orElse( "" );
    }

    private AssociationsList enrichWithMetadata( final Page<Association> page, final String endpointUrl ) {
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

    public AssociationsList daoToDto( final Page<AssociationDao> associationsList, final User userDetails, final CompanyDetails companyDetails ) {
        final var endpointUrl = computeEndpointUrl( companyDetails );
        final var users = Objects.isNull( userDetails ) ? usersService.fetchUserDetails( associationsList.stream() ) : Map.of( userDetails.getUserId(), userDetails );
        final var companies = Objects.isNull( companyDetails ) ? companyService.fetchCompanyProfiles( associationsList.stream() ) : Map.of( companyDetails.getCompanyNumber(), companyDetails );
        final var associations = associationsList.map( associationDao -> {
            final var user = Objects.isNull( associationDao.getUserId() ) ? null : users.getOrDefault( associationDao.getUserId(), null );
            final var company = companies.get( associationDao.getCompanyNumber() );
            return daoToDto( associationDao, user, company );
        } );
        return enrichWithMetadata( associations, endpointUrl );
    }

}
