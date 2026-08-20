# Policy Explorer Service

사내 인사(HR) 규정 개정안이 **기존 사내 문서와 충돌하는지 로컬 LLM으로 검출**하는 백오피스
서비스입니다. 외부 생성형 AI API는 사용하지 않습니다.

- 기술 스택: Python 3.12 · FastAPI · LangGraph · LangChain · ChromaDB · BM25 · Ollama
- 설계 근거와 이식 시 변경점은 [CLAUDE.md](CLAUDE.md)와 [docs/](docs/) 참고

## 처리 흐름

```text
PDF/DOCX 업로드 → 청킹 → bge-m3 임베딩 → Chroma + BM25 적재

신규 규정 텍스트 입력
  → ① 규정 팩트 추출 (qwen3.5:4b)
  → ② 하이브리드 검색 (Chroma 벡터 50% + BM25 키워드 50%)
  → ③ 충돌 판단 (qwen3.5:4b, 검색된 조각마다 1회)
  → ④ 마크다운 리포트 생성
```

## 사전 준비 — Ollama

**기본은 이제 Docker Ollama입니다.** `compose.yaml`의 공유 `ollama`/`ollama-model-init` 서비스가
`ai-tutor-service`/`curriculum-service`와 함께 이 서비스가 쓰는 모델(`qwen3.5:4b`, `bge-m3`)도
자동으로 pull합니다 — 별도 준비 없이 `docker compose up`만 하면 됩니다.

GPU 패스스루가 중요한 환경(예: macOS Metal — Docker Desktop의 리눅스 VM에는 GPU가 전달되지
않아 컨테이너 Ollama는 CPU 추론만 가능)에서는 호스트에서 직접 실행 중인 Ollama에 붙이는 대안도
여전히 지원합니다. **두 경로의 단계별 절차는 [DEV_RUN.md](DEV_RUN.md)를 참고하세요.**

## 실행

리포 루트에서 실행합니다.

```bash
docker compose up -d --build policy-explorer-service
```

이 서비스는 **Consul·config-server에 의존하지 않습니다.** 다른 서비스를 함께 띄울 필요가 없습니다.

### 통신 확인

```bash
curl localhost:8086/health
```

```json
{
  "status": "UP",
  "ollama": {
    "status": "UP",
    "base_url": "http://ollama:11434",
    "models": { "llm": "qwen3.5:4b", "embedding": "bge-m3" }
  },
  "chroma": {
    "status": "UP",
    "collection": "policy_docs_bge_m3",
    "persisted_chunks": 0,
    "loaded_chunks": 0
  },
  "consul": {
    "status": "UP",
    "service_name": "policy-explorer-service",
    "agent": "consul-1:8500",
    "service_id": "policy-explorer-service-172.20.0.2-8086"
  }
}
```

| 본문 값 | 뜻 |
|---|---|
| `status: UP` | 모든 의존 컴포넌트 정상 |
| `status: DEGRADED` | 프로세스는 살아 있으나 아래 중 하나에 문제 |
| `ollama.status: DOWN` | Ollama에 도달 못 함 — `ollama` 컨테이너가 떠 있는지(또는 override한 호스트 Ollama가 실행 중인지) 확인 |
| `ollama.status: MODEL_MISSING` | 도달했지만 필요한 모델이 없음. `missing_models` 필드 확인 → `ollama pull` |
| `chroma.status: DOWN` | 벡터 스토어 접근 실패 — 볼륨 마운트 확인 |
| `consul.status: DISABLED` | `CONSUL_HOST` 미설정. 단독 실행에서는 정상입니다 |
| `consul.status: DOWN` | 등록 실패. **gateway의 `lb://` 조회가 실패합니다.** Consul 3노드가 모두 떴는지 확인 |

> 의존 컴포넌트가 죽어도 HTTP 200을 반환합니다. Ollama 하나 때문에 컨테이너가 재시작 루프에
> 빠지지 않게 하기 위함이며, 어디가 문제인지는 본문에 드러납니다.

Swagger UI: <http://localhost:8086/docs>

## API

두 가지 경로로 접근할 수 있습니다.

