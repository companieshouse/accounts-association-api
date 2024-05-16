package uk.gov.companieshouse.accounts.association.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.companieshouse.accounts.association.models.UserContext;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.util.security.AuthorisationUtil;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Objects;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {


    @Autowired
    UsersService usersService;


    private static final Logger LOGGER = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        return isOauth2User(request, response) && isValidAuthorisedUser(request, response);

    }

    private boolean isOauth2User(HttpServletRequest request, HttpServletResponse response) {

        if (!AuthorisationUtil.isOauth2User(request)) {
            LOGGER.debugRequest(request, "invalid user", null);
            response.setStatus(401);
            return false;
        }

        return true;

    }

    private boolean isValidAuthorisedUser(HttpServletRequest request, HttpServletResponse response) {
        String identity = AuthorisationUtil.getAuthorisedIdentity(request);
        final User user = usersService.fetchUserDetails(identity);
        if (Objects.nonNull(user)) {
            UserContext.setLoggedUser(user);
            return true;
        }
        LOGGER.debugRequest(request, "no user found with identity [" + identity + "]", null);
        response.setStatus(403);
        return false;

    }

    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        UserContext.clear();
    }
}
