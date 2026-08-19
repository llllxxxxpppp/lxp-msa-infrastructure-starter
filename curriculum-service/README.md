# 맞춤형 커리큘럼 설계 및 수강 신청 자동화 봇

## 단일 서비스 실행

다음 절차를 따라 본 `curriculum-service` 만 단독으로 실행해볼 수 있습니다.

1. 다음 명령어를 실행하여 Ollama Docker Container를 실행합니다.

    ```bash
    # cpu 사용
    docker compose up -d

    # gpu 사용
    docker compose -f compose.cuda.yaml -d
    ```

1. 다음 명령어를 실행하여 채팅 및 임베딩 모델을 다운받습니다. 이 과정은 첫 실행에만 필요합니다.

    ```bash
    docker exec curriculum-service-ollama-1 ollama pull qwen3.5:4b
    docker exec curriculum-service-ollama-1 ollama pull qwen3-embedding:0.6b
    ```

    기본 임베딩 모델은 `OLLAMA_EMBEDDING_MODEL` 환경 변수로 변경할 수 있습니다.
    서버를 시작할 때 `COURSE_SERVICE_BASE_URL`(기본값 `http://course-service`)의
    Course Service에서 강좌를 조회하고 임베딩하여 인메모리 Chroma 컬렉션을 새로
    구성합니다. 이어서 강좌 변경 이벤트를 받기 위해 RabbitMQ에 연결하므로,
    Course Service와 Ollama, RabbitMQ가 먼저 실행 중이어야 합니다.

    브로커 접속 정보는 아래 환경 변수로 지정합니다. 기본값은 로컬 실행 기준이며,
    컨테이너로 띄울 때는 `compose.yaml`이 값을 덮어씁니다.

    | 환경 변수 | 기본값 |
    | --- | --- |
    | `RABBITMQ_HOST` | `localhost` |
    | `RABBITMQ_PORT` | `5672` |
    | `RABBITMQ_USERNAME` | `admin` |
    | `RABBITMQ_PASSWORD` | `admin` |

    로컬 테스트에서 fixture 강좌 데이터를 사용하려면 `PROVIDER=json`을 설정합니다.

1. 다음 명령어를 입력하여 FastAPI 서버를 실행합니다.

    ```bash
    uv run uvicorn app.main:app --reload
    ```

## 대화 세션 운영

채팅 요청은 Gateway가 인증 결과로 전달하는 `X-User-Id` 헤더를 사용합니다.
`X-User-Id`는 공백을 제거한 뒤 1 이상의 정수여야 하며, 누락되거나 잘못된
값이면 `401 Unauthorized`를 반환합니다. 요청 본문은 `message`만 사용하고
사용자나 대화를 식별하는 값은 본문에서 받지 않습니다.

사용자마다 하나의 대화 상태를 인메모리로 유지합니다. 마지막 채팅 처리가 끝난
시점부터 `SESSION_TIMEOUT_SECONDS` 이상 활동이 없으면 다음 요청 진입 시 또는
주기 정리 작업에서 세션과 LangGraph 체크포인트를 제거합니다. 채팅 처리 중인
세션은 만료시키지 않습니다.

| 환경 변수 | 기본값 | 설명 |
| --- | --- | --- |
| `SESSION_TIMEOUT_SECONDS` | `1800` | 마지막 채팅 처리 완료 후 세션을 유지할 시간(초) |
| `SESSION_CLEANUP_INTERVAL_SECONDS` | `60` | 비활성 세션을 주기적으로 검사할 간격(초) |

두 환경 변수는 모두 양의 정수여야 합니다.

세션과 체크포인트는 프로세스 메모리에만 저장되므로 서비스가 재시작되면 모든
대화가 소실됩니다. 이 구현은 단일 인스턴스 운영을 전제로 합니다. 다중
인스턴스로 전환하려면 `X-User-Id` 기반 고정 라우팅을 적용하거나 Redis 계열의
공유 체크포인터를 별도로 구성해야 합니다.

## 강좌 변경 이벤트 수신

Course Service가 강좌를 공개·비공개·삭제할 때 발행하는 이벤트를 받아 임베딩
인덱스를 갱신합니다. 검색할 때마다 Course Service를 조회하지 않기 때문에,
이 연동이 없으면 인덱스가 기동 시점에 멈춰 있게 됩니다.

