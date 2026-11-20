package uk.gov.companieshouse.accounts.association.exceptions;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static uk.gov.companieshouse.accounts.association.AccountsAssociationServiceApplication.APPLICATION_NAMESPACE;

public class UnauthorisedRuntimeException extends RuntimeException {

    private static final Logger LOG = LoggerFactory.getLogger( APPLICATION_NAMESPACE );

    public UnauthorisedRuntimeException(final String xRequestId, final String exceptionMessage, final Exception loggingMessage ) {
        super( exceptionMessage );
        LOG.errorContext( xRequestId, loggingMessage, null );
    }

}
