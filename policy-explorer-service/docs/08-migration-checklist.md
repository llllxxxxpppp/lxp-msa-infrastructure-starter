# 08. 이식 체크리스트 (Migration Checklist)

> 🔄 **이식 반영 — 이 문서는 진행 상황을 담도록 갱신됐습니다**
> PoC 리포의 원본은 "앞으로 할 일" 목록이었습니다. 이식 작업이 실제로 진행됐으므로 **완료 항목에
> 체크하고, 이식 과정에서 새로 발견된 항목을 추가**했습니다. 코드를 고치기 전에 이 문서를 먼저
> 보세요 — 무엇이 끝났고 무엇이 **의도적으로** 남아 있는지 구분되어 있습니다.

작업 범위는 "컨테이너 ↔ Ollama 통신 골격"으로 한정했습니다. 남은 항목은 근거 없이 미뤄둔 것이
아니라, 리뷰 단위를 작게 유지하려고 분리한 것입니다.

---

## ✅ 완료 (이식 작업에서 처리)

- [x] **환경변수화** — 포트, `OLLAMA_BASE_URL`/`OLLAMA_MODEL`, 임베딩 모델, `CHROMA_PERSIST_DIR`,
      `UPLOAD_DIR`, `LOG_DIR`, `CURRENT_ENGINE`/`CURRENT_MODEL`을 [`app/config.py`](../app/config.py)로
      일괄 추출. `compose.yaml`이 값을 주입한다. ([03](03-environment-config.md))
- [x] **`ChatOllama`에 `base_url` 주입** — 원본은 인자가 없어 `localhost:11434`(= 컨테이너 자신)를
      봤다. 컨테이너에서 Ollama에 도달할 수 없던 결정적 원인. 원본 `docs/03`이 표로 지적했던
      "Ollama 호스트는 langchain-ollama 기본값 사용, 별도 지정 없음"이 이 항목이다.
- [x] **헬스체크 엔드포인트 추가** — `GET /health`가 프로세스 · Ollama 도달 · **필요 모델 보유** ·
      Chroma 접근을 확인한다. `compose.yaml`의 `healthcheck`에 연결. ([07](07-operations-runbook.md))
- [x] **의존성 그룹 분리** — `vllm`/`bitsandbytes`/`langchain-openai`를
      `[project.optional-dependencies].vllm`으로 분리. `.venv` **9.1GB → 360MB**.
      단순 최적화가 아니라 필수였다: `vllm`은 linux/arm64 휠이 없어 필수 의존성으로 두면
      **이미지 빌드 자체가 실패**한다. ([05](05-runtime-requirements.md))
