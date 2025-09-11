package uk.gov.companieshouse.accounts.association.exceptions;

import static uk.gov.companieshouse.accounts.association.utils.RequestContextUtil.getXRequestId;
import static uk.gov.companieshouse.accounts.association.utils.StaticPropertyUtil.APPLICATION_NAMESPACE;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class ForbiddenRuntimeException extends RuntimeException {

    private static final Logger LOG = LoggerFactory.getLogger(APPLICATION_NAMESPACE);

    public ForbiddenRuntimeException(final String exceptionMessage, final Exception loggingMessage) {
        super(exceptionMessage);
        LOG.errorContext(getXRequestId(), loggingMessage, null);
    }

}
