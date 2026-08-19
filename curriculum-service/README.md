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
- 브로커 장애 등으로 이벤트를 놓쳤다면 `PUT /api/courses`로 전체를 다시
  적재할 수 있습니다.
- 초기 적재에 실패하면 경고를 남기고 빈 인덱스로 기동합니다. Course Service가
  늦게 뜨는 경우를 위해서이며, 이후 이벤트나 `PUT /api/courses`로 채워집니다.

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

- 임베딩 강좌 관리 API

    - `GET /api/courses`: 현재 임베딩된 강좌 조회
    - `PUT /api/courses`: 전체 강좌 조회 및 임베딩 재구성
    - `PUT /api/courses/{courseId}`: 특정 강좌 조회 및 임베딩 갱신

- 테스트 요청

    여러 대화에 동일한 `thread_id` 를 사용하면 대화를 이어나갈 수 있습니다.

    ```bash
    curl -X POST http://localhost:8000/chat \
        -H 'Content-Type: application/json' \
        -d '{
            "thread_id": "1",
            "message": "데이터 분석가이고 고급 분석 기법을 공부하고 싶어요."
        }'
    ```
