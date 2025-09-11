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
import uk.gov.companieshouse.api.accounts.user.model.User;

@Component
@Mapper(componentModel = "spring")
public abstract class AssociationsListUserMapper extends AssociationMapper {

    public AssociationsList daoToDto(final Page<AssociationDao> associationsList, final User user) {
        if (Objects.isNull(user)){
            LOGGER.errorContext(getXRequestId(), new Exception("User cannot be null"), null);
            throw new IllegalArgumentException("User cannot be null");
        }

        final var companies = companyService.fetchCompanyProfiles(associationsList.stream());
        final var associations = associationsList.map(associationDao -> {
            final var company = companies.get(associationDao.getCompanyNumber());
            return daoToDto(associationDao, user, company);
        });

        return enrichWithMetadata(associations, "");
    }

}
