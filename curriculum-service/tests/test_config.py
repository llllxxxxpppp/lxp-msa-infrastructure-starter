"""애플리케이션 설정 값 검증 테스트."""

import os
from unittest import TestCase
from unittest.mock import patch

from app.config import _positive_int_env


class PositiveIntegerEnvironmentTest(TestCase):
    def test_returns_default_when_environment_variable_is_missing(self) -> None:
        with patch.dict(os.environ, {}, clear=True):
            value = _positive_int_env("SESSION_TIMEOUT_SECONDS", 1800)

        self.assertEqual(value, 1800)

    def test_returns_positive_environment_value(self) -> None:
        with patch.dict(
            os.environ,
            {"SESSION_CLEANUP_INTERVAL_SECONDS": "120"},
            clear=True,
        ):
            value = _positive_int_env("SESSION_CLEANUP_INTERVAL_SECONDS", 60)

        self.assertEqual(value, 120)

    def test_rejects_non_positive_or_non_integer_value(self) -> None:
        for raw_value in ("0", "-1", "1.5", "invalid"):
            with self.subTest(raw_value=raw_value):
                with patch.dict(
                    os.environ,
                    {"SESSION_TIMEOUT_SECONDS": raw_value},
                    clear=True,
                ):
                    with self.assertRaisesRegex(
                        ValueError,
                        "SESSION_TIMEOUT_SECONDS",
                    ):
                        _positive_int_env("SESSION_TIMEOUT_SECONDS", 1800)
