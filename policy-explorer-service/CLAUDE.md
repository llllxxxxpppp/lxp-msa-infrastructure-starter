# policy-explorer-service

사내 인사(HR) 규정 개정안이 **기존 사내 문서와 충돌하는지 로컬 LLM으로 검출**하는 백오피스
서비스입니다. 민감한 사내 문서를 다루므로 외부 생성형 AI API를 쓰지 않고, 온프레미스 Ollama에서
구동되는 모델만 사용합니다.

```text
[신규 규정 텍스트]
   ① extract_rules      LLM이 핵심 규정(팩트)을 구조화 추출        (Ollama /api/chat)
   ② retrieve_legacy    Chroma 벡터 + BM25 키워드 50:50 하이브리드 검색 (Ollama /api/embed)
   ③ analyze_conflicts  LLM이 기존 내용과 신규 팩트를 비교해 충돌 판단  (Ollama /api/chat)
   ④ generate_report    충돌 항목만 모아 마크다운 표 리포트 생성        (LLM 미사용)
[extracted_rules, conflict_count, markdown_report]
```

---

## ⚠️ 먼저 읽을 것 — 별도 PoC 리포를 반드시 참고한다

이 서비스는 **처음부터 여기서 만든 것이 아닙니다.** 별도 저장소 **`policy-explorer-service`(PoC
리포)** 에서 엔진·모델·검색 전략을 실측 비교하며 검증한 결과물을 이 모노레포로 이식한 것입니다.

**이 폴더의 코드만 보면 "왜 이렇게 만들었는지"를 알 수 없습니다.** 설계 판단의 근거는 전부 PoC
리포에 있습니다. 아래 상황에서는 반드시 그쪽을 먼저 확인하세요.

| 이런 의문이 들면 | PoC 리포의 이 파일을 본다 |
|---|---|
| 왜 Ollama인가? 왜 vLLM이 아닌가? | `select_reason.md` 1절 — 동시 100건 부하 실측 비교 |
| 왜 Qwen2.5-7B인가? EXAONE은 왜 후보였나? | `select_reason.md` 2절 |
| 왜 하이브리드 검색(벡터+BM25 50:50)인가? | `select_reason.md` 5절 |
| 왜 LangGraph인가? | `select_reason.md` 6절 |
| 왜 PDF/DOCX만 지원하고 Excel·HWP는 빼는가? | `select_reason.md` 8절 |
| 프로젝트 전체 목적과 향후 과제는? | `PROJECT_PLAN.md` |
| 성능 수치의 원본 데이터는? | `test-result.md`, `test/2026081*-ASYNC-*.md` |
| 벤치마크 스크립트 4종(엔진×모델)은 어디 있나? | `lxp-{ollama,vllm}-{qwen,exaone}.py` — **이식 대상 아님** |
| 이식 전 원본 코드는? | `lxp-ollama-qwen-fileupload.py` (448줄, 단일 파일) |

이 폴더의 [`docs/`](docs/)는 PoC 리포의 `docs/` 01~09를 이식하면서 **이식 결과를 반영해
갱신한 것**입니다. 각 문서 상단의 `> 🔄 이식 반영` 블록에 원본과 달라진 점이 표시되어 있습니다.

> 코드를 고치기 전에 [`docs/08-migration-checklist.md`](docs/08-migration-checklist.md)를 먼저
> 보세요. 무엇이 끝났고 무엇이 의도적으로 남아 있는지 정리되어 있습니다.

---

## 이식하면서 달라진 점

원본 스크립트와 대조할 때 이 표를 기준으로 보세요. **그대로 보존한 것**과 **의도적으로 바꾼 것**을
구분해야 합니다.

### 그대로 보존한 것 (건드리지 말 것)

| 항목 | 값 |
|---|---|
| LangGraph 4단계 구조와 노드 이름 | `extract_rules` → `retrieve_legacy` → `analyze_conflicts` → `generate_report` |
| 프롬프트 문구 | 한국어 강제 지시문 포함 (Qwen/EXAONE의 중국어 혼입 대응) |
| 하이브리드 검색 가중치 | Chroma 0.5 : BM25 0.5 |
| 청킹 파라미터 | `chunk_size=500`, `chunk_overlap=50`, `top_k=2` |
| API 경로와 응답 필드 | PoC `docs/02`의 계약 유지 |
| Chroma 저장 시 `ids` 명시 | 원본이 발견해 고친 삭제 버그. **절대 되돌리지 말 것** |
| 지원 확장자 | `.pdf`, `.docx` |

### 의도적으로 바꾼 것

