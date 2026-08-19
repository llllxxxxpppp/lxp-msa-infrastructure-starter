"""Gateway가 전달한 인증 헤더를 검증하는 FastAPI 의존성."""

from typing import Annotated

from fastapi import Depends, Header, HTTPException, status


def get_authenticated_user_id(
    x_user_id: Annotated[str | None, Header(alias="X-User-Id")] = None,
) -> int:
    """Gateway 사용자 ID를 검증하고 정규화한 정수 값을 반환합니다."""

    if x_user_id is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="유효한 X-User-Id 헤더가 필요합니다.",
        )

    try:
        user_id = int(x_user_id.strip())
    except ValueError as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="유효한 X-User-Id 헤더가 필요합니다.",
        ) from exc

    if user_id < 1:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="유효한 X-User-Id 헤더가 필요합니다.",
        )

    return user_id


def require_admin(
    user_id: Annotated[int, Depends(get_authenticated_user_id)],
    x_role: Annotated[str | None, Header(alias="X-Role")] = None,
) -> int:
    """인증된 사용자가 관리자인지 검사하고 사용자 ID를 반환합니다."""

    if x_role != "ROLE_ADMIN":
        raise HTTPException(
            status_code=status.HTTP_403_FORBIDDEN,
            detail="관리자 권한이 필요합니다.",
        )

    return user_id
