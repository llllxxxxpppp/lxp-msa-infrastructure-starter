"use client";

import { useState } from "react";
import { Button } from "@/components/ui/Button";
import { MaterialIcon } from "@/components/ui/MaterialIcon";

// MOCK: policy-explorer-service가 아직 빈 폴더라(백엔드 없음) 파일 목록/변경 제안 모두 하드코딩이다.
const MOCK_FILES = ["취업규칙_2024.pdf (p.12 제4조)", "인사관리규정.pdf (p.5 제2항)"];

interface PolicyAiAssistModalProps {
  onClose: () => void;
}

export function PolicyAiAssistModal({ onClose }: PolicyAiAssistModalProps) {
  const [request, setRequest] = useState("");
  const [selectedFile, setSelectedFile] = useState(MOCK_FILES[0]);

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
          </div>

          <div className="gap-stack-md grid min-h-0 flex-1 grid-cols-1 md:grid-cols-2">
            <div className="flex flex-col gap-2">
              <h3 className="text-label-md text-on-surface-variant">해당 파일 및 위치</h3>
              <div className="border-outline-variant bg-surface flex-1 overflow-hidden rounded-lg border">
                <ul className="divide-outline-variant divide-y">
                  {MOCK_FILES.map((file) => (
                    <li
                      key={file}
                      onClick={() => setSelectedFile(file)}
                      className={`p-stack-sm hover:bg-surface-container-low flex cursor-pointer items-center gap-2 ${
                        selectedFile === file ? "bg-secondary-fixed/30" : ""
                      }`}
                    >
                      <MaterialIcon name="description" className="text-primary text-[18px]" />
                      <span className="text-body-sm">{file}</span>
                    </li>
                  ))}
                </ul>
              </div>
            </div>

            <div className="flex flex-col gap-2">
              <h3 className="text-label-md text-on-surface-variant">변경 제안 상세</h3>
              <div className="border-outline-variant bg-surface-container-low p-stack-md text-body-sm flex-1 rounded-lg border">
                <p className="text-outline line-through">
                  제4조(수습기간) 신규 채용된 자의 수습기간은 채용일로부터 3개월로 한다.
                </p>
                <p className="text-secondary mt-2 font-semibold">
                  제4조(수습기간) 신규 채용된 자의 수습기간은 채용일로부터 2개월로 한다.
                </p>
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
          {/* MOCK: 실제 적용 로직 없음. 백엔드 준비되면 이 버튼에서 API 호출 후 onClose(). */}
          <Button onClick={onClose}>변경 사항 적용</Button>
        </div>
      </div>
    </div>
  );
}
