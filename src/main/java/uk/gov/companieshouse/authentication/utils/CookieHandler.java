package uk.gov.companieshouse.authentication.utils;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.Cookie;
import org.apache.tomcat.util.codec.binary.Base64;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.authentication.AuthenticationServiceApplication;
import uk.gov.companieshouse.encryption.jwe.CHJweEncrypt;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.security.SecureRandom;
import java.util.Objects;

@Component
public class CookieHandler {
    protected static final Logger LOGGER = LoggerFactory
            .getLogger(AuthenticationServiceApplication.APPLICATION_NAME_SPACE);

    @Value("${cookie.secure.only}")
    private boolean secureCookie;
    @Value("${key.oauth2.request}")
    private String oauth2RequestKey;

    public CHJweEncrypt chJweEncrypt;

    @PostConstruct
    public void init() {
        chJweEncrypt = new CHJweEncrypt(oauth2RequestKey);
    }

    /**
     * account.ch.gov.uk equivalent - https://github.com/companieshouse/account.ch.gov.uk/blob/master/lib/AccountChGovUk/Plugins/OAuth2Helper.pm#L134
     * @return a cookie populated with encoded FLP value
     */
    public Cookie generateProviderCookie(String provider) {
        LOGGER.info("Creating FLP cookie");

        try {
            JSONObject payload = new JSONObject();
            payload.put("provider", provider);

            String cookieContentEncoded = chJweEncrypt.createJWEToken(payload);
            LOGGER.info("Encoded FLP: " + cookieContentEncoded);

            Cookie c = new Cookie("__FLP", cookieContentEncoded);
            c.setPath("/");
            c.setMaxAge(400 * 86400); // 400 days is max age
            c.setHttpOnly(true);
            if (secureCookie) {
                c.setSecure(true);
            }
            return c;
        } catch (Exception e) {
            LOGGER.error("Failed to create FLP cookie ");
            return null;
        }
    }

    /**
     * @param series is a value to be encoded in the cookie
     * @return a cookie populated with encoded ZXS value
     */
    public Cookie generateSignedInCookie(String series) {
        var payload = new JSONObject();

        payload.put("series", series);

        // always generate a token to make sure any previous "remembered" logins are
        // forgotten about if we're not setting an auto-login
        var random = new SecureRandom();
        var bytes = new byte[6];
        random.nextBytes(bytes);
        payload.put("token", Base64.encodeBase64URLSafeString(bytes));

        String email = "demo@ch.gov.uk"; // TODO get email address from OneLogin data
        payload.put("email", email);

        String cookieContentEncoded = chJweEncrypt.createJWEToken(payload);
        LOGGER.trace("Encoded ZXS: " + cookieContentEncoded);
        LOGGER.trace("ZXS Series: " + payload.get("series"));
        LOGGER.trace("ZXS Token: " + payload.get("token"));
        LOGGER.trace("ZXS Email: " + payload.get("email"));

        Cookie c = new Cookie("__ZXS", cookieContentEncoded);
        c.setPath("/");
        c.setHttpOnly(true);
        if (secureCookie) {
            c.setSecure(true);
        }
        return c;
    }
}
