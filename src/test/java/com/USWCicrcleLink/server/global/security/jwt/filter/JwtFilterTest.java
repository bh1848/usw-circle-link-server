package com.USWCicrcleLink.server.global.security.jwt.filter;

import com.USWCicrcleLink.server.global.security.exception.CustomAuthenticationEntryPoint;
import com.USWCicrcleLink.server.global.security.exception.CustomAuthenticationException;
import com.USWCicrcleLink.server.global.security.jwt.JwtProvider;
import com.USWCicrcleLink.server.global.security.jwt.domain.TokenValidationResult;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class JwtFilterTest {

    private static final String PERMIT_ALL_PATH = "/public/**";
    private static final String PERMIT_ALL_REQUEST_URI = "/public/notices";
    private static final String PROTECTED_REQUEST_URI = "/clubs";
    private static final String ACCESS_TOKEN = "access-token";
    private static final String TOKEN_EXPIRED_ERROR = "TOKEN_EXPIRED";
    private static final String INVALID_TOKEN_ERROR = "INVALID_TOKEN";

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private CustomAuthenticationEntryPoint customAuthenticationEntryPoint;

    @Mock
    private FilterChain filterChain;

    @Spy
    private List<String> permitAllPaths = new ArrayList<>(List.of(PERMIT_ALL_PATH));

    @InjectMocks
    private JwtFilter jwtFilter;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    class permitAll_경로_요청_테스트 {

        @Test
        void permitAll_경로_요청이면_토큰_검증_없이_필터를_통과한다() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", PERMIT_ALL_REQUEST_URI);
            MockHttpServletResponse response = new MockHttpServletResponse();

            jwtFilter.doFilterInternal(request, response, filterChain);

            then(filterChain).should().doFilter(request, response);
            then(jwtProvider).shouldHaveNoInteractions();
            then(customAuthenticationEntryPoint).shouldHaveNoInteractions();
        }
    }

    @Nested
    class 유효한_토큰_테스트 {

        @Test
        void 유효한_토큰이면_Authentication을_설정하고_필터를_통과한다() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", PROTECTED_REQUEST_URI);
            MockHttpServletResponse response = new MockHttpServletResponse();
            Authentication authentication = mock(Authentication.class);

            given(jwtProvider.resolveAccessToken(request)).willReturn(ACCESS_TOKEN);
            given(jwtProvider.validateAccessToken(ACCESS_TOKEN)).willReturn(TokenValidationResult.VALID);
            given(jwtProvider.getAuthentication(ACCESS_TOKEN)).willReturn(authentication);

            jwtFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
            then(jwtProvider).should().resolveAccessToken(request);
            then(jwtProvider).should().validateAccessToken(ACCESS_TOKEN);
            then(jwtProvider).should().getAuthentication(ACCESS_TOKEN);
            then(filterChain).should().doFilter(request, response);
            then(customAuthenticationEntryPoint).shouldHaveNoInteractions();
        }
    }

    @Nested
    class 만료된_토큰_테스트 {

        @Test
        void 만료된_토큰이면_CustomAuthenticationEntryPoint를_호출한다() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", PROTECTED_REQUEST_URI);
            MockHttpServletResponse response = new MockHttpServletResponse();
            ArgumentCaptor<CustomAuthenticationException> exceptionCaptor = ArgumentCaptor.forClass(CustomAuthenticationException.class);

            given(jwtProvider.resolveAccessToken(request)).willReturn(ACCESS_TOKEN);
            given(jwtProvider.validateAccessToken(ACCESS_TOKEN)).willReturn(TokenValidationResult.EXPIRED);

            jwtFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            then(customAuthenticationEntryPoint).should().commence(
                    eq(request),
                    eq(response),
                    exceptionCaptor.capture()
            );
            assertThat(exceptionCaptor.getValue().getMessage()).isEqualTo(TOKEN_EXPIRED_ERROR);
            then(filterChain).should(never()).doFilter(request, response);
            then(jwtProvider).should().resolveAccessToken(request);
            then(jwtProvider).should().validateAccessToken(ACCESS_TOKEN);
            then(jwtProvider).shouldHaveNoMoreInteractions();
        }
    }

    @Nested
    class 유효하지_않은_토큰_테스트 {

        @Test
        void 유효하지_않은_토큰이면_CustomAuthenticationEntryPoint를_호출한다() throws Exception {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", PROTECTED_REQUEST_URI);
            MockHttpServletResponse response = new MockHttpServletResponse();
            ArgumentCaptor<CustomAuthenticationException> exceptionCaptor = ArgumentCaptor.forClass(CustomAuthenticationException.class);

            given(jwtProvider.resolveAccessToken(request)).willReturn(ACCESS_TOKEN);
            given(jwtProvider.validateAccessToken(ACCESS_TOKEN)).willReturn(TokenValidationResult.INVALID);

            jwtFilter.doFilterInternal(request, response, filterChain);

            assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
            then(customAuthenticationEntryPoint).should().commence(
                    eq(request),
                    eq(response),
                    exceptionCaptor.capture()
            );
            assertThat(exceptionCaptor.getValue().getMessage()).isEqualTo(INVALID_TOKEN_ERROR);
            then(filterChain).should(never()).doFilter(request, response);
            then(jwtProvider).should().resolveAccessToken(request);
            then(jwtProvider).should().validateAccessToken(ACCESS_TOKEN);
            then(jwtProvider).shouldHaveNoMoreInteractions();
        }
    }
}
