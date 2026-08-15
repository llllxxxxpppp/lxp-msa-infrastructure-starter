"""Ollama 및 구조화 출력 체인 설정."""

import logging
from json import JSONDecodeError

from langchain_core.exceptions import OutputParserException
from langchain_core.runnables import Runnable, RunnableLambda
from langchain_ollama import ChatOllama
from pydantic import BaseModel, ValidationError

from app.config import OLLAMA_BASE_URL, OLLAMA_MODEL
from app.models import CurriculumPlan, FeedbackResult, InterviewResult

logger = logging.getLogger(__name__)

STRUCTURED_OUTPUT_EXCEPTIONS = (
    OutputParserException,
    ValidationError,
    JSONDecodeError,
)

llm = ChatOllama(
    model=OLLAMA_MODEL,
    base_url=OLLAMA_BASE_URL,
    temperature=0,
    reasoning=False,
    num_predict=2048,
)


def _unwrap_structured_response(response: dict, schema_name: str) -> BaseModel:
    parsing_error = response.get("parsing_error")
    if parsing_error is not None:
        raw_message = response.get("raw")
        raw_content = str(getattr(raw_message, "content", ""))[:1000]
        logger.warning(
            "%s 구조화 출력 파싱에 실패했습니다. error=%r raw=%r",
            schema_name,
            parsing_error,
            raw_content,
        )
        raise parsing_error

    parsed = response.get("parsed")
    if parsed is None:
        raise OutputParserException(f"{schema_name} 구조화 출력이 비어 있습니다.")
    return parsed


def _create_structured_llm(schema: type[BaseModel]) -> Runnable:
    structured_llm = llm.with_structured_output(
        schema,
        method="json_schema",
        include_raw=True,
    )
    parser = RunnableLambda(
        lambda response: _unwrap_structured_response(response, schema.__name__)
    )
    return (structured_llm | parser).with_retry(
        retry_if_exception_type=STRUCTURED_OUTPUT_EXCEPTIONS,
        stop_after_attempt=3,
    )


interviewer_llm = _create_structured_llm(InterviewResult)
feedback_llm = _create_structured_llm(FeedbackResult)
planner_llm = _create_structured_llm(CurriculumPlan)
