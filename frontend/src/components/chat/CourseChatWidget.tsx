"use client";

import { FormEvent, useState } from "react";
import { Button } from "@/components/ui/Button";
import { MaterialIcon } from "@/components/ui/MaterialIcon";
import { apiFetchResponse } from "@/lib/api-client";

/**
 * design/course-chatbot/{chatbot-buttoon,chatbot-chat-screen} 정적 목업 기반.
 * ai-tutor-service의 `POST /api/ai/courses/{courseId}/chat` SSE(`token`/`sources`/`done`)에 연결한다.
 */

interface ChatSource {
  filename: string;
  page_number: number;
}

interface ChatMessage {
  role: "assistant" | "user";
  text: string;
  sources?: ChatSource[];
}

interface CourseChatWidgetProps {
  courseId: number;
  courseTitle: string;
}

export function CourseChatWidget({ courseId, courseTitle }: CourseChatWidgetProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [input, setInput] = useState("");
  const [isSending, setIsSending] = useState(false);
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      role: "assistant",
      text: `안녕하세요! "${courseTitle}" 강의에 대해 궁금한 점이 있으신가요?`,
    },
  ]);

  async function handleSend(event: FormEvent) {
    event.preventDefault();
    const text = input.trim();
    if (!text || isSending) return;

    setMessages((prev) => [...prev, { role: "user", text }, { role: "assistant", text: "" }]);
    setInput("");
    setIsSending(true);

    try {
      const response = await apiFetchResponse(`/api/ai/courses/${courseId}/chat`, {
        method: "POST",
        body: { question: text },
        headers: { Accept: "text/event-stream" },
      });

      const reader = response.body!.getReader();
      const decoder = new TextDecoder();
      let buffer = "";

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;
        buffer += decoder.decode(value, { stream: true });

        const blocks = buffer.split(/\r?\n\r?\n/);
        buffer = blocks.pop() ?? "";

        for (const block of blocks) {
          const lines = block.split(/\r?\n/);
          const eventLine = lines.find((line) => line.startsWith("event:"));
          const dataLine = lines.find((line) => line.startsWith("data:"));
          if (!eventLine || !dataLine) continue;

          const eventName = eventLine.slice(6).trim();
          const data = dataLine.slice(5).trim();

          if (eventName === "token") {
            const { content } = JSON.parse(data) as { content: string };
            setMessages((prev) => {
              const next = [...prev];
              const last = next[next.length - 1];
              next[next.length - 1] = { ...last, text: last.text + content };
              return next;
            });
          } else if (eventName === "sources") {
            const sources = JSON.parse(data) as ChatSource[];
            setMessages((prev) => {
              const next = [...prev];
              next[next.length - 1] = { ...next[next.length - 1], sources };
              return next;
            });
          }
        }
      }
    } catch {
      setMessages((prev) => {
        const next = [...prev];
        next[next.length - 1] = {
          role: "assistant",
          text: "답변을 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.",
        };
        return next;
      });
    } finally {
      setIsSending(false);
    }
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
            {message.sources && message.sources.length > 0 && (
              <ul className="text-label-sm text-outline mt-stack-sm space-y-0.5">
                {message.sources.map((source, sourceIndex) => (
                  <li key={sourceIndex}>
                    {source.filename} · {source.page_number}쪽
                  </li>
                ))}
              </ul>
            )}
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
          disabled={isSending}
          className="border-outline-variant bg-surface text-body-sm text-on-surface focus:border-secondary focus:ring-secondary flex-1 rounded-lg border px-4 py-2.5 focus:ring-1 focus:outline-none disabled:opacity-50"
        />
        <Button type="submit" disabled={isSending} className="px-4 py-2.5">
          <MaterialIcon name="send" className="text-[18px]" />
        </Button>
      </form>
    </div>
  );
}
