# 02. API 명세 (API Specification)

> 🔄 **이식 반영**
> - **`GET /health`가 추가**됐습니다(원본에 없던 엔드포인트). 아래 A/B 절에는 없으니
>   [../README.md](../README.md)를 보세요.
> - 모든 엔드포인트에 `response_model`을 명시해, **OpenAPI 스펙에 실제 응답 스키마가
>   드러납니다**(원본은 200 응답 스키마가 비어 있었습니다).
> - 실행 중인 서비스에서 추출한 실제 스펙:
>   [openapi/policy-explorer-service.openapi.json](openapi/policy-explorer-service.openapi.json)
>   ← **codegen에는 이 파일을 1차 소스로 쓰세요.** 아래 본문이 참조하는 PoC 시절 스펙
>   (`lxp-*.openapi.json`)은 이식하지 않았습니다. `/health`와 응답 스키마가 빠져 있어서입니다.
> - 경로와 응답 필드는 원본 계약을 그대로 유지했습니다.


> 이 문서는 실제 코드(PoC 리포 `lxp-ollama-qwen-fileupload.py`,
> PoC 리포 `lxp-ollama-qwen.py` 등)를 근거로 작성되었습니다. 형식적으로 검증된
> 실제 OpenAPI 3.1 스펙은 [`openapi/`](openapi/) 폴더에 있으니, 클라이언트
> 코드 생성(codegen) 등에는 문서 대신 그 파일을 1차 소스로 사용하세요.
>
> - PoC 리포 `docs/openapi/lxp-ollama-qwen.openapi.json` — 벤치마크
>   4종 대표 스펙 (`lxp-ollama-exaone.py`/`lxp-vllm-qwen.py`/`lxp-vllm-exaone.py`도 엔드포인트
>   구조가 완전히 동일하여 대표 1개만 첨부했습니다. `engine`/`model` 응답 필드의 값만 다릅니다.)
> - PoC 리포 `docs/openapi/lxp-ollama-qwen-fileupload.openapi.json`
>   — 파일 업로드 RAG 버전(실서비스 후보) 전체 스펙

## 공통 사항
- 모든 응답은 `application/json`.
- ~~별도 `response_model`이 지정되지 않은 엔드포인트는 OpenAPI 스펙상 200 응답 스키마가
  비어 있습니다.~~ → **이식 시 해소.** 4개 엔드포인트 전부 `response_model`을 명시했습니다.
  아래 표의 응답 예시는 실제 코드 반환값과 일치하며, 기계가 읽을 스펙은
  [openapi/policy-explorer-service.openapi.json](openapi/policy-explorer-service.openapi.json)입니다.
- **인증 없음.** 현재 어떤 엔드포인트에도 인증/인가가 걸려 있지 않습니다. MSA 게이트웨이 레벨에서
  반드시 보강해야 합니다 (→ [06-data-and-security.md](06-data-and-security.md)).
- 에러 형식: 검증 실패(422)는 FastAPI 표준 `HTTPValidationError`(`detail: [{loc, msg, type}]`),
  그 외 비즈니스 에러는 `HTTPException`을 통해 `{"detail": "<메시지>"}` 형태(400/500)로 반환됩니다.

---

## A. 전 서비스 공통 — 규정 충돌 분석

### `POST /api/policies/analyze`
5개 서비스 모두 동일한 계약을 갖습니다. 다만 검색 대상 문서(②단계)가 벤치마크 4종은 하드코딩
샘플 4건, 파일업로드 버전은 사용자가 업로드한 문서라는 점만 다릅니다.

**Request**
```json
{ "new_policy_text": "2026년 신규 인사 규정 안내입니다. 반차 사용 기준 시간을 4.5시간으로 변경합니다." }
```

**Response `200`** (코드 기준, 파일업로드 버전은 `documents_in_store` 필드가 추가됨 —
PoC 리포 `lxp-ollama-qwen-fileupload.py`):
```json
{
  "status": "success",
  "engine": "Ollama",
  "model": "qwen3.5:4b",
  "total_time_seconds": 12.34,
  "documents_in_store": 150,
  "extracted_rules": [
    { "keyword": "반차", "fact": "반차 사용 기준 시간을 4.5시간으로 변경" }
  ],
  "conflict_count": 1,
  "conflicts": [
    {
      "source": "취업규칙_2024.pdf",
      "page": 11,
      "old_content": "제4조(수습기간) 신규 채용된 자의 수습기간은 채용일로부터 3개월로 한다.",
      "new_fact": "반차 사용 기준 시간을 4.5시간으로 변경",
      "action_suggested": "3개월을 2개월로 변경 권장",
      "reasoning": "기존 콘텐츠는 수습기간을 3개월로 정하고 있으나, 신규 규정 팩트는 이를 2개월로 단축하도록 요구하고 있어 두 내용이 서로 상충합니다."
    }
  ],
  "markdown_report": "## 🚨 사내 콘텐츠 규정 충돌 검출 리포트\n..."
}
```
> `documents_in_store`는 `lxp-ollama-qwen.py` 등 벤치마크 4종에는 없습니다.
> `conflicts`는 이 모노레포 이식 과정에서 추가된 **파일업로드 버전 전용 additive 필드**입니다.
> `page`는 PDF에서만 채워지는 0-index 페이지 번호이며(사람이 읽는 페이지는 `page + 1`), DOCX 등
> 페이지 개념이 없는 포맷은 `null`입니다. 프론트엔드 "해당 파일 및 위치"/"변경 제안 상세" UI가
> `markdown_report` 문자열 파싱 없이 바로 쓸 수 있도록 `conflict_report`를 구조화된 형태로 노출합니다.
> `action_suggested`(한 문장 조치)와 `reasoning`(판단 근거)은 원래 한 필드였으나, LLM이 결론과
> 근거를 뒤섞어 긴 서술형 문단을 반환해 UI 가독성이 떨어지는 문제가 있어 분리했습니다.

