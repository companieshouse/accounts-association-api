package uk.gov.companieshouse.authentication.controller;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.gov.ch.oauth.exceptions.UnauthorisedException;
import uk.gov.companieshouse.authentication.service.AuthenticationService;
import uk.gov.companieshouse.authentication.utils.CookieHandler;
import java.io.IOException;
import uk.gov.companieshouse.encryption.jwe.CHJweEncrypt;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Objects;

@Controller
@RequestMapping({"/authentication/oauth2"})
public class Oauth2Controller extends BaseController {

    @Value("${key.oauth2.request}")
    private String oauth2RequestKey;

    @Autowired
    AuthenticationService authenticationService;

    @Autowired
    CookieHandler cookieHandler;

    @Override
    public String getTemplateName() {
        return null;
    }

    public CHJweEncrypt chJweEncrypt;

    @PostConstruct
    public void init() {
        chJweEncrypt = new CHJweEncrypt(oauth2RequestKey);
    }

    //callback after oauth2 is complete
    @GetMapping(path = "/callback/one-login")
    public void callbackOneLogin(@RequestParam final Map<String, String> allParams,
                                   @CookieValue(name = "__ZXS", required = false) String zxs,
                                   final HttpServletResponse response) throws UnauthorisedException, IOException {

        // TODO handle error responses from OneLogin

        response.sendRedirect(authenticationService.validateAndRedirectRequest(allParams, response));
    }

    // TODO this endpoint should be removed once proper signin journey implemented
    @GetMapping(path = "/callback/test")
    public String callbackTest(@CookieValue(name = "__ZXS", required = false) String zxs,
            final HttpServletResponse response) {

        // Create provider cookie
        Cookie flpCookie = cookieHandler.generateProviderCookie("gov-uk-one-login");
        if (Objects.nonNull(flpCookie)) {
            response.addCookie(flpCookie);
        }

        var zxsSeries = "";

        // check if ZXS cookie already exists
        // original implementation: https://github.com/companieshouse/account.ch.gov.uk/blob/master/lib/AccountChGovUk/Plugins/OAuth2Helper.pm#L164-L168
        if (zxs != null) {
            LOGGER.trace("Existing __ZXS cookie received: " + zxs);
            JSONObject decodedZXS;
            decodedZXS = chJweEncrypt.decryptPayload(zxs);
            LOGGER.trace("Previous Series: " + decodedZXS.getString("series"));
            LOGGER.trace("Previous Token: " + decodedZXS.getString("token"));
            LOGGER.trace("Previous Email: " + decodedZXS.getString("email"));
            zxsSeries = decodedZXS.getString("series");
        } else {
            var random = new SecureRandom();
            var bytes = new byte[6];
            random.nextBytes(bytes);
            zxsSeries = Base64.encodeBase64URLSafeString(bytes);
        }

        var zxsCookie = cookieHandler.generateSignedInCookie(zxsSeries);
        if (Objects.nonNull(zxsCookie)) {
            response.addCookie(zxsCookie);
        }

        // TODO update .zxs_key in Redis session

        return "redirect:/authentication";
    }
}
