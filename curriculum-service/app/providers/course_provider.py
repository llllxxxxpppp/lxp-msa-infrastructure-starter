"""강좌 데이터 공급자 인터페이스."""

from abc import ABC, abstractmethod

from pydantic import BaseModel, ConfigDict, Field


class Course(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    course_id: int = Field(alias="courseId", ge=1)
    instructor_id: int = Field(alias="instructorId", ge=1)
    title: str
    description: str
    category: str
    category_label: str = Field(alias="categoryLabel")
    difficulty: str
    difficulty_label: str = Field(alias="difficultyLabel")
    duration_minutes: int = Field(alias="durationMinutes", ge=1)


class CourseProvider(ABC):
    """강좌 데이터 출처를 추상화합니다."""

    @abstractmethod
    def get_courses(self) -> list[Course]:
        """사용 가능한 전체 강좌를 반환합니다."""

        raise NotImplementedError
