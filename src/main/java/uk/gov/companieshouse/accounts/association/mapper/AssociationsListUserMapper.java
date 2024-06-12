package uk.gov.companieshouse.accounts.association.mapper;

import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.utils.MapperUtil;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.user.model.User;

import java.util.Optional;

@Component
public class AssociationsListUserMapper {


    private final BaseMapper baseMapper;


    private final MapperUtil mapperUtil;

    private static final String END_POINT_URL = "";

    private static final String DEFAULT_DISPLAY_NAME = "Not provided";

    @Autowired
    public AssociationsListUserMapper(final BaseMapper baseMapper, final MapperUtil mapperUtil) {
        this.baseMapper = baseMapper;
        this.mapperUtil = mapperUtil;
    }

    public AssociationsList daoToDto(final Page<AssociationDao> associationsList, @NotNull final User user) {
        var associationList = associationsList.map(baseMapper::daoToDto)
                .map(mapperUtil::enrichAssociationWithCompanyName)
                .map(association -> {
                    association.setUserEmail(user.getEmail());
                    association.setDisplayName(
                            Optional.ofNullable(
                                    user.getDisplayName()
                            ).orElse(DEFAULT_DISPLAY_NAME));
                    return association;
                });
        return mapperUtil.enrichWithMetadata(associationList, END_POINT_URL);
    }

}