| # | 변경 | 이유 |
|---|---|---|
| 1 | **`ChatOllama`에 `base_url` 추가** | 원본은 인자가 없어 `localhost:11434`를 봤다. 컨테이너 안에서 `localhost`는 컨테이너 자신이라 Ollama에 도달할 수 없다. **이 한 줄이 통신의 관문이다.** |
| 2 | **임베딩 `ko-sroberta` → `bge-m3`** | 원본은 `jhgan/ko-sroberta-multitask`(768차원)를 컨테이너 안에서 torch로 직접 계산했다. Ollama가 서빙하는 `bge-m3`(1024차원)로 옮겨 이미지에서 torch·sentence-transformers를 제거했다. 부수효과로 생성·임베딩이 모두 Ollama를 타게 됐다. **PoC `select_reason.md` 3절의 채택 근거를 우리가 바꾼 것이므로, 검색 품질 재검증이 필요하면 이 지점을 본다.** |
| 3 | Chroma 컬렉션 분리 | 임베딩 차원이 768→1024로 바뀌어 기존 컬렉션과 호환되지 않는다. `policy_docs_bge_m3` |
| 4 | 하드코딩 → 환경변수 | PoC `docs/03`의 목록을 [`app/config.py`](app/config.py)로 일괄 추출 |
| 5 | 단일 파일 → `app/` 패키지 | 아래 구조 참고 |
| 12 | **Consul 등록** | Java 서비스는 `spring-cloud-starter-consul-discovery`가 자동으로 하지만 Python이라 Consul HTTP API를 직접 호출한다([`app/consul.py`](app/consul.py), 표준 라이브러리만). gateway가 `lb://`로 찾게 하려는 것이며, 등록 실패는 기동을 막지 않고 `/health`의 `consul` 항목에 드러난다 |
| 6 | **import 부작용 제거** | 원본은 import만 해도 Chroma에 접속하고 데이터를 복원했다. `lifespan`으로 옮겨 실행 시점을 명시했다 |
| 7 | **`GET /health` 추가** | 원본에 없었다. 프로세스·Ollama 도달·필요 모델 보유·Chroma 접근을 한 번에 확인한다 |
| 8 | 로그를 stdout 기본으로 | `LOG_DIR`을 설정하면 파일도 남긴다 |
| 9 | 모델 라벨을 실제 모델에서 파생 | 원본은 상수여서 모델을 바꾸면 응답 라벨이 거짓이 됐다 |
| 10 | `langchain_community.vectorstores.Chroma` → `langchain_chroma` | 폐기 예정. 같은 리포의 ai-bot-service와 라이브러리를 통일 |
| 11 | 의존성 분리 | `vllm`·`bitsandbytes`를 optional로. **`vllm`은 linux/arm64 휠이 없어 필수 의존성으로 두면 이미지 빌드가 실패한다.** `.venv` 9.1GB → 360MB |
| 12 | `AnalyzeResponse`에 `conflicts`(구조화된 `conflict_report`) 추가, `page`(PDF 페이지 번호) 메타데이터를 `search_results`/`conflict_report`까지 전달 | 백오피스 AI Assistance 모달이 "해당 파일 및 위치"/"변경 제안 상세"를 `markdown_report` 문자열 파싱 없이 렌더링하려면 구조화된 위치 정보가 필요했다. `page`는 `PyPDFLoader`가 이미 채집하던 값을 응답까지 흘려보낸 것뿐이라 파이프라인 로직 변경은 없다 |
| 13 | **문서 메타데이터 DB(SQLite) 도입** ([`app/metadata_db.py`](app/metadata_db.py)) + **경로 순회 취약점 수정**(`{document_id}/{원본파일명}` UUID 하위 디렉터리 저장) + **체크섬(SHA-256) 기반 중복 감지** + **컨테이너 기동 시 `SEED_DOCUMENTS_DIR` 자동 색인**, `GET /api/policies/documents`·`UploadResponse` 스키마 확장 | PoC `docs/09`가 제안만 해두고 미구현이던 항목([08](docs/08-migration-checklist.md) 🔴). 백오피스 파일 업로드 UI를 실제로 연동하려니 "문서가 하나도 없어 테스트 불가" 문제가 드러나 이번에 구현. 체크섬 중복 감지 덕분에 API 재업로드와 컨테이너 재기동 시 시드 문서 재처리가 같은 로직으로 방지된다 |
| 14 | 체크섬 중복 감지를 `status='ready'`만 보던 것 → 같은 체크섬의 **모든 행**을 조회(`find_all_by_checksum`)해 `ready`가 아닌 행은 항상 정리(`RagStore._discard_stale_document`)하도록 변경 | 버그 수정. Ollama 콜드스타트 중 자동 시드가 실패하면 `failed` 행이 영구히 남는데, 기존 로직(및 그 1차 수정판 `find_by_checksum`도 "가장 최근 행 1개"만 봤다)은 이미 `ready` 행이 더 최근이면 그것만 보고 끝나서 더 오래된 `failed` 잔재를 영영 못 찾았다 — 재기동을 반복해도 "같은 문서가 중복(실패 1개+성공 1개)으로 보이는" 현상이 실제로 재현됐다. 지금은 상태·최근순 무관하게 전부 훑어 `ready`가 아닌 행을 항상 정리한다 |

