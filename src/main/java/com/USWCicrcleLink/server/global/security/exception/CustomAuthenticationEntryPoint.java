package com.USWCicrcleLink.server.global.security.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final String RESPONSE_CONTENT_TYPE = "application/json";
    private static final String DEFAULT_PROFILE = "dev";
    private static final String PRODUCTION_PROFILE = "prod";
    private static final String AUTH_REQUIRED_ERROR = "AUTH_REQUIRED";
    private static final String TOKEN_EXPIRED_ERROR = "TOKEN_EXPIRED";
    private static final String INVALID_TOKEN_ERROR = "INVALID_TOKEN";
    private static final String AUTH_REQUIRED_MESSAGE = "인증이 필요합니다.";
    private static final String TOKEN_EXPIRED_MESSAGE = "토큰이 만료되었습니다.";
    private static final String INVALID_TOKEN_MESSAGE = "인증이 필요합니다.";

    private final ObjectMapper objectMapper;
    private final String activeProfile;

    public CustomAuthenticationEntryPoint(ObjectMapper objectMapper, Environment environment) {
        this.objectMapper = objectMapper;
        this.activeProfile = environment.getProperty("spring.profiles.active", DEFAULT_PROFILE);
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException {
        response.setContentType(RESPONSE_CONTENT_TYPE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String errorCode = AUTH_REQUIRED_ERROR;
        String errorMessage = AUTH_REQUIRED_MESSAGE;
        int status = HttpServletResponse.SC_UNAUTHORIZED;

        if (authException instanceof CustomAuthenticationException) {
            errorCode = authException.getMessage();
            if (TOKEN_EXPIRED_ERROR.equals(errorCode)) {
                errorMessage = TOKEN_EXPIRED_MESSAGE;
            } else if (INVALID_TOKEN_ERROR.equals(errorCode)) {
                errorMessage = INVALID_TOKEN_MESSAGE;
                if (PRODUCTION_PROFILE.equals(activeProfile)) {
                    log.error("[SECURITY ALERT] 변조된 JWT 감지 - IP: {} | 요청 경로: {}", request.getRemoteAddr(), request.getRequestURI());
                } else {
                    log.warn("[JWT WARNING] 변조된 JWT 감지 - IP: {} | 요청 경로: {}", request.getRemoteAddr(), request.getRequestURI());
                }
            }
        }

        response.setStatus(status);
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("status", status);
        responseBody.put("errorCode", errorCode);
        responseBody.put("message", errorMessage);

        response.getWriter().write(objectMapper.writeValueAsString(responseBody));
    }
}
