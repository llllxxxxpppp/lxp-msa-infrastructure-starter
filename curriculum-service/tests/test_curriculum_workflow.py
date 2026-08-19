"""커리큘럼 워크플로우의 단계 구성 테스트."""

from unittest import IsolatedAsyncioTestCase, TestCase
from unittest.mock import AsyncMock, Mock

from app.services.llm_service import CurriculumPlan, CurriculumStep
from app.workflows.curriculum_workflow import CurriculumWorkflow


def _candidate(course_id: int, difficulty_label: str) -> dict[str, str | int]:
    return {
        "courseId": course_id,
        "title": f"{difficulty_label} 강좌",
        "difficultyLabel": difficulty_label,
        "durationMinutes": 60,
    }


class CurriculumWorkflowStageTest(TestCase):
    def setUp(self) -> None:
        self.course_service = Mock()
        self.workflow = CurriculumWorkflow(
            course_service=self.course_service,
            llm_service=Mock(),
        )

    def test_normalize_plan_skips_stage_without_candidate(self) -> None:
        candidates = [_candidate(1, "입문"), _candidate(2, "실전")]
        plan = CurriculumPlan(
            summary="요약",
            steps=[
                CurriculumStep(stage="입문", course_id=1, reason="기초부터"),
                CurriculumStep(stage="실전", course_id=2, reason="적용해 보기"),
                CurriculumStep(stage="심화", course_id=999, reason="없는 강좌"),
            ],
        )

        curriculum = CurriculumWorkflow._normalize_plan(plan, candidates)

        self.assertEqual(
            [step["stage"] for step in curriculum["steps"]],
            ["입문", "실전"],
        )

    def test_fallback_curriculum_skips_missing_stage(self) -> None:
        self.course_service.get_first_course_by_difficulty_label.return_value = None
        state = {"retrieved_courses": [_candidate(1, "입문")]}

        curriculum = self.workflow._fallback_curriculum(state)

        self.assertEqual([step["stage"] for step in curriculum["steps"]], ["입문"])

    def test_fallback_curriculum_does_not_query_index_when_candidate_exists(
        self,
    ) -> None:
        state = {
            "retrieved_courses": [
                _candidate(1, "입문"),
                _candidate(2, "실전"),
                _candidate(3, "심화"),
            ]
        }

        curriculum = self.workflow._fallback_curriculum(state)

        self.assertEqual(len(curriculum["steps"]), 3)
        self.course_service.get_first_course_by_difficulty_label.assert_not_called()

    def test_fallback_curriculum_falls_back_to_index(self) -> None:
        self.course_service.get_first_course_by_difficulty_label.side_effect = (
            lambda stage: _candidate(9, stage) if stage == "심화" else None
        )
        state = {"retrieved_courses": [_candidate(1, "입문")]}

        curriculum = self.workflow._fallback_curriculum(state)

        self.assertEqual(
            [step["stage"] for step in curriculum["steps"]],
            ["입문", "심화"],
        )


class CurriculumWorkflowPlannerTest(IsolatedAsyncioTestCase):
    async def test_planner_leaves_no_draft_when_no_stage_is_filled(self) -> None:
        course_service = Mock()
        course_service.get_first_course_by_difficulty_label.return_value = None
        llm_service = Mock()
        llm_service.structured_output_exceptions = (ValueError,)
        llm_service.create_plan = AsyncMock(
            return_value=CurriculumPlan(summary="요약", steps=[])
        )
        workflow = CurriculumWorkflow(
            course_service=course_service,
            llm_service=llm_service,
        )
        state = {
            "user_profile": {"job": "마케터"},
            "target_goal": "데이터 분석",
            "retrieved_courses": [],
        }

        update = await workflow.planner_node(state)

        self.assertNotIn("draft_curriculum", update)
        self.assertEqual(update["status"], "interviewing")
        self.assertIn("찾지 못했습니다", update["messages"][0].content)
