package uk.gov.companieshouse.accounts.association.mapper;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import uk.gov.companieshouse.accounts.association.mapper.abstracts.AssociationsListDaoToDtoMapper;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;

@Mapper( componentModel = "spring" )
public abstract class AssociationsListCompanyDaoToDtoMapper extends AssociationsListDaoToDtoMapper {

    @Autowired
    protected CompanyService companyService;

    @Autowired
    protected AssociationUserDaoToDtoMapper associationUserDaoToDtoMapper;

    private Function<Association, Association> enrichWithCompanyName( final String companyNumber ){
        final var companyProfile = companyService.fetchCompanyProfile( companyNumber );
        final var companyName = companyProfile.getCompanyName();
        return association -> {
            association.setCompanyName( companyName );
            return association;
        };
    }

    @AfterMapping
    protected void enrichWithItems( Page<uk.gov.companieshouse.accounts.association.models.Association> page, @MappingTarget AssociationsList list, @Context Map<String, String> context ) {
        final var companyNumber = context.get( "companyNumber" );
        final var pageContent = page.getContent();

        List<Association> items = List.of();
        if ( !pageContent.isEmpty() ) {
            final var enrichWithCompanyName = enrichWithCompanyName(companyNumber);

            items = pageContent.parallelStream()
                               .map(associationUserDaoToDtoMapper::daoToDto)
                               .map(enrichWithCompanyName)
                               .toList();
        }
        list.items(items);
    }

    public abstract AssociationsList daoToDto( Page<uk.gov.companieshouse.accounts.association.models.Association> page, @Context Map<String, String> companyNumberAndEndpointUri );

}
