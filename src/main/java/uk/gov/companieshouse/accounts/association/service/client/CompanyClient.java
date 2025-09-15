package uk.gov.companieshouse.accounts.association.service.client;

import static uk.gov.companieshouse.accounts.association.models.Constants.REST_CLIENT_EXCEPTION;
import static uk.gov.companieshouse.accounts.association.models.Constants.REST_CLIENT_FINISH;
import static uk.gov.companieshouse.accounts.association.models.Constants.REST_CLIENT_START;
import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.ParsingUtil.parseJsonTo;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

import java.net.URI;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException.BadRequest;
import org.springframework.web.client.HttpClientErrorException.NotFound;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;
import uk.gov.companieshouse.accounts.association.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.accounts.association.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.api.company.CompanyDetails;

@Component
public class CompanyClient {

    private final RestClient companyRestClient;

    @Autowired
    public CompanyClient(@Qualifier("companyRestClient") RestClient companyRestClient) {
        this.companyRestClient = companyRestClient;
    }

    public CompanyDetails requestCompanyProfile(final String companyNumber, final String xRequestId) {
        final var uri = UriComponentsBuilder.newInstance()
                .path("/company/{companyNumber}/company-detail")
                .buildAndExpand(companyNumber)
                .toUri();
        return parseJsonTo(sendRequest(uri, xRequestId), CompanyDetails.class);
    }

    private String sendRequest(final URI uri, final String xRequestId) {
        try {
            LOGGER.infoContext(xRequestId, String.format(REST_CLIENT_START, uri), null);
            var response = companyRestClient.get().uri(uri).retrieve().body(String.class);
            LOGGER.infoContext(xRequestId, String.format(REST_CLIENT_FINISH, uri), null);
            return response;
        } catch (NotFound exception) {
            LOGGER.infoContext(getXRequestId(), String.format("No company found: %s", uri), null);
            throw new NotFoundRuntimeException(exception.getMessage(), exception);
        } catch (BadRequest exception) {
            LOGGER.errorContext(getXRequestId(), String.format("Bad request made: %s", uri),
                    exception, null);
            throw new InternalServerErrorRuntimeException(exception.getMessage(), exception);
        } catch (RestClientException exception) {
            LOGGER.errorContext(getXRequestId(), String.format(REST_CLIENT_EXCEPTION, uri),
                    exception, null);
            throw new InternalServerErrorRuntimeException(exception.getMessage(), exception);
        }
    }
}
