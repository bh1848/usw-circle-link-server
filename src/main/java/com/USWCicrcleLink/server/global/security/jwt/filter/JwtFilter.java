package com.USWCicrcleLink.server.global.security.jwt.filter;

import com.USWCicrcleLink.server.global.security.details.CustomAdminDetails;
import com.USWCicrcleLink.server.global.security.details.CustomLeaderDetails;
import com.USWCicrcleLink.server.global.security.details.CustomUserDetails;
import com.USWCicrcleLink.server.global.security.exception.CustomAuthenticationEntryPoint;
import com.USWCicrcleLink.server.global.security.exception.CustomAuthenticationException;
import com.USWCicrcleLink.server.global.security.jwt.JwtProvider;
import com.USWCicrcleLink.server.global.security.jwt.domain.TokenValidationResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private static final String TOKEN_EXPIRED_ERROR = "TOKEN_EXPIRED";
    private static final String INVALID_TOKEN_ERROR = "INVALID_TOKEN";
    private static final String MDC_USER_TYPE_KEY = "userType";
    private static final String MDC_USER_UUID_KEY = "userUUID";

    private final JwtProvider jwtProvider;
    private final List<String> permitAllPaths;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final PathMatcher pathMatcher = new AntPathMatcher();

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String requestPath = request.getRequestURI();
        if (isPermitAllPath(requestPath)) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = jwtProvider.resolveAccessToken(request);

        try {
            TokenValidationResult tokenValidationResult = jwtProvider.validateAccessToken(accessToken);
            switch (tokenValidationResult) {
                case EXPIRED -> throw new CustomAuthenticationException(TOKEN_EXPIRED_ERROR);
                case INVALID -> throw new CustomAuthenticationException(INVALID_TOKEN_ERROR);
                case VALID -> {
                    Authentication auth = jwtProvider.getAuthentication(accessToken);
                    SecurityContextHolder.getContext().setAuthentication(auth);
                    setMdcUserDetails(auth, request.getMethod(), request.getRequestURI());
                    filterChain.doFilter(request, response);
                }
            }
        } catch (AuthenticationException exception) {
            SecurityContextHolder.clearContext();
            customAuthenticationEntryPoint.commence(request, response, exception);
        } finally {
            MDC.clear();
        }
    }

    private void setMdcUserDetails(Authentication auth, String method, String path) {
        if (auth.getPrincipal() instanceof CustomAdminDetails adminDetails) {
            MDC.put(MDC_USER_TYPE_KEY, "Admin");
            MDC.put(MDC_USER_UUID_KEY, adminDetails.getAdminUUID().toString());
        } else if (auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            MDC.put(MDC_USER_TYPE_KEY, "User");
            MDC.put(MDC_USER_UUID_KEY, userDetails.getUserUUID().toString());
        } else if (auth.getPrincipal() instanceof CustomLeaderDetails leaderDetails) {
            MDC.put(MDC_USER_TYPE_KEY, "Leader");
            MDC.put(MDC_USER_UUID_KEY, leaderDetails.getLeaderUUID().toString());
        }

        if (log.isInfoEnabled()) {
            log.info("[{}: {}] {} 요청 경로: {}", MDC.get(MDC_USER_TYPE_KEY), MDC.get(MDC_USER_UUID_KEY), method, path);
        }
    }

    private boolean isPermitAllPath(String requestPath) {
        return permitAllPaths.stream().anyMatch(permitPath -> pathMatcher.match(permitPath, requestPath));
    }
}
