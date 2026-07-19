package com.lcs.auth.jwt;

import com.lcs.auth.exception.ExpiredJwtCustomException;
import com.lcs.auth.exception.InvalidJwtCustomException;
import com.lcs.auth.exception.InvalidRefreshTokenException;
import com.lcs.auth.service.RefreshService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshService refreshService;

    private final SecurityContextHolderStrategy securityContextHolderStrategy =
            SecurityContextHolder.getContextHolderStrategy();

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            RefreshService refreshService
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.refreshService = refreshService;
    }

    private void tryRefresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = jwtTokenProvider.resolveRefreshToken(request);
        if (refreshToken == null) {
            securityContextHolderStrategy.clearContext();
            return;
        }

        try {
            String newToken = refreshService.refreshAccessToken(refreshToken);
            Authentication newAuthentication = jwtTokenProvider.getAuthentication(newToken);
            setAuthentication(newAuthentication);
            response.setHeader("New-Access-Token", newToken);
        } catch (InvalidRefreshTokenException e) {
            securityContextHolderStrategy.clearContext();
        } catch (Exception e) { // NOPMD - catch-all for unexpected exceptions during refresh
            securityContextHolderStrategy.clearContext();
        }
    }

    private void setAuthentication(Authentication authentication) {
        SecurityContext context = securityContextHolderStrategy.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextHolderStrategy.setContext(context);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String accessToken = jwtTokenProvider.resolveToken(request);

        try {
            if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
                Authentication authentication = jwtTokenProvider.getAuthentication(accessToken);
                setAuthentication(authentication);
            }
        } catch (ExpiredJwtCustomException e) {
            tryRefresh(request, response);
        } catch (InvalidJwtCustomException e) {
            securityContextHolderStrategy.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
