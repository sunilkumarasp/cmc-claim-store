package uk.gov.hmcts.cmc.claimstore.exceptions;

public class InvalidApplicationException extends RuntimeException {
    public InvalidApplicationException() {
        super("");
    }

    public InvalidApplicationException(String message) {
        super(message);
    }

    public InvalidApplicationException(String message, Throwable throwable) {
        super(message, throwable);
    }
}
