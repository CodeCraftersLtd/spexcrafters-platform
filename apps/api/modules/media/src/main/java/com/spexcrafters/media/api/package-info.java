/**
 * Public API of the media bounded context: the {@link com.spexcrafters.media.api.ObjectStorage}
 * and {@link com.spexcrafters.media.api.MalwareScanner} ports and their value objects.
 * Business modules depend on this package only — never on the AWS SDK or the adapters in
 * {@code ...infrastructure} (ArchUnit-enforced). The media module holds no persistent state:
 * object bytes live in S3-compatible storage, evidence metadata lives in the owning context.
 */
package com.spexcrafters.media.api;
