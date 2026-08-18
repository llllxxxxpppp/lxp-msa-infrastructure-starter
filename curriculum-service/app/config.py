"""애플리케이션 설정."""

import logging
import os

logging.basicConfig(level=logging.INFO)

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
