package org.example.jobportalappbackend.exception;

/**
 * No row for this id. The message matches the text the previous delete endpoint returned, so clients that display
 * {@code detail} still see the same sentence. The id is kept for logs; it is not required in the sentence.
 */
public final class JobNotFoundException extends PortalException {

    private final int postId;

    public JobNotFoundException(int postId) {
        super("This Job Post does not exist!");
        this.postId = postId;
    }

    public int postId() {
        return postId;
    }
}
