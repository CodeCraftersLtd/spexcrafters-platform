package com.spexcrafters.supplier.api;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

/**
 * An authorized, backend-streamed evidence download. The URL never leaves the server; the
 * controller copies {@code stream} to the response and closes it. {@code filename} is the
 * sanitized original filename for the {@code Content-Disposition} header.
 */
public record EvidenceDownload(InputStream stream, long contentLength, String contentType, String filename)
        implements Closeable {

    @Override
    public void close() throws IOException {
        stream.close();
    }
}
