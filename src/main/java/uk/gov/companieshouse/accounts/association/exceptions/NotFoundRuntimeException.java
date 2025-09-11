package uk.gov.companieshouse.accounts.association.exceptions;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

public class NotFoundRuntimeException extends RuntimeException {

    public NotFoundRuntimeException(final String exceptionMessage, final Exception loggingMessage) {
        super(exceptionMessage);
        LOGGER.errorContext(getXRequestId(), loggingMessage, null);
    }

}


