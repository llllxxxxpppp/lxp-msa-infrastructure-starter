# AI Tutor Service

강좌에 등록된 PDF를 기반으로 답변과 출처를 제공하는 RAG 학습 챗봇 서비스입니다.

외부 생성형 AI API는 사용하지 않으며, Ollama에서 실행되는 로컬 Qwen 모델을 사용합니다.

## 처리 흐름

```text
PDF 업로드
→ 페이지별 텍스트 추출
→ 텍스트 청킹
→ Qwen3 Embedding 벡터화
→ Chroma 저장

로그인 사용자 질문
→ course_id 기준 유사도 검색
→ 관련도 임계값 미만 결과 제거
→ Qwen3-8B 답변 생성
→ SSE 답변·파일명·페이지 전송
```

검색 근거가 부족하면 다음 문장을 반환합니다.

```text
업로드된 강의 자료에서 해당 내용을 찾을 수 없습니다.
```

## 기술 스택

* Python
* FastAPI
* LangChain
* Ollama
* Qwen3-8B
* Qwen3-Embedding-0.6B
* Chroma
* pypdf
* SSE

## 사전 준비

### Ollama 모델 다운로드

```powershell
ollama pull qwen3:8b
ollama pull qwen3-embedding:0.6b
```

모델을 사용하려면 Ollama가 실행 중이어야 합니다.

## 로컬 실행

```powershell
cd ai-bot-service
uv sync
uv run uvicorn app.main:app --reload
```

실행 후 Swagger에서 API를 확인할 수 있습니다.

```text
http://localhost:8000/docs
```

## Docker 실행

프로젝트 루트에서 64바이트 이상의 `JWT_SECRET`을 설정합니다.

```dotenv
JWT_SECRET=64바이트_이상의_랜덤값
```

Ollama를 실행한 상태에서 Course Service, AI 서비스, Gateway를 실행합니다.

```powershell
docker compose up -d --build course-service ai-bot-service gateway
```

회원가입과 로그인을 새로 진행해야 한다면 Auth Service와 Member Service도 함께 실행해야 합니다.

```powershell
docker compose up -d --build auth-service member-service course-service ai-bot-service gateway
```

Docker 환경에서는 Gateway를 통해 AI API를 호출합니다.

```text
http://localhost:8080/api/ai/**
```

## 환경변수

| 환경변수                      | 기본값                      | 용도             |
| ------------------------- | ------------------------ | -------------- |
| `OLLAMA_BASE_URL`         | `http://localhost:11434` | Ollama 주소      |
| `OLLAMA_EMBEDDING_MODEL`  | `qwen3-embedding:0.6b`   | 임베딩 모델         |
| `COURSE_SERVICE_URL`      | `http://localhost:8083`  | 담당 강좌 확인       |
| `RAG_MIN_RELEVANCE_SCORE` | `0.5`                    | RAG 검색 관련도 임계값 |

Docker에서는 Ollama 주소로 다음 값을 사용합니다.

```text
http://host.docker.internal:11434
```

## API

| 메서드      | 경로                                                    | 설명        | 권한      |
| -------- | ----------------------------------------------------- | --------- | ------- |
| `POST`   | `/api/ai/courses/{course_id}/documents`               | PDF 업로드   | 담당 강사   |
| `GET`    | `/api/ai/courses/{course_id}/documents`               | PDF 목록 조회 | 담당 강사   |
| `DELETE` | `/api/ai/courses/{course_id}/documents/{document_id}` | PDF 삭제    | 담당 강사   |
| `POST`   | `/api/ai/courses/{course_id}/chat`                    | PDF 기반 질문 | 로그인 사용자 |

PDF 관리 API는 Gateway가 전달한 `X-User-Id`를 이용해 해당 강좌의 담당 강사인지 다시 확인합니다.

## PDF 저장 방식

업로드된 PDF에서 다음 데이터를 추출하여 Chroma에 저장합니다.

* 텍스트 청크
* 임베딩 벡터
* `course_id`
* `document_id`
* 파일명
* 페이지 번호

원본 PDF 파일 자체는 별도의 파일 스토리지에 보존하지 않습니다.

## SSE 응답

채팅 API는 다음 순서로 SSE 이벤트를 반환합니다.

```text
event: token
data: {"content": "답변 조각"}

event: sources
data: [{"filename": "강의자료.pdf", "page_number": 1}]

event: done
data: {}
```

## React 실행

프로젝트 루트의 `frontend`에서 실행합니다.

```powershell
cd frontend
npm install
npm run dev
```

접속 주소:

```text
http://localhost:5173
```

메인 화면 우측 하단의 구름 모양 버튼을 누르면 챗봇이 열립니다.

## 테스트

```powershell
cd ai-bot-service
uv run pytest
```

테스트 항목:

* Health API
* 관련도가 낮은 검색 결과 거절
* 관련도가 높은 검색 결과의 답변·출처·SSE 순서

## 현재 구현 범위

* PDF 업로드·목록 조회·삭제 백엔드 API 구현
* 로그인 사용자용 React 챗봇 위젯 구현
* 강사용 React PDF 관리 화면은 이번 범위에 포함하지 않음
* PDF에서 추출한 텍스트·벡터·메타데이터는 각 로컬 환경의 Chroma에 저장
* 원본 PDF 파일은 별도로 보존하지 않음
* 외부 생성형 AI API는 사용하지 않음
