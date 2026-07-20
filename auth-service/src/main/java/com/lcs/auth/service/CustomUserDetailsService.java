package com.lcs.auth.service;

import com.lcs.auth.client.MemberClient;
import com.lcs.auth.client.dto.response.MemberLoginInfoResponseDTO;
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
    private final MemberClient memberClient;

    public CustomUserDetailsService(MemberClient memberClient) {
        this.memberClient = memberClient;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        MemberLoginInfoResponseDTO member = memberClient.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + username));

        GrantedAuthority authority = new SimpleGrantedAuthority("ROLE_" + member.role());

        return new CustomUserPrincipal(
                member.id(),
                username,
                member.password(),
                List.of(authority),
                !member.deleted() && !member.suspended());
    }
}
