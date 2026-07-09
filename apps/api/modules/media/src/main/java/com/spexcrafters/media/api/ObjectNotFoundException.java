package com.spexcrafters.media.api;

/**
 * Thrown when a storage operation targets an object that does not exist (e.g. finalizing an
 * upload whose bytes never arrived). The owning context translates this into its own
 * fail-closed problem — a missing object is never treated as valid evidence.
 */
public class ObjectNotFoundException extends RuntimeException {

    public ObjectNotFoundException(String key) {
        super("Stored object not found: " + key);
    }
}
