package uk.gov.companieshouse.accounts.association.mapper;


import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.MapperUtil.enrichWithMetadata;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;

@Component
public class AssociationsListCompanyMapper {

    private final UsersService usersService;
    private final AssociationMapper associationMapper;

    public AssociationsListCompanyMapper( final UsersService usersService, final AssociationMapper associationMapper ) {
        this.usersService = usersService;
        this.associationMapper = associationMapper;
    }

    public Association daoToDto( final AssociationDao associationDao, final User user, final CompanyDetails company ) {
        return associationMapper.daoToDto( associationDao, user, company );
    }

    public AssociationsList daoToDto( final Page<AssociationDao> associationsList, final CompanyDetails company ) {
        if ( Objects.isNull( company ) ){
            LOGGER.errorContext( getXRequestId(), new Exception( "Company cannot be null" ), null );
            throw new IllegalArgumentException( "Company cannot be null" );
        }
        final var endpointUrl = String.format( "/companies/%s", company.getCompanyNumber() );

        final var users = usersService.fetchUserDetails( associationsList.stream() );
        final var associations = associationsList.map( associationDao -> {
            final var user = Objects.isNull( associationDao.getUserId() ) ? null : users.getOrDefault( associationDao.getUserId(), AssociationMapper.toUnresolvedUser( associationDao.getUserId(), associationDao.getUserEmail() ) );
            return associationMapper.daoToDto( associationDao, user, company );
        } );

        return enrichWithMetadata( associations, endpointUrl );
    }

}
