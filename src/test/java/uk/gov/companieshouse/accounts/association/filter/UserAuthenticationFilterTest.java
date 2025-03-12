package uk.gov.companieshouse.accounts.association.filter;

import static org.mockito.ArgumentMatchers.argThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.companieshouse.accounts.association.common.TestDataManager;

@ExtendWith( MockitoExtension.class )
@Tag( "unit-test" )
class UserAuthenticationFilterTest {

    private UserAuthenticationFilter userAuthenticationFilter;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @BeforeEach
    void setup(){
        userAuthenticationFilter = new UserAuthenticationFilter();
    }

    private ArgumentMatcher<Authentication> springRoleWasAssigned( final String springRole ){
        return authentication -> authentication.getAuthorities()
                .stream()
                .map( GrantedAuthority::getAuthority )
                .toList()
                .contains( springRole );
    }

    @Test
    void doFilterInternalWithoutEricIdentityDoesNotAddAnyRolesTests() {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity-Type","oauth2" );
        request.addHeader( "ERIC-Authorised-Key-Roles", "*" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        final var securityContext = Mockito.mock( SecurityContext.class );
        SecurityContextHolder.setContext( securityContext );

        userAuthenticationFilter.doFilterInternal( request, response, filterChain );

        Mockito.verify( securityContext ).setAuthentication( argThat( springRoleWasAssigned( "ROLE_UNKNOWN" ) ) );
    }

    @Test
    void doFilterInternalWithoutEricIdentityTypeDoesNotAddAnyRoles() {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        final var securityContext = Mockito.mock( SecurityContext.class );
        SecurityContextHolder.setContext( securityContext );

        userAuthenticationFilter.doFilterInternal( request, response, filterChain );

        Mockito.verify( securityContext ).setAuthentication( argThat( springRoleWasAssigned( "ROLE_UNKNOWN" ) ) );
    }

    @Test
    void doFilterInternalWithMalformedEricIdentityTypeDoesNotAddAnyRoles() {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        request.addHeader( "Eric-Identity-Type", "magic" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        final var securityContext = Mockito.mock( SecurityContext.class );
        SecurityContextHolder.setContext( securityContext );

        userAuthenticationFilter.doFilterInternal( request, response, filterChain );

        Mockito.verify( securityContext ).setAuthentication( argThat( springRoleWasAssigned( "ROLE_UNKNOWN" ) ) );
    }

    @Test
    void doFilterInternalWithValidOAuth2RequestRequestAddsBasicOAuthRole() {
        final var request = new MockHttpServletRequest();
        request.addHeader( "X-Request-Id", "theId123" );
        request.addHeader( "Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ" );
        request.addHeader( "Eric-Identity-Type","oauth2" );
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock( FilterChain.class );

        final var securityContext = Mockito.mock( SecurityContext.class );
        SecurityContextHolder.setContext( securityContext );

        userAuthenticationFilter.doFilterInternal( request, response, filterChain );

        Mockito.verify( securityContext ).setAuthentication( argThat( springRoleWasAssigned( "ROLE_BASIC_OAUTH" ) ) );
    }

}
