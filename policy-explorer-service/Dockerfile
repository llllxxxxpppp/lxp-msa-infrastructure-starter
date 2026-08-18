# policy-explorer-service 컨테이너 이미지
#
# 빌드 컨텍스트는 리포 루트다(Java 서비스들과 동일한 패턴). 따라서 COPY 경로에
# `policy-explorer-service/` 접두사가 붙는다.
#   docker compose build policy-explorer-service
#   docker build -f policy-explorer-service/Dockerfile -t policy-explorer-service .
#
# GPU는 필요 없다. LLM 추론과 임베딩 모두 외부 Ollama가 담당하고(OLLAMA_BASE_URL),
# 이 컨테이너는 그 HTTP API를 호출하는 클라이언트다.
FROM ghcr.io/astral-sh/uv:python3.12-bookworm-slim

WORKDIR /app

# 의존성 레이어를 코드보다 먼저 만든다. 코드만 고칠 때는 이 아래 RUN이 캐시에서
# 재사용되므로 재빌드가 빠르다(패키지 250개 재설치를 건너뛴다).
#   --frozen           : uv.lock을 그대로 쓴다(해결 단계 생략, 버전 드리프트 방지)
#   --no-dev           : pytest/httpx 같은 개발 전용 그룹을 제외한다
#   --no-install-project: 아직 코드를 안 복사했으므로 프로젝트 자신은 건너뛴다
COPY policy-explorer-service/pyproject.toml policy-explorer-service/uv.lock ./
RUN uv sync --frozen --no-dev --no-install-project

# 애플리케이션 코드
COPY policy-explorer-service/app ./app

# 런타임 산출물 디렉터리. compose에서 볼륨으로 마운트한다.
#   /data/state   : Chroma 벡터 데이터 (+ LOG_DIR 지정 시 로그)
#   /data/uploads : 업로드 원본 파일
RUN mkdir -p /data/state /data/uploads

# 문서화 목적. 실제 리슨 포트는 SERVICE_PORT 환경변수가 결정한다.
EXPOSE 8086

# app/main.py의 __main__ 블록이 config.SERVICE_PORT로 uvicorn을 띄운다.
# 포트를 CMD에 박지 않아 SERVICE_PORT 하나로 일관되게 제어된다.
CMD ["/app/.venv/bin/python", "-m", "app.main"]
