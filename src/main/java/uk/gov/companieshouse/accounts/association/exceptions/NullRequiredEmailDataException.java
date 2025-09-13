package uk.gov.companieshouse.accounts.association.exceptions;

import uk.gov.companieshouse.accounts.association.utils.MessageType;

public class NullRequiredEmailDataException extends RuntimeException {

    /**
     * Constructs a new exception with the specified detail message.  The cause is not initialized, and
     * may subsequently be initialized by a call to {@link #initCause}.
     *
     * @param message the detail message. The detail message is saved for later retrieval by the
     *                {@link #getMessage()} method.
     */
    public NullRequiredEmailDataException(String message, MessageType messageType) {
        super(String.format("%s - message type: %s", message, messageType));
    }
}
