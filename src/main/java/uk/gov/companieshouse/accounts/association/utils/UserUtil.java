package uk.gov.companieshouse.accounts.association.utils;

import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;

import java.util.Optional;
import uk.gov.companieshouse.accounts.association.models.Association;
import uk.gov.companieshouse.api.accounts.user.model.User;

public final class UserUtil {

    private UserUtil(){}

    public static String mapToDisplayValue( final User user, final String defaultValue ){
        return Optional.ofNullable( user )
                .map( userData -> Optional.ofNullable( userData.getDisplayName() ).orElse( userData.getEmail() ) )
                .orElse( defaultValue );
    }

    public static boolean isRequestingUser( final Association targetAssociation ){
        final var idMatches = Optional.ofNullable( targetAssociation )
                .map( Association::getUserId )
                .filter( userId -> userId.equals( getEricIdentity() ) )
                .isPresent();

        final var emailMatch = Optional.ofNullable( targetAssociation )
                .map( Association::getUserEmail )
                .filter( userEmail -> userEmail.equals( getUser().getEmail() ) )
                .isPresent();

        return idMatches || emailMatch;
    }

}
