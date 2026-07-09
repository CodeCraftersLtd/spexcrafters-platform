package com.spexcrafters.media.api;

import java.time.Duration;
import java.util.Optional;

/**
 * Port over the private evidence object store (ADR-023). Keys are server-generated and
 * opaque; the port never derives keys from client input and never exposes public URLs.
 * Buckets and credentials are an adapter concern — callers pass only the safe key.
 *
 * <p>The staged upload protocol is: {@link #presignUpload} (server authorizes and returns a
 * short-lived, constrained PUT) → client PUTs bytes → the caller {@link #head}s / {@link
 * #readAllBytes} to validate and finalize server-side.
 */
public interface ObjectStorage {

    /**
     * Mints a short-lived presigned {@code PUT} for a single object, constrained to the
     * declared content type and an upper size bound. The URL and any signed headers are
     * secrets — never log them.
     */
    PresignedUpload presignUpload(String key, String contentType, long maxBytes, Duration ttl);

    /** Metadata of a stored object, or empty if the object does not exist. */
    Optional<StoredObjectMetadata> head(String key);

    /**
     * Reads the whole object into memory. Evidence objects are small documents/images with
     * an enforced size cap, so this is safe and is used to compute the sha256 and validate
     * magic bytes during finalize. Throws {@link ObjectNotFoundException} if absent.
     */
    byte[] readAllBytes(String key);

    /**
     * Opens the object for authorized backend streaming to a response. The caller owns the
     * returned stream and must close it. Throws {@link ObjectNotFoundException} if absent.
     */
    ObjectStream open(String key);

    /** Deletes the object. Idempotent: deleting a missing object is not an error. */
    void delete(String key);
}
