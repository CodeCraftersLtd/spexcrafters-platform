'use client';

import { useRouter } from 'next/navigation';
import { useTranslations } from 'next-intl';
import { useRef, useState, type DragEvent } from 'react';

import type { Evidence, EvidenceUploadTicket } from '@spexcrafters/api-client';
import { Alert, Button } from '@spexcrafters/ui';

import type { Translator } from '@/i18n/translator';
import { sendJson } from '@/lib/csrf-client';
import { readBffError } from '@/features/auth/client-errors';
import { translateSupplierError } from '@/features/suppliers/errors';
import {
  EVIDENCE_ACCEPT_ATTR,
  validateEvidenceFile,
  type EvidenceValidationError,
} from '@/features/suppliers/evidence';
import { EVIDENCE_TYPE_CODES, taxonomyLabel } from '@/features/suppliers/taxonomy';

import styles from './supplier.module.css';

interface EvidenceSectionProps {
  supplierId: string;
  evidence: Evidence[];
  canUpload: boolean;
  canDelete: boolean;
}

/** Upload the bytes directly to storage with progress (never through the BFF). */
function putToStorage(
  ticket: EvidenceUploadTicket,
  file: File,
  onProgress: (percent: number) => void,
): Promise<void> {
  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open(ticket.method || 'PUT', ticket.url, true);
    for (const [name, value] of Object.entries(ticket.requiredHeaders ?? {})) {
      xhr.setRequestHeader(name, value);
    }
    xhr.upload.onprogress = (event) => {
      if (event.lengthComputable) {
        onProgress(Math.round((event.loaded / event.total) * 100));
      }
    };
    xhr.onload = () =>
      xhr.status >= 200 && xhr.status < 300
        ? resolve()
        : reject(new Error(`storage ${xhr.status}`));
    xhr.onerror = () => reject(new Error('network'));
    xhr.send(file);
  });
}

