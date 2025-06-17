package uk.gov.companieshouse.accounts.association.interceptor;

import static uk.gov.companieshouse.accounts.association.models.Constants.KEY;
import static uk.gov.companieshouse.accounts.association.models.Constants.OAUTH2;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.api.util.security.EricConstants.ERIC_IDENTITY_TYPE;
import static uk.gov.companieshouse.api.util.security.RequestUtils.getRequestHeader;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.service.UsersService;

@Component
public class OAuth2AndKeyRequestLifecycleInterceptor extends OAuthRequestLifecycleInterceptor {

    public OAuth2AndKeyRequestLifecycleInterceptor( final UsersService usersService ) {
        super( usersService );
    }

    @Override
    public boolean preHandle( final HttpServletRequest request, final HttpServletResponse response, final Object handler ) {
        if( OAUTH2.equals( getRequestHeader( request, ERIC_IDENTITY_TYPE ) ) ){
            return super.preHandle( request, response, handler );
        }

        if ( KEY.equals( getRequestHeader( request, ERIC_IDENTITY_TYPE ) ) ) {
            logStartRequestProcessing( request, LOGGER );
            setupRequestContext( request, null );
            return true;
        }

        response.setStatus( 403 );
        return false;
    }

}
