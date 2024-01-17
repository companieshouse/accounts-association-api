package uk.gov.companieshouse.accounts.association.exceptions;

public class BadRequestRuntimeException extends RuntimeException {

    public BadRequestRuntimeException(String message) {
        super(message);
    }
}
