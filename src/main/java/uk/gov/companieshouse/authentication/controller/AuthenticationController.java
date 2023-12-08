package uk.gov.companieshouse.authentication.controller;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import uk.gov.companieshouse.authentication.exceptions.JweException;
import uk.gov.companieshouse.authentication.models.AuthenticatedEmailAddress;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import uk.gov.companieshouse.authentication.service.AuthenticationService;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
@Controller
@RequestMapping({"/authentication"})
public class AuthenticationController extends BaseController {

    private static final String AUTHENTICATE_TEMPLATE = "authenticate/authenticate";
    private static final String ENTER_EMAIL_TEMPLATE = "authenticate/enter-email";

    @Autowired
    AuthenticationService authenticationService;

    @Override
    public String getTemplateName() {
        return AUTHENTICATE_TEMPLATE;
    }

    @GetMapping
    public String getAuthenticate(Model model) {
        return AUTHENTICATE_TEMPLATE;
    }

    @GetMapping(path = "/email")
    public String getEmail(Model model) {
        model.addAttribute("authenticatedEmailAddress", new AuthenticatedEmailAddress());
        return ENTER_EMAIL_TEMPLATE;
    }

    
    @PostMapping(path = "/email")
    public void postEmail(
            @ModelAttribute("authenticatedEmailAddress") AuthenticatedEmailAddress emailAddress,
            final Map<String, String> allParams,
            final HttpServletRequest request,
            final HttpServletResponse response) throws IOException, JweException {
        HashMap<String, Object> logMap = new HashMap();
        logMap.put("AuthenticatedEmailAddress", emailAddress.getEmailAddress());
        authenticationService.generateAuthorisationRequest(allParams, request, response);
    }

}
