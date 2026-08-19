"""환경변수 기반 설정.

PoC 리포(policy-explorer-service)의 `lxp-ollama-qwen-fileupload.py`에 하드코딩되어 있던
값들을 한곳으로 모은 모듈이다. 어떤 값을 왜 환경변수로 빼야 하는지는 그 리포의
`docs/03-environment-config.md`에 정리되어 있다.

🚨 이 모듈은 import 시 환경변수만 읽는다. Ollama/Chroma 같은 외부 연결은 하지 않는다.
   (연결은 main.py의 lifespan에서 명시적으로 수행한다.)
"""

import os


def _env_int(name: str, default: int) -> int:
    """정수형 환경변수를 읽는다. 비어 있거나 숫자가 아니면 기본값을 쓴다."""
    raw = os.environ.get(name, "").strip()
    try:
        return int(raw) if raw else default
    except ValueError:
        return default


# ---------------------------------------------------------
# 서비스
# ---------------------------------------------------------
# 인프라 리포의 808x 포트 컨벤션을 따른다.
# (gateway 8080 / auth 8081 / member 8082 / course 8083 / subscription 8084,
#  8085는 prometheus-docker.yml에 payment-service로 예약되어 있어 8086을 쓴다.)
SERVICE_PORT = _env_int("SERVICE_PORT", 8086)

# ---------------------------------------------------------
# Ollama (생성 + 임베딩 둘 다 여기로 간다)
# ---------------------------------------------------------
# 🚨 컨테이너 안에서 localhost는 "컨테이너 자신"이므로 반드시 이 값을 주입해야 한다.
#    docker compose에서는 http://host.docker.internal:11434 (호스트 Ollama)를 넣는다.
OLLAMA_BASE_URL = os.environ.get("OLLAMA_BASE_URL", "http://localhost:11434").rstrip("/")

# 생성 모델 — 규정 팩트 추출(Node 1)과 충돌 분석(Node 3)에 쓰인다.
OLLAMA_MODEL = os.environ.get("OLLAMA_MODEL", "qwen2.5:7b")

# 임베딩 모델 — PoC 리포는 HuggingFace `jhgan/ko-sroberta-multitask`(768차원)를 컨테이너
# 안에서 torch로 직접 계산했다. 이식 시 Ollama가 서빙하는 bge-m3(1024차원)로 교체해
# 이미지에서 torch/sentence-transformers를 제거했다. (근거: CLAUDE.md의 "이식 시 변경점")
OLLAMA_EMBEDDING_MODEL = os.environ.get("OLLAMA_EMBEDDING_MODEL", "bge-m3")

# ---------------------------------------------------------
# 벡터 스토어 / 파일 저장
# ---------------------------------------------------------
# 🚨 임베딩 모델이 바뀌면 벡터 차원도 바뀐다(768 -> 1024). 기존 컬렉션에 그대로 넣으면
#    차원 불일치로 실패하므로, 컬렉션명과 persist 경로를 새로 분리했다.
CHROMA_PERSIST_DIR = os.environ.get("CHROMA_PERSIST_DIR", "./chroma_db_fileupload")
CHROMA_COLLECTION_NAME = os.environ.get("CHROMA_COLLECTION_NAME", "policy_docs_bge_m3")

UPLOAD_DIR = os.environ.get("UPLOAD_DIR", "./uploaded_documents")

# 문서 단위 메타데이터(docs/09-data-architecture.md 제안 스키마)를 담는 SQLite 파일.
# Chroma와 마찬가지로 상태 볼륨(policy-explorer-state)에 둬야 재기동 시 유실되지 않는다.
METADATA_DB_PATH = os.environ.get("METADATA_DB_PATH", "./documents.db")

# 컨테이너 기동 시 자동으로 색인할 샘플 문서 디렉터리. Dockerfile이 이 경로를 이미지에
# 구워 넣는다(빌드 컨텍스트 기준 policy-explorer-service/seed-documents). 디렉터리가 없으면
# 그냥 건너뛴다 — 로컬 개발에서 샘플 문서 없이 띄워도 기동에 지장이 없다.
SEED_DOCUMENTS_DIR = os.environ.get("SEED_DOCUMENTS_DIR", "./seed-documents")

