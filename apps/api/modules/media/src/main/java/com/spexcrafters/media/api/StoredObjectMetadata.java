package com.spexcrafters.media.api;

/**
 * Server-observed metadata of a stored object, read during finalize to enforce the size
 * bound and cross-check the declared content type.
 *
 * @param contentLength the object size in bytes as recorded by storage
 * @param contentType   the stored content type, or {@code null} if storage recorded none
 */
public record StoredObjectMetadata(long contentLength, String contentType) {
}
