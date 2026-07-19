package com.lcs.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.lcs.auth.client.MemberClient;
import com.lcs.auth.client.dto.response.MemberLoginInfoResponseDTO;
import com.lcs.auth.principal.CustomUserPrincipal;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService 단위 테스트")
class CustomUserDetailsServiceTest {

    @Mock
    private MemberClient memberClient;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("활성 회원이면 member-service 응답을 활성화된 CustomUserPrincipal로 변환한다")
    void loadUserByUsername_activeMember_returnsEnabledPrincipal() {
        MemberLoginInfoResponseDTO member =
                new MemberLoginInfoResponseDTO(1L, "user@test.com", "encoded-password", false, "USER");
        given(memberClient.findByEmail("user@test.com")).willReturn(Optional.of(member));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("user@test.com");

        assertThat(userDetails).isInstanceOf(CustomUserPrincipal.class);
        CustomUserPrincipal principal = (CustomUserPrincipal) userDetails;
        assertThat(principal.getUserId()).isEqualTo(1L);
        assertThat(principal.getUsername()).isEqualTo("user@test.com");
        assertThat(principal.getPassword()).isEqualTo("encoded-password");
        assertThat(principal.isEnabled()).isTrue();
        assertThat(principal.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_USER");
    }

    @Test
    @DisplayName("탈퇴한 회원이면 비활성화된 CustomUserPrincipal을 반환한다")
    void loadUserByUsername_deletedMember_returnsDisabledPrincipal() {
        MemberLoginInfoResponseDTO member =
                new MemberLoginInfoResponseDTO(1L, "user@test.com", "encoded-password", true, "USER");
        given(memberClient.findByEmail("user@test.com")).willReturn(Optional.of(member));

        UserDetails userDetails = customUserDetailsService.loadUserByUsername("user@test.com");

        assertThat(userDetails.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("member-service에 회원이 없으면 UsernameNotFoundException이 발생한다")
    void loadUserByUsername_memberNotFound_throwsUsernameNotFoundException() {
        given(memberClient.findByEmail("missing@test.com")).willReturn(Optional.empty());

        assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("missing@test.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }
}