export function EvidenceSection({
  supplierId,
  evidence,
  canUpload,
  canDelete,
}: EvidenceSectionProps) {
  const t = useTranslations('evidence');
  const taxonomy = useTranslations('taxonomy') as unknown as Translator;
  const serverErrors = useTranslations('errors.server') as unknown as Translator;

  const router = useRouter();
  const inputRef = useRef<HTMLInputElement>(null);
  const [evidenceType, setEvidenceType] = useState<string>(EVIDENCE_TYPE_CODES[0]);
  const [dragActive, setDragActive] = useState(false);
  const [validationError, setValidationError] = useState<EvidenceValidationError | null>(null);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [progress, setProgress] = useState<number | null>(null);
  const [uploaded, setUploaded] = useState(false);
  const [lastFile, setLastFile] = useState<File | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  function validationMessage(error: EvidenceValidationError): string {
    return error.values
      ? t(`error.${error.code}`, error.values)
      : t(`error.${error.code}`);
  }

  async function upload(file: File) {
    setValidationError(null);
    setUploadError(null);
    setUploaded(false);
    const invalid = validateEvidenceFile(file);
    if (invalid) {
      setValidationError(invalid);
      return;
    }
    setLastFile(file);
    setProgress(0);
    try {
      // 1. Initiate — obtain a presigned direct-to-storage ticket.
      const initiate = await sendJson(
        `/api/supplier/${encodeURIComponent(supplierId)}/evidence/initiate-upload`,
        'POST',
        { evidenceTypeCode: evidenceType, filename: file.name, mediaType: file.type },
      );
      if (!initiate.ok) {
        setUploadError(translateSupplierError(await readBffError(initiate), serverErrors));
        setProgress(null);
        return;
      }
      const ticket = (await initiate.json()) as EvidenceUploadTicket;

      // 2. PUT the bytes straight to storage.
      await putToStorage(ticket, file, setProgress);

      // 3. Finalize — server verifies size, sha256, magic bytes.
      const finalize = await sendJson(
        `/api/supplier/${encodeURIComponent(supplierId)}/evidence/${encodeURIComponent(ticket.evidenceId)}/finalize`,
        'POST',
        {},
      );
      if (!finalize.ok) {
        setUploadError(t('error.finalizeFailed'));
        setProgress(null);
        return;
      }
      setUploaded(true);
      setProgress(null);
      setLastFile(null);
      router.refresh();
    } catch {
      setUploadError(t('error.uploadFailed'));
      setProgress(null);
    }
  }

  function onDrop(event: DragEvent<HTMLDivElement>) {
    event.preventDefault();
    setDragActive(false);
    const file = event.dataTransfer.files?.[0];
    if (file) {
      void upload(file);
    }
  }

  async function remove(id: string) {
    setUploadError(null);
    setBusyId(id);
    try {
      const response = await sendJson(
        `/api/supplier/${encodeURIComponent(supplierId)}/evidence/${encodeURIComponent(id)}`,
        'DELETE',
      );
      if (response.ok || response.status === 204) {
        router.refresh();
        return;
      }
      setUploadError(translateSupplierError(await readBffError(response), serverErrors));
    } finally {
      setBusyId(null);
    }
  }

  const hasUnscanned = evidence.some((item) => item.scanStatus !== 'CLEAN');

  return (
    <section className={styles.stack} aria-label={t('title')}>
      <p className={styles.intro}>{t('intro')}</p>
      {hasUnscanned ? <Alert tone="warning">{t('scanBanner')}</Alert> : null}

      {evidence.length === 0 ? (
        <p className={styles.empty}>{t('empty')}</p>
      ) : (
        <ul className={styles.list}>
          {evidence.map((item) => (
            <li key={item.id} className={styles.row}>
              <div className={styles.rowMain}>
                <span className={styles.summaryValue} dir="ltr">{item.originalFilename}</span>
                <span className={styles.rowMeta}>
                  {taxonomyLabel(taxonomy, 'evidenceType', item.evidenceTypeCode)} ·{' '}
                  {t('scanLabel')}: {t(`scan.${item.scanStatus}`)} ·{' '}
                  {t('reviewLabel')}: {t(`review.${item.reviewStatus}`)}
                </span>
              </div>
              <div className={styles.rowActions}>
                {item.downloadable ? (
                  <a
                    className={styles.badge}
                    href={`/api/supplier/${encodeURIComponent(supplierId)}/evidence/${encodeURIComponent(item.id)}/download`}
                  >
                    {t('download')}
                  </a>
                ) : null}
                {canDelete ? (
                  <Button
                    variant="secondary"
                    size="sm"
                    type="button"
                    loading={busyId === item.id}
                    onClick={() => void remove(item.id)}
                  >
                    {t('remove')}
                  </Button>
                ) : null}
              </div>
            </li>
          ))}
        </ul>
      )}

      {canUpload ? (
        <div className={styles.stack}>
          {uploadError ? <Alert tone="danger">{uploadError}</Alert> : null}
          {validationError ? <Alert tone="danger">{validationMessage(validationError)}</Alert> : null}
          {uploaded ? <Alert tone="success">{t('uploaded')}</Alert> : null}

          <label htmlFor="evidence-type">{t('typeLabel')}</label>
          <select
            id="evidence-type"
            className={styles.select}
            value={evidenceType}
            onChange={(event) => setEvidenceType(event.target.value)}
          >
            {EVIDENCE_TYPE_CODES.map((code) => (
              <option key={code} value={code}>{taxonomyLabel(taxonomy, 'evidenceType', code)}</option>
            ))}
          </select>

          <div
            className={`${styles.dropzone} ${dragActive ? styles.dropzoneActive : ''}`}
            onDragOver={(event) => {
              event.preventDefault();
              setDragActive(true);
            }}
            onDragLeave={() => setDragActive(false)}
            onDrop={onDrop}
          >
            <p>{t('dropzone')}</p>
            <p className={styles.rowMeta}>{t('dropzoneHint')}</p>
            <input
              ref={inputRef}
              type="file"
              accept={EVIDENCE_ACCEPT_ATTR}
              aria-label={t('fileLabel')}
              onChange={(event) => {
                const file = event.target.files?.[0];
                if (file) {
                  void upload(file);
                }
                event.target.value = '';
              }}
            />
          </div>

          {progress !== null ? (
            <div>
              <p className={styles.rowMeta}>{t('uploading', { percent: progress })}</p>
              <div className={styles.progressTrack} role="progressbar" aria-valuenow={progress} aria-valuemin={0} aria-valuemax={100}>
                <div className={styles.progressBar} style={{ inlineSize: `${progress}%` }} />
              </div>
            </div>
          ) : null}

          {uploadError && lastFile ? (
            <div className={styles.actions}>
              <Button variant="secondary" size="sm" type="button" onClick={() => void upload(lastFile)}>
                {t('retry')}
              </Button>
            </div>
          ) : null}
        </div>
      ) : null}
    </section>
  );
}
