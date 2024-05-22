package uk.gov.companieshouse.accounts.association.mapper;

import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.utils.MapperUtil;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationWithInvitations;

@Component
public class AssociationMapper {


    private final MapperUtil mapperUtil;


    private final BaseMapper baseMapper;

    @Autowired
    public AssociationMapper(final MapperUtil mapperUtil, final BaseMapper baseMapper) {
        this.mapperUtil = mapperUtil;
        this.baseMapper = baseMapper;
    }

    protected void enrichAssociation(AssociationWithInvitations association) {
        mapperUtil.enrichAssociationWithUserDetails(association);
        mapperUtil.enrichAssociationWithCompanyName(association);
        mapperUtil.enrichInvitations(association);
    }


    public AssociationWithInvitations daoToDto(final AssociationDao associationDao) {
        if(Objects.isNull(associationDao)){
            return null;
        }
        AssociationWithInvitations association = baseMapper.daoToDto(associationDao);
        enrichAssociation(association);
        return association;
    }
}
