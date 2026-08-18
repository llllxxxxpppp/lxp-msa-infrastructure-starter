# policy-explorer-service 문서 인덱스

이 폴더는 별도 저장소 **`policy-explorer-service`(PoC 리포)** 의 `docs/` 01~09를 이 모노레포로
**이식하면서 이식 결과를 반영해 갱신한** 문서 세트입니다.

- PoC 리포가 "이 프로젝트를 로컬에서 어떻게 만들고 검증했는가"를 다룹니다
  (`PROJECT_PLAN.md`, `select_reason.md`, `test/`).
- 이 폴더는 **"그것을 MSA 모노레포로 어떻게 가져왔고, 지금 어떤 상태인가"** 를 다룹니다.

서비스 개요·이식 시 변경점 요약·작업 규칙은 [../CLAUDE.md](../CLAUDE.md)에,
실행 방법은 [../README.md](../README.md)에 있습니다.

## 🔄 각 문서 상단의 "이식 반영" 블록을 먼저 보세요

원본 문서는 "현재는 프로토타입이고 환경변수도 헬스체크도 없다"는 전제로 쓰여 있었습니다.
이식하면서 상당 부분이 해소됐으므로, **각 문서 맨 위에 `> 🔄 이식 반영` 블록을 넣어 원본과
달라진 점을 표시**했습니다. 본문에 남은 서술이 현재 상태와 다를 수 있으니 이 블록을 기준으로
읽으세요.

## 언제 어떤 문서를 보면 되나요?

| 상황 | 문서 |
|---|---|
| 서비스가 뭔지, 어떤 구성요소가 있는지 처음 파악 | [01-service-overview.md](01-service-overview.md) |
| 이 서비스를 호출하는 클라이언트/게이트웨이를 만들어야 한다 | [02-api-specification.md](02-api-specification.md), [`openapi/`](openapi/) |
| 환경변수로 무엇을 제어할 수 있는지 | [../README.md](../README.md) (최신) · [03-environment-config.md](03-environment-config.md) (원본에서 무엇이 하드코딩이었는지의 기록) |
| Docker Compose로 띄워야 한다 / Ollama를 왜 compose에 안 뒀는지 | [04-deployment-guide.md](04-deployment-guide.md) |
| 서버 사양(GPU/디스크/네트워크)을 산정해야 한다 | [05-runtime-requirements.md](05-runtime-requirements.md) |
| 보안 검토 / 개인정보 취급 여부 | [06-data-and-security.md](06-data-and-security.md) |
| 운영 중 장애 대응, 헬스체크, 스케일링 | [07-operations-runbook.md](07-operations-runbook.md) |
| **무엇이 끝났고 무엇이 남았는지** | [08-migration-checklist.md](08-migration-checklist.md) ⭐ |
| 업로드 문서의 메타데이터/원본 파일을 어디에 어떻게 저장할지 | [09-data-architecture.md](09-data-architecture.md) |
| 왜 Ollama·Qwen·하이브리드 검색인가 (설계 근거) | **PoC 리포** `select_reason.md` |

## 코드를 고치기 전에

[08-migration-checklist.md](08-migration-checklist.md)를 먼저 보세요. 특히 아래는 **의도적으로
남긴 것**이며, 모르고 건드리면 원본 설계 의도를 깨뜨릴 수 있습니다.

- 업로드 경로 순회 취약점 (🔴, 코드에 `🚨 TODO` 주석 있음)
- 인증/인가 부재 (🔴, Gateway 라우팅과 함께 처리)
- 문서 메타데이터 DB 미도입 (🔴)
- `langchain-community` sunset (🟡, 대체 패키지 미존재)

## 실측 스펙

[`openapi/policy-explorer-service.openapi.json`](openapi/policy-explorer-service.openapi.json)은
**실행 중인 컨테이너에서 추출한** OpenAPI 3.1 스펙입니다. 클라이언트 코드 생성에는 문서 대신 이
파일을 1차 소스로 쓰세요. (PoC 시절 스펙 `lxp-*.openapi.json`은 `/health`와 응답 스키마가 빠져
있어 이식하지 않았습니다. PoC 리포 `docs/openapi/`에 그대로 있습니다.)
