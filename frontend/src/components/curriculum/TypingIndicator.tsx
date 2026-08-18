/** 봇 응답을 기다리는 동안 보여주는 자리. 봇에 스트리밍이 없어 중간 텍스트를 흘릴 수 없다. */
export function TypingIndicator() {
  return (
    <li className="flex max-w-[85%] flex-col">
      <div
        className="bg-surface-container-lowest border-outline-variant text-on-surface-variant rounded-2xl rounded-tl-sm border p-4 text-sm tracking-[0.3em] shadow-sm"
        role="status"
        aria-label="답변을 작성하는 중"
      >
        <span aria-hidden="true">● ● ●</span>
      </div>
    </li>
  );
}
