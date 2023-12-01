package uk.gov.companieshouse.authentication.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.ch.oauth.IOAuthCoordinator;
import uk.gov.ch.oauth.exceptions.UnauthorisedException;
import uk.gov.companieshouse.session.Session;
import uk.gov.companieshouse.session.handler.SessionHandler;
import java.io.IOException;
import java.util.Map;

@Service
public class AuthenticationService {
    @Autowired
    IOAuthCoordinator coordinator;

    @Value("${cookie.secret}")
    private String cookieSecret;


    public void generateAuthorisationRequest(final Map<String, String> allParams,
                                             final HttpServletRequest request,
                                             final HttpServletResponse response) throws IOException {


        final String authoriseUri = coordinator.getAuthoriseUriFromRequest(request, allParams);
        updateSession(request);
        //TODO redirect via a oidcClient not old login.
        response.sendRedirect(authoriseUri);
    }

    public String validateAndRedirectRequest(final Map<String, String> allParams,
                                     final HttpServletResponse response) throws UnauthorisedException {
        return coordinator.getPostCallbackRedirectURL(response, allParams);
    }

    private void updateSession (HttpServletRequest request){
        final Session chSession = (Session) request
                .getAttribute(SessionHandler.CHS_SESSION_REQUEST_ATT_KEY);

        Map<String, Object> sessionData = chSession.getData();


        String agent = request.getHeader("user-agent");
        String clientIp = request.getRemoteAddr();
        String secret = cookieSecret;

        String sig = DigestUtils.sha1Hex(agent + clientIp + secret);
        sessionData.put(".client.signature", sig);
        sessionData.put("pst", "all");
        chSession.store();

    }

}

