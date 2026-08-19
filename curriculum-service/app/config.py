"""애플리케이션 설정."""

import logging
import os

logging.basicConfig(level=logging.INFO)


def _positive_int_env(name: str, default: int) -> int:
    raw_value = os.getenv(name, str(default))
    try:
        value = int(raw_value)
    except ValueError as exc:
        raise ValueError(f"{name} 환경 변수는 양의 정수여야 합니다.") from exc
    if value <= 0:
        raise ValueError(f"{name} 환경 변수는 양의 정수여야 합니다.")
    return value


OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "qwen3.5:4b")
OLLAMA_EMBEDDING_MODEL = os.getenv(
    "OLLAMA_EMBEDDING_MODEL",
    "qwen3-embedding:0.6b",
)
COURSE_SERVICE_BASE_URL = os.getenv(
    "COURSE_SERVICE_BASE_URL",
    "http://course-service",
)
PROVIDER = os.getenv("PROVIDER", "http").lower()
RABBITMQ_HOST = os.getenv("RABBITMQ_HOST", "localhost")
RABBITMQ_PORT = int(os.getenv("RABBITMQ_PORT", "5672"))
RABBITMQ_USERNAME = os.getenv("RABBITMQ_USERNAME", "admin")
RABBITMQ_PASSWORD = os.getenv("RABBITMQ_PASSWORD", "admin")
SESSION_TIMEOUT_SECONDS = _positive_int_env("SESSION_TIMEOUT_SECONDS", 1800)
SESSION_CLEANUP_INTERVAL_SECONDS = _positive_int_env(
    "SESSION_CLEANUP_INTERVAL_SECONDS",
    60,
)
