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

export function PolicyAiAssistModal({ onClose }: PolicyAiAssistModalProps) {
  const [request, setRequest] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [conflicts, setConflicts] = useState<ConflictItem[] | null>(null);
  const [selectedIndex, setSelectedIndex] = useState<number | null>(null);

  const selected = selectedIndex != null ? conflicts?.[selectedIndex] : undefined;

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
                    <p className="text-outline line-through">{selected.old_content}</p>
                    <p className="text-secondary mt-2 font-semibold">{selected.action_suggested}</p>
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
          {/* MOCK: 실제 적용 로직 없음. 백엔드에 변경 반영 API가 준비되면 이 버튼에서 호출 후 onClose(). */}
          <Button onClick={onClose}>변경 사항 적용</Button>
        </div>
      </div>
    </div>
  );
}
