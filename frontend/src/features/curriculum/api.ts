import { apiFetch, apiFetchResponse } from "@/lib/api-client";
import type { ChatClient, ChatRequest, ChatStreamEvent, ChatStreamMetadata } from "./types";

const CHAT_PATH = "/api/curriculum/chat";
const STREAM_PATH = `${CHAT_PATH}/stream`;

interface ParsedSseEvent {
  event: string;
  data: unknown;
}

function parseEventBlock(block: string): ParsedSseEvent | null {
  let event = "";
  const dataLines: string[] = [];

  for (const line of block.split(/\r?\n/)) {
    if (line.startsWith("event:")) {
      event = line.slice("event:".length).trim();
    } else if (line.startsWith("data:")) {
      dataLines.push(line.slice("data:".length).trimStart());
    }
  }

  if (!event || dataLines.length === 0) {
    return null;
  }

  return { event, data: JSON.parse(dataLines.join("\n")) as unknown };
}

function toStreamEvent(parsed: ParsedSseEvent): ChatStreamEvent | null {
  if (parsed.event === "start") {
    return { type: "start" };
  }

  if (parsed.event === "metadata") {
    return { type: "metadata", data: parsed.data as ChatStreamMetadata };
  }

  if (parsed.event === "delta") {
    const content = (parsed.data as { content?: unknown }).content;
    if (typeof content !== "string") {
      throw new Error("스트리밍 응답의 텍스트 형식이 올바르지 않습니다.");
    }
    return { type: "delta", content };
  }

  if (parsed.event === "done") {
    return { type: "done" };
  }

  if (parsed.event === "error") {
    const message = (parsed.data as { message?: unknown }).message;
    throw new Error(typeof message === "string" ? message : "봇 응답 스트리밍에 실패했습니다.");
  }

  return null;
}

/** Gateway를 통해 현재 사용자의 커리큘럼 추천 대화를 호출한다. */
export const curriculumChatClient: ChatClient = {
  async *stream(request: ChatRequest, signal?: AbortSignal): AsyncIterable<ChatStreamEvent> {
    const response = await apiFetchResponse(STREAM_PATH, {
      method: "POST",
      body: request,
      headers: { Accept: "text/event-stream" },
      signal,
    });
    if (!response.body) {
      throw new Error("브라우저에서 스트리밍 응답을 읽을 수 없습니다.");
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";

    try {
      while (true) {
        const { value, done } = await reader.read();
        buffer += decoder.decode(value, { stream: !done });

        let boundary = buffer.match(/\r?\n\r?\n/);
        while (boundary?.index !== undefined) {
          const block = buffer.slice(0, boundary.index);
          buffer = buffer.slice(boundary.index + boundary[0].length);
          const parsed = parseEventBlock(block);
          const event = parsed ? toStreamEvent(parsed) : null;

          if (event) {
            yield event;
            if (event.type === "done") {
              return;
            }
          }
          boundary = buffer.match(/\r?\n\r?\n/);
        }

        if (done) {
          break;
        }
      }
    } finally {
      reader.releaseLock();
    }

    throw new Error("스트리밍 응답이 완료되기 전에 연결이 종료되었습니다.");
  },

  reset(): Promise<void> {
    return apiFetch<void>(`${CHAT_PATH}/session`, { method: "DELETE" });
  },
};
