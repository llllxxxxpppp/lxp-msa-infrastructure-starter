"use client";

import { useEffect, useRef, useState } from "react";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/Chip";
import { MaterialIcon } from "@/components/ui/MaterialIcon";
import { PolicyAiAssistModal } from "@/components/policy/PolicyAiAssistModal";
import { DocumentPreviewModal } from "@/components/policy/DocumentPreviewModal";
import { ApiError } from "@/types/api";
import { getDocumentContent, listDocuments, uploadDocument } from "@/features/policy/api";
import type { DocumentInfo } from "@/features/policy/types";

const ACCEPTED_EXTENSIONS = [".pdf", ".docx"];

function formatBytes(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  const units = ["KB", "MB", "GB"];
  let value = bytes / 1024;
  let unitIndex = 0;
  while (value >= 1024 && unitIndex < units.length - 1) {
    value /= 1024;
    unitIndex += 1;
  }
  return `${value.toFixed(1)} ${units[unitIndex]}`;
}

function fileTypeLabel(filename: string): string {
  const ext = filename.slice(filename.lastIndexOf(".")).toLowerCase();
  if (ext === ".pdf") return "PDF 문서";
  if (ext === ".docx") return "DOCX 문서";
  return "문서";
}

function statusChip(doc: DocumentInfo) {
  if (doc.status === "ready") return { tone: "success" as const, label: "Vector화 완료" };
  if (doc.status === "failed") return { tone: "error" as const, label: "처리 실패" };
  return { tone: "warning" as const, label: "처리 중" };
}

function isAcceptedFile(file: File): boolean {
  const name = file.name.toLowerCase();
  return ACCEPTED_EXTENSIONS.some((ext) => name.endsWith(ext));
}

function isPdf(filename: string): boolean {
  return filename.toLowerCase().endsWith(".pdf");
}

