package uk.gov.companieshouse.accounts.association.mapper;

import java.util.Optional;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.accounts.association.mapper.abstracts.AssociationDaoToDtoMapper;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.user.model.User;

@Mapper( componentModel = "spring" )
public abstract class AssociationUserDaoToDtoMapper extends AssociationDaoToDtoMapper {

    @Autowired
    protected UsersService usersService;

    private static final String DEFAULT_DISPLAY_NAME = "Not provided";

    @AfterMapping
    protected void enrichAssociationWithUserDetails( @MappingTarget Association association ){
        final var userId = association.getUserId();

        final var userDetails = usersService.fetchUserDetails( userId );
        final var userEmail = userDetails.getEmail();

        final var userDisplayName =
                Optional.of( userDetails )
                        .map( User::getDisplayName )
                        .orElse( DEFAULT_DISPLAY_NAME );

        association.setUserEmail( userEmail );
        association.setDisplayName( userDisplayName );
    }

    public abstract Association daoToDto( uk.gov.companieshouse.accounts.association.models.Association association );

}
