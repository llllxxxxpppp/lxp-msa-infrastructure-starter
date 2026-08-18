# seed-documents/

컨테이너가 기동될 때(`app/main.py`의 `lifespan`) **자동으로 색인되는** 샘플 문서 디렉터리입니다.
`Dockerfile`이 이 디렉터리를 이미지에 그대로 포함시키므로, `docker compose up -d --build`만 하면
여기 있는 `.pdf`/`.docx` 파일이 전부 청킹·임베딩되어 ChromaDB + SQLite 메타데이터 DB에 자동
적재됩니다.

## 무엇을 넣어야 하는가

- **민감하지 않은 샘플/템플릿 규정 문서만** 넣습니다. 이 디렉터리는 git으로 추적되므로, 실제 사내
  기밀 문서(진짜 취업규칙, 인사평가 기준 등)는 **절대 넣지 마세요.**
- 개발/테스트 팀이 AI Assistance 기능(`/api/policies/analyze`)을 항상 같은 데이터로 재현 가능하게
  테스트할 수 있도록 하는 것이 목적입니다.
- 지원 확장자는 `.pdf`, `.docx`뿐입니다(`app/rag.py`의 `SUPPORTED_EXTENSIONS`).

## 동작 방식

- 이미 색인된(SHA-256 체크섬 일치) 파일은 컨테이너를 재기동해도 다시 임베딩하지 않습니다
  (`RagStore.add_document()`의 중복 감지 로직을 그대로 재사용).
- 문서를 추가/교체하려면 이 디렉터리에 파일을 넣고 이미지를 재빌드(`docker compose up -d --build`)
  하면 됩니다.
- 기동 로그(`docker compose logs policy-explorer-service`)에서
  `[Startup] 시드 문서 색인 결과: ingested=N duplicate=M failed=K` 메시지로 결과를 확인할 수
  있습니다.

## 실제 민감한 사내 문서를 다뤄야 한다면

git이 아니라 별도 공유 저장소(사내 네트워크 드라이브 등)에서 받아 `/api/policies/documents/upload`
API로 직접 업로드하세요. 벡터/메타데이터 자체를 git으로 공유하지 않는 이유는
[../docs/09-data-architecture.md](../docs/09-data-architecture.md)를 참고하세요.
