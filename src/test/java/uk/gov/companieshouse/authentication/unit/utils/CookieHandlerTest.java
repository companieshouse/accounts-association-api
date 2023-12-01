package uk.gov.companieshouse.authentication.unit.utils;

import jakarta.servlet.http.Cookie;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.authentication.utils.CookieHandler;
import uk.gov.companieshouse.encryption.jwe.CHJweEncrypt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@ExtendWith(MockitoExtension.class)
class CookieHandlerTest {

    private final static String TEST_KEY="ZW5jcnlwdGlvbi1qYXZhLWxpYnJhcnktdGVzdC1rZXk=";
    private static CookieHandler cookieHandler = new CookieHandler();

    @BeforeAll
    public static void setup() {
        cookieHandler.chJweEncrypt = new CHJweEncrypt(TEST_KEY);
    }

    @Test
    @DisplayName("Check valid FLP cookie created")
    void generateProviderCookieSuccess() {
        Cookie c = cookieHandler.generateProviderCookie("gov_uk_one_login");
        assertEquals("__FLP", c.getName());
        assertEquals("{\"provider\":\"gov_uk_one_login\"}",cookieHandler.chJweEncrypt.decryptPayload(c.getValue()).toString());
    }

    @Test
    @DisplayName("Check valid ZXS cookie created")
    void generateSignedInCookieSuccess() {
        Cookie c = cookieHandler.generateSignedInCookie("12345");
        assertEquals("__ZXS", c.getName());
        JSONObject decryptedCookie = cookieHandler.chJweEncrypt.decryptPayload(c.getValue());
        assertEquals("12345", decryptedCookie.get("series"));
        assertNotNull(decryptedCookie.get("token"));
        assertEquals("demo@ch.gov.uk", decryptedCookie.get("email"));
    }

}
