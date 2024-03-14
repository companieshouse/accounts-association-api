package uk.gov.companieshouse.accounts.association.mapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.mapstruct.AfterMapping;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import uk.gov.companieshouse.accounts.association.mapper.abstracts.AssociationsListDaoToDtoMapper;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.associations.model.Association;
import uk.gov.companieshouse.api.accounts.associations.model.AssociationsList;
import uk.gov.companieshouse.api.accounts.user.model.User;

@Mapper( componentModel = "spring" )
public abstract class AssociationsListUserDaoToDtoMapper extends AssociationsListDaoToDtoMapper {

    @Autowired
    protected UsersService usersService;

    @Autowired
    protected AssociationCompanyDaoToDtoMapper associationCompanyDaoToDtoMapper;

    private static final String DEFAULT_DISPLAY_NAME = "Not provided";

    private Function<Association, Association> enrichWithUserEmailAndDisplayName( final String userId ){
        final var userDetails = usersService.fetchUserDetails( userId );
        final var userEmail = userDetails.getEmail();

        final var userDisplayName =
                Optional.of( userDetails )
                        .map( User::getDisplayName )
                        .orElse( DEFAULT_DISPLAY_NAME );

        return association -> {
            association.setUserEmail( userEmail );
            association.setDisplayName( userDisplayName );
            return association;
        };
    }

    @AfterMapping
    protected void enrichWithItems( Page<uk.gov.companieshouse.accounts.association.models.Association> page, @MappingTarget AssociationsList list, @Context Map<String, String> context ){
        final var userId = context.get( "userId" );
        final var pageContent = page.getContent();

        List<Association> items = List.of();
        if ( !pageContent.isEmpty() ) {
            final var enrichWithUserEmailAndDisplayName = enrichWithUserEmailAndDisplayName(userId);
            items = pageContent.stream()
                               .map(associationCompanyDaoToDtoMapper::daoToDto)
                               .map(enrichWithUserEmailAndDisplayName)
                               .toList();
        }

        list.items( items );
    }

    public abstract AssociationsList daoToDto( Page<uk.gov.companieshouse.accounts.association.models.Association> page, @Context Map<String, String> userIdAndEndpointUri );


}
