package uk.gov.companieshouse.accounts.association.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.util.security.AuthorisationUtil;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Objects;

@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);


    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {

        return hasAuthorisedIdentity(request, response) && hasValidAuthorisedIdentityType(request, response);

    }

    private boolean hasAuthorisedIdentity(HttpServletRequest request, HttpServletResponse response) {

        if (Objects.isNull(AuthorisationUtil.getAuthorisedIdentity(request))) {
            LOGGER.debugRequest(request, "no authorised identity", null);
            response.setStatus(401);
            return false;
        }

        return true;

    }

    private boolean hasValidAuthorisedIdentityType(HttpServletRequest request, HttpServletResponse response) {
        String identityType = AuthorisationUtil.getAuthorisedIdentityType(request);

        if (Objects.nonNull(identityType) && identityType.equals("oauth2")) {
            return true;
        }
        LOGGER.debugRequest(request, "invalid identity type [" + identityType + "]", null);
        response.setStatus(403);
        return false;

    }
}
