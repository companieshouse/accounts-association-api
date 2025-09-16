package uk.gov.companieshouse.accounts.association.utils;

import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getEricIdentity;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getUser;

import java.util.Optional;
import uk.gov.companieshouse.accounts.association.models.AssociationDao;
import uk.gov.companieshouse.api.accounts.user.model.User;

public final class UserUtil {

    private UserUtil() {}

    public static String mapToDisplayValue(final User user, final String defaultValue){
        return Optional.ofNullable(user)
                .map(userData -> Optional.ofNullable(userData.getDisplayName()).orElse(userData.getEmail()))
                .orElse(defaultValue);
    }

    public static boolean isRequestingUser(final AssociationDao targetAssociation){
        final var idMatches = Optional.ofNullable(targetAssociation)
                .map(AssociationDao::getUserId)
                .filter(userId -> userId.equals(getEricIdentity()))
                .isPresent();

        final var emailMatch = Optional.ofNullable(targetAssociation)
                .map(AssociationDao::getUserEmail)
                .filter(userEmail -> userEmail.equals(getUser().getEmail()))
                .isPresent();

        return idMatches || emailMatch;
    }

}
