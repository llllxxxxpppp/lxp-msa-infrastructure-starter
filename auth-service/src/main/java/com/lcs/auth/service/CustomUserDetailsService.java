package com.lcs.auth.service;

import com.lcs.auth.principal.CustomUserPrincipal;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final MemberRepository memberRepository;

    public CustomUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + username));

        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + member.getRole().name());

        return new CustomUserPrincipal(
                member.getId().value(),
                member.getEmail(),
                member.getPassword(),
                List.of(authority),
                member.isDeleted());
    }
}
