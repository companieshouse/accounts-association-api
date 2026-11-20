package uk.gov.companieshouse.accounts.association.utils;

import static uk.gov.companieshouse.accounts.association.AccountsAssociationServiceApplication.APPLICATION_NAMESPACE;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public final class LoggingUtil {

    public static final Logger LOGGER = LoggerFactory.getLogger( APPLICATION_NAMESPACE );

    private LoggingUtil() {
    }
}
