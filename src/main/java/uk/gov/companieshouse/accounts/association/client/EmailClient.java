package uk.gov.companieshouse.accounts.association.client;


import org.springframework.stereotype.Component;
import uk.gov.companieshouse.accounts.association.exceptions.EmailSendException;
import uk.gov.companieshouse.api.InternalApiClient;
import uk.gov.companieshouse.api.chskafka.SendEmail;
import uk.gov.companieshouse.api.error.ApiErrorResponseException;
import uk.gov.companieshouse.api.model.ApiResponse;

import java.util.function.Supplier;

import static java.lang.String.format;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;


@Component
public class EmailClient {


    Supplier<InternalApiClient> internalApiClientSupplier;

    public EmailClient(
            Supplier<InternalApiClient> internalApiClientSupplier) {
        this.internalApiClientSupplier = internalApiClientSupplier;
    }

    public void sendEmail(SendEmail sendEmail, String xRequestId) throws EmailSendException {
        try {
            var internalApiClient = internalApiClientSupplier.get();
            internalApiClient.getHttpClient().setRequestId(xRequestId);
            LOGGER.info(format("Sending email with request id: %s and data: %s",
                    xRequestId, sendEmail.getJsonData()));
            var emailHandler = internalApiClient.sendEmailHandler();
            var emailPost = emailHandler.postSendEmail("/send-email", sendEmail);
            ApiResponse<Void> response = emailPost.execute();
            LOGGER.info(format("Posted '%s' email to CHS Kafka API: Response %d, Request ID: %s",
                    sendEmail.getMessageType(), response.getStatusCode(), xRequestId));
        } catch (ApiErrorResponseException ex) {
            LOGGER.error(String.format("Error sending email with  data: %s and request-id: %s", sendEmail.getJsonData(), xRequestId), ex);
            throw new EmailSendException(ex.getMessage());
        }
    }

}