# ---------------------------------------------------------
# RAG 파라미터 (TOP_K는 PoC 원본 값, 청킹 값은 seed-documents 실측으로 조정)
# ---------------------------------------------------------
# seed-documents 50개(표준취업규칙 + 사내 문서 49개)를 실측해 정한 값이다.
# 800은 두 문서군의 의미 단위(조문 / [ 목적 ] 섹션)가 모두 한 청크에 담기는 최소 크기다.
#   - 500: 조문 온전성 52%, 섹션 온전성 67%
#   - 800: 조문 온전성 93%, 섹션 온전성 100%
#   - 600으로 줄이면 사내 문서 섹션이 넘쳐 68%로 무너진다.
RAG_CHUNK_SIZE = _env_int("RAG_CHUNK_SIZE", 800)
# 조문 경계에서 잘릴 때 앞 조문의 꼬리를 남겨 문맥을 잇는다(크기의 15%).
RAG_CHUNK_OVERLAP = _env_int("RAG_CHUNK_OVERLAP", 120)
RAG_TOP_K = _env_int("RAG_TOP_K", 2)

# ---------------------------------------------------------
# 로깅
# ---------------------------------------------------------
# 미설정이면 stdout만 사용한다(컨테이너 로그 드라이버에 위임).
# 값이 있으면 그 디렉터리에 performance.log를 함께 남긴다.
LOG_DIR = os.environ.get("LOG_DIR", "").strip()

# ---------------------------------------------------------
# 응답/로그 라벨
# ---------------------------------------------------------
# PoC 리포에서는 소스코드 상수여서, 모델만 바꾸면 라벨이 실제 모델과 어긋났다.
# 기본값을 실제 사용 모델에서 파생시켜 어긋날 수 없게 한다.
CURRENT_ENGINE = os.environ.get("CURRENT_ENGINE", "Ollama")
CURRENT_MODEL = os.environ.get("CURRENT_MODEL", OLLAMA_MODEL)

# 헬스체크에서 Ollama 응답을 기다리는 시간(초)
HEALTH_TIMEOUT_SECONDS = _env_int("HEALTH_TIMEOUT_SECONDS", 3)

# ---------------------------------------------------------
# Consul (서비스 디스커버리)
# ---------------------------------------------------------
# gateway가 `lb://<이름>` 으로 이 서비스를 찾을 수 있도록 Consul에 등록한다.
# Java 서비스는 spring-cloud-starter-consul-discovery가 자동으로 하지만,
# 이 서비스는 Python이라 Consul HTTP API를 직접 호출한다(app/consul.py).
#
# CONSUL_HOST가 비어 있으면 등록을 건너뛴다. IDE·터미널에서 단독 실행할 때를 위한 것이다.
CONSUL_HOST = os.environ.get("CONSUL_HOST", "").strip()
CONSUL_PORT = _env_int("CONSUL_PORT", 8500)

# Consul 카탈로그에 등록될 이름. gateway 라우트의 lb:// 대상과 같아야 한다.
SERVICE_NAME = os.environ.get("SERVICE_NAME", "policy-explorer-service")

# Consul이 우리 /health를 호출하는 주기·타임아웃.
# Java 서비스의 health-check-interval(10s)과 맞춘다.
CONSUL_CHECK_INTERVAL = os.environ.get("CONSUL_CHECK_INTERVAL", "10s")
CONSUL_CHECK_TIMEOUT = os.environ.get("CONSUL_CHECK_TIMEOUT", "5s")

# critical 상태가 이 시간을 넘으면 Consul이 카탈로그에서 자동 제거한다.
# config-repo/application.yml의 health-check-critical-timeout과 같은 값이며,
# Consul이 강제하는 최소값이 1분이라 그보다 작은 값은 무시된다.
CONSUL_DEREGISTER_AFTER = os.environ.get("CONSUL_DEREGISTER_AFTER", "1m")
