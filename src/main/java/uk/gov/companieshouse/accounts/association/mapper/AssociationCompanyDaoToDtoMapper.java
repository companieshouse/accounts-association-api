package uk.gov.companieshouse.accounts.association.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.accounts.association.mapper.abstracts.AssociationDaoToDtoMapper;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;

@Mapper( componentModel = "spring" )
public abstract class AssociationCompanyDaoToDtoMapper extends AssociationDaoToDtoMapper {

    @Autowired
    protected CompanyService companyService;

    @AfterMapping
    protected void enrichAssociationWithCompanyProfile( @MappingTarget Association association ){
        final var companyNumber = association.getCompanyNumber();
        final var companyProfile = companyService.fetchCompanyProfile( companyNumber );
        final var companyName = companyProfile.getCompanyName();
        association.setCompanyName( companyName );
    }

    public abstract Association daoToDto( uk.gov.companieshouse.accounts.association.models.Association association );


}
