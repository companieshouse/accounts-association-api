package uk.gov.companieshouse.accounts.association.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.api.accounts.associations.model.Association;

@Mapper(componentModel = "spring")
public abstract class AssociationMapper extends BaseMapper {

    @Autowired
    MapperUtil mapperUtil;

    @AfterMapping
    protected void enrichAssociation(@MappingTarget Association association) {
        mapperUtil.enrichAssociationWithUserDetails(association);
        mapperUtil.enrichAssociationWithCompanyName(association);
        enrichAssociationWithLinks(association);
    }

    protected void setMapperUtil(MapperUtil mapperUtil) {
        this.mapperUtil = mapperUtil;
    }
}
