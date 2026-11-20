package uk.gov.companieshouse.accounts.association.mapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.company.CompanyDetails;

@Mapper( componentModel = "spring" )
public interface AssociationDaoMapper {

    default OffsetDateTime localDateTimeToOffsetDateTime( final LocalDateTime localDateTime ) {
        return Objects.isNull( localDateTime ) ? null : OffsetDateTime.of( localDateTime, ZoneOffset.UTC );
    }

    @Mapping( target = "status", expression = "java(Association.StatusEnum.fromValue(associationDao.getStatus()))" )
    @Mapping( target = "approvalRoute", expression = "java(Association.ApprovalRouteEnum.fromValue(associationDao.getApprovalRoute()))" )
    Association daoToDto( final AssociationDao associationDao, @Context final User userDetails,
                          @Context final CompanyDetails companyDetails, @Context final AssociationMapper associationMapper );
}