- [x] **임베딩 device 문제 해소** — 원본 🟡 항목("가용 시 자동으로 GPU를 쓰므로 device 명시
      옵션화 필요")은 임베딩을 Ollama `bge-m3`로 옮기면서 사라졌다. 컨테이너에 torch가 없다.
- [x] **`response_model` 명시** — 4개 엔드포인트 전부. OpenAPI 스펙에 실제 응답 스키마가
      드러난다. ([02](02-api-specification.md))
- [x] **`.gitignore` 보강** — `chroma_db*/`, `uploaded_documents/`, `logs/`, `.venv/` 제외.
      루트 `.gitignore`는 Java 기준이라 Python 산출물 항목이 없었다.
- [x] **import 부작용 제거** — 원본은 모듈 import만으로 Chroma 접속·데이터 복원·폴더 생성이
      일어났다. FastAPI `lifespan`으로 옮겨 실행 시점을 명시했다.
- [x] **Chroma를 전용 독립 패키지로 교체** — `langchain_community.vectorstores.Chroma`는 폐기
      예정. `langchain-chroma`로 옮겨 같은 리포의 `ai-bot-service`와 라이브러리를 통일했다.
- [x] **컨테이너화 + compose 통합** — [`Dockerfile`](../Dockerfile), 리포 루트 `compose.yaml`.
      볼륨 2개로 상태·원본 파일 분리 영속화. ([04](04-deployment-guide.md))

---

## 🔴 필수 — 아직 남음

- [ ] **경로 순회 취약점 수정** — [`app/rag.py`](../app/rag.py) `add_document()`의
      `os.path.join(config.UPLOAD_DIR, filename)`이 업로드 파일명을 그대로 쓴다.
      `os.path.basename()` + 화이트리스트 검증, 또는 서버 생성 UUID 파일명으로 교체.
      **코드에 `🚨 TODO` 주석으로 표시되어 있다.** ([06](06-data-and-security.md))
      > 원본 동작을 그대로 이식한 상태다. 아래 "메타데이터 DB" 항목과 함께 처리하면
      > 저장 키를 `{document_id}/{original_filename}`으로 바꿔 두 문제를 동시에 해소할 수 있다.
- [ ] **인증/인가 추가** — 현재 8086 포트가 직접 노출되어 있고 어떤 엔드포인트에도 인증이 없다.
      Gateway 라우팅(아래)이 붙으면 JWT 검증이 앞단에 생기지만, 업로드/초기화
      (`DELETE /api/policies/documents`)의 인가 정책은 별도로 설계해야 한다. ([06](06-data-and-security.md))
- [ ] **Gateway 라우팅** — `config-repo/gateway.yml`과 `gateway/src/main/resources/application.yml`에
      `/api/policies/**` 라우트 추가 + `compose.yaml`의 `gateway`에 호스트/포트 주입.
      > 경로를 `/api/ai/**`로 잡지 않는다. PR #61 리뷰에서 "이 프로젝트에 AI 기반 서비스가 총 3개
      > 들어갈 예정"이라는 지적이 있었다. AI 여부가 아니라 **도메인**으로 경로를 가른다.
- [ ] **문서 메타데이터 DB(SQLite) 도입** — `documents` 테이블 스키마 적용 후 업로드/조회/초기화를
      SQLite 기반으로 전환. 동일 파일명 재업로드 시 조용히 덮어써지는 문제와 경로 순회를 함께
      완화한다. ([09](09-data-architecture.md))

---

## 🟡 권장

- [ ] **`langchain-community` 이전** — 패키지 **전체가 sunset**(더 이상 유지보수되지 않음)이다.
      이식 과정에서 발견한 항목. 현재 세 곳에서 쓴다:
      `PyPDFLoader`/`Docx2txtLoader`(document_loaders), `BM25Retriever`(retrievers).
      > Chroma는 `langchain-chroma`로 옮겼지만 나머지는 **독립 패키지가 없다**
      > (`langchain-pypdf`/`langchain-docx`/`langchain-bm25` 모두 PyPI 미존재).
      > `langchain_classic`에도 있지만 "community에서 가져오라"고 되돌려 보내는 호환 껍데기다.
      > 즉 Chroma만 바꿔도 sunset 경고는 남는다. 대체 경로가 생길 때까지 관찰 대상.
- [ ] **Loki 로그 연동** — 현재 stdout만 쓴다. 리포의 Java 서비스들은
      `LOG_DIR: /logs` + `./infrastructure/logs:/logs`로 파일을 남기고 Alloy가 Loki로 보낸다.
      연동하려면 Alloy의 서비스명 추출 규칙(`/var/log/lxp/{서비스명}.log`)에 맞춰 로그 파일명을
      `performance.log` → `policy-explorer-service.log`로 바꿔야 한다.
      ([07](07-operations-runbook.md))
- [ ] **구조화 로깅** — 텍스트 포맷을 JSON으로 전환하면 중앙 로그 수집과의 연동이 쉬워진다.
- [ ] **테스트 작성** — `tests/` 미작성. `pytest`/`httpx`는 dev 그룹에 이미 선언되어 있다
      (컨테이너에는 `--no-dev`로 설치되지 않는다). 최소 항목: `/health`, 미지원 확장자 400,
      Ollama를 모킹한 파이프라인.
- [ ] **업로드 엔드포인트 검증** — `POST /api/policies/documents/upload`는 실제 PDF/DOCX 파일이 필요해
      아직 검증하지 않았다. 나머지 4개 엔드포인트는 컨테이너에서 확인 완료.
- [ ] **NFS 공유 볼륨** — 업로드 원본을 로컬 named volume에서 NFS로 전환. NFS 서버 구축/운영
      주체를 다른 팀과 사전 협의 필요. ([09](09-data-architecture.md))
- [ ] **이미지 용량 추가 절감 검토** — 797MB 중 의존성 408MB의 대부분이 `chromadb`의 전이
      의존성이다(`onnxruntime` 76MB, `chromadb_rust_bindings` 50MB, `kubernetes` 41MB,
      `grpc` 38MB). `onnxruntime`은 chromadb 기본 임베딩 함수용인데 우리는 Ollama 임베딩을
      쓰므로 실사용하지 않는다.

---

## 🟢 장기 검토 (아키텍처 변경 수반)

- [ ] **멀티 레플리카 지원** — BM25 인메모리 상태 + 로컬 Chroma persist 구조는 단일 인스턴스
      전제다. 외부 Chroma 서버 모드나 pgvector로 전환할지 검토. ([07](07-operations-runbook.md))
- [ ] **임베딩 교체 후 검색 품질 재검증** — 이식 시 `jhgan/ko-sroberta-multitask`(768차원,
      한국어 특화)에서 `bge-m3`(1024차원, 다국어)로 바꿨다. PoC 리포 `select_reason.md` 3절의
      채택 근거를 변경한 것이므로, 실제 사내 문서로 검색 품질을 비교 측정하는 것이 좋다.
- [ ] **Excel/HWP 업로드 지원 여부 재검토** — 현재 PDF/DOCX만. 사유는 PoC 리포
      `select_reason.md` 8절.
- [ ] **문서 단위 삭제 API** — 현재 `DELETE /api/policies/documents`는 전체 초기화만 지원.
      메타데이터 DB 도입 후 `DELETE /api/policies/documents/{document_id}` 추가 여부 결정.
- [ ] **메타데이터 DB 확장** — 멀티 레플리카로 가면 SQLite를 PostgreSQL 등으로 승격 검토.
- [ ] **벤치마크 스크립트 운영 방식** — PoC 리포의 엔진×모델 비교 스크립트 4종을 사내 CI 성능
      회귀 테스트로 재활용할지, 이식 대상에서 계속 제외할지 결정. 재활용한다면
      `pyproject.toml`의 `vllm` extra를 쓰면 된다. ([01](01-service-overview.md))

---

## 진행 방법

1. 🔴 항목을 하나씩 별도 PR로 진행한다 (한 항목 = 한 변경으로 리뷰를 쉽게 유지).
   > PR #61(ai-bot)이 4546줄 한 번에 올라가 리뷰가 멈춘 사례가 있다.
2. 각 항목 완료 시 이 체크리스트와 관련 문서(02/03/06/07)를 함께 갱신한다.
3. 리포 루트 [`AGENTS.md`](../../AGENTS.md)에 따라 파일 수정·의존성 변경·compose 변경·컨테이너
   기동은 사용자 승인 후 진행한다.
