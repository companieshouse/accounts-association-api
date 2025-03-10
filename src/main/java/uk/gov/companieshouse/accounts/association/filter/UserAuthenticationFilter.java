package uk.gov.companieshouse.accounts.association.filter;

import static uk.gov.companieshouse.accounts.association.models.Constants.OAUTH2;
import static uk.gov.companieshouse.accounts.association.models.Constants.UNKNOWN;
import static uk.gov.companieshouse.accounts.association.models.Constants.X_REQUEST_ID;
import static uk.gov.companieshouse.accounts.association.models.SpringRole.BASIC_OAUTH_ROLE;
import static uk.gov.companieshouse.accounts.association.models.SpringRole.UNKNOWN_ROLE;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.api.util.security.RequestUtils.getRequestHeader;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Collections;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;
import uk.gov.companieshouse.accounts.association.models.SpringRole;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData;
import uk.gov.companieshouse.accounts.association.models.context.RequestContextData.RequestContextDataBuilder;

public class UserAuthenticationFilter extends OncePerRequestFilter {

    private RequestContextData buildRequestContextData( final HttpServletRequest request ){
        return new RequestContextDataBuilder()
                .setXRequestId( request )
                .setEricIdentity( request )
                .setEricIdentityType( request )
                .build();
    }

    private boolean isValidOAuth2Request( final RequestContextData requestContextData ) {
        return !requestContextData.getEricIdentity().equals( UNKNOWN ) && requestContextData.getEricIdentityType().equals( OAUTH2 );
    }

    private SpringRole computeSpringRole( final RequestContextData requestContextData ){
        LOGGER.debugContext( requestContextData.getXRequestId(), "Checking if this is a valid OAuth2 Request...", null );
        return !isValidOAuth2Request( requestContextData ) ? UNKNOWN_ROLE : BASIC_OAUTH_ROLE;
    }

    private void setSpringRole( final RequestContextData requestContextData, final String role ){
        LOGGER.debugContext( requestContextData.getXRequestId(), String.format( "Adding Spring role: %s", role ), null );
        SecurityContextHolder.getContext().setAuthentication( new PreAuthenticatedAuthenticationToken( UNKNOWN, UNKNOWN, Collections.singleton( new SimpleGrantedAuthority( String.format( "ROLE_%s", role ) ) ) ) );
    }

    @Override
    protected void doFilterInternal( final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain ) {
        try {
            final var requestContextData = buildRequestContextData( request );
            setSpringRole( requestContextData, computeSpringRole( requestContextData ).getValue() );
            filterChain.doFilter( request, response );
        } catch ( Exception exception ) {
            LOGGER.errorContext( getRequestHeader( request, X_REQUEST_ID ), exception, null );
            response.setStatus( 403 );
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

}
