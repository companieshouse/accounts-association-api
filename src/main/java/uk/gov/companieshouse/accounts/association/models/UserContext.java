package uk.gov.companieshouse.accounts.association.models;

import uk.gov.companieshouse.api.accounts.user.model.User;

public final class UserContext {

    private static ThreadLocal<User> userContextThreadLocal;

    public static User getLoggedUser() {
        return userContextThreadLocal != null ? userContextThreadLocal.get() : null;
    }

    private UserContext(){}
    public static void setLoggedUser(final User user) {
        userContextThreadLocal = new ThreadLocal<>();
        userContextThreadLocal.set(user);
    }

    public static void clear(){
        userContextThreadLocal.remove();
    }
}
