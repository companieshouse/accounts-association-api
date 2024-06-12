package uk.gov.companieshouse.accounts.association.mapper;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationLinks;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Mapper(componentModel = "spring")
public abstract class BaseMapper {


    private static final String DEFAULT_KIND = "association";


    @AfterMapping
    protected void enrichAssociation(@MappingTarget Association association) {
        enrichAssociationWithLinks(association);
    }

    @Mapping(target = "status",
            expression = "java(Association.StatusEnum.fromValue(associationDao.getStatus()))")
    @Mapping(target = "approvalRoute",
            expression = "java(Association.ApprovalRouteEnum.fromValue(associationDao.getApprovalRoute()))")
    public abstract Association daoToDto(final AssociationDao associationDao);

    public OffsetDateTime localDateTimeToOffsetDateTime(LocalDateTime localDateTime) {
        return Objects.isNull(localDateTime) ? null : OffsetDateTime.of(localDateTime, ZoneOffset.UTC);
    }


    public void enrichAssociationWithLinks(Association association) {
        final var associationId = association.getId();
        final var self = String.format("/associations/%s", associationId);
        final var links = new AssociationLinks().self(self);

        association.setLinks(links);
        association.setKind(DEFAULT_KIND);

    }
}
