package uk.gov.companieshouse.accounts.association.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class AuthorizationInterceptorTest {

    AuthorizationInterceptor interceptor = new AuthorizationInterceptor();

    @Test
    void preHandleShouldReturn401WhenNoAuthHeadersAreProvided() {

        HttpServletRequest request = new MockHttpServletRequest();

        HttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, null));
        assertEquals(401, response.getStatus());
    }

    @Test
    void preHandleShouldReturn403WhenAuthHeaderAndNoAuthHeaderTypeAreProvided() {


        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-Identity", "abcd123456");

        HttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, null));
        assertEquals(403, response.getStatus());
    }

    @Test
    void preHandleShouldReturn403WhenAuthHeaderAndAuthHeaderTypeNotOauthAreProvided() {


        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "abcd123456");
        request.addHeader("Eric-identity-type", "key");

        HttpServletResponse response = new MockHttpServletResponse();
        assertFalse(interceptor.preHandle(request, response, null));
        assertEquals(403, response.getStatus());
    }

    @Test
    void preHandleShouldReturnTrueWhenAuthHeaderAndAuthHeaderTypeOauthAreProvided() {


        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Eric-identity", "abcd123456");
        request.addHeader("Eric-identity-type", "oauth2");

        HttpServletResponse response = new MockHttpServletResponse();
        assertTrue(interceptor.preHandle(request, response, null));
    }
}