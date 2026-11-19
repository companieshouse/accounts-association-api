package uk.gov.companieshouse.accounts.association.mapper;

import static uk.gov.companieshouse.accounts.association.models.Constants.DEFAULT_DISPLAY_NAME;
import static uk.gov.companieshouse.accounts.association.models.Constants.DEFAULT_KIND;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.accounts.association.service.CompanyService;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationLinks;
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
    protected void enrichWithUserDetails( @MappingTarget final Association association, @Context User userDetails ){
        if ( Objects.isNull( userDetails ) ){
            userDetails = Optional.ofNullable( association.getUserId() )
                    .map( userId -> fetchUserDetailsOrPlaceholder( userId, association.getUserEmail() ) )
                    .orElse( new User().email( association.getUserEmail() ) );
        }
        association.setUserEmail( userDetails.getEmail() );
        association.setDisplayName( Optional.ofNullable( userDetails.getDisplayName() ).orElse( DEFAULT_DISPLAY_NAME ) );
    }

    private User fetchUserDetailsOrPlaceholder( final String userId, final String storedUserEmail ){
        try {
            return usersService.fetchUserDetails( userId, getXRequestId() );
        } catch ( NotFoundRuntimeException exception ){
            LOGGER.errorContext( getXRequestId(), new Exception( "User not found in accounts-user-api; using placeholder user details", exception ), new HashMap<>( Map.of( "user_id", userId ) ) );
            return toUnresolvedUser( userId, storedUserEmail );
        }
    }

    // Used when a user_id no longer exists in accounts-user-api. Consumers (e.g. your-companies-web) render
    // userEmail as the person's heading, so DEFAULT_DISPLAY_NAME is used when no email is stored on the association.
    protected static User toUnresolvedUser( final String userId, final String storedUserEmail ){
        return new User()
                .userId( userId )
                .email( Optional.ofNullable( storedUserEmail ).orElse( DEFAULT_DISPLAY_NAME ) );
    }

    @AfterMapping
    protected void enrichWithCompanyDetails( @MappingTarget final Association association, @Context CompanyDetails companyDetails ) {
        if ( Objects.isNull( companyDetails ) ){
            companyDetails = companyService.fetchCompanyProfile( association.getCompanyNumber() );
        }
        association.setCompanyName( companyDetails.getCompanyName() );
        association.setCompanyStatus( companyDetails.getCompanyStatus() );
    }

    @AfterMapping
    protected void enrichAssociationWithLinksAndKind( @MappingTarget final Association association ) {
        final var associationId = association.getId();
        final var self = String.format( "/associations/%s", associationId );
        final var links = new AssociationLinks().self( self );

        association.setLinks( links );
        association.setKind( DEFAULT_KIND );
    }

    @Mapping( target = "status", expression = "java(Association.StatusEnum.fromValue(associationDao.getStatus()))" )
    @Mapping( target = "approvalRoute", expression = "java(Association.ApprovalRouteEnum.fromValue(associationDao.getApprovalRoute()))" )
    public abstract Association daoToDto( final AssociationDao associationDao, @Context final User userDetails, @Context final CompanyDetails companyDetails );

}
