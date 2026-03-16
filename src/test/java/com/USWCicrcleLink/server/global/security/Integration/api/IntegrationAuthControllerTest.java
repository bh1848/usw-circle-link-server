package com.USWCicrcleLink.server.global.security.Integration.api;

import com.USWCicrcleLink.server.global.security.Integration.service.IntegrationAuthService;
import com.USWCicrcleLink.server.global.security.jwt.dto.AccessTokenResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = IntegrationAuthControllerTest.TestConfig.class)
@WebMvcTest(IntegrationAuthController.class)
class IntegrationAuthControllerTest {

    private static final String BASE_URL = "/integration";
    private static final String LOGOUT_URL = BASE_URL + "/logout";
    private static final String REFRESH_TOKEN_URL = BASE_URL + "/refresh-token";
    private static final String LOGOUT_SUCCESS_MESSAGE = "로그아웃 성공";
    private static final String REFRESH_TOKEN_SUCCESS_MESSAGE = "새로운 엑세스 토큰이 발급됐습니다.";
    private static final String REFRESH_TOKEN_UNAUTHORIZED_MESSAGE = "리프레시 토큰이 유효하지 않습니다. 로그아웃됐습니다.";
    private static final String ACCESS_TOKEN = "new-access-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private IntegrationAuthService integrationAuthService;

    @Nested
    class logout_테스트 {

        @Test
        void 로그아웃에_성공하면_200을_반환한다() throws Exception {
            MvcResult mvcResult = mockMvc.perform(post(LOGOUT_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(LOGOUT_SUCCESS_MESSAGE))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andReturn();

            assertThat(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(LOGOUT_SUCCESS_MESSAGE);
            then(integrationAuthService).should().logout(any(), any());
        }
    }

    @Nested
    class refreshToken_테스트 {

        @Test
        void 정상_토큰이면_새_AccessToken을_포함한_200을_반환한다() throws Exception {
            AccessTokenResponse response = new AccessTokenResponse(ACCESS_TOKEN);
            given(integrationAuthService.refreshToken(any(), any())).willReturn(response);

            mockMvc.perform(post(REFRESH_TOKEN_URL))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(REFRESH_TOKEN_SUCCESS_MESSAGE))
                    .andExpect(jsonPath("$.data.accessToken").value(ACCESS_TOKEN));

            then(integrationAuthService).should().refreshToken(any(), any());
        }

        @Test
        void 토큰이_null이면_401을_반환한다() throws Exception {
            given(integrationAuthService.refreshToken(any(), any())).willReturn(null);

            MvcResult mvcResult = mockMvc.perform(post(REFRESH_TOKEN_URL))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(REFRESH_TOKEN_UNAUTHORIZED_MESSAGE))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andReturn();

            assertThat(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(REFRESH_TOKEN_UNAUTHORIZED_MESSAGE);
            then(integrationAuthService).should().refreshToken(any(), any());
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(IntegrationAuthController.class)
    static class TestConfig {
    }
}
