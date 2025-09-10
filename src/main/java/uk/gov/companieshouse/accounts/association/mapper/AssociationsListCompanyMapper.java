package uk.gov.companieshouse.accounts.association.mapper;


import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.MapperUtil.enrichWithMetadata;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

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
        if ( Objects.isNull( company ) ){
            LOGGER.errorContext( getXRequestId(), new Exception( "Company cannot be null" ), null );
            throw new IllegalArgumentException( "Company cannot be null" );
        }
        final var endpointUrl = String.format( "/companies/%s", company.getCompanyNumber() );

        final var users = usersService.fetchUsersDetails( associationsList.stream() );
        final var associations = associationsList.map( associationDao -> {
            final var user = Objects.isNull( associationDao.getUserId() ) ? null : users.getOrDefault( associationDao.getUserId(), null );
            return daoToDto( associationDao, user, company );
        } );

        return enrichWithMetadata( associations, endpointUrl );
    }

}