| 경로 | 주소 | 인증 |
|---|---|---|
| **Gateway 경유** (클라이언트용) | `http://localhost:8080/api/policies/**` | JWT 필수 · **`ROLE_ADMIN`만** |
| 직접 호출 (개발용) | `http://localhost:8086/api/policies/**` | 없음 |

Gateway는 JWT를 검증한 뒤 `X-User-Id`·`X-Role` 헤더를 주입해 전달합니다. 라우팅은
`lb://policy-explorer-service`이며 gateway가 **Consul에서 주소를 조회**합니다 — 다른
서비스와 같은 방식입니다.

이 서비스는 기동 시 Consul에 자기를 등록하고 종료 시 해제합니다
([`app/consul.py`](app/consul.py)). `CONSUL_HOST`가 없으면 등록을 건너뛰므로 단독 실행도
그대로 됩니다.

> ⚠️ gateway와 서비스의 경로 prefix는 **반드시 같아야 합니다.** gateway 라우트에
> `StripPrefix`·`RewritePath` 필터가 없어 경로를 그대로 전달하기 때문입니다.


| 메서드 | 경로 | 설명 |
|---|---|---|
| `GET` | `/health` | 프로세스 · Ollama 도달 · 모델 보유 · Chroma 접근 확인 |
| `POST` | `/api/policies/documents/upload` | PDF/DOCX 업로드 → 청킹 → Chroma(+BM25) 적재 |
| `GET` | `/api/policies/documents` | 적재된 문서 목록 조회 (id/상태/청크수/크기/업로드일 — SQLite 메타데이터 DB 기준) |
| `DELETE` | `/api/policies/documents` | 업로드 문서 전체 초기화 |
| `POST` | `/api/policies/analyze` | 규정 충돌 분석 + 마크다운 리포트 |

실제 OpenAPI 3.1 스펙: [docs/openapi/policy-explorer-service.openapi.json](docs/openapi/policy-explorer-service.openapi.json)

```bash
# 문서 업로드
curl -X POST localhost:8086/api/policies/documents/upload -F "file=@규정문서.pdf"

# 적재 현황
curl localhost:8086/api/policies/documents

# 충돌 분석
curl -X POST localhost:8086/api/policies/analyze \
  -H "Content-Type: application/json" \
  -d '{"new_policy_text":"반차 사용 기준 시간을 4.5시간으로 변경합니다."}'

# 전체 초기화
curl -X DELETE localhost:8086/api/policies/documents
```

응답 예 (기존 문서에 "반차는 4시간 기준"이 적재된 상태):

```json
{
  "status": "success",
  "engine": "Ollama",
  "model": "qwen3.5:4b",
  "total_time_seconds": 12.28,
  "documents_in_store": 1,
  "extracted_rules": [{ "keyword": "반차 사용 기준 시간", "fact": "4.5시간으로 변경" }],
  "conflict_count": 1,
  "markdown_report": "## 🚨 사내 콘텐츠 규정 충돌 검출 리포트\n..."
}
```

> ⚠️ Excel(`.xlsx`/`.xls`), HWP(`.hwp`/`.hwpx`)는 미지원입니다. 사유는 PoC 리포
> `select_reason.md` 8절 참고.

## 환경변수