---

## 구조

```text
policy-explorer-service/
├─ app/
│  ├─ config.py       환경변수만 읽는다. 외부 연결 없음
│  ├─ metadata_db.py  DocumentMetadataStore — 문서 단위 SQLite 메타데이터 (docs/09)
│  ├─ rag.py          RagStore — Chroma + BM25 앙상블, 업로드/조회/초기화, 자동 시드
│  ├─ graph.py        LangGraph 4노드 + LLM 구조화 출력 스키마
│  ├─ api.py          /api/policies/* 라우터 4개
│  ├─ main.py     앱 조립 + lifespan + /health
│  └─ consul.py   Consul 등록·해제 (표준 라이브러리만)
├─ seed-documents/    컨테이너 기동 시 자동 색인되는 샘플 문서 (git 추적, 민감 문서 금지)
├─ docs/              PoC 리포 docs 01~09 이식본 (+ openapi 실측 스펙)
├─ Dockerfile         uv 베이스. 빌드 컨텍스트는 리포 루트. seed-documents/도 이미지에 포함
└─ pyproject.toml
```

상태(Chroma 연결, BM25 인덱스)는 `RagStore` 인스턴스가 들고 있고, `app.state.store` /
`app.state.graph`로 꺼내 씁니다. 전역 변수를 쓰지 않습니다. 문서 단위 메타데이터(SQLite)는
`RagStore.metadata_store`(`DocumentMetadataStore`)가 들고 있습니다.

> **BM25는 프로세스 메모리에만 존재합니다.** 기동 시 `restore_from_persisted()`가 Chroma에서
> 청크를 읽어 다시 세웁니다. 그래서 이 서비스는 **단일 인스턴스 전제**입니다
> (PoC `docs/07` 참고). 레플리카를 늘리려면 설계 변경이 필요합니다.

---

## 실행과 검증

Ollama는 **compose가 띄우지 않습니다.** 호스트에서 실행 중인 Ollama에 붙습니다. 이유와 전환
방법은 [README.md](README.md)와 `compose.yaml`의 주석을 보세요.

```bash
# 리포 루트에서
docker compose up -d --build policy-explorer-service
curl localhost:8086/health
```

---

## 이번 브랜치에 포함되지 않은 것

작업 범위를 "컨테이너 ↔ Ollama 통신 골격"으로 한정했습니다. 아래는 **의도적으로 남긴 것**이며,
`docs/08`에 항목으로 등록되어 있습니다.

- ~~Gateway 라우팅~~ → **완료.** `/api/policies/**` + `ROLE_ADMIN` 제한. Consul에 등록하므로
  라우트는 `lb://policy-explorer-service`이며, 다른 서비스와 같은 형태다.
  경로를 AI 여부가 아니라 도메인으로 가른 이유: 이 프로젝트에 AI 기반 서비스가 여러 개
  들어갈 예정이므로 `/api/ai/**` 하나를 공유할 수 없다.
- **경로 순회 취약점** — `rag.py`의 `add_document()`가 업로드 파일명을 그대로 경로에 쓴다.
  원본 동작을 보존한 상태이며 코드에 `🚨 TODO` 주석으로 표시했다.
- **문서 메타데이터 DB(SQLite)** — PoC `docs/09`의 제안 설계. 미구현.
- **Loki 로그 연동** — 현재 stdout만. 연동하려면 Alloy의 파일명 규칙
  (`/var/log/lxp/{서비스명}.log`)에 맞춰 로그 파일명을 바꿔야 한다.
- **`langchain-community` 이전** — 패키지 전체가 sunset이지만 `PyPDFLoader`/`Docx2txtLoader`/
  `BM25Retriever`는 독립 패키지가 없어(PyPI 미존재) 대체할 수 없다.
- **테스트** — `tests/` 미작성.

---

## 작업 규칙

리포 루트의 [`AGENTS.md`](../AGENTS.md)를 따릅니다. 특히:

- 파일 수정·의존성 변경·compose 변경·컨테이너 기동은 **사용자 승인 후** 진행한다.
- 임의 리팩토링은 하지 않는다. 위 "그대로 보존한 것" 표의 값은 근거 없이 바꾸지 않는다.
- 답변은 한국어로 한다.
