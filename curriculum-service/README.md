# 맞춤형 커리큘럼 설계 및 수강 신청 자동화 봇

## 실행

1. 다음 명령어를 실행하여 Ollama Docker Container를 실행합니다.

    ```bash
    # cpu 사용
    docker compose up -d

    # gpu 사용
    docker compose -f compose.cuda.yaml -d
    ```

1. 다음 명령어를 실행하여 채팅 및 임베딩 모델을 다운받습니다. 이 과정은 첫 실행에만 필요합니다.

    ```bash
    docker exec curriculum-service-ollama-1 ollama pull qwen3.5:4b
    docker exec curriculum-service-ollama-1 ollama pull qwen3-embedding:0.6b
    ```

    기본 임베딩 모델은 `OLLAMA_EMBEDDING_MODEL` 환경 변수로 변경할 수 있습니다.
    서버를 시작할 때 강좌 데이터를 임베딩하여 인메모리 Chroma 컬렉션을 새로
    구성하므로 Ollama가 먼저 실행 중이어야 합니다.

1. 다음 명령어를 입력하여 FastAPI 서버를 실행합니다.

    ```bash
    uv run uvicorn app.main:app --reload
    ```

## 테스트

- API 문서 주소: `http://localhost:8000/docs`

- 테스트 요청

    여러 대화에 동일한 `thread_id` 를 사용하면 대화를 이어나갈 수 있습니다.

    ```bash
    curl -X POST http://localhost:8000/chat \
        -H 'Content-Type: application/json' \
        -d '{
            "thread_id": "user-1",
            "message": "마케터이고 캠페인 성과를 개선하고 싶어요."
        }'
    ```
