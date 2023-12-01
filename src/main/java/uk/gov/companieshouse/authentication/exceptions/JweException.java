package uk.gov.companieshouse.authentication.exceptions;

public class JweException extends Exception {
    public JweException(String error) {
        super(error);
    }
}

