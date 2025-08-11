package uk.gov.companieshouse.accounts.association.filter;

import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_READ_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_UPDATE_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.KEY;
import static uk.gov.companieshouse.accounts.association.models.Constants.OAUTH2;
import static uk.gov.companieshouse.accounts.association.models.Constants.UNKNOWN;
import static uk.gov.companieshouse.accounts.association.models.Constants.X_REQUEST_ID;
import static uk.gov.companieshouse.accounts.association.models.SpringRole.ADMIN_READ_ROLE;
import static uk.gov.companieshouse.accounts.association.models.SpringRole.ADMIN_UPDATE_ROLE;
import static uk.gov.companieshouse.accounts.association.models.SpringRole.BASIC_OAUTH_ROLE;
import static uk.gov.companieshouse.accounts.association.models.SpringRole.KEY_ROLE;
import static uk.gov.companieshouse.accounts.association.models.SpringRole.UNKNOWN_ROLE;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.api.util.security.RequestUtils.getRequestHeader;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
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
                .setEricAuthorisedKeyRoles( request )
                .setAdminPrivileges( request )
                .build();
    }

    private boolean isValidOAuth2Request( final RequestContextData requestContextData ) {
        return !requestContextData.getEricIdentity().equals( UNKNOWN ) && requestContextData.getEricIdentityType().equals( OAUTH2 );
    }

    private boolean isValidAPIKeyRequest( final RequestContextData requestContextData ) {
        return !requestContextData.getEricIdentity().equals( UNKNOWN ) && requestContextData.getEricIdentityType().equals( KEY ) && requestContextData.getEricAuthorisedKeyRoles().equals( "*" );
    }

    private Set<SpringRole> computeSpringRole( final RequestContextData requestContextData ){
        LOGGER.debugContext( requestContextData.getXRequestId(), "Checking if this is a valid API Key Request...", null );
        if ( isValidAPIKeyRequest( requestContextData ) ) {
            return Set.of( KEY_ROLE );
        }

        LOGGER.debugContext( requestContextData.getXRequestId(), "Checking if this is a valid OAuth2 Request...", null );
        if ( !isValidOAuth2Request( requestContextData ) ) {
            return Set.of( UNKNOWN_ROLE );
        }
        LOGGER.debugContext( requestContextData.getXRequestId(), "Confirmed this is a valid OAuth2 Request.", null );

        final var roles = new HashSet<>( Set.of( BASIC_OAUTH_ROLE ) );
        if ( requestContextData.getAdminPrivileges().contains( ADMIN_READ_PERMISSION ) ) {
            roles.add( ADMIN_READ_ROLE );
        }
        if ( requestContextData.getAdminPrivileges().contains( ADMIN_UPDATE_PERMISSION ) ) {
            roles.add( ADMIN_UPDATE_ROLE );
        }
        return roles;
    }

    private void setSpringRoles( final RequestContextData requestContextData, final Set<SpringRole> springRoles ){
        LOGGER.debugContext( requestContextData.getXRequestId(), String.format( "Adding Spring roles: %s", springRoles.stream().map( SpringRole::getValue ).collect( Collectors.joining( ", " ) ) ), null );
        final var roles = springRoles.parallelStream()
                .map( SpringRole::getValue )
                .map( role -> String.format( "ROLE_%s", role ) )
                .map( SimpleGrantedAuthority::new )
                .toList();
        SecurityContextHolder.getContext().setAuthentication( new PreAuthenticatedAuthenticationToken( UNKNOWN, UNKNOWN, roles ) );
    }

    @Override
    protected void doFilterInternal( final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain ) {
        try {
            final var requestContextData = buildRequestContextData( request );
            setSpringRoles( requestContextData, computeSpringRole( requestContextData ) );
            filterChain.doFilter( request, response );
        } catch ( Exception exception ) {
            LOGGER.errorContext( getRequestHeader( request, X_REQUEST_ID ), exception, null );
            response.setStatus( 403 );
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

}
