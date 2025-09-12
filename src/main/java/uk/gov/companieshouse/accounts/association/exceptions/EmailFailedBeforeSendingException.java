package uk.gov.companieshouse.accounts.association.exceptions;

public class EmailFailedBeforeSendingException extends RuntimeException {

    public EmailFailedBeforeSendingException(final String exceptionMessage) {
        super(exceptionMessage);
    }

}
