"use client";

import { useState } from "react";
import { Card } from "@/components/ui/Card";
import { Button } from "@/components/ui/Button";
import { Chip } from "@/components/ui/Chip";
import { MaterialIcon } from "@/components/ui/MaterialIcon";
import { PolicyAiAssistModal } from "@/components/policy/PolicyAiAssistModal";

// MOCK: policy-explorer-service가 빈 폴더라(백엔드 없음) 파일 목록/메타정보 모두 하드코딩이다.
const MOCK_FILES = [
  { name: "취업규칙_2024.pdf", icon: "description" },
  { name: "보안가이드라인.pdf", icon: "description" },
  { name: "신규입사자_교육.mp4", icon: "movie" },
];

const MOCK_META: Record<string, { type: string; size: string; uploadedAt: string }> = {
  "취업규칙_2024.pdf": { type: "PDF 문서", size: "3.2 MB", uploadedAt: "2024. 05. 20" },
  "보안가이드라인.pdf": { type: "PDF 문서", size: "1.1 MB", uploadedAt: "2024. 03. 02" },
  "신규입사자_교육.mp4": { type: "동영상", size: "128 MB", uploadedAt: "2024. 01. 15" },
};

export default function PolicyExplorerPage() {
  const [selectedFile, setSelectedFile] = useState(MOCK_FILES[0].name);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const meta = MOCK_META[selectedFile];

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

      <div className="gap-stack-lg grid flex-1 grid-cols-1 lg:grid-cols-2">
        {/* 좌측: 파일 트리 + 메타 정보 */}
        <div className="gap-stack-lg flex flex-col">
          <Card className="flex flex-1 flex-col p-stack-md">
            <h3 className="mb-stack-sm shrink-0 border-b border-outline-variant pb-2 text-headline-sm text-primary">
              파일 Tree
            </h3>
            <div className="min-h-0 flex-1 overflow-y-auto">
              <div className="mt-2 flex items-center gap-2 rounded p-1 text-on-surface">
                <MaterialIcon name="folder_open" className="text-primary" />
                <span className="text-label-md">사내 규정 문서</span>
              </div>
              <ul className="mt-1 ml-6 space-y-1 border-l border-outline-variant pl-2">
                {MOCK_FILES.map((file) => (
                  <li
                    key={file.name}
                    onClick={() => setSelectedFile(file.name)}
                    className={`flex cursor-pointer items-center gap-2 rounded p-1 ${
                      selectedFile === file.name
                        ? "bg-secondary-fixed text-on-secondary-fixed"
                        : "text-on-surface hover:bg-surface-container"
                    }`}
                  >
                    <MaterialIcon name={file.icon} className="text-[18px]" />
                    <span className="text-body-sm">{file.name}</span>
                  </li>
                ))}
              </ul>
            </div>
          </Card>

          <Card className="p-stack-md">
            <h3 className="mb-stack-sm border-outline-variant text-headline-sm text-primary border-b pb-2">
              파일 메타 정보
            </h3>
            <div className="text-body-sm mt-2 flex flex-col gap-2">
              <MetaRow label="파일명" value={selectedFile} />
              <MetaRow label="종류" value={meta.type} />
              <MetaRow label="크기" value={meta.size} />
              <MetaRow label="업로드일" value={meta.uploadedAt} />
              <div className="flex items-start justify-between pt-2">
                <span className="text-on-surface-variant w-20 shrink-0">상태</span>
                <Chip tone="success">Vector화 완료</Chip>
              </div>
            </div>
          </Card>
        </div>

        {/* 우측: 업로드 + 미리보기 */}
        <div className="gap-stack-lg flex flex-col">
          <div className="border-outline-variant bg-surface-container-lowest p-stack-lg hover:border-secondary hover:bg-surface-container-low flex h-48 shrink-0 cursor-pointer flex-col items-center justify-center rounded-lg border-2 border-dashed transition-colors">
            <div className="bg-surface-container mb-4 flex h-16 w-16 items-center justify-center rounded-full">
              <MaterialIcon name="cloud_upload" className="text-outline text-[32px]" />
            </div>
            <h3 className="text-headline-md text-primary mb-1">Drag &amp; Drop</h3>
            <p className="text-body-md text-on-surface-variant">파일 업로드</p>
          </div>

          <Card className="flex flex-1 flex-col p-stack-md">
            <h3 className="mb-stack-sm shrink-0 border-b border-outline-variant pb-2 text-headline-sm text-primary">
              Preview (파일 미리보기)
            </h3>
            <div className="mt-2 flex min-h-0 flex-1 flex-col items-center justify-center gap-2 rounded border border-outline-variant bg-surface p-stack-lg text-center text-on-surface-variant">
              <MaterialIcon name="visibility" className="mb-2 text-[64px] text-outline-variant" />
              <p>- PDF 등 문서면 내용 미리 보기</p>
              <p>- mp4 등 영상이면 영상 내용 및 프리뷰</p>
            </div>
          </Card>
        </div>
      </div>

      {isModalOpen && <PolicyAiAssistModal onClose={() => setIsModalOpen(false)} />}
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