export default function PolicyExplorerPage() {
  const [documents, setDocuments] = useState<DocumentInfo[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isUploading, setIsUploading] = useState(false);
  const [uploadError, setUploadError] = useState<string | null>(null);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isPreviewModalOpen, setIsPreviewModalOpen] = useState(false);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [isPreviewLoading, setIsPreviewLoading] = useState(false);
  const [previewError, setPreviewError] = useState<string | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  async function loadDocuments() {
    // 업로드 완료 후 이벤트 핸들러에서 목록을 새로고침할 때 쓰는 함수다. 마운트 시 초기
    // 로드는 아래 useEffect가 직접 처리한다(set-state-in-effect 린트 때문에 분리).
    setIsLoading(true);
    setError(null);
    try {
      const docs = await listDocuments();
      setDocuments(docs);
      setSelectedId((prev) => prev ?? docs[0]?.id ?? null);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "문서 목록을 불러오지 못했습니다.");
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    // 마운트 시 최초 목록 조회 — 데이터 패칭 effect의 표준 패턴이라 set-state-in-effect
    // 규칙은 이 두 줄에 한해 비활성화한다(courses 페이지와 동일한 컨벤션).
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setIsLoading(true);
    setError(null);
    listDocuments()
      .then((docs) => {
        setDocuments(docs);
        setSelectedId((prev) => prev ?? docs[0]?.id ?? null);
      })
      .catch((err) =>
        setError(err instanceof ApiError ? err.message : "문서 목록을 불러오지 못했습니다.")
      )
      .finally(() => setIsLoading(false));
  }, []);

  async function handleUpload(files: FileList | null) {
    if (!files || files.length === 0) return;
    const fileList = Array.from(files);
    const rejected = fileList.filter((f) => !isAcceptedFile(f));
    if (rejected.length > 0) {
      setUploadError(`지원하지 않는 파일 형식입니다: ${rejected.map((f) => f.name).join(", ")}`);
      return;
    }

    setIsUploading(true);
    setUploadError(null);
    try {
      for (const file of fileList) {
        await uploadDocument(file);
      }
      await loadDocuments();
    } catch (err) {
      setUploadError(err instanceof ApiError ? err.message : "업로드에 실패했습니다.");
    } finally {
      setIsUploading(false);
    }
  }

  const selected = documents.find((d) => d.id === selectedId) ?? null;

  useEffect(() => {
    // 트리에서 선택한 문서가 바뀔 때마다 원본 파일을 받아 Preview 카드에 보여준다.
    // <iframe src="게이트웨이 절대 URL">로 직접 주면 Authorization 헤더가 안 붙어 401이
    // 나므로, fetch → Blob → object URL을 거친다. cleanup에서 이전 object URL을 해제해
    // 문서를 계속 바꿔가며 볼 때 메모리가 누적되지 않게 한다.
    // 문서를 새로 선택하면 이전 문서를 보던 확대 모달은 닫는다(내용만 바뀌는 어색함 방지).
    setIsPreviewModalOpen(false);

    if (!selectedId) {
      setPreviewUrl(null);
      setPreviewError(null);
      return;
    }

    let cancelled = false;
    let objectUrl: string | null = null;
    setIsPreviewLoading(true);
    setPreviewError(null);
    setPreviewUrl(null);

    getDocumentContent(selectedId)
      .then((blob) => {
        if (cancelled) return;
        objectUrl = URL.createObjectURL(blob);
        setPreviewUrl(objectUrl);
      })
      .catch((err) => {
        if (cancelled) return;
        setPreviewError(err instanceof ApiError ? err.message : "미리보기를 불러오지 못했습니다.");
      })
      .finally(() => {
        if (!cancelled) setIsPreviewLoading(false);
      });

    return () => {
      cancelled = true;
      if (objectUrl) URL.revokeObjectURL(objectUrl);
    };
  }, [selectedId]);

  return (
    <div className="gap-stack-lg flex h-full flex-col">
      <header className="flex items-center justify-between">
        <div>
          <h2 className="text-headline-lg text-primary">백오피스 사내 규정 관리</h2>
          <p className="mt-base text-body-md text-on-surface-variant">
            사내 규정 관련 문서 업로드 및 관리 (사내 파일 관리)
          </p>
        </div>
        <Button onClick={() => setIsModalOpen(true)}>
          <MaterialIcon name="smart_toy" className="text-[20px]" />
          AI Assistance
        </Button>
      </header>

      <div className="gap-stack-lg grid min-h-0 flex-1 grid-cols-1 lg:grid-cols-2">
        {/* 좌측: 파일 트리 + 메타 정보 */}
        <div className="gap-stack-lg flex min-h-0 flex-col">
          <Card className="flex min-h-0 flex-1 flex-col p-stack-md">
            <h3 className="mb-stack-sm shrink-0 border-b border-outline-variant pb-2 text-headline-sm text-primary">
              파일 Tree
            </h3>
            <div className="min-h-0 max-h-[55vh] flex-1 overflow-y-auto">
              <div className="mt-2 flex items-center gap-2 rounded p-1 text-on-surface">
                <MaterialIcon name="folder_open" className="text-primary" />
                <span className="text-label-md">사내 규정 문서</span>
              </div>
              {isLoading && <p className="text-body-sm text-slate-text mt-2 ml-6">불러오는 중...</p>}
              {error && <p className="text-body-sm text-error-red mt-2 ml-6">{error}</p>}
              {!isLoading && !error && documents.length === 0 && (
                <p className="text-body-sm text-slate-text mt-2 ml-6">
                  업로드된 문서가 없습니다. 우측 영역에서 파일을 업로드해 주세요.
                </p>
              )}
              {!isLoading && !error && documents.length > 0 && (
                <ul className="mt-1 ml-6 space-y-1 border-l border-outline-variant pl-2">
                  {documents.map((doc) => (
                    <li
                      key={doc.id}
                      onClick={() => setSelectedId(doc.id)}
                      className={`flex cursor-pointer items-center gap-2 rounded p-1 ${
                        selectedId === doc.id
                          ? "bg-secondary-fixed text-on-secondary-fixed"
                          : "text-on-surface hover:bg-surface-container"
                      }`}
                    >
                      <MaterialIcon name="description" className="text-[18px]" />
                      <span className="text-body-sm">{doc.original_filename}</span>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </Card>

          <Card className="p-stack-md">
            <h3 className="mb-stack-sm border-outline-variant text-headline-sm text-primary border-b pb-2">
              파일 메타 정보
            </h3>
            {selected ? (
              <div className="text-body-sm mt-2 flex flex-col gap-2">
                <MetaRow label="파일명" value={selected.original_filename} />
                <MetaRow label="종류" value={fileTypeLabel(selected.original_filename)} />
                <MetaRow label="크기" value={formatBytes(selected.size_bytes)} />
                <MetaRow
                  label="업로드일"
                  value={new Date(selected.uploaded_at).toLocaleDateString("ko-KR")}
                />
                <div className="flex items-start justify-between pt-2">
                  <span className="text-on-surface-variant w-20 shrink-0">상태</span>
                  <Chip tone={statusChip(selected).tone}>{statusChip(selected).label}</Chip>
                </div>
                {selected.status === "failed" && selected.error_message && (
                  <p className="text-body-sm text-error-red">{selected.error_message}</p>
                )}
              </div>
            ) : (
              <p className="text-body-sm text-slate-text mt-2">선택된 문서가 없습니다.</p>
            )}
          </Card>
        </div>

        {/* 우측: 업로드 + 미리보기 */}
        <div className="gap-stack-lg flex min-h-0 flex-col">
          <input
            ref={fileInputRef}
            type="file"
            accept={ACCEPTED_EXTENSIONS.join(",")}
            multiple
            className="hidden"
            onChange={(e) => {
              handleUpload(e.target.files);
              e.target.value = "";
            }}
          />
          <div
            onClick={() => fileInputRef.current?.click()}
            onDragOver={(e) => e.preventDefault()}
            onDrop={(e) => {
              e.preventDefault();
              handleUpload(e.dataTransfer.files);
            }}
            className="border-outline-variant bg-surface-container-lowest p-stack-lg hover:border-secondary hover:bg-surface-container-low flex h-48 shrink-0 cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed transition-colors"
          >
            <div className="bg-surface-container mb-4 flex h-16 w-16 items-center justify-center rounded-full">
              <MaterialIcon name="cloud_upload" className="text-outline text-[32px]" />
            </div>
            <h3 className="text-headline-md text-primary mb-1">Drag &amp; Drop</h3>
            <p className="text-body-md text-on-surface-variant">
              {isUploading ? "업로드 중..." : "파일 업로드 (PDF, DOCX)"}
            </p>
            {uploadError && <p className="text-body-sm text-error-red mt-2">{uploadError}</p>}
          </div>

          <Card className="flex min-h-0 flex-1 flex-col p-stack-md">
            <div className="mb-stack-sm flex shrink-0 items-center justify-between border-b border-outline-variant pb-2">
              <h3 className="text-headline-sm text-primary">Preview (파일 미리보기)</h3>
              {selected && (
                <button
                  type="button"
                  onClick={() => setIsPreviewModalOpen(true)}
                  aria-label="전체화면으로 보기"
                  className="text-outline hover:text-primary"
                >
                  <MaterialIcon name="fullscreen" />
                </button>
              )}
            </div>
            {!selected ? (
              <div className="mt-2 flex min-h-0 flex-1 flex-col items-center justify-center gap-2 rounded border border-outline-variant bg-surface p-stack-lg text-center text-on-surface-variant">
                <MaterialIcon name="visibility" className="mb-2 text-[64px] text-outline-variant" />
                <p>- PDF 등 문서면 내용 미리 보기</p>
                <p>- mp4 등 영상이면 영상 내용 및 프리뷰</p>
              </div>
            ) : isPreviewLoading ? (
              <div className="mt-2 flex min-h-0 flex-1 flex-col items-center justify-center gap-2 rounded border border-outline-variant bg-surface p-stack-lg text-center text-on-surface-variant">
                <p>불러오는 중...</p>
              </div>
            ) : previewError ? (
              <div className="mt-2 flex min-h-0 flex-1 flex-col items-center justify-center gap-2 rounded border border-outline-variant bg-surface p-stack-lg text-center text-error-red">
                <p>{previewError}</p>
              </div>
            ) : previewUrl && isPdf(selected.original_filename) ? (
              <iframe
                src={previewUrl}
                title={selected.original_filename}
                className="mt-2 min-h-0 w-full flex-1 rounded border border-outline-variant"
              />
            ) : previewUrl ? (
              <div className="mt-2 flex min-h-0 flex-1 flex-col items-center justify-center gap-2 rounded border border-outline-variant bg-surface p-stack-lg text-center text-on-surface-variant">
                <MaterialIcon name="description" className="mb-2 text-[64px] text-outline-variant" />
                <p>이 형식은 미리보기를 지원하지 않습니다.</p>
                <a
                  href={previewUrl}
                  download={selected.original_filename}
                  className="text-primary underline"
                >
                  다운로드
                </a>
              </div>
            ) : null}
          </Card>
        </div>
      </div>

      {isModalOpen && <PolicyAiAssistModal onClose={() => setIsModalOpen(false)} />}
      {isPreviewModalOpen && selected && (
        <DocumentPreviewModal
          filename={selected.original_filename}
          previewUrl={previewUrl}
          isLoading={isPreviewLoading}
          error={previewError}
          onClose={() => setIsPreviewModalOpen(false)}
        />
      )}
    </div>
  );
}

function MetaRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="border-surface-container-high flex items-start justify-between border-b pb-2">
      <span className="text-on-surface-variant w-20 shrink-0">{label}</span>
      <span className="text-on-surface text-right break-all">{value}</span>
    </div>
  );
}
