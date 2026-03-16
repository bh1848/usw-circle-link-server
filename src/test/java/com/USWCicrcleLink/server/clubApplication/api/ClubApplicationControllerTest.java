package com.USWCicrcleLink.server.clubApplication.api;

import com.USWCicrcleLink.server.clubApplication.service.ClubApplicationService;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.GlobalExceptionHandler;
import com.USWCicrcleLink.server.global.exception.errortype.ClubApplicationException;
import com.USWCicrcleLink.server.global.exception.errortype.ClubException;
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
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = ClubApplicationControllerTest.TestConfig.class)
@WebMvcTest(ClubApplicationController.class)
class ClubApplicationControllerTest {

    private static final String BASE_URL = "/apply";
    private static final String CAN_APPLY_URL = BASE_URL + "/can-apply/{clubUUID}";
    private static final String CAN_APPLY_MESSAGE = "지원 가능";
    private static final String GOOGLE_FORM_SUCCESS_MESSAGE = "구글 폼 URL 조회 성공";
    private static final String SUBMIT_SUCCESS_MESSAGE = "지원서 제출 성공";
    private static final String GOOGLE_FORM_URL = "https://forms.google.com/test";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClubApplicationService clubApplicationService;

    @Nested
    class canApply_테스트 {

        @Test
        void 지원_가능하면_200을_반환한다() throws Exception {
            UUID clubUUID = UUID.randomUUID();

            MvcResult mvcResult = mockMvc.perform(get(CAN_APPLY_URL, clubUUID))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(CAN_APPLY_MESSAGE))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andReturn();

            assertThat(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(CAN_APPLY_MESSAGE);
            then(clubApplicationService).should().checkIfCanApply(clubUUID);
        }

        @Test
        void 이미_지원한_경우_400을_반환한다() throws Exception {
            UUID clubUUID = UUID.randomUUID();

            willThrow(new ClubApplicationException(ExceptionType.ALREADY_APPLIED))
                    .given(clubApplicationService)
                    .checkIfCanApply(clubUUID);

            mockMvc.perform(get(CAN_APPLY_URL, clubUUID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ExceptionType.ALREADY_APPLIED.getCode()))
                    .andExpect(jsonPath("$.message").value(ExceptionType.ALREADY_APPLIED.getMessage()));
        }

        @Test
        void 이미_회원인_경우_400을_반환한다() throws Exception {
            UUID clubUUID = UUID.randomUUID();

            willThrow(new ClubApplicationException(ExceptionType.ALREADY_MEMBER))
                    .given(clubApplicationService)
                    .checkIfCanApply(clubUUID);

            mockMvc.perform(get(CAN_APPLY_URL, clubUUID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ExceptionType.ALREADY_MEMBER.getCode()))
                    .andExpect(jsonPath("$.message").value(ExceptionType.ALREADY_MEMBER.getMessage()));
        }
    }

    @Nested
    class getGoogleFormUrl_테스트 {

        @Test
        void 구글_폼_URL을_조회하면_200을_반환한다() throws Exception {
            UUID clubUUID = UUID.randomUUID();

            willReturn(GOOGLE_FORM_URL)
                    .given(clubApplicationService)
                    .getGoogleFormUrlByClubUUID(clubUUID);

            mockMvc.perform(get(BASE_URL + "/{clubUUID}", clubUUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(GOOGLE_FORM_SUCCESS_MESSAGE))
                    .andExpect(jsonPath("$.data").value(GOOGLE_FORM_URL));

            then(clubApplicationService).should().getGoogleFormUrlByClubUUID(clubUUID);
        }

        @Test
        void URL_조회_대상이_없으면_404를_반환한다() throws Exception {
            UUID clubUUID = UUID.randomUUID();

            willThrow(new ClubException(ExceptionType.CLUB_INTRO_NOT_EXISTS))
                    .given(clubApplicationService)
                    .getGoogleFormUrlByClubUUID(clubUUID);

            mockMvc.perform(get(BASE_URL + "/{clubUUID}", clubUUID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ExceptionType.CLUB_INTRO_NOT_EXISTS.getCode()))
                    .andExpect(jsonPath("$.message").value(ExceptionType.CLUB_INTRO_NOT_EXISTS.getMessage()));
        }
    }

    @Nested
    class submitClubApplication_테스트 {

        @Test
        void 지원서를_제출하면_200을_반환한다() throws Exception {
            UUID clubUUID = UUID.randomUUID();

            MvcResult mvcResult = mockMvc.perform(post(BASE_URL + "/{clubUUID}", clubUUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(SUBMIT_SUCCESS_MESSAGE))
                    .andExpect(jsonPath("$.data").doesNotExist())
                    .andReturn();

            assertThat(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(SUBMIT_SUCCESS_MESSAGE);
            then(clubApplicationService).should().submitClubApplication(clubUUID);
        }

        @Test
        void 중복_지원이면_400을_반환한다() throws Exception {
            UUID clubUUID = UUID.randomUUID();

            willThrow(new ClubApplicationException(ExceptionType.ALREADY_APPLIED))
                    .given(clubApplicationService)
                    .submitClubApplication(clubUUID);

            mockMvc.perform(post(BASE_URL + "/{clubUUID}", clubUUID))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ExceptionType.ALREADY_APPLIED.getCode()))
                    .andExpect(jsonPath("$.message").value(ExceptionType.ALREADY_APPLIED.getMessage()));
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(ClubApplicationController.class)
    static class TestConfig {
    }
}
