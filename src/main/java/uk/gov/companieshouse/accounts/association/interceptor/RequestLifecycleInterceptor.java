package uk.gov.companieshouse.accounts.association.interceptor;

import static uk.gov.companieshouse.accounts.association.models.Constants.KEY;
import static uk.gov.companieshouse.accounts.association.models.Constants.X_REQUEST_ID;
import static uk.gov.companieshouse.accounts.association.models.context.RequestContext.setRequestContext;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.RequestUtils.getRequestHeader;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.models.context.RequestContext;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData.RequestContextDataBuilder;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.logging.util.RequestLogger;

@Component
public class RequestLifecycleInterceptor implements HandlerInterceptor, RequestLogger {

    private final UsersService usersService;

    public RequestLifecycleInterceptor(final UsersService usersService) {
        this.usersService = usersService;
    }

    protected void setupRequestContext(final HttpServletRequest request, final User user){
        final var requestContextData = new RequestContextDataBuilder()
                .setXRequestId(request)
                .setEricIdentity(request)
                .setEricIdentityType(request)
                .setAdminPrivileges(request)
                .setUser(user)
                .build();

        setRequestContext(requestContextData);
    }

    @Override
    public boolean preHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler) {
        logStartRequestProcessing(request, LOGGER);

        if (KEY.equalsIgnoreCase(getRequestHeader(request, ERIC_IDENTITY_TYPE))) {
            setupRequestContext(request, null);
            return true;
        }

        try {
            final var user = usersService.fetchUserDetails(getRequestHeader(request, ERIC_IDENTITY), getRequestHeader(request, X_REQUEST_ID));
            setupRequestContext(request, user);
            return true;
        } catch (NotFoundRuntimeException exception) {
            LOGGER.debugContext(getRequestHeader(request, X_REQUEST_ID), String.format("Unable to find user %s", getRequestHeader(request, ERIC_IDENTITY)), null);
            response.setStatus(403);
            return false;
        }
    }

    @Override
    public void postHandle(final HttpServletRequest request, final HttpServletResponse response, final Object handler, final ModelAndView modelAndView) {
        logEndRequestProcessing(request, response, LOGGER);
    }

    @Override
    public void afterCompletion(final HttpServletRequest request, final HttpServletResponse response, final Object handler, final Exception exception) {
        RequestContext.clear();
    }

}
