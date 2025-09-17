package uk.gov.companieshouse.accounts.association.exceptions;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;
import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;

public class BadRequestRuntimeException extends RuntimeException {

    public BadRequestRuntimeException(final String exceptionMessage) {
        super(exceptionMessage);
        LOGGER.errorContext(getXRequestId(), this, null);
    }

}
