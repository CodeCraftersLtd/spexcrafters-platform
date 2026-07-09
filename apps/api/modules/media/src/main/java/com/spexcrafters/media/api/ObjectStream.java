package com.spexcrafters.media.api;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * An open, authorized read of a stored object for backend streaming. The caller must close
 * it (try-with-resources) to release the underlying storage connection.
 *
 * @param stream        the object bytes; owned by the caller
 * @param contentLength the object size in bytes
 * @param contentType   the stored content type
 */
public record ObjectStream(InputStream stream, long contentLength, String contentType)
        implements Closeable {

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
