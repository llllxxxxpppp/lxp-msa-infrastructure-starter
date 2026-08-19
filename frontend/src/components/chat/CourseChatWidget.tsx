"use client";

import { FormEvent, useState } from "react";
import { Button } from "@/components/ui/Button";
import { MaterialIcon } from "@/components/ui/MaterialIcon";

/**
 * design/course-chatbot/{chatbot-buttoon,chatbot-chat-screen} 정적 목업.
 *
 * ⚠️ 실제 AI/챗봇 백엔드가 리포에 없다(어떤 서비스도 이 기능을 제공하지 않음).
 * 열림/닫힘 토글과 메시지 리스트는 실제 로컬 상태로 동작하지만, 답변은 하드코딩된 스크립트다.
 * 백엔드가 준비되면 handleSend 안의 setTimeout 부분을 실제 API 호출로 교체하면 된다.
 */

interface ChatMessage {
  role: "assistant" | "user";
  text: string;
}

interface CourseChatWidgetProps {
  courseTitle: string;
  lectureTitles: string[];
}

export function CourseChatWidget({ courseTitle, lectureTitles }: CourseChatWidgetProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [selectedLecture, setSelectedLecture] = useState(lectureTitles[0] ?? courseTitle);
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      role: "assistant",
      text: `안녕하세요! 선택하신 "${lectureTitles[0] ?? courseTitle}" 강의에 대해 궁금한 점이 있으신가요?`,
    },
  ]);

  function handleSend(event: FormEvent) {
    event.preventDefault();
    const text = input.trim();
    if (!text) return;

    setMessages((prev) => [
      ...prev,
      { role: "user", text },
      // MOCK: 실제 AI 백엔드 연동 전까지는 고정 안내 문구로 응답한다.
      {
        role: "assistant",
        text: "아직 준비 중인 기능이에요. 실제 AI 답변은 백엔드 연동 후 제공될 예정입니다.",
      },
    ]);
    setInput("");
  }

  if (!isOpen) {
    return (
      <button
        type="button"
        onClick={() => setIsOpen(true)}
        aria-label="AI 학습 도우미 열기"
        className="right-margin-desktop bottom-margin-desktop bg-secondary text-on-secondary hover:bg-secondary-container fixed flex h-14 w-14 items-center justify-center rounded-full shadow-[0_12px_24px_-8px_rgba(0,0,0,0.12),0_4px_8px_-4px_rgba(0,0,0,0.08)] transition-colors"
      >
        <MaterialIcon name="smart_toy" />
      </button>
    );
  }

  return (
    <div className="right-margin-desktop bottom-margin-desktop border-outline-variant bg-surface fixed z-50 flex h-[600px] w-full max-w-[400px] flex-col overflow-hidden rounded-xl border shadow-[0_12px_24px_-8px_rgba(0,0,0,0.12),0_4px_8px_-4px_rgba(0,0,0,0.08)]">
      <div className="bg-primary-fixed p-stack-md flex items-start justify-between">
        <div>
          <h2 className="text-label-md text-on-primary-fixed">AI 학습 도우미</h2>
          <p className="text-body-sm text-on-primary-fixed-variant mt-1">
            강의 자료에 대해 질문해 보세요.
          </p>
        </div>
        <button
          type="button"
          onClick={() => setIsOpen(false)}
          aria-label="닫기"
          className="text-on-primary-fixed-variant hover:text-on-primary-fixed"
        >
          <MaterialIcon name="close" />
        </button>
      </div>

      {lectureTitles.length > 0 && (
        <div className="border-outline-variant p-stack-sm border-b">
          <select
            value={selectedLecture}
            onChange={(e) => setSelectedLecture(e.target.value)}
            className="border-outline-variant bg-surface text-body-sm text-on-surface focus:border-secondary focus:ring-secondary w-full cursor-pointer appearance-none rounded-lg border py-2 pr-10 pl-3 focus:ring-1 focus:outline-none"
          >
            {lectureTitles.map((title) => (
              <option key={title}>{title}</option>
            ))}
          </select>
        </div>
      )}

      <div className="space-y-stack-sm p-stack-md flex-1 overflow-y-auto">
        {messages.map((message, index) => (
          <div
            key={index}
            className={`p-stack-sm text-body-sm max-w-[85%] rounded-lg ${
              message.role === "assistant"
                ? "bg-surface-container-low text-on-surface"
                : "bg-secondary text-on-secondary ml-auto"
            }`}
          >
            {message.text}
          </div>
        ))}
      </div>

      <form
        onSubmit={handleSend}
        className="border-outline-variant p-stack-sm flex items-center gap-2 border-t"
      >
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          placeholder="질문을 입력하세요."
          className="border-outline-variant bg-surface text-body-sm text-on-surface focus:border-secondary focus:ring-secondary flex-1 rounded-lg border px-4 py-2.5 focus:ring-1 focus:outline-none"
        />
        <Button type="submit" className="px-4 py-2.5">
          <MaterialIcon name="send" className="text-[18px]" />
        </Button>
      </form>
    </div>
  );
}
