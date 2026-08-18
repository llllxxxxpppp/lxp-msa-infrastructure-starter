from types import SimpleNamespace
from unittest.mock import Mock

from fastapi.testclient import TestClient
from langchain_core.documents import Document

from app import chat
from app.main import app

client = TestClient(app)


def test_low_relevance_result_is_refused(monkeypatch):
    """관련도가 낮으면 Ollama를 호출하지 않고 거절한다."""

    vector_store_mock = Mock()
    vector_store_mock.similarity_search_with_relevance_scores.return_value = [
        (
            Document(
                page_content="질문과 관련 없는 내용",
                metadata={
                    "filename": "unrelated.pdf",
                    "page_number": 1,
                },
            ),
            0.2,
        )
    ]

    llm_mock = Mock()

    monkeypatch.setattr(chat, "vector_store", vector_store_mock)
    monkeypatch.setattr(chat, "llm", llm_mock)
    monkeypatch.setattr(chat, "MIN_RELEVANCE_SCORE", 0.5)

    response = client.post(
        "/api/ai/courses/1/chat",
        json={"question": "배우자 출산휴가는 며칠이야?"},
    )

    assert response.status_code == 200
    assert chat.REFUSAL in response.text
    assert "event: sources\ndata: []" in response.text
    assert "event: done\ndata: {}" in response.text

    # 관련 자료가 없으므로 모델을 실행하면 안 된다.
    llm_mock.stream.assert_not_called()


def test_high_relevance_result_is_answered(monkeypatch):
    """관련도가 높으면 답변과 출처를 전송한다."""

    vector_store_mock = Mock()
    vector_store_mock.similarity_search_with_relevance_scores.return_value = [
        (
            Document(
                page_content="배우자 출산휴가는 20일이다.",
                metadata={
                    "filename": "leave.pdf",
                    "page_number": 1,
                },
            ),
            0.8,
        )
    ]

    llm_mock = Mock()
    llm_mock.stream.return_value = [
        SimpleNamespace(content="20일입니다."),
    ]

    monkeypatch.setattr(chat, "vector_store", vector_store_mock)
    monkeypatch.setattr(chat, "llm", llm_mock)
    monkeypatch.setattr(chat, "MIN_RELEVANCE_SCORE", 0.5)

    response = client.post(
        "/api/ai/courses/1/chat",
        json={"question": "배우자 출산휴가는 며칠이야?"},
    )

    assert response.status_code == 200
    assert '"content": "20일입니다."' in response.text
    assert '"filename": "leave.pdf"' in response.text
    assert '"page_number": 1' in response.text

    assert response.text.index("event: token") < response.text.index(
        "event: sources"
    )
    assert response.text.index("event: sources") < response.text.index(
        "event: done"
    )

    llm_mock.stream.assert_called_once()
