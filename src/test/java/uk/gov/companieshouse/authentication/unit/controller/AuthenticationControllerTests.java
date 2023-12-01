package uk.gov.companieshouse.authentication.unit.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.Model;
import uk.gov.companieshouse.authentication.controller.AuthenticationController;
import uk.gov.companieshouse.authentication.models.AuthenticatedEmailAddress;
import uk.gov.companieshouse.authentication.service.AuthenticationService;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;


@ExtendWith(MockitoExtension.class)
class AuthenticationControllerTests {

    private static final String AUTHENTICATE_TEMPLATE = "authenticate/authenticate";
    private static final String ENTER_EMAIL_TEMPLATE = "authenticate/enter-email";
    @InjectMocks
    private AuthenticationController controller;

    @Mock
    private Model model;

    @Mock
    AuthenticationService authenticationService;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }


    @Test
    void getAuthenticationTemplate() {
        String pageTemplate = controller.getAuthenticate(model);
        assertEquals(AUTHENTICATE_TEMPLATE, pageTemplate);
    }

    @Test
    void getEmailAuthenticationTemplate() {
        model.addAttribute("authenticatedEmailAddress", new AuthenticatedEmailAddress());
        String pageTemplate = controller.getEmail(model);
        assertEquals(ENTER_EMAIL_TEMPLATE, pageTemplate);
    }

    @Test
    void postEmail() throws Exception {
        AuthenticatedEmailAddress authenticatedEmailAddress = new AuthenticatedEmailAddress();
        authenticatedEmailAddress.setEmailAddress("test");
        Map<String, String> parms = new HashMap<>();

        doNothing().when(authenticationService).generateAuthorisationRequest(parms,request, response);
        controller.postEmail(authenticatedEmailAddress, parms, request, response);
        verify(authenticationService).generateAuthorisationRequest(parms, request, response);
    }

}
