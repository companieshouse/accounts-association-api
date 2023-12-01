package uk.gov.companieshouse.authentication.unit.controller;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.companieshouse.authentication.controller.Oauth2Controller;
import uk.gov.companieshouse.authentication.service.AuthenticationService;
import uk.gov.companieshouse.authentication.utils.CookieHandler;
import uk.gov.companieshouse.encryption.jwe.CHJweEncrypt;

import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class Oauth2ControllerTests {

    private static final String REDIRECT_URL = "/test";
    private final static String TEST_KEY="ZW5jcnlwdGlvbi1qYXZhLWxpYnJhcnktdGVzdC1rZXk=";

    @InjectMocks
    private Oauth2Controller controller;

    @Mock
    CookieHandler cookieHandler;

    @Mock
    AuthenticationService authenticationService;

    private MockHttpServletResponse response;
    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
        controller.chJweEncrypt = new CHJweEncrypt(TEST_KEY);
    }



    @Test
    void getTemplate() {
        String pageTemplate = controller.getTemplateName();
        assertNull(pageTemplate);
    }

    @Test
    void getCallbackFromOneLoginTest() throws Exception {

        Cookie cookie = new Cookie("FLP", "1234567");
        Map<String, String> params = new HashMap<>();

        when(authenticationService.validateAndRedirectRequest(params, response)).thenReturn(REDIRECT_URL);

        controller.callbackOneLogin(params, null, response);

        verify(authenticationService).validateAndRedirectRequest(any(), any());

    }

    @Test
    void getCallbackTestFlp() throws Exception {

        Cookie flpCookie = new Cookie("__FLP", "1234567");

        when(cookieHandler.generateProviderCookie(any())).thenReturn(flpCookie);

        controller.callbackTest(null, response);

        assertEquals("1234567", response.getCookie("__FLP").getValue());

    }

    @Test
    void getCallbackTestZxs() throws Exception {
        Cookie zxsCookie = new Cookie("__ZXS", "9876543");

        when(cookieHandler.generateSignedInCookie(any())).thenReturn(zxsCookie);

        controller.callbackTest(null, response);

        assertEquals("9876543", response.getCookie("__ZXS").getValue());
    }

    @Test
    void getCallbackTestExistingZxs() throws Exception {
        Cookie zxsCookie = new Cookie("__ZXS", "eyJlbmMiOiJBMTI4Q0JDLUhTMjU2IiwiYWxnIjoiZGlyIn0..WmDVgQ1tR9W9w8T34j0Xvg.d-3C0HWJWKit7Jdrn7EU68CFaQpjdGodJxY8U3sx65Vm3GwhzwxhBYWy-aPHcU2i6wtC7TfhmHGK8TCfgA-F7A.5YJMb1pGYgQIfrafM063Nw");
        when(cookieHandler.generateSignedInCookie("12345")).thenReturn(new Cookie("__ZXS", "correctZXS"));

        controller.callbackTest(zxsCookie.getValue(), response);

        assertEquals("correctZXS", response.getCookie("__ZXS").getValue());
    }

}
