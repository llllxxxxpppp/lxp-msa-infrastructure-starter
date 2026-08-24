"use client";

import { MaterialIcon } from "@/components/ui/MaterialIcon";

interface DocumentPreviewModalProps {
  filename: string;
  previewUrl: string | null;
  isLoading: boolean;
  error: string | null;
  onClose: () => void;
}

function isPdf(filename: string): boolean {
  return filename.toLowerCase().endsWith(".pdf");
}

/**
 * "파일 Tree" 옆 Preview 카드가 좁아서 문서를 크게 보고 싶을 때 쓰는 전체화면 미리보기 모달.
 * `PolicyAiAssistModal`과 같은 `fixed inset-0` 오버레이 패턴을 그대로 재사용한다.
 *
 * 파일을 새로 fetch하지 않는다 — 호출부(`policy-explorer/page.tsx`)가 이미 들고 있는
 * previewUrl/isLoading/error 상태를 그대로 받아 더 큰 화면에 보여주기만 한다.
 */
export function DocumentPreviewModal({
  filename,
  previewUrl,
  isLoading,
  error,
  onClose,
}: DocumentPreviewModalProps) {
  return (
    <div className="bg-primary/40 p-margin-mobile fixed inset-0 z-50 flex items-center justify-center backdrop-blur-sm">
      <div className="border-outline-variant bg-surface-container-lowest flex h-[90vh] w-full max-w-5xl flex-col overflow-hidden rounded-xl border shadow-2xl">
        <div className="border-outline-variant bg-surface-container-low p-stack-md flex items-center justify-between border-b">
          <h2 className="text-headline-sm text-primary truncate">{filename}</h2>
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="text-outline hover:text-primary"
          >
            <MaterialIcon name="close" />
          </button>
        </div>
        <div className="min-h-0 flex-1 p-stack-md">
          {isLoading ? (
            <p className="text-body-sm text-slate-text">불러오는 중...</p>
          ) : error ? (
            <p className="text-body-sm text-error-red">{error}</p>
          ) : previewUrl && isPdf(filename) ? (
            <iframe
              src={previewUrl}
              title={filename}
              className="h-full w-full rounded border border-outline-variant"
            />
          ) : previewUrl ? (
            <div className="flex h-full flex-col items-center justify-center gap-2 text-center text-on-surface-variant">
              <MaterialIcon name="description" className="mb-2 text-[64px] text-outline-variant" />
              <p>이 형식은 미리보기를 지원하지 않습니다.</p>
              <a href={previewUrl} download={filename} className="text-primary underline">
                다운로드
              </a>
            </div>
          ) : null}
        </div>
      </div>
    </div>
  );
}
