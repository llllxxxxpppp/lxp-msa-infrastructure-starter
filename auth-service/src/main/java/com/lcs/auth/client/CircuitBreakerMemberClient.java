package com.lcs.auth.client;

import com.lcs.auth.client.dto.response.MemberLoginInfoResponseDTO;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 활성 raw MemberClient(rest/grpc 중 하나)를 서킷 브레이커로 감싸는 데코레이터.
 *
 * <p>로그인은 member 확인이 필수이므로 <b>fail-fast</b>한다. fallback을 두지 않아 회로 OPEN 시
 * {@link io.github.resilience4j.circuitbreaker.CallNotPermittedException}이 대기 없이 전파되고,
 * {@code GlobalExceptionHandler}가 이를 503으로 매핑한다. 회로 CLOSED 중 통신 오류는 raw가 던지는
 * {@code MemberServiceUnavailableException}으로 나가 CB가 실패로 집계한다.
 * {@link Primary}라 {@code CustomUserDetailsService}는 이 데코레이터를 주입받는다.
 */
@Component
@Primary
public class CircuitBreakerMemberClient implements MemberClient {

    private final MemberClient delegate;

    public CircuitBreakerMemberClient(@Qualifier("memberClientRaw") MemberClient delegate) {
        this.delegate = delegate;
    }

    @Override
    @CircuitBreaker(name = "member-service")
    public Optional<MemberLoginInfoResponseDTO> findByEmail(String email) {
        return delegate.findByEmail(email);
    }
}
