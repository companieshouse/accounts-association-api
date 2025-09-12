package uk.gov.companieshouse.accounts.association.filter;

import static org.mockito.ArgumentMatchers.argThat;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_READ_PERMISSION;
import static uk.gov.companieshouse.accounts.association.models.Constants.ADMIN_UPDATE_PERMISSION;

import jakarta.servlet.FilterChain;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class UserAuthenticationFilterTest {

    private UserAuthenticationFilter userAuthenticationFilter;

    @BeforeEach
    void setup(){
        userAuthenticationFilter = new UserAuthenticationFilter();
    }

    private ArgumentMatcher<Authentication> springRolesWereAssigned(final List<String> springRoles){
        return authentication -> authentication.getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .toList()
                .containsAll(springRoles);
    }

    @Test
    void doFilterInternalWithoutEricIdentityDoesNotAddAnyRolesTests() {
        final var request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "theId123");
        request.addHeader("Eric-Identity-Type","oauth2");
        request.addHeader("ERIC-Authorised-Key-Roles", "*");
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock(FilterChain.class);

        final var securityContext = Mockito.mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        userAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Mockito.verify(securityContext).setAuthentication(argThat(springRolesWereAssigned(List.of("ROLE_UNKNOWN"))));
    }

    @Test
    void doFilterInternalWithoutEricIdentityTypeDoesNotAddAnyRoles() {
        final var request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "theId123");
        request.addHeader("Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock(FilterChain.class);

        final var securityContext = Mockito.mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        userAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Mockito.verify(securityContext).setAuthentication(argThat(springRolesWereAssigned(List.of("ROLE_UNKNOWN"))));
    }

    @Test
    void doFilterInternalWithMalformedEricIdentityTypeDoesNotAddAnyRoles() {
        final var request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "theId123");
        request.addHeader("Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        request.addHeader("Eric-Identity-Type", "magic");
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock(FilterChain.class);

        final var securityContext = Mockito.mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        userAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Mockito.verify(securityContext).setAuthentication(argThat(springRolesWereAssigned(List.of("ROLE_UNKNOWN"))));
    }

    @Test
    void doFilterInternalWithValidOAuth2RequestRequestAddsBasicOAuthRole() {
        final var request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "theId123");
        request.addHeader("Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        request.addHeader("Eric-Identity-Type","oauth2");
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock(FilterChain.class);

        final var securityContext = Mockito.mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        userAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Mockito.verify(securityContext).setAuthentication(argThat(springRolesWereAssigned(List.of("ROLE_BASIC_OAUTH"))));
    }

    @Test
    void doFilterInternalWithValidAPIKeyRequestAddsKeyRole() {
        final var request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "theId123");
        request.addHeader("Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        request.addHeader("Eric-Identity-Type","key");
        request.addHeader("ERIC-Authorised-Key-Roles", "*");
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock(FilterChain.class);

        final var securityContext = Mockito.mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        userAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Mockito.verify(securityContext).setAuthentication(argThat(springRolesWereAssigned(List.of("ROLE_KEY"))));
    }

    private static Stream<Arguments> adminPrivilegeScenarios(){
        return Stream.of(
                Arguments.of(ADMIN_READ_PERMISSION, List.of("ROLE_BASIC_OAUTH", "ROLE_ADMIN_READ")),
                Arguments.of(ADMIN_UPDATE_PERMISSION, List.of("ROLE_BASIC_OAUTH", "ROLE_ADMIN_UPDATE")),
                Arguments.of(String.format("%s %s" , ADMIN_READ_PERMISSION, ADMIN_UPDATE_PERMISSION), List.of("ROLE_BASIC_OAUTH", "ROLE_ADMIN_READ", "ROLE_ADMIN_UPDATE")),
                Arguments.of(String.format("%s %s /admin/something/else" , ADMIN_READ_PERMISSION, ADMIN_UPDATE_PERMISSION), List.of("ROLE_BASIC_OAUTH", "ROLE_ADMIN_READ", "ROLE_ADMIN_UPDATE"))
       );
    }

    @ParameterizedTest
    @MethodSource("adminPrivilegeScenarios")
    void doFilterInternalWithValidAdminRequestAddsAdminRoles(final String ericAuthorisedRoles, final List<String> expectedSpringRoles){
        final var request = new MockHttpServletRequest();
        request.addHeader("X-Request-Id", "theId123");
        request.addHeader("Eric-Identity", "67ZeMsvAEgkBWs7tNKacdrPvOmQ");
        request.addHeader("Eric-Identity-Type","oauth2");
        request.addHeader("Eric-Authorised-Roles", ericAuthorisedRoles);
        final var response = new MockHttpServletResponse();
        final var filterChain = Mockito.mock(FilterChain.class);

        final var securityContext = Mockito.mock(SecurityContext.class);
        SecurityContextHolder.setContext(securityContext);

        userAuthenticationFilter.doFilterInternal(request, response, filterChain);

        Mockito.verify(securityContext).setAuthentication(argThat(springRolesWereAssigned(expectedSpringRoles)));

    }

}