| 이벤트 | 처리 |
| --- | --- |
| `course.published` | 해당 강좌를 조회해 인덱스에 추가·갱신 |
| `course.unpublished` | 인덱스에서 제거 |
| `course.deleted` | 인덱스에서 제거 |

- 익스체인지 `course.events`(topic)는 Course Service가, 큐
  `curriculum.course-sync`와 DLQ는 본 서비스가 선언합니다.
- 기동은 `큐 선언 → 초기 적재 → 소비 시작` 순서로 진행합니다. 적재하는 동안
  발생한 이벤트가 큐에 쌓였다가 처리되어 유실 구간이 생기지 않습니다.
- 처리에 실패한 메시지는 `curriculum.course-sync.dlq`로 보냅니다. 재처리를
  요청하지 않으므로 브로커와 본 서비스 사이에서 메시지가 맴돌지 않습니다.
- 브로커 장애 등으로 이벤트를 놓쳤다면 `PUT /api/curriculum/courses`로 전체를
  다시 적재할 수 있습니다.
- 초기 적재에 실패하면 경고를 남기고 빈 인덱스로 기동합니다. Course Service가
  늦게 뜨는 경우를 위해서이며, 이후 이벤트나 `PUT /api/curriculum/courses`로
  채워집니다.

## 테스트

### 데이터 프로바이더

기본적으로 Course Service의 강좌 데이터를 사용합니다. 개발 및 테스트 목적으로
`curriculum-service/tests/fixtures/courses.json`을 사용하려면 `PROVIDER=json`을
설정한 뒤 실행합니다.

**macOS / Linux**

```bash
PROVIDER=json docker compose up --build curriculum-service
```

**Windows (PowerShell)**

```powershell
$env:PROVIDER = "json"
docker compose up --build curriculum-service
```

### 엔드포인트 및 요청 스니펫

- API 문서 주소: `http://localhost:8000/docs`
- `GET /health`: 인증 없이 서비스 상태 조회

    아래 경로는 단독 실행(`localhost:8000`) 기준입니다. `compose.yaml`으로 띄울
    때는 호스트 포트를 게시하지 않으므로 Gateway를 통해서만 접근합니다. 이때
    주소는 `http://localhost:8080/api/curriculum/...`이고 `X-User-Id`·`X-Role`
    대신 `Authorization: Bearer <accessToken>`을 보내면 Gateway가 검증 후 두
    헤더를 주입합니다.

- 채팅 API

    - `POST /api/curriculum/chat`: 현재 사용자의 대화를 이어서 처리
    - `DELETE /api/curriculum/chat/session`: 현재 사용자의 대화를 초기화하며,
      세션 존재 여부와 관계없이 `204 No Content` 반환

- 임베딩 강좌 관리 API

    - `GET /api/curriculum/courses`: 현재 임베딩된 강좌 조회
    - `PUT /api/curriculum/courses`: 전체 강좌 조회 및 임베딩 재구성
    - `PUT /api/curriculum/courses/{courseId}`: 특정 강좌 조회 및 임베딩 갱신

    모든 임베딩 강좌 관리 API는 유효한 `X-User-Id`와 대소문자를 구분하여
    정확히 일치하는 `X-Role: ROLE_ADMIN` 헤더가 필요합니다. 사용자 인증이
    잘못되면 `401 Unauthorized`, 관리자 역할이 없거나 다르면
    `403 Forbidden`을 반환합니다.

- 테스트 요청

    같은 `X-User-Id`를 사용하면 이전 대화 상태를 이어갑니다.

    ```bash
    curl -X POST http://localhost:8000/api/curriculum/chat \
        -H 'Content-Type: application/json' \
        -H 'X-User-Id: 1' \
        -d '{
            "message": "데이터 분석가이고 고급 분석 기법을 공부하고 싶어요."
        }'
    ```

    현재 사용자의 대화를 초기화합니다.

    ```bash
    curl -X DELETE http://localhost:8000/api/curriculum/chat/session \
        -H 'X-User-Id: 1'
    ```

    관리자 권한으로 현재 임베딩된 강좌를 조회합니다.

    ```bash
    curl http://localhost:8000/api/curriculum/courses \
        -H 'X-User-Id: 1' \
        -H 'X-Role: ROLE_ADMIN'
    ```
