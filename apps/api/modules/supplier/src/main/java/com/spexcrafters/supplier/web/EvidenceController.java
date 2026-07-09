package com.spexcrafters.supplier.web;

import com.spexcrafters.supplier.api.EvidenceDto;
import com.spexcrafters.supplier.api.EvidenceDownload;
import com.spexcrafters.supplier.api.EvidenceService;
import com.spexcrafters.supplier.api.EvidenceUploadTicketDto;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Evidence resource. Uploads are presigned direct-to-storage (initiate/finalize); downloads
 * are backend-streamed after a per-request authorization check (no URL leaves the server).
 */
@RestController
@RequestMapping("/api/v1/suppliers/{supplierId}/evidence")
public class EvidenceController {

    private final EvidenceService evidenceService;

    public EvidenceController(EvidenceService evidenceService) {
        this.evidenceService = evidenceService;
    }

    /** operationId: listEvidence */
    @GetMapping
    public List<EvidenceDto> listEvidence(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID supplierId) {
        return evidenceService.list(AuthenticatedUser.id(jwt), supplierId);
    }

    /** operationId: initiateEvidenceUpload */
    @PostMapping("/initiate-upload")
    @ResponseStatus(HttpStatus.CREATED)
    public EvidenceUploadTicketDto initiateEvidenceUpload(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID supplierId, @Valid @RequestBody InitiateUploadRequest request) {
        return evidenceService.initiateUpload(AuthenticatedUser.id(jwt), supplierId,
                request.evidenceTypeCode(), request.filename(), request.mediaType(), request.documentLocale());
    }

    /** operationId: finalizeEvidenceUpload */
    @PostMapping("/{evidenceId}/finalize")
    public EvidenceDto finalizeEvidenceUpload(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID supplierId, @PathVariable UUID evidenceId,
            @Valid @RequestBody(required = false) FinalizeUploadRequest request) {
        String expected = request == null ? null : request.expectedSha256();
        return evidenceService.finalizeUpload(AuthenticatedUser.id(jwt), supplierId, evidenceId, expected);
    }

    /** operationId: deleteEvidence */
    @DeleteMapping("/{evidenceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEvidence(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID supplierId,
            @PathVariable UUID evidenceId) {
        evidenceService.delete(AuthenticatedUser.id(jwt), supplierId, evidenceId);
    }

    /** operationId: downloadEvidence */
    @GetMapping("/{evidenceId}/download")
    public ResponseEntity<InputStreamResource> downloadEvidence(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID supplierId, @PathVariable UUID evidenceId) throws IOException {
        EvidenceDownload download = evidenceService.download(AuthenticatedUser.id(jwt), supplierId, evidenceId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(download.filename()).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .header("X-Evidence-Scan-State", "PENDING_SCAN")
                .contentType(MediaType.parseMediaType(download.contentType()))
                .contentLength(download.contentLength())
                .body(new InputStreamResource(download.stream()));
    }
}
