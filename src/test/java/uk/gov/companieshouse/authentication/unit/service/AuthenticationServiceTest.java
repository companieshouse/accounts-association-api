package uk.gov.companieshouse.authentication.unit.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import uk.gov.ch.oauth.IOAuthCoordinator;
import uk.gov.ch.oauth.exceptions.UnauthorisedException;
import uk.gov.companieshouse.authentication.service.AuthenticationService;
import uk.gov.companieshouse.session.Session;
import uk.gov.companieshouse.session.SessionImpl;
import uk.gov.companieshouse.session.handler.SessionHandler;
import uk.gov.companieshouse.session.store.Store;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @InjectMocks
    private AuthenticationService authenticationService;
    @Mock
    private IOAuthCoordinator coordinator;
    @Mock
    private Store store;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }
    @Test
    void testGenerationAuthorisationRequest() throws IOException {
        Map<String, String> parms = new HashMap<>();

        Session session = new SessionImpl(store, "1234", new HashMap<>());

        request.setAttribute(SessionHandler.CHS_SESSION_REQUEST_ATT_KEY, session);
        request.setAttribute("user-agent", "abcdef");
        request.setRemoteAddr("remoteAddress");

        when(coordinator.getAuthoriseUriFromRequest(request, parms)).thenReturn("/authorisation");
        authenticationService.generateAuthorisationRequest(parms, request, response);

        assertEquals("all", session.getData().get("pst"));
        assertNotNull(session.getData().get(".client.signature"));

        verify(coordinator).getAuthoriseUriFromRequest(request, parms);
    }

    @Test
    void testValidateAndRedirectRequest() throws  UnauthorisedException {
        Map<String, String> parms = new HashMap<>();

        when(coordinator.getPostCallbackRedirectURL(response, parms)).thenReturn("/authorisation");
        authenticationService.validateAndRedirectRequest(parms, response);
        verify(coordinator).getPostCallbackRedirectURL(response, parms);


    }
}
