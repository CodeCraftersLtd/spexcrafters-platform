package com.spexcrafters.media.infrastructure;

import com.spexcrafters.media.api.ObjectNotFoundException;
import com.spexcrafters.media.api.ObjectStorage;
import com.spexcrafters.media.api.ObjectStream;
import com.spexcrafters.media.api.PresignedUpload;
import com.spexcrafters.media.api.StoredObjectMetadata;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

/**
 * S3/MinIO adapter for {@link ObjectStorage}. All operations target the single private
 * evidence bucket from configuration; the key is supplied by the caller and never derived
 * from client input. The authoritative size and checksum validation happens in the owning
 * context during finalize (via {@link #head}/{@link #readAllBytes}); the presigned PUT only
 * pins the content type and communicates the size bound to the client.
 */
@Component
class S3ObjectStorage implements ObjectStorage {

    private final S3Client s3;
    private final S3Presigner presigner;
    private final String bucket;
    private final Duration presignTtl;

    S3ObjectStorage(S3Client s3, S3Presigner presigner, S3StorageProperties properties) {
        this.s3 = s3;
        this.presigner = presigner;
        this.bucket = properties.evidenceBucket();
        this.presignTtl = properties.presignTtl();
    }

    @Override
    public PresignedUpload presignUpload(String key, String contentType, long maxBytes, Duration ttl) {
        Duration effectiveTtl = ttl != null ? ttl : presignTtl;
        PutObjectRequest put = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();
        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(effectiveTtl)
                .putObjectRequest(put)
                .build();
        PresignedPutObjectRequest presigned = presigner.presignPutObject(presignRequest);
        return new PresignedUpload(
                "PUT",
                java.net.URI.create(presigned.url().toString()),
                Instant.now().plus(effectiveTtl),
                Map.of("Content-Type", contentType),
                maxBytes);
    }

    @Override
    public Optional<StoredObjectMetadata> head(String key) {
        try {
            HeadObjectResponse response = s3.headObject(HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build());
            return Optional.of(new StoredObjectMetadata(response.contentLength(), response.contentType()));
        } catch (NoSuchKeyException ex) {
            return Optional.empty();
        }
    }

    @Override
    public byte[] readAllBytes(String key) {
        try {
            return s3.getObjectAsBytes(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build()).asByteArray();
        } catch (NoSuchKeyException ex) {
            throw new ObjectNotFoundException(key);
        }
    }

    @Override
    public ObjectStream open(String key) {
        try {
            ResponseInputStream<GetObjectResponse> stream = s3.getObject(GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(key)
                    .build(), ResponseTransformer.toInputStream());
            GetObjectResponse response = stream.response();
            return new ObjectStream(stream, response.contentLength(), response.contentType());
        } catch (NoSuchKeyException ex) {
            throw new ObjectNotFoundException(key);
        }
    }

    @Override
    public void delete(String key) {
        s3.deleteObject(builder -> builder.bucket(bucket).key(key));
    }
}
