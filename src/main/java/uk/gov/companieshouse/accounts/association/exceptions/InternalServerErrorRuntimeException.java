package uk.gov.companieshouse.accounts.association.exceptions;

import static uk.gov.companieshouse.accounts.association.utils.LoggingUtil.LOGGER;

public class InternalServerErrorRuntimeException extends RuntimeException {

    public InternalServerErrorRuntimeException(final String xRequestId, final String exceptionMessage, final Exception loggingMessage ) {
        super( exceptionMessage );
        LOGGER.errorContext( xRequestId, loggingMessage, null );
    }

}


