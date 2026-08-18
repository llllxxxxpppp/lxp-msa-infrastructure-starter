FROM ghcr.io/astral-sh/uv:python3.12-bookworm-slim

WORKDIR /app

COPY policy-explorer-service/pyproject.toml policy-explorer-service/uv.lock ./
RUN uv sync --frozen --no-dev --no-install-project

COPY policy-explorer-service/app ./app
COPY policy-explorer-service/seed-documents ./seed-documents

RUN mkdir -p /data/state /data/uploads

EXPOSE 8086

CMD ["/app/.venv/bin/python", "-m", "app.main"]
