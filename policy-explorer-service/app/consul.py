"""Consul 서비스 등록·해제.

Java 서비스는 `spring-cloud-starter-consul-discovery`가 기동 시 자동으로 등록하지만,
이 서비스는 Python이라 Consul HTTP API를 직접 호출한다. 새 의존성을 넣지 않기 위해
표준 라이브러리(`urllib`)만 사용한다.

정책
  - CONSUL_HOST가 비어 있으면 등록 자체를 건너뛴다(단독 실행용).
  - 등록에 실패해도 서비스 기동을 막지 않는다. 대신 `GET /health`의 `consul` 항목에
    드러나므로 조용히 묻히지 않는다. 등록이 안 되면 gateway의 `lb://` 조회가 실패한다.

Java 서비스와 다른 점
  - 헬스체크 경로가 `/actuator/health`가 아니라 `/health`다.
  - 인스턴스 ID에 랜덤값이 아니라 IP·포트를 쓴다. 재기동 시 같은 ID로 덮어써져
    카탈로그에 유령 인스턴스가 남지 않는다.
"""

import json
import logging
import socket
from typing import Optional
from urllib.error import URLError
from urllib.request import Request, urlopen

from app import config

logger = logging.getLogger(__name__)


class ConsulRegistration:
    """등록 상태를 들고 있는 객체. main.py의 lifespan이 생성하고 app.state에 보관한다."""

    def __init__(self) -> None:
        self.enabled: bool = bool(config.CONSUL_HOST)
        self.service_id: Optional[str] = None
        self.address: Optional[str] = None
        self.status: str = "PENDING" if self.enabled else "DISABLED"
        self.error: Optional[str] = None

    # -----------------------------------------------------
    def _put(self, path: str, payload: Optional[dict] = None) -> None:
        data = json.dumps(payload).encode() if payload is not None else None
        headers = {"Content-Type": "application/json"} if data else {}
        request = Request(
            f"http://{config.CONSUL_HOST}:{config.CONSUL_PORT}{path}",
            data=data,
            headers=headers,
            method="PUT",
        )
        with urlopen(request, timeout=config.HEALTH_TIMEOUT_SECONDS + 2):
            pass

    # -----------------------------------------------------
    def register(self) -> None:
        """Consul 카탈로그에 자신을 등록한다. 실패는 경고만 남긴다."""
        if not self.enabled:
            logger.info("[Consul] CONSUL_HOST가 없어 등록을 건너뜁니다(단독 실행).")
            return

        # 컨테이너 IP. Java 서비스의 prefer-ip-address: true 와 같은 방식이다.
        # Consul이 이 주소로 헬스체크를 호출하므로 같은 네트워크에서 닿아야 한다.
        address = socket.gethostbyname(socket.gethostname())
        port = config.SERVICE_PORT
        service_id = f"{config.SERVICE_NAME}-{address}-{port}"

        payload = {
            "ID": service_id,
            "Name": config.SERVICE_NAME,
            "Address": address,
            "Port": port,
            "Check": {
                "HTTP": f"http://{address}:{port}/health",
                "Interval": config.CONSUL_CHECK_INTERVAL,
                "Timeout": config.CONSUL_CHECK_TIMEOUT,
                "DeregisterCriticalServiceAfter": config.CONSUL_DEREGISTER_AFTER,
            },
        }

        try:
            self._put("/v1/agent/service/register", payload)
        except (URLError, TimeoutError, OSError) as e:  # noqa: BLE001
            self.status, self.error = "DOWN", str(e)
            logger.warning(
                "[Consul] 등록 실패 — gateway의 lb://%s 조회가 실패할 수 있습니다: %s",
                config.SERVICE_NAME,
                e,
            )
            return

        self.service_id, self.address = service_id, address
        self.status, self.error = "UP", None
        logger.info("[Consul] 등록 완료 — %s (%s:%s)", service_id, address, port)

    # -----------------------------------------------------
    def deregister(self) -> None:
        """종료 시 카탈로그에서 자신을 제거한다.

        정상 종료로 해제되지 않은 경우에도 Check의 DeregisterCriticalServiceAfter가
        일정 시간 뒤 정리한다.
        """
        if not self.service_id:
            return
        try:
            self._put(f"/v1/agent/service/deregister/{self.service_id}")
            logger.info("[Consul] 해제 완료 — %s", self.service_id)
        except (URLError, TimeoutError, OSError) as e:  # noqa: BLE001
            logger.warning("[Consul] 해제 실패 — %s", e)
        finally:
            self.service_id = None
            self.status = "DEREGISTERED"

    # -----------------------------------------------------
    def snapshot(self) -> dict:
        """/health 응답에 넣을 현재 상태."""
        result = {"status": self.status}
        if self.enabled:
            result["service_name"] = config.SERVICE_NAME
            result["agent"] = f"{config.CONSUL_HOST}:{config.CONSUL_PORT}"
            if self.service_id:
                result["service_id"] = self.service_id
        if self.error:
            result["error"] = self.error
        return result
