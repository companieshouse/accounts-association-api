package uk.gov.companieshouse.accounts.association.mapper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.utils.MapperUtil;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.company.CompanyDetails;

@Component
public class AssociationsListCompanyMapper {


    private static final String END_POINT_URL = "/companies/";
    private final BaseMapper baseMapper;

    private final MapperUtil mapperUtil;

    @Autowired
    public AssociationsListCompanyMapper(final BaseMapper baseMapper,final MapperUtil mapperUtil) {
        this.baseMapper = baseMapper;
        this.mapperUtil = mapperUtil;
    }


    public AssociationsList daoToDto(final Page<AssociationDao> associationsList, final CompanyDetails companyDetails) {
        var associationList = associationsList.map(baseMapper::daoToDto)
                .map(mapperUtil::enrichAssociationWithUserDetails)
                .map(association -> {
                    association.setCompanyName(companyDetails.getCompanyName());
                    return association;
                });
        return mapperUtil.enrichWithMetadata(associationList, END_POINT_URL.concat(companyDetails.getCompanyNumber()));
    }


}
