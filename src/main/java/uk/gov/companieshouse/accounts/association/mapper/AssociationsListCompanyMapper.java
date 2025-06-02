package uk.gov.companieshouse.accounts.association.mapper;


import static uk.gov.companieshouse.accounts.association.utils.MapperUtil.enrichWithMetadata;

import java.util.Objects;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.company.CompanyDetails;

@Component
@Mapper( componentModel = "spring" )
public abstract class AssociationsListCompanyMapper extends AssociationMapper {

    public AssociationsList daoToDto( final Page<AssociationDao> associationsList, final CompanyDetails company ) {
        final var endpointUrl = String.format( "/companies/%s", company.getCompanyNumber() );

        final var users = usersService.fetchUserDetails( associationsList.stream() );
        final var associations = associationsList.map( associationDao -> {
            final var user = Objects.isNull( associationDao.getUserId() ) ? null : users.getOrDefault( associationDao.getUserId(), null );
            return daoToDto( associationDao, user, company );
        } );

        return enrichWithMetadata( associations, endpointUrl );
    }

}
