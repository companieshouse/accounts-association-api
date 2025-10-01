package uk.gov.companieshouse.accounts.association.exceptions;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;

public class BadRequestRuntimeException extends RuntimeException {

    public BadRequestRuntimeException( final String xRequestId, final String exceptionMessage, final Exception loggingMessage ) {
        super( exceptionMessage );
        LOGGER.errorContext( xRequestId, loggingMessage, null );
    }

}
