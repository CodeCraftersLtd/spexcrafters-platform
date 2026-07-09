package com.spexcrafters.supplier.api;

import java.util.List;

/**
 * A cursor page of the reviewer queue. {@code nextCursor} is an opaque token (the last item's
 * id) or {@code null} when the queue is exhausted.
 */
public record ReviewQueuePageDto(List<ReviewQueueItemDto> items, String nextCursor) {
}
