package org.example.jobportalappbackend.exception;

/**
 * The caller is logged in, but they are not the account that created the job.
 * The message is chosen by the service ("edit" vs "delete") so it stays the same sentence as before.
 */
public final class ForbiddenJobAccessException extends PortalException {

    public ForbiddenJobAccessException(String message) {
        super(message);
    }
}
