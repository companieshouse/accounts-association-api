package uk.gov.companieshouse.accounts.association.interceptor;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.accounts.association.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class AuthorizationInterceptorTest {

    @Mock
    private UsersService usersService;

    @InjectMocks
    private AuthorizationInterceptor interceptor;

    @BeforeEach
    void setup(){
         interceptor = new AuthorizationInterceptor(usersService);
    }

    @Test
    void preHandleWithoutHeadersReturns401() {
        final var request = new MockHttpServletRequest();
        final var response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, null));
        assertEquals(401, response.getStatus());
    }

    @Test
    void preHandleWithoutEricIdentityReturns401() {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-Identity-Type", "oauth2");
        final var response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, null));
        assertEquals(401, response.getStatus());
    }



    @Test
    void preHandleWithoutEricIdentityTypeReturns401() {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-Identity", "abcd123456");
        final var response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, null));
        assertEquals(401, response.getStatus());
    }

    @Test
    void preHandleWithIncorrectEricIdentityTypeReturns401() {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-Identity", "abcd123456");
        request.addHeader("Eric-Identity-Type", "key");

        final var response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, null));
        assertEquals(401, response.getStatus());
    }

    @Test
    void preHandleWithMalformedOrNonexistentEricIdentityReturn403() {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-Identity", "$$$");
        request.addHeader( "ERIC-Identity-Type", "oauth2" );
        final var response = new MockHttpServletResponse();

        Mockito.doThrow( new NotFoundRuntimeException( "accounts-association-api", "Not found" ) ).when( usersService ).fetchUserDetails( anyString() );

        assertFalse(interceptor.preHandle(request, response, null));
        assertEquals(403, response.getStatus());
    }

    @Test
    void preHandleShouldReturnTrueWhenAuthHeaderAndAuthHeaderTypeOauthAreProvided() {
        final var request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "111");
        request.addHeader("Eric-identity-type", "oauth2");

        Mockito.doReturn( new User().userId( "111" ) ).when( usersService ).fetchUserDetails( "111" );

        HttpServletResponse response = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(request, response, null));
    }

}