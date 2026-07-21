package com.lcs.course.infrastructure;

import com.lcs.course.application.port.InstructorStatusClient;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 활성 raw 클라이언트(stub/rest/grpc 중 하나)를 서킷 브레이커로 감싸는 데코레이터.
 *
 * <p>강사 정지 확인은 부가 검증이므로 member 장애 시 <b>fail-open</b>한다.
 * 회로 OPEN 또는 호출 실패 시 fallback이 {@code false}(정지 아님)를 돌려주어 강의 작업을 통과시키고
 * 경고 로그를 남긴다. {@link Primary}라 {@code CourseService}는 이 데코레이터를 주입받는다.
 */
@Component
@Primary
public class CircuitBreakerInstructorStatusClient implements InstructorStatusClient {

    private static final Logger log =
            LoggerFactory.getLogger(CircuitBreakerInstructorStatusClient.class);

    private final InstructorStatusClient delegate;

    public CircuitBreakerInstructorStatusClient(
            @Qualifier("instructorStatusClientRaw") InstructorStatusClient delegate) {
        this.delegate = delegate;
    }

    @Override
    @CircuitBreaker(name = "member-service", fallbackMethod = "isSuspendedFallback")
    public boolean isSuspended(Long instructorId) {
        return delegate.isSuspended(instructorId);
    }

    // fail-open: member 조회 실패 시 정지 아님으로 간주해 작업을 통과시키고 경고를 남긴다.
    private boolean isSuspendedFallback(Long instructorId, Throwable t) {
        log.warn("member-service 강사 정지 조회 실패 → fail-open(통과 처리). instructorId={}, cause={}",
                instructorId, t.toString());
        return false;
    }
}
