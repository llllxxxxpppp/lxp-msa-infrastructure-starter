package com.lcs.gateway.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lcs.gateway.jwt.exception.ExpiredTokenException;
import com.lcs.gateway.jwt.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtTokenValidatorTest {

    private static final String SECRET =
            "4d553a82c87c2a2e0b7000d63eb926f3ef75fd528977b9c956efcec692845953";
    private static final String OTHER_SECRET =
            "ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

    private JwtTokenValidator validator;
    private SecretKey key;

    @BeforeEach
    void setUp() {
        validator = new JwtTokenValidator();
        ReflectionTestUtils.setField(validator, "secretKey", SECRET);
        validator.init();
        key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
    }

    private String token(SecretKey signingKey, long userId, String roles, long expiryOffsetMillis) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject("user@example.com")
                .claim("userId", userId)
                .claim("roles", roles)
                .issuedAt(new Date(now))
                .expiration(new Date(now + expiryOffsetMillis))
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    @Test
    void 유효한_토큰은_클레임을_반환한다() {
        String jwt = token(key, 42L, "ROLE_ADMIN,ROLE_MEMBER", 60_000);

        Claims claims = validator.parseClaims(jwt);

        assertThat(validator.getUserId(claims)).isEqualTo(42L);
        assertThat(validator.getRoles(claims)).isEqualTo("ROLE_ADMIN,ROLE_MEMBER");
    }

    @Test
    void 만료된_토큰은_ExpiredTokenException을_던진다() {
        String jwt = token(key, 1L, "ROLE_MEMBER", -1_000); // 이미 만료

        assertThatThrownBy(() -> validator.parseClaims(jwt))
                .isInstanceOf(ExpiredTokenException.class);
    }

    @Test
    void 다른_키로_서명된_토큰은_InvalidTokenException을_던진다() {
        SecretKey wrongKey = Keys.hmacShaKeyFor(OTHER_SECRET.getBytes(StandardCharsets.UTF_8));
        String jwt = token(wrongKey, 1L, "ROLE_MEMBER", 60_000);

        assertThatThrownBy(() -> validator.parseClaims(jwt))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void 형식이_깨진_토큰은_InvalidTokenException을_던진다() {
        assertThatThrownBy(() -> validator.parseClaims("not-a-jwt"))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void roles_클레임이_없으면_빈_문자열을_반환한다() {
        String jwt = Jwts.builder()
                .subject("user@example.com")
                .claim("userId", 7L)
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(key, Jwts.SIG.HS512)
                .compact();

        Claims claims = validator.parseClaims(jwt);

        assertThat(validator.getRoles(claims)).isEmpty();
    }

    @Test
    void resolveToken은_Bearer_접두어를_제거한다() {
        assertThat(validator.resolveToken("Bearer abc.def.ghi")).isEqualTo("abc.def.ghi");
    }

    @Test
    void resolveToken은_Bearer가_아니거나_null이면_null을_반환한다() {
        assertThat(validator.resolveToken(null)).isNull();
        assertThat(validator.resolveToken("abc.def.ghi")).isNull();
        assertThat(validator.resolveToken("Basic abc")).isNull();
    }

    @Test
    void init으로_동일_비밀키를_쓰면_검증에_성공한다() {
        String jwt = token(key, 99L, "ROLE_INSTRUCTOR", 60_000);

        assertThatCode(() -> validator.parseClaims(jwt)).doesNotThrowAnyException();
    }
}
