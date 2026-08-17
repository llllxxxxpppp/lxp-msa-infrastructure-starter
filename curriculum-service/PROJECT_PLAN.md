# 맞춤형 커리큘럼 설계 및 수강 신청 자동화 봇

## 설명

**단순 RAG:** "마케팅 관련 강의 추천해 줘." → 마케팅 강의 목록 제공.
**LangGraph 적용:** 학습자의 직무, 현재 역량, 목표를 분석하여 커리큘럼을 짜주고, 실제 LMS DB와 연동해 수강 신청까지 완료하는 플로우.

- **Workflow:**
    1. **에이전트 1 (인터뷰어):** "어떤 역량을 기르고 싶으신가요?" 대화를 통해 요구사항을 수집. (상태 저장)
    2. **에이전트 2 (검색 및 기획):** RAG를 통해 사내외 강의 풀에서 적합한 콘텐츠를 찾고, LangGraph의 분기 처리를 통해 "입문-실전-심화" 3단계 커리큘럼을 구성.
    3. **에이전트 3 (액션 수행):** 구성된 커리큘럼에 대해 학습자가 동의하면, **LMS의 수강 신청 API를 호출(Tool Use)하여 실제 학습 계획표에 등록**. - 제외
- **기대 효과:** 단순한 검색창을 넘어, AI가 컨설턴트 역할을 하고 시스템 제어까지 수행하는 진정한 개인화 경험을 제공합니다.
- 필요 사항: 강의 데이터, FE(or 대화를 주고 받을 수 있는 공간?), LLM API, LangGraph, RAG, vector DB?

## 구체화

### 3. 맞춤형 커리큘럼 설계 봇 (수강 신청 제외, 큐레이션 집중)

단순 추천을 넘어, 사용자와 대화하며 부족한 정보를 채우고 최종적으로 '입문-실전-심화'의 체계적인 로드맵을 제안하는 대화형 에이전트입니다.

### 📍 LangGraph 상태(State) 정의

LangGraph가 대화 내내 기억하고 업데이트해야 할 데이터 구조입니다.

| **상태 변수명** | **데이터 타입** | **설명** |
| --- | --- | --- |
| `chat_history` | List[Message] | 사용자와 AI가 나눈 대화 기록 |
| `user_profile` | Dict | 직무, 연차, 현재 역량 수준 등 추출된 정보 |
| `target_goal` | String | 학습자가 최종적으로 달성하고 싶은 목표 |
| `missing_info` | List[String] | 커리큘럼을 짜기 위해 추가로 질문해야 할 항목 |
| `draft_curriculum` | Dict | 검색된 강의를 바탕으로 구성된 초안 데이터 |

### ⚙️ 워크플로우 (Node & Edge)

1. **인터뷰어 노드 (Interviewer Node)**
    - **역할:** 사용자의 첫 질문을 분석하고, `user_profile`과 `target_goal`을 채웁니다.
    - **로직:** 직무, 현재 수준, 목표 세 가지가 모두 파악되었는지 검사합니다. 부족한 항목이 있다면(`missing_info` 발생) 다음 노드로 넘어가지 않고 사용자에게 역질문(예: "현재 엑셀 활용 수준이 어떻게 되시나요?")을 던집니다.
    - **프롬프트 팁:** "당신은 LXP의 학습 컨설턴트입니다. 무작정 강의를 추천하지 말고, 직무/수준/목표를 파악하기 위해 한 번에 하나씩만 질문하세요."
2. **검색 노드 (Retriever Node)**
    - **역할:** 정보 수집이 완료되면 실행되며, 사내 강의 DB(벡터 DB)에서 관련 콘텐츠를 검색합니다.
    - **로직:** 사용자의 '직무+목표'를 키워드로 변환하여 RAG 검색을 수행합니다. 이때 메타데이터(난이도: 초급/중급/고급) 필터링을 함께 적용하여 골고루 데이터를 가져옵니다.
3. **기획자 노드 (Planner Node)**
    - **역할:** 검색된 강의 리스트를 바탕으로 "입문 - 실전 - 심화" 3단계 커리큘럼 로드맵을 작성합니다.
    - **로직:** 각 단계별로 가장 적합한 강의를 1~2개씩 매칭하고, **'왜 이 강의를 이 순서로 들어야 하는지'** 학습자 맞춤형 추천 사유를 텍스트로 생성합니다.
4. **피드백 노드 (Feedback Node)**
    - **역할:** 생성된 커리큘럼을 사용자에게 보여주고 피드백을 받습니다.
    - **분기(Conditional Edge):** 사용자가 "좋아"라고 하면 종료, "실전 단계 강의가 너무 긴데 다른 거 없어?"라고 하면 `chat_history`에 반영 후 다시 **검색 노드**나 **기획자 노드**로 돌아가 수정안을 제시합니다.

### Course Service 계약

1. 강좌 데이터는 다음 구조의 JSON으로 응답된다.

    - 구조
        - `courseId`: int, 1 이상
        - `instructorId`: int, 1 이상
        - `title`: String
        - `description`: String
        - `category`: String,
        - `categoryLabel`: String, `category` 의 한글 표기명
        - `difficulty`: String,
        - `difficultyLabel`: String, `difficulty` 의 한글 표기명
        - `durationMinutes`: int, 1 이상

    - 예시

        ```json
        {
            "courseId": 30017,
            "instructorId": 12,
            "title": "실무 SQL과 대시보드 만들기",
            "description": "SQL로 데이터를 추출하고 핵심 지표를 보여주는 대시보드를 설계합니다.",
            "category": "DATA_ANALYSIS",
            "categoryLabel": "데이터 분석",
            "difficulty": "PRACTICAL",
            "difficultyLabel": "실전",
            "durationMinutes": 420
        }
        ```

    - category - categoryLabel 매핑

        - BACKEND - 백엔드 개발
        - FRONTEND - 프론트엔드 개발
        - MOBILE - 모바일 개발
        - DEVOPS - 데브옵스·인프라
        - SECURITY - 보안
        - DATA_ANALYSIS - 데이터 분석
        - DATA_ENGINEERING - 데이터 엔지니어링
        - AI_ML - AI·머신러닝
        - PRODUCT - 프로덕트
        - DESIGN - 디자인·UX

    - difficulty - difficultyLabel 매핑
        - BEGINNER - 입문
        - PRACTICAL - 실전
        - ADVANCED - 심화
