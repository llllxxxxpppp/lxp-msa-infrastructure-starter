import { useState } from "react";

import "./AiTutorWidget.css";

export default function AiTutorWidget({
  courses,
  accessToken,
}) {
  const [isOpen, setIsOpen] = useState(false);
  const [courseId, setCourseId] = useState(
    String(courses[0]?.id ?? ""),
  );
  const [question, setQuestion] = useState("");
  const [messages, setMessages] = useState([]);
  const [sources, setSources] = useState([]);
  const [isLoading, setIsLoading] = useState(false);

  const appendAnswer = (content) => {
    setMessages((current) => {
      const next = [...current];
      const lastIndex = next.length - 1;

      next[lastIndex] = {
        ...next[lastIndex],
        content: next[lastIndex].content + content,
      };

      return next;
    });
  };

  const sendQuestion = async (event) => {
    event.preventDefault();

    const value = question.trim();

    if (!value || isLoading) {
      return;
    }

    if (!accessToken) {
      setMessages((current) => [
        ...current,
        {
          role: "assistant",
          content: "로그인이 필요합니다.",
        },
      ]);
      return;
    }

    setQuestion("");
    setSources([]);
    setIsLoading(true);

    // 사용자 질문과 빈 AI 답변을 먼저 화면에 추가한다.
    setMessages((current) => [
      ...current,
      { role: "user", content: value },
      { role: "assistant", content: "" },
    ]);

    try {
      const response = await fetch(
        `/api/ai/courses/${courseId}/chat`,
        {
          method: "POST",
          headers: {
            Authorization: `Bearer ${accessToken}`,
            "Content-Type": "application/json",
          },
          body: JSON.stringify({ question: value }),
        },
      );

      if (!response.ok || !response.body) {
        throw new Error();
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder();
      let buffer = "";

      while (true) {
        const { value: chunk, done } = await reader.read();

        if (done) {
          break;
        }

        buffer += decoder.decode(chunk, { stream: true });

        const events = buffer.split("\n\n");
        buffer = events.pop() ?? "";

        for (const block of events) {
          const eventName = block.match(
            /^event:\s*(.+)$/m,
          )?.[1];

          const dataText = block.match(
            /^data:\s*(.*)$/m,
          )?.[1];

          if (!eventName || !dataText) {
            continue;
          }

          const data = JSON.parse(dataText);

          if (eventName === "token") {
            appendAnswer(data.content);
          }

          if (eventName === "sources") {
            setSources(data);
          }
        }
      }
    } catch {
      appendAnswer("답변을 불러오지 못했습니다.");
    } finally {
      setIsLoading(false);
    }
  };

  if (!isOpen) {
    return (
      <button
        className="ai-cloud-button"
        type="button"
        aria-label="AI 학습 도우미 열기"
        onClick={() => setIsOpen(true)}
      >
        <span>AI</span>
      </button>
    );
  }

  return (
    <section
      className="ai-chat-modal"
      role="dialog"
      aria-label="AI 학습 도우미"
    >
      <header className="ai-chat-header">
        <div>
          <strong>AI 학습 도우미</strong>
          <p>강의 자료에 대해 질문해 보세요.</p>
        </div>

        <button
          type="button"
          aria-label="닫기"
          onClick={() => setIsOpen(false)}
        >
          ×
        </button>
      </header>

      <select
        className="ai-course-select"
        value={courseId}
        onChange={(event) => setCourseId(event.target.value)}
      >
        {courses.map((course) => (
          <option
            key={course.id}
            value={course.id}
          >
            {course.title}
          </option>
        ))}
      </select>

      <div
        className="ai-message-list"
        aria-live="polite"
      >
        {messages.length === 0 && (
          <p className="ai-guide">
            궁금한 내용을 입력해 주세요.
          </p>
        )}

        {messages.map((message, index) => (
          <div
            key={index}
            className={`ai-message ${message.role}`}
          >
            {message.content ||
              (isLoading ? "답변 생성 중..." : "")}
          </div>
        ))}

        {sources.length > 0 && (
          <div className="ai-sources">
            <strong>출처</strong>

            {sources.map((source) => (
              <p
                key={`${source.filename}-${source.page_number}`}
              >
                {source.filename} · {source.page_number}페이지
              </p>
            ))}
          </div>
        )}
      </div>

      <form
        className="ai-chat-form"
        onSubmit={sendQuestion}
      >
        <input
          value={question}
          placeholder="질문을 입력하세요."
          aria-label="질문"
          onChange={(event) => setQuestion(event.target.value)}
        />

        <button
          type="submit"
          disabled={!question.trim() || isLoading}
        >
          전송
        </button>
      </form>
    </section>
  );
}