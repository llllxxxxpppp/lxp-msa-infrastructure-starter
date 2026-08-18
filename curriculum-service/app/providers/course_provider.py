"""강좌 데이터 공급자 인터페이스."""

from abc import ABC, abstractmethod

from pydantic import BaseModel


class Course(BaseModel):
    id: str
    title: str
    category: str
    difficulty: str
    duration: str
    description: str


class CourseProvider(ABC):
    """강좌 데이터 출처를 추상화합니다."""

    @abstractmethod
    def get_courses(self) -> list[Course]:
        """사용 가능한 전체 강좌를 반환합니다."""

        raise NotImplementedError
