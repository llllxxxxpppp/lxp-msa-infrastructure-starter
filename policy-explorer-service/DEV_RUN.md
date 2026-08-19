# 로컬 실행 가이드 — Ollama 연결 방법 (Docker / 로컬 중 선택)

이 서비스는 생성(`qwen3.5:4b`)·임베딩(`bge-m3`) 둘 다 Ollama를 씁니다. Ollama를 **어디서
실행할지** 두 가지 경로가 있고, 리포 루트의 [DEV_RUN.md](../DEV_RUN.md)와 별개로 이 서비스에
특화된 절차만 여기 정리합니다. 배경(왜 두 경로가 있는지)은 [README.md](README.md#리눅스--nvidia-gpu-서버로-옮길-때)와
[CLAUDE.md](CLAUDE.md)를 참고하세요.

| | 기본: Docker Ollama | 대안: 로컬(호스트) Ollama |
|---|---|---|
| 언제 쓰나 | 대부분의 경우 (별도 설치 불필요) | GPU 패스스루가 중요한 경우 (예: macOS Metal) |
| Ollama 위치 | `compose.yaml`의 `ollama` 컨테이너 | 개발자 PC에 직접 설치 |
| 모델 준비 | `ollama-model-init` 컨테이너가 자동 pull | 직접 `ollama pull` |
| 속도 | Docker Desktop 리눅스 VM 안 → GPU 미전달 시 CPU 추론(느림) | 호스트 GPU 100% 사용(빠름) |

---

## 경로 1 — 기본: Docker Ollama

이 프로젝트의 다른 AI 서비스(`ai-tutor-service`, `curriculum-service`)와 **같은 Ollama
컨테이너를 공유**합니다. 이 서비스가 쓰는 모델(`qwen3.5:4b`, `bge-m3`)도 그 컨테이너의
초기화 컨테이너(`ollama-model-init`)가 함께 자동으로 받아옵니다 — 사람이 따로 준비할 게
없습니다.

```bash
# 리포 루트에서
docker compose up -d --build ollama ollama-model-init policy-explorer-service
```

> 💡 `policy-explorer-service`는 `depends_on`으로 `ollama-model-init`이 끝나길 기다리도록
> 설정돼 있으므로, 위처럼 `policy-explorer-service`만 지정해도 `ollama`/`ollama-model-init`이
> 자동으로 함께 뜹니다. 전체 스택을 띄우는 `docker compose up -d --build`도 그대로 동작합니다.

**첫 실행은 오래 걸립니다** — `ollama-model-init`이 `qwen3.5:4b`(약 2.5GB)·
`qwen3-embedding:0.6b`(약 0.6GB)·`bge-m3`(약 1.2GB) 세 모델을 순서대로 pull합니다(합쳐서
수 GB, 네트워크 속도에 따라 수 분~수십 분). 두 번째 실행부터는 `ollama-data` 볼륨에 캐시돼
있어 즉시 끝납니다.

### 확인

```bash
docker compose ps ollama ollama-model-init policy-explorer-service
# ollama-model-init은 "Exited (0)"가 정상 종료(성공)입니다 — 계속 떠 있는 상태가 아닙니다.

curl localhost:8086/health
```

`ollama.status`가 `UP`이고 `base_url`이 `http://ollama:11434`, `models.llm`이 `qwen3.5:4b`,
`models.embedding`이 `bge-m3`로 나오면 정상입니다. 응답 해석표는
[README.md](README.md#통신-확인)를 참고하세요.

---

## 경로 2 — 대안: 로컬(호스트) Ollama

GPU 패스스루가 중요해서 호스트에서 직접 Ollama를 돌리고 싶다면, `.env`에서
`OLLAMA_BASE_URL`을 override합니다. **코드도 `compose.yaml`도 고치지 않습니다.**

### 1) 호스트에 Ollama 설치 후 모델 준비

```bash
# Ollama 실행 확인
curl http://localhost:11434          # -> "Ollama is running"

# 이 서비스가 쓰는 모델 2개
ollama pull qwen3.5:4b
ollama pull bge-m3

ollama list                          # 두 모델이 보여야 한다
```

### 2) 리포 루트 `.env`에 override 추가

```dotenv
OLLAMA_BASE_URL=http://host.docker.internal:11434
```

`policy-explorer-service`의 `extra_hosts` 설정 덕분에 컨테이너 안에서도
`host.docker.internal`이 호스트 머신으로 정상 해석됩니다.

> ⚠️ 이 override는 `policy-explorer-service`에만 적용됩니다. 같은 스택에 있는
> `ai-tutor-service`/`curriculum-service`는 여전히 공유 Docker Ollama(`http://ollama:11434`)를
> 그대로 씁니다 — 필요하면 그 두 서비스도 각자 환경변수를 따로 override해야 합니다.

### 3) 기동 및 확인

```bash
docker compose up -d --build policy-explorer-service
curl localhost:8086/health
```

`ollama.base_url`이 `http://host.docker.internal:11434`로 나오면 override가 적용된 것입니다.

### 컨테이너 없이 완전히 로컬로 실행하고 싶다면

```bash
cd policy-explorer-service
uv sync
uv run python -m app.main       # http://localhost:8086
```

`OLLAMA_BASE_URL` 기본값이 `http://localhost:11434`라 위 1)에서 준비한 호스트 Ollama에 바로
붙습니다. `.env` override는 필요 없습니다(compose를 안 거치므로).

---

## 문제 해결

- **`ollama.status: DOWN`**: 경로 1이면 `docker compose ps ollama`로 컨테이너가 떠 있는지,
  경로 2면 호스트에서 `curl http://localhost:11434`가 응답하는지 확인하세요.
- **`ollama.status: MODEL_MISSING`**: `missing_models` 필드에 어떤 모델이 없는지 나옵니다.
  경로 1이면 `docker compose logs ollama-model-init`으로 pull이 실패하지 않았는지 확인하고,
  경로 2면 `ollama pull <모델명>`으로 직접 받으세요.
- **`ollama-model-init`이 계속 "Restarting"으로 보임**: 정상입니다 — 이 컨테이너는 pull이
  끝나면 `Exited (0)`으로 완전히 종료됩니다. 재시작 정책이 걸려 있지 않으니, 계속 재시작
  중이라면 pull 자체가 실패하고 있는 것입니다 — 로그를 확인하세요.
- **첫 기동이 너무 오래 걸림**: 위 "첫 실행은 오래 걸립니다" 참고 — 네트워크 속도에 따라
  정상적으로 수 분~수십 분 걸릴 수 있습니다. `docker compose logs -f ollama-model-init`으로
  진행 상황을 볼 수 있습니다.
