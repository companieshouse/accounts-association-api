package uk.gov.companieshouse.authentication.integration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import uk.gov.ch.oauth.IOAuthCoordinator;
import uk.gov.companieshouse.authentication.controller.AuthenticationController;
import uk.gov.companieshouse.session.Session;
import uk.gov.companieshouse.session.handler.SessionHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
class AuthenticationControllerTest {

    @InjectMocks
    private AuthenticationController controller;
    @Autowired
    private MockMvc mvc;

    @MockBean
    protected SecurityFilterChain filterChain;

    @MockBean
    protected IOAuthCoordinator coordinator;

    @MockBean
    Session session;

    @Test
    @DisplayName("Template successfully rendered")
    void getTemplate() throws Exception {
        this.mvc.perform(get("/authentication"))
                .andExpect(status().isOk())
                .andExpect(view().name(controller.getTemplateName()));
    }

    @Test
    @DisplayName("Integration - Test enter email address page is returned.")
    void getLandingPageTest() throws Exception {
        this.mvc.perform(get("/authentication/email"))
                .andExpect(status().isOk())
                .andExpect(view().name("authenticate/enter-email"));
    }

    @Test
    @DisplayName("Integration - Test Post to authorisation url will redirect user")
    void buildAuthorisationRequest() throws Exception {

        doNothing().when(session).store();
        when(coordinator.getAuthoriseUriFromRequest(any(), any())).thenReturn("/redirectURI");

        MvcResult result = this.mvc.perform(post("/authentication/email")
                        .requestAttr(SessionHandler.CHS_SESSION_REQUEST_ATT_KEY, session)
                )
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/redirectURI"))
                .andReturn();

        assertEquals("", result.getResponse().getContentAsString());
        verify(coordinator).getAuthoriseUriFromRequest(any(), any());

    }
}
