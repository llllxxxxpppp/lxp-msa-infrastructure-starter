# 05. 실행 전제조건 (Runtime Requirements)

> 🔄 **이식 반영 — 의존성 용량과 GPU 요구가 크게 달라졌습니다**
> - **`.venv` 9.1GB → 360MB.** `vllm`/`bitsandbytes`를 optional로 분리하고,
>   임베딩을 Ollama에 맡겨 `sentence-transformers`·torch를 제거했습니다.
>   (`vllm`은 linux/arm64 휠이 없어 필수 의존성으로 두면 **이미지 빌드 자체가 실패**합니다.)
> - **컨테이너 이미지 797MB** (의존성 레이어 408MB + 베이스 389MB).
> - **HuggingFace Hub 접근이 더 이상 필요하지 않습니다.** 임베딩 모델을 HF에서 받지 않습니다.
>   대신 Ollama에 `qwen2.5:7b`와 `bge-m3`가 있어야 합니다.
> - 이 서비스 컨테이너는 GPU가 필요 없습니다. GPU가 필요한 쪽은 Ollama입니다.
>   macOS에서는 Ollama를 호스트에서 실행해야 GPU를 씁니다(Docker VM에 Metal 미전달).


## 소프트웨어
| 항목 | 버전/값 | 근거 |
|---|---|---|
| Python | 3.12 (`.python-version`, `pyproject.toml`의 `requires-python = ">=3.12"`) | |
| 패키지 관리자 | [uv](https://github.com/astral-sh/uv) | `uv.lock`으로 의존성 고정 |
| 핵심 의존성 | `fastapi`/`uvicorn`/`langchain`/`langchain-core`/`langchain-community`/`langchain-chroma`/`langchain-classic`/`langchain-ollama`/`langgraph`/`chromadb`/`rank-bm25`/`pypdf`/`docx2txt` ([../pyproject.toml](../pyproject.toml) 전체 목록) | 필수 의존성 15개 |
| 선택 의존성 | `vllm`/`bitsandbytes`/`langchain-openai` → `[project.optional-dependencies].vllm` | 이 서비스는 쓰지 않는다. PoC 벤치마크 스크립트를 돌릴 때만 `uv sync --extra vllm` |

### ✅ 의존성 용량 (이식 시 해소)
원본은 `vllm`·`bitsandbytes`·`sentence-transformers` 같은 CUDA/torch 기반 패키지가 필수
의존성으로 묶여 있어 `.venv`가 **약 9.1GB**였습니다. 이식하면서 정리한 결과:

| 항목 | 원본 | 이식 후 |
|---|---|---|
| `.venv` | 약 9.1GB | **360MB** |
| torch / vllm / sentence-transformers | 포함 | **부재** (설치 확인) |
| 컨테이너 이미지 | (미측정) | **797MB** = 의존성 레이어 408MB + 베이스 389MB |

이건 최적화가 아니라 **필수 조건**이었습니다. `vllm`은 linux/arm64 휠이 없어 필수 의존성으로
두면 이미지 빌드 자체가 실패합니다.

남은 408MB의 대부분은 `chromadb`의 전이 의존성입니다 — `onnxruntime` 76MB,
`chromadb_rust_bindings` 50MB, `kubernetes` 41MB, `grpc` 38MB. `onnxruntime`은 chromadb
기본 임베딩 함수용인데 우리는 Ollama 임베딩을 쓰므로 실사용하지 않습니다. 추가 절감 여지는
[08-migration-checklist.md](08-migration-checklist.md)의 🟡 항목으로 남겼습니다.

## 하드웨어 (GPU/VRAM)
`README.md`에 기록된 실제 서빙 옵션 기준입니다.

| 모델 | 엔진 | 양자화 | GPU 메모리 사용률 설정 | `max-model-len` |
|---|---|---|---|---|
| Qwen2.5-7B-Instruct | vLLM | bitsandbytes | 0.8 | 2048 |
| EXAONE-3.0-7.8B-Instruct | vLLM | bitsandbytes | 0.8~0.9 | 2048~4096 |
| qwen2.5:7b | Ollama | Ollama 자체 양자화(GGUF, 기본값) | - | - |
| exaone3.5:7.8b | Ollama | Ollama 자체 양자화(GGUF, 기본값) | - | - |

- 7~8B급 모델을 `bitsandbytes` 8bit/4bit 양자화로 서빙하는 것을 전제로 하므로, **최소 12GB
  이상의 VRAM을 갖춘 GPU**(예: RTX 3060 12GB 이상)를 권장합니다. 정확한 최소 사양은 실제
  배포 전 목표 `max-model-len`/`max-num-seqs`로 재측정이 필요합니다.
- **임베딩도 Ollama가 담당합니다**(`bge-m3`). 이식 시 컨테이너 내 torch 계산에서 옮겼으므로,
  이 서비스 컨테이너는 GPU도 torch도 필요하지 않습니다. GPU가 필요한 쪽은 Ollama뿐입니다.
- **macOS에서는 Ollama를 호스트에서 실행해야 GPU를 씁니다.** Docker Desktop은 리눅스 VM 안에서
  돌고 Apple Metal은 그 VM으로 전달되지 않습니다. 실측: 호스트 Ollama에서 `ollama ps`가
  `qwen2.5:7b / 4.6GB / 100% GPU`를 보고하고, 규정 팩트 추출 1회가 약 7초입니다.
- vLLM/Ollama 각 엔진의 동시 처리량 차이는 PoC 리포 `select_reason.md` 1절의
  실측 데이터를 참고해 용량 산정에 반영하세요 (동시 요청 100건 기준 vLLM+EXAONE이 가장
  빠르고 Ollama+Qwen이 가장 느림).

## 네트워크/디스크 (최초 기동 시)
| 항목 | 필요 이유 |
|---|---|
| ~~HuggingFace Hub 접근~~ | **이식 후 불필요.** 임베딩을 Ollama에 맡겨 `huggingface.co`에서 모델을 받지 않습니다. 폐쇄망 대응이 그만큼 단순해졌습니다. |
| **Ollama 모델 레지스트리 접근** | `ollama pull qwen2.5:7b`(4.7GB, 생성) + `ollama pull bge-m3`(1.2GB, 임베딩). **이 두 개가 이 서비스의 유일한 모델 요구사항입니다.** Ollama를 컨테이너로 운영한다면 `/root/.ollama` 볼륨에 영구 저장이 필요합니다 ([04](04-deployment-guide.md)) |
| HuggingFace 모델 저장소 접근 (vLLM) | **이 서비스에는 불필요.** PoC 벤치마크 스크립트를 돌릴 때만 해당 (`Qwen/Qwen2.5-7B-Instruct`, `LGAI-EXAONE/EXAONE-3.0-7.8B-Instruct` 원본 가중치 수 GB~십수 GB) |
| 디스크 여유 공간 | Ollama 모델 약 6GB + 컨테이너 이미지 797MB + ChromaDB 데이터 + uv 캐시. **20GB 이상** 권장 (원본의 50GB 권고는 `vllm` 스택과 vLLM 원본 가중치를 전제한 값입니다) |

## 다음 문서
- 보안/데이터 취급 검토 → [06-data-and-security.md](06-data-and-security.md)