**Response `500`**: LLM 호출 실패, 파싱 오류 등 — `{"detail": "<에러 메시지>"}`

**참고 (코드 위치)**
- 벤치마크: PoC 리포 `lxp-ollama-qwen.py`
- 파일업로드: PoC 리포 `lxp-ollama-qwen-fileupload.py`

---

## B. 파일 업로드 RAG 버전 전용 (`lxp-ollama-qwen-fileupload.py`)

### `POST /api/policies/documents/upload`
PDF 또는 DOCX 문서를 업로드하면 청킹 후 ChromaDB(+BM25)에 적재합니다.
(PoC 리포 `lxp-ollama-qwen-fileupload.py`)

**Request**: `multipart/form-data`, 필드명 `file` (지원 확장자: `.pdf`, `.docx`)
```bash
curl -X POST http://localhost:8086/api/policies/documents/upload -F "file=@규정문서.pdf"
```

**Response `200`**
```json
{
  "status": "success",
  "document_id": "b703b657e2f346de94d65348c06d4e41",
  "filename": "규정문서.pdf",
  "num_source_documents": 85,
  "num_chunks": 149,
  "total_chunks_in_store": 150,
  "elapsed_seconds": 8.59
}
```
> 🔄 **이식 반영**: `document_id`가 추가됐습니다(문서 메타데이터 DB 도입, [09](09-data-architecture.md)
> 참고). 동일 내용(SHA-256 체크섬 일치)의 문서가 이미 색인돼 있으면 재임베딩하지 않고
> `"status": "duplicate"`를 반환합니다(이 경우 `document_id`는 기존 문서의 id, `num_source_documents`는
> `0`).

**Response `400`**: 지원하지 않는 확장자, 또는 텍스트 추출 실패
```json
{ "detail": "지원하지 않는 파일 형식입니다 (.xlsx). 업로드 가능한 확장자: .docx, .pdf" }
```

> 🔄 **이식 반영 — 경로 순회 취약점 해소됨**: 업로드 파일명은 더 이상 저장 경로에 그대로 쓰이지
> 않습니다. `os.path.basename()`으로 경로 조각을 제거하고, `{document_id}/{원본파일명}` 형태의
> UUID 하위 디렉터리에 저장합니다. 동일 파일명을 재업로드해도 서로 다른 `document_id` 아래 각각
> 보관되어 덮어써지지 않습니다. 상세 내용은 [06-data-and-security.md](06-data-and-security.md)를
> 참고하세요.

### `GET /api/policies/documents`
현재 적재된 문서 목록을 문서 메타데이터 DB(SQLite) 기준으로 조회합니다.
(PoC 리포 `lxp-ollama-qwen-fileupload.py`에서 이식 후 [09](09-data-architecture.md) 제안대로 확장)

**Response `200`**
```json
[
  {
    "id": "b703b657e2f346de94d65348c06d4e41",
    "original_filename": "규정문서.pdf",
    "status": "ready",
    "chunk_count": 149,
    "size_bytes": 2456123,
    "uploaded_at": "2026-08-18T09:00:00+00:00",
    "error_message": null
  }
]
```
> 🔄 **이식 반영**: 예전에는 `{source, chunk_count}`만 반환하고 `all_chunks`를 매번 순회해
> 집계했습니다. 이제 문서 단위 메타데이터 DB(SQLite)에서 바로 조회하며, `status`가
> `uploading`/`ready`/`failed`일 수 있습니다(`failed`인 경우 `error_message`에 사유가 담깁니다).

### `DELETE /api/policies/documents`
업로드된 문서를 모두 초기화합니다 (ChromaDB + BM25 인메모리 캐시).
(PoC 리포 `lxp-ollama-qwen-fileupload.py`)

**Response `200`**
```json
{ "status": "success", "message": "업로드된 문서를 모두 초기화했습니다." }
```

---

## 다음 문서
- 이 엔드포인트들을 컨테이너 환경에서 어떤 설정으로 띄울지 → [03-environment-config.md](03-environment-config.md)
