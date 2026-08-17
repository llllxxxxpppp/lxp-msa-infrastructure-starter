FROM ghcr.io/astral-sh/uv:python3.12-bookworm-slim

WORKDIR /app

# 의존성 파일을 먼저 복사해서 Docker 캐시를 사용한다.
COPY ai-bot-service/pyproject.toml ai-bot-service/uv.lock ./
RUN uv sync --frozen --no-dev --no-install-project

# FastAPI 소스코드
COPY ai-bot-service/app ./app

CMD ["/app/.venv/bin/uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]