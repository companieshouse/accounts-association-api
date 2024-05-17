package uk.gov.companieshouse.accounts.association.models;

import uk.gov.companieshouse.api.accounts.user.model.User;

public final class UserContext {

    private static ThreadLocal<User> userContext;

    public static User getLoggedUser() {
        return userContext != null ? userContext.get() : null;
    }

    public static void setLoggedUser(final User user) {
        userContext = new ThreadLocal<>();
        userContext.set(user);
    }

    public static void clear(){
        userContext.remove();
    }
}
