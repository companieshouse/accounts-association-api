package uk.gov.companieshouse.accounts.association.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.accounts.association.exceptions.EmailSendException;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.chskafka.SendEmail;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.handler.chskafka.PrivateSendEmailHandler;
import uk.gov.companieshouse.api.handler.chskafka.request.PrivateSendEmailPost;
import uk.gov.companieshouse.api.http.HttpClient;
import uk.gov.companieshouse.api.model.ApiResponse;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit-test")
@ExtendWith(MockitoExtension.class)
class EmailClientTest {

    public static final String KEY_VALUE = "{\"key\":\"value\"}";
    public static final String TEST_REQUEST_ID = "test-request-id";
    public static final String SEND_EMAIL_URL = "/send-email";
    private static final String TEST_MESSAGE = "test-message";
    private EmailClient emailClient;

    @Mock
    private Supplier<InternalApiClient> internalApiClientSupplier;

    @Mock
    private InternalApiClient internalApiClient;

    @Mock
    private PrivateSendEmailHandler privateSendEmailHandler;

    @Mock
    private PrivateSendEmailPost privateSendEmailPost;
    @Mock
    HttpClient httpClient;
    @Mock
    private ApiResponse<Void> apiResponse;

    @BeforeEach
    void setUp() {
        emailClient = new EmailClient(internalApiClientSupplier);
        when(internalApiClientSupplier.get()).thenReturn(internalApiClient);
        when(internalApiClient.getHttpClient()).thenReturn(httpClient);
        when(internalApiClient.sendEmailHandler()).thenReturn(privateSendEmailHandler);
    }

    @Test
    void shouldSendEmailSuccessfully() throws Exception {
        // Arrange
        SendEmail sendEmail = new SendEmail();
        sendEmail.setMessageType(TEST_MESSAGE);
        sendEmail.setJsonData(KEY_VALUE);

        when(privateSendEmailHandler.postSendEmail(SEND_EMAIL_URL, sendEmail)).thenReturn(privateSendEmailPost);
        when(privateSendEmailPost.execute()).thenReturn(apiResponse);
        when(apiResponse.getStatusCode()).thenReturn(200);

        // Act
        emailClient.sendEmail(sendEmail, TEST_REQUEST_ID);

        // Assert
        verify(internalApiClient).getHttpClient();
        verify(privateSendEmailHandler).postSendEmail(SEND_EMAIL_URL, sendEmail);
        verify(privateSendEmailPost).execute();
    }

    @Test
    void shouldThrowEmailSendExceptionWhenApiErrorOccurs() throws Exception {
        // Arrange
        SendEmail sendEmail = new SendEmail();
        sendEmail.setMessageType(TEST_MESSAGE);
        sendEmail.setJsonData(KEY_VALUE);

        when(privateSendEmailHandler.postSendEmail(SEND_EMAIL_URL, sendEmail)).thenReturn(privateSendEmailPost);
        when(privateSendEmailPost.execute()).thenThrow(ApiErrorResponseException.class);

        // Act & Assert
        assertThrows(EmailSendException.class, () -> emailClient.sendEmail(sendEmail, TEST_REQUEST_ID));
        verify(privateSendEmailPost).execute();
    }
}