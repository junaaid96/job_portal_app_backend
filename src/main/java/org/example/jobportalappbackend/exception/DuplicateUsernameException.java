package org.example.jobportalappbackend.exception;

/** {@code user_name} already exists. Mapped to HTTP 409. */
public final class DuplicateUsernameException extends PortalException {

    public DuplicateUsernameException(String message) {
        super(message);
    }
}
