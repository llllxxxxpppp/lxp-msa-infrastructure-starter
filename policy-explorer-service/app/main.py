"""FastAPI 애플리케이션 조립 + 헬스체크.

PoC 리포(policy-explorer-service)의 `lxp-ollama-qwen-fileupload.py`를 이식한 서비스의
진입점이다. 서비스 전체 개요와 파이프라인 설명은 ../CLAUDE.md 및 ../docs/ 참고.

원본에 없던 것
  - `GET /health` : 프로세스 / Ollama 도달 / Chroma 접근을 한 번에 확인한다.
    PoC 리포 `docs/07-operations-runbook.md`가 "헬스체크 엔드포인트 없음"으로 지적한 항목이며,
    docker compose의 healthcheck와 "컨테이너 <-> Ollama 통신 성공"의 증명 수단이다.
  - lifespan : 원본은 import만 해도 Chroma에 연결됐다. 연결·복원을 여기로 모았다.
  - Consul 등록 : gateway가 lb:// 로 찾을 수 있도록 기동 시 등록하고 종료 시 해제한다.
    등록 실패는 기동을 막지 않으며 /health의 consul 항목에 드러난다.
"""

import json
import logging
import os
import sys
from contextlib import asynccontextmanager
from urllib.error import URLError
from urllib.request import urlopen

from fastapi import FastAPI

from app import config
from app.api import router
from app.consul import ConsulRegistration
from app.graph import build_graph
from app.rag import RagStore

# ---------------------------------------------------------
# 로깅 — 기본은 stdout(컨테이너 로그 드라이버에 위임).
# LOG_DIR이 설정된 경우에만 performance.log 파일을 함께 남긴다.
# ---------------------------------------------------------
_handlers: list[logging.Handler] = [logging.StreamHandler(sys.stdout)]
if config.LOG_DIR:
    os.makedirs(config.LOG_DIR, exist_ok=True)
    _handlers.append(
        logging.FileHandler(os.path.join(config.LOG_DIR, "performance.log"), encoding="utf-8")
    )

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(message)s",
    handlers=_handlers,
)
logger = logging.getLogger(__name__)


def _normalize_model_tag(name: str) -> str:
    """Ollama는 태그를 생략하면 :latest로 취급한다. 비교를 위해 형식을 맞춘다."""
    return name if ":" in name else f"{name}:latest"


def _check_ollama() -> dict:
    """Ollama에 실제로 HTTP 요청을 보내 도달 여부와 필요한 모델 보유 여부를 확인한다."""
    result = {"status": "DOWN", "base_url": config.OLLAMA_BASE_URL}
    try:
        with urlopen(
            f"{config.OLLAMA_BASE_URL}/api/tags", timeout=config.HEALTH_TIMEOUT_SECONDS
        ) as response:
            payload = json.load(response)
    except (URLError, TimeoutError, OSError, json.JSONDecodeError) as e:
        result["error"] = str(e)
        return result

    installed = {_normalize_model_tag(m.get("model", "")) for m in payload.get("models", [])}
    required = {
        "llm": config.OLLAMA_MODEL,
        "embedding": config.OLLAMA_EMBEDDING_MODEL,
    }
    missing = [
        name for name in required.values() if _normalize_model_tag(name) not in installed
    ]

    result["status"] = "UP"
    result["models"] = required
    if missing:
        result["status"] = "MODEL_MISSING"
        result["missing_models"] = missing
    return result


def _check_chroma(store: RagStore | None) -> dict:
    """Chroma 컬렉션에 실제로 접근되는지 확인한다."""
    if store is None:
        return {"status": "DOWN", "error": "store not initialized"}
    try:
        return {
            "status": "UP",
            "collection": config.CHROMA_COLLECTION_NAME,
            "persisted_chunks": store.ping(),
            "loaded_chunks": store.chunk_count,
        }
    except Exception as e:  # noqa: BLE001
        return {"status": "DOWN", "error": str(e)}


def _check_consul(registration: ConsulRegistration | None) -> dict:
    """Consul 등록 상태를 반환한다. 등록을 쓰지 않는 실행에서는 DISABLED."""
    if registration is None:
        return {"status": "DOWN", "error": "registration not initialized"}
    return registration.snapshot()


@asynccontextmanager
async def lifespan(app: FastAPI):
    """기동 시 RAG 저장소를 연결하고 기존 청크를 복원한 뒤 그래프를 조립한다."""
    logger.info(
        "[Startup] Ollama=%s / llm=%s / embedding=%s",
        config.OLLAMA_BASE_URL,
        config.OLLAMA_MODEL,
        config.OLLAMA_EMBEDDING_MODEL,
    )
    store = RagStore()
    store.restore_from_persisted()

    seed_result = store.seed_from_directory(config.SEED_DOCUMENTS_DIR)
    if seed_result["ingested"] or seed_result["duplicate"] or seed_result["failed"]:
        logger.info(
            "[Startup] 시드 문서 색인 결과: ingested=%d duplicate=%d failed=%d",
            seed_result["ingested"],
            seed_result["duplicate"],
            seed_result["failed"],
        )

    app.state.store = store
    app.state.graph = build_graph(store)

    # 그래프까지 준비된 뒤에 등록한다. Consul이 곧 /health를 호출하므로
    # 응답할 준비가 되기 전에 등록하면 첫 체크가 실패할 수 있다.
    registration = ConsulRegistration()
    registration.register()
    app.state.consul = registration

    logger.info("[Startup] 준비 완료 (port=%s)", config.SERVICE_PORT)
    yield

    # 종료 시 카탈로그에서 제거해 gateway가 죽은 인스턴스로 라우팅하지 않게 한다.
    registration.deregister()


app = FastAPI(
    title="LXP Policy Explorer Service",
    description="사내 규정 개정안이 기존 문서와 충돌하는지 로컬 LLM으로 검출하는 백오피스 서비스",
    lifespan=lifespan,
)
app.include_router(router)


@app.get("/health", tags=["health"])
async def health() -> dict:
    """프로세스 / Ollama / Chroma / Consul 등록 상태를 한 번에 반환한다.

    의존 컴포넌트(Ollama)가 죽어도 200을 반환한다. 컨테이너 자신은 살아 있으므로
    healthcheck를 실패시켜 재시작 루프에 빠지게 하지 않는 편이 안전하다.
    대신 본문의 status가 UP -> DEGRADED로 바뀌어 어디가 문제인지 드러난다.
    """
    ollama = _check_ollama()
    chroma = _check_chroma(getattr(app.state, "store", None))
    consul = _check_consul(getattr(app.state, "consul", None))

    # Consul을 쓰지 않는 실행(DISABLED)은 정상으로 본다.
    consul_ok = consul["status"] in ("UP", "DISABLED")
    overall = (
        "UP"
        if ollama["status"] == "UP" and chroma["status"] == "UP" and consul_ok
        else "DEGRADED"
    )
    return {"status": overall, "ollama": ollama, "chroma": chroma, "consul": consul}


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(app, host="0.0.0.0", port=config.SERVICE_PORT)
