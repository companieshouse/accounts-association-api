package uk.gov.companieshouse.accounts.association.mapper;

import static uk.gov.companieshouse.accounts.association.models.Constants.DEFAULT_DISPLAY_NAME;
import static uk.gov.companieshouse.accounts.association.models.Constants.DEFAULT_KIND;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import java.util.Optional;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationLinks;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;

@Mapper( componentModel = "spring" )
public abstract class AssociationMapper {

    @Autowired
    protected UsersService usersService;

    @Autowired
    protected CompanyService companyService;

    protected OffsetDateTime localDateTimeToOffsetDateTime( final LocalDateTime localDateTime ) {
        return Objects.isNull( localDateTime ) ? null : OffsetDateTime.of( localDateTime, ZoneOffset.UTC );
    }

    @AfterMapping
    protected void enrichWithUserDetails(@MappingTarget final uk.gov.companieshouse.api.accounts.associations.model.Association association, @Context User userDetails ){
        if ( Objects.isNull( userDetails ) ){
            userDetails = Optional.ofNullable( association.getUserId() )
                    .map( user -> usersService.fetchUserDetails( user, getXRequestId() ) )
                    .orElse( new User().email( association.getUserEmail() ) );
        }
        association.setUserEmail( userDetails.getEmail() );
        association.setDisplayName( Optional.ofNullable( userDetails.getDisplayName() ).orElse( DEFAULT_DISPLAY_NAME ) );
    }

    @AfterMapping
    protected void enrichWithCompanyDetails(@MappingTarget final uk.gov.companieshouse.api.accounts.associations.model.Association association, @Context CompanyDetails companyDetails ) {
        if ( Objects.isNull( companyDetails ) ){
            companyDetails = companyService.fetchCompanyProfile( association.getCompanyNumber() );
        }
        association.setCompanyName( companyDetails.getCompanyName() );
        association.setCompanyStatus( companyDetails.getCompanyStatus() );
    }

    @AfterMapping
    protected void enrichAssociationWithLinksAndKind( @MappingTarget final uk.gov.companieshouse.api.accounts.associations.model.Association association ) {
        final var associationId = association.getId();
        final var self = String.format( "/associations/%s", associationId );
        final var links = new AssociationLinks().self( self );

        association.setLinks( links );
        association.setKind( DEFAULT_KIND );
    }

    @Mapping( target = "status", expression = "java(Association.StatusEnum.fromValue(association.getStatus()))" )
    @Mapping( target = "approvalRoute", expression = "java(Association.ApprovalRouteEnum.fromValue(association.getApprovalRoute()))" )
    public abstract uk.gov.companieshouse.api.accounts.associations.model.Association daoToDto(final Association association, @Context final User userDetails, @Context final CompanyDetails companyDetails );

}