| 환경변수 | 기본값 | 용도 |
|---|---|---|
| `SERVICE_PORT` | `8086` | uvicorn 리슨 포트 |
| `OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama 주소 (compose 기본값은 공유 Docker Ollama `http://ollama:11434`. 호스트 Ollama로 override하려면 `http://host.docker.internal:11434` — [DEV_RUN.md](DEV_RUN.md)) |
| `OLLAMA_MODEL` | `qwen3.5:4b` | 생성 모델 (ai-tutor-service/curriculum-service와 공유) |
| `OLLAMA_EMBEDDING_MODEL` | `bge-m3` | 임베딩 모델 |
| `CHROMA_PERSIST_DIR` | `./chroma_db_fileupload` | 벡터 데이터 경로 (compose는 `/data/state/chroma`) |
| `CHROMA_COLLECTION_NAME` | `policy_docs_bge_m3` | Chroma 컬렉션명 |
| `METADATA_DB_PATH` | `./documents.db` | 문서 메타데이터 SQLite 경로 (compose는 `/data/state/documents.db`) |
| `SEED_DOCUMENTS_DIR` | `./seed-documents` | 기동 시 자동 색인할 샘플 문서 디렉터리(Dockerfile이 이미지에 포함) |
| `UPLOAD_DIR` | `./uploaded_documents` | 업로드 원본 경로 (compose는 `/data/uploads`) |
| `LOG_DIR` | (미설정) | 설정하면 `performance.log`도 남긴다. 미설정 시 stdout만 |
| `RAG_CHUNK_SIZE` / `RAG_CHUNK_OVERLAP` / `RAG_TOP_K` | `500` / `50` / `2` | 청킹·검색 파라미터 |
| `CURRENT_ENGINE` / `CURRENT_MODEL` | `Ollama` / `${OLLAMA_MODEL}` | 응답·로그 라벨 |
| `HEALTH_TIMEOUT_SECONDS` | `3` | `/health`가 Ollama를 기다리는 시간 |
| `CONSUL_HOST` | (미설정) | Consul 주소. **비어 있으면 등록을 건너뜁니다** (단독 실행용) |
| `CONSUL_PORT` | `8500` | Consul 포트 |
| `SERVICE_NAME` | `policy-explorer-service` | Consul 카탈로그 이름. gateway의 `lb://` 대상 |
| `CONSUL_CHECK_INTERVAL` / `_TIMEOUT` | `10s` / `5s` | Consul이 `/health`를 호출하는 주기·타임아웃 |
| `CONSUL_DEREGISTER_AFTER` | `1m` | critical이 이 시간을 넘으면 카탈로그에서 자동 제거 |

## 리눅스 + NVIDIA GPU 서버로 옮길 때

공유 Docker Ollama(`compose.yaml`의 `ollama` 서비스)가 이미 기본이므로 이 서비스만을 위한 별도
설정은 필요 없습니다. `ollama` 서비스 블록에 `deploy.resources.reservations.devices`로 GPU를
예약하면 정책 탐색기를 포함한 모든 서비스의 Ollama 호출이 GPU를 씁니다(policy-explorer-service
쪽 설정은 그대로 두면 됩니다). macOS에서는 GPU가 Docker Desktop의 리눅스 VM으로 전달되지
않으므로 이 구성을 기본으로 두지 않았습니다 — 대신 호스트 Ollama로 override하는 방법을
[DEV_RUN.md](DEV_RUN.md)에 정리했습니다.

## 로컬 실행 (컨테이너 없이)

```bash
cd policy-explorer-service
uv sync
uv run python -m app.main       # http://localhost:8086
```

`OLLAMA_BASE_URL` 기본값이 `http://localhost:11434`라 호스트에서 실행 중인 Ollama에 바로
붙습니다(`ollama pull qwen3.5:4b && ollama pull bge-m3` 필요 — [DEV_RUN.md](DEV_RUN.md) 참고).

## 데이터 저장 위치

| 데이터 | 볼륨 | 컨테이너 경로 |
|---|---|---|
| Chroma 벡터 데이터 | `policy-explorer-state` | `/data/state/chroma` |
| 업로드 원본 파일 | `policy-explorer-uploads` | `/data/uploads` |

**볼륨이 없으면 재기동 시 업로드 문서가 전부 유실됩니다.** 기동 시
`restore_from_persisted()`가 Chroma에서 청크를 읽어 BM25 인메모리 인덱스를 다시 세웁니다.

## 현재 범위

포함:

- PDF/DOCX 업로드 → 청킹 → Chroma(+BM25) 적재
- 규정 충돌 검출 4단계 파이프라인
- 환경변수 기반 설정, `/health`, 컨테이너 이미지, compose 통합

미포함 (사유와 후속 작업은 [docs/08-migration-checklist.md](docs/08-migration-checklist.md)):

- Gateway 라우팅 (`/api/policies/**`)
- 인증/인가
- 업로드 경로 순회 취약점 수정
- 문서 메타데이터 DB(SQLite), 원본 파일 공유 스토리지(NFS)
- Loki 로그 연동
- 멀티 레플리카 (현재 단일 인스턴스 전제)
- 테스트
