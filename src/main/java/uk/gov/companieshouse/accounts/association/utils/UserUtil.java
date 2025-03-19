package uk.gov.companieshouse.accounts.association.utils;

import java.util.Optional;
import uk.gov.companieshouse.api.accounts.user.model.User;

public final class UserUtil {

    private UserUtil(){}

    public static String mapToDisplayValue( final User user, final String defaultValue ){
        return Optional.ofNullable( user )
                .map( userData -> Optional.ofNullable( userData.getDisplayName() ).orElse( userData.getEmail() ) )
                .orElse( defaultValue );
    }

}
