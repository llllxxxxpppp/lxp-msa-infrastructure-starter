"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { MaterialIcon } from "@/components/ui/MaterialIcon";
import { ApiError } from "@/types/api";
import { analyzePolicy } from "@/features/policy/api";
import type { ConflictItem } from "@/features/policy/types";

interface PolicyAiAssistModalProps {
  onClose: () => void;
}

/** 리스트/상세에 쓰는 "파일명 (p.N)" 라벨. 페이지 정보가 없으면 파일명만 보여준다. */
function locationLabel(conflict: ConflictItem): string {
  return conflict.page != null ? `${conflict.source} (p.${conflict.page + 1})` : conflict.source;
}

/** CSV 필드 이스케이프: 쉼표·큰따옴표·줄바꿈이 있으면 큰따옴표로 감싸고 내부 "는 ""로 이스케이프. */
function csvEscape(value: string): string {
  if (/[",\n]/.test(value)) {
    return `"${value.replace(/"/g, '""')}"`;
  }
  return value;
}

/**
 * conflicts 전체를 "해당 파일 및 위치"/"변경 제안 상세" 열로 정리한 CSV 문자열을 만든다.
 * 최상단에 사용자가 작성한 원본 요청("변경하고자 하는 규정 작성")을 먼저 넣어, 리포트만 봐도
 * 어떤 요청으로 나온 결과인지 알 수 있게 한다.
 */
function buildConflictsCsv(conflicts: ConflictItem[], request: string): string {
  const requestBlock = ["변경하고자 하는 규정 작성", csvEscape(request), ""].join("\r\n");

  const header = ["번호", "파일명", "위치", "기존 내용", "신규 규정 팩트", "변경 제안", "판단 근거"];
  const rows = conflicts.map((c, i) => [
    String(i + 1),
    c.source,
    c.page != null ? `p.${c.page + 1}` : "",
    c.old_content,
    c.new_fact,
    c.action_suggested,
    c.reasoning,
  ]);
  const table = [header, ...rows].map((row) => row.map(csvEscape).join(",")).join("\r\n");

  return requestBlock + "\r\n" + table;
}

/** CSV 문자열을 파일로 다운로드한다. 엑셀에서 한글이 깨지지 않도록 UTF-8 BOM을 붙인다. */
function downloadCsv(csv: string, filename: string) {
  const blob = new Blob(["﻿" + csv], { type: "text/csv;charset=utf-8;" });
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = filename;
  link.click();
  URL.revokeObjectURL(url);
}

/** "YYYYMMDD_HHmm" 형태의 타임스탬프(파일명용). */
function timestampForFilename(date: Date): string {
  const pad = (n: number) => String(n).padStart(2, "0");
  return (
    `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}` +
    `_${pad(date.getHours())}${pad(date.getMinutes())}`
  );
}

export function PolicyAiAssistModal({ onClose }: PolicyAiAssistModalProps) {
  const [request, setRequest] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [conflicts, setConflicts] = useState<ConflictItem[] | null>(null);
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);

  const selected = selectedIndex != null ? conflicts?.[selectedIndex] : undefined;

  function handleExportReport() {
    if (!conflicts || conflicts.length === 0) return;
    const csv = buildConflictsCsv(conflicts, request);
    downloadCsv(csv, `정책변경제안_${timestampForFilename(new Date())}.csv`);
  }

  async function handleGenerate() {
    if (!request.trim() || isLoading) return;
    setIsLoading(true);
    setError(null);
    try {
      const result = await analyzePolicy({ new_policy_text: request });
      setConflicts(result.conflicts);
      setSelectedIndex(result.conflicts.length > 0 ? 0 : null);
    } catch (err) {
      setError(
        err instanceof ApiError ? err.message : "변경 제안을 생성하지 못했습니다. 잠시 후 다시 시도해 주세요."
      );
      setConflicts(null);
      setSelectedIndex(null);
    } finally {
      setIsLoading(false);
    }
  }

  return (
    <div className="bg-primary/40 p-margin-mobile fixed inset-0 z-50 flex items-center justify-center backdrop-blur-sm">
      <div className="border-outline-variant bg-surface-container-lowest flex max-h-[90vh] w-full max-w-4xl flex-col overflow-hidden rounded-xl border shadow-2xl">
        <div className="border-outline-variant bg-surface-container-low p-stack-md flex items-center justify-between border-b">
          <div className="flex items-center gap-2">
            <MaterialIcon name="smart_toy" className="text-secondary" />
            <h2 className="text-headline-sm text-primary">AI 규정 변경 지원</h2>
          </div>
          <button
            type="button"
            onClick={onClose}
            aria-label="닫기"
            className="text-outline hover:text-primary"
          >
            <MaterialIcon name="close" />
          </button>
        </div>

        <div className="gap-stack-md p-stack-md flex flex-1 flex-col overflow-y-auto">
          <div className="flex flex-col gap-2">
            <label className="text-label-md text-on-surface-variant">
              변경하고자 하는 규정 작성
            </label>
            <textarea
              value={request}
              onChange={(e) => setRequest(e.target.value)}
              className="border-outline-variant bg-surface p-stack-md text-body-md text-on-surface focus:border-secondary focus:ring-secondary h-32 w-full rounded-lg border focus:ring-1 focus:outline-none"
              placeholder="예: 신규 입사자의 수습 기간 규정을 3개월에서 2개월로 단축하고 싶습니다."
            />
            <div className="flex items-center justify-between">
              {error ? <p className="text-body-sm text-error-red">{error}</p> : <span />}
              <Button
                type="button"
                variant="secondary"
                disabled={!request.trim() || isLoading}
                onClick={handleGenerate}
              >
                {isLoading ? "분석 중..." : "제안 생성"}
              </Button>
            </div>
          </div>

          <div className="gap-stack-md grid min-h-0 flex-1 grid-cols-1 md:grid-cols-2">
            <div className="flex flex-col gap-2">
              <h3 className="text-label-md text-on-surface-variant">해당 파일 및 위치</h3>
              <div className="border-outline-variant bg-surface flex-1 overflow-hidden rounded-lg border">
                {conflicts == null ? (
                  <p className="text-body-sm text-slate-text p-stack-md">
                    먼저 규정을 작성하고 &quot;제안 생성&quot;을 눌러 주세요.
                  </p>
                ) : conflicts.length === 0 ? (
                  <p className="text-body-sm text-slate-text p-stack-md">
                    충돌하는 기존 규정을 찾지 못했습니다.
                  </p>
                ) : (
                  <ul className="divide-outline-variant divide-y">
                    {conflicts.map((conflict, index) => (
                      <li
                        key={`${conflict.source}-${conflict.page ?? "na"}-${index}`}
                        onClick={() => setSelectedIndex(index)}
                        className={`p-stack-sm hover:bg-surface-container-low flex cursor-pointer items-center gap-2 ${
                          selectedIndex === index ? "bg-secondary-fixed/30" : ""
                        }`}
                      >
                        <MaterialIcon name="description" className="text-primary text-[18px]" />
                        <span className="text-body-sm">{locationLabel(conflict)}</span>
                      </li>
                    ))}
                  </ul>
                )}
              </div>
            </div>

            <div className="flex flex-col gap-2">
              <h3 className="text-label-md text-on-surface-variant">변경 제안 상세</h3>
              <div className="border-outline-variant bg-surface-container-low p-stack-md text-body-sm flex-1 rounded-lg border">
                {selected ? (
                  <>
                    <p className="text-outline line-through whitespace-pre-wrap">{selected.old_content}</p>
                    <p className="text-secondary mt-2 font-semibold whitespace-pre-wrap">
                      {selected.action_suggested}
                    </p>
                    {selected.reasoning && (
                      <details className="mt-2">
                        <summary className="text-label-sm text-on-surface-variant cursor-pointer">
                          판단 근거 보기
                        </summary>
                        <p className="text-body-sm text-on-surface-variant mt-1 whitespace-pre-wrap">
                          {selected.reasoning}
                        </p>
                      </details>
                    )}
                  </>
                ) : (
                  <p className="text-body-sm text-slate-text">좌측 목록에서 항목을 선택해 주세요.</p>
                )}
              </div>
            </div>
          </div>
        </div>

        <div className="gap-stack-sm border-outline-variant bg-surface-container-low p-stack-md flex justify-end border-t">
          <button
            type="button"
            onClick={onClose}
            className="px-stack-md text-label-md text-on-surface-variant hover:bg-surface-container rounded-lg py-2"
          >
            취소
          </button>
          <Button
            type="button"
            disabled={!conflicts || conflicts.length === 0}
            onClick={handleExportReport}
          >
            레포트 출력
          </Button>
        </div>
      </div>
    </div>
  );
}
