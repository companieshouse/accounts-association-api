package uk.gov.companieshouse.accounts.association.mapper;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.MapperUtil.enrichWithMetadata;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;

@Component
public class AssociationsListUserMapper {

    private final CompanyService companyService;
    private final AssociationMapper associationMapper;

    public AssociationsListUserMapper( final CompanyService companyService, final AssociationMapper associationMapper ) {
        this.companyService = companyService;
        this.associationMapper = associationMapper;
    }

    public Association daoToDto( final AssociationDao associationDao, final User user, final CompanyDetails company ) {
        return associationMapper.daoToDto( associationDao, user, company );
    }

    public AssociationsList daoToDto( final Page<AssociationDao> associationsList, final User user ) {
        if ( Objects.isNull( user ) ){
            LOGGER.errorContext( getXRequestId(), new Exception( "User cannot be null" ), null );
            throw new IllegalArgumentException( "User cannot be null" );
        }

        final var companies = companyService.fetchCompanyProfiles( associationsList.stream() );
        final var associations = associationsList.map( associationDao -> {
            final var company = companies.get( associationDao.getCompanyNumber() );
            return associationMapper.daoToDto( associationDao, user, company );
        } );

        return enrichWithMetadata( associations, "" );
    }

}
