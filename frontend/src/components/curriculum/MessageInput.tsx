import { useState, type FormEvent } from "react";

interface MessageInputProps {
  onSend: (text: string) => void;
  /** 응답을 기다리는 동안 잠근다. */
  disabled: boolean;
}

export function MessageInput({ onSend, disabled }: MessageInputProps) {
  const [text, setText] = useState("");

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    if (disabled || !text.trim()) {
      return;
    }
    onSend(text);
    setText("");
  }

  return (
    <form
      onSubmit={handleSubmit}
      className="bg-surface-container-lowest border-outline-variant sticky bottom-0 mt-6 flex items-center space-x-2 border-t py-4"
    >
      <input
        type="text"
        value={text}
        onChange={(event) => setText(event.target.value)}
        placeholder={disabled ? "응답을 기다리는 중…" : "메시지를 입력하세요"}
        aria-label="메시지"
        disabled={disabled}
        className="border-outline-variant focus:ring-primary bg-surface-container-lowest text-on-surface placeholder:text-on-surface-variant w-full flex-1 rounded-xl border py-3 pr-10 pl-4 text-sm transition-shadow focus:border-transparent focus:ring-2 focus:outline-none disabled:opacity-60"
      />
      <button
        type="submit"
        disabled={disabled || !text.trim()}
        className="bg-surface-container border-outline-variant text-primary hover:bg-outline-variant flex-shrink-0 rounded-xl border px-6 py-3 text-sm font-medium transition-colors disabled:cursor-not-allowed disabled:opacity-50"
      >
        보내기
      </button>
    </form>
  );
}
