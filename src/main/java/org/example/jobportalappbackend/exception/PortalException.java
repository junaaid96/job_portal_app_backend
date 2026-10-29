package org.example.jobportalappbackend.exception;

/**
 * A failure we know how to explain to the client. The class is {@code sealed}: the only subclasses are the ones
 * listed in {@code permits}. The exception handler can then use a {@code switch} that the compiler checks for
 * completeness. Adding a new failure means adding a permitted subclass and a branch, not a forgotten status code.
 */
public abstract sealed class PortalException extends RuntimeException
        permits JobNotFoundException, ForbiddenJobAccessException, DuplicateUsernameException {

    protected PortalException(String message) {
        super(message);
    }
}
