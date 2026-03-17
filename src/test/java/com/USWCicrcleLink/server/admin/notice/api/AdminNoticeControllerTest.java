package com.USWCicrcleLink.server.admin.notice.api;

import com.USWCicrcleLink.server.admin.notice.dto.AdminNoticeCreationRequest;
import com.USWCicrcleLink.server.admin.notice.dto.AdminNoticeListResponse;
import com.USWCicrcleLink.server.admin.notice.dto.AdminNoticePageListResponse;
import com.USWCicrcleLink.server.admin.notice.dto.AdminNoticeUpdateRequest;
import com.USWCicrcleLink.server.admin.notice.dto.NoticeDetailResponse;
import com.USWCicrcleLink.server.admin.notice.service.AdminNoticeService;
import com.USWCicrcleLink.server.global.exception.ExceptionType;
import com.USWCicrcleLink.server.global.exception.GlobalExceptionHandler;
import com.USWCicrcleLink.server.global.exception.errortype.NoticeException;
import com.USWCicrcleLink.server.global.validation.validator.SanitizationBinder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Import({GlobalExceptionHandler.class, SanitizationBinder.class})
@AutoConfigureMockMvc(addFilters = false)
@ContextConfiguration(classes = AdminNoticeControllerTest.TestConfig.class)
@WebMvcTest(AdminNoticeController.class)
class AdminNoticeControllerTest {

    private static final String BASE_URL = "/notices";
    private static final String SUCCESS_LIST_MESSAGE = "공지사항 리스트 조회 성공";
    private static final String SUCCESS_DETAIL_MESSAGE = "공지사항 조회 성공";
    private static final String SUCCESS_CREATE_MESSAGE = "공지사항 생성 성공";
    private static final String SUCCESS_UPDATE_MESSAGE = "공지사항 수정 성공";
    private static final String SUCCESS_DELETE_MESSAGE = "공지사항 삭제 성공";
    private static final String ADMIN_NAME = "관리자";
    private static final String NOTICE_TITLE = "공지 제목";
    private static final String NOTICE_CONTENT = "공지 내용";
    private static final String PHOTO_FILE_NAME = "notice.png";
    private static final String PHOTO_CONTENT_TYPE = "image/png";
    private static final String REQUEST_PART_NAME = "request";
    private static final String PHOTOS_PART_NAME = "photos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminNoticeService adminNoticeService;

    @Nested
    class getNotices_테스트 {

        @Test
        void 공지사항_목록을_페이징_조회하면_200을_반환한다() throws Exception {
            AdminNoticePageListResponse response = AdminNoticePageListResponse.builder()
                    .content(List.of(AdminNoticeListResponse.builder()
                            .noticeUUID(UUID.randomUUID())
                            .noticeTitle(NOTICE_TITLE)
                            .adminName(ADMIN_NAME)
                            .noticeCreatedAt(LocalDateTime.of(2026, 3, 17, 10, 0))
                            .build()))
                    .totalPages(1)
                    .totalElements(1)
                    .currentPage(0)
                    .build();

            given(adminNoticeService.getNotices(any())).willReturn(response);

            MvcResult mvcResult = mockMvc.perform(get(BASE_URL)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.message").value(SUCCESS_LIST_MESSAGE))
                    .andExpect(jsonPath("$.data.content[0].noticeTitle").value(NOTICE_TITLE))
                    .andExpect(jsonPath("$.data.content[0].adminName").value(ADMIN_NAME))
                    .andExpect(jsonPath("$.data.totalPages").value(1))
                    .andExpect(jsonPath("$.data.totalElements").value(1))
                    .andExpect(jsonPath("$.data.currentPage").value(0))
                    .andReturn();

            assertThat(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(SUCCESS_LIST_MESSAGE);
            then(adminNoticeService).should().getNotices(any());
        }

        @Test
        void 공지사항_조회_중_예외가_발생하면_400을_반환한다() throws Exception {
            given(adminNoticeService.getNotices(any())).willThrow(new NoticeException(ExceptionType.NOTICE_CHECKING_ERROR));

            mockMvc.perform(get(BASE_URL))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value(ExceptionType.NOTICE_CHECKING_ERROR.getCode()))
                    .andExpect(jsonPath("$.message").value(ExceptionType.NOTICE_CHECKING_ERROR.getMessage()));
        }
    }

    @Nested
    class getNoticeByUUID_테스트 {

        @Test
        void 공지사항_단건을_조회하면_200을_반환한다() throws Exception {
            UUID noticeUUID = UUID.randomUUID();
            NoticeDetailResponse response = NoticeDetailResponse.builder()
                    .noticeUUID(noticeUUID)
                    .noticeTitle(NOTICE_TITLE)
                    .noticeContent(NOTICE_CONTENT)
                    .noticePhotos(List.of("https://cdn.test/notice.png"))
                    .noticeCreatedAt(LocalDateTime.of(2026, 3, 17, 11, 0))
                    .adminName(ADMIN_NAME)
                    .build();

            given(adminNoticeService.getNoticeByUUID(noticeUUID)).willReturn(response);

            mockMvc.perform(get(BASE_URL + "/{noticeUUID}", noticeUUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(SUCCESS_DETAIL_MESSAGE))
                    .andExpect(jsonPath("$.data.noticeUUID").value(noticeUUID.toString()))
                    .andExpect(jsonPath("$.data.noticeTitle").value(NOTICE_TITLE))
                    .andExpect(jsonPath("$.data.noticeContent").value(NOTICE_CONTENT))
                    .andExpect(jsonPath("$.data.adminName").value(ADMIN_NAME));

            then(adminNoticeService).should().getNoticeByUUID(noticeUUID);
        }

        @Test
        void 존재하지_않는_UUID로_조회하면_404를_반환한다() throws Exception {
            UUID noticeUUID = UUID.randomUUID();
            given(adminNoticeService.getNoticeByUUID(noticeUUID)).willThrow(new NoticeException(ExceptionType.NOTICE_NOT_EXISTS));

            mockMvc.perform(get(BASE_URL + "/{noticeUUID}", noticeUUID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ExceptionType.NOTICE_NOT_EXISTS.getCode()))
                    .andExpect(jsonPath("$.message").value(ExceptionType.NOTICE_NOT_EXISTS.getMessage()));
        }
    }

    @Nested
    class createNotice_테스트 {

        @Test
        void 사진_없이_공지사항을_생성하면_200을_반환한다() throws Exception {
            AdminNoticeCreationRequest request = new AdminNoticeCreationRequest(
                    "<script>alert('xss')</script>" + NOTICE_TITLE,
                    "<script>alert('xss')</script><b>" + NOTICE_CONTENT + "</b>",
                    List.of()
            );
            MockMultipartFile requestPart = createJsonPart(request);
            List<String> presignedUrls = List.of();

            given(adminNoticeService.createNotice(any(AdminNoticeCreationRequest.class), eq(null))).willReturn(presignedUrls);

            mockMvc.perform(multipart(BASE_URL)
                            .file(requestPart))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(SUCCESS_CREATE_MESSAGE))
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data.length()").value(0));

            then(adminNoticeService).should().createNotice(argThat(createdRequest ->
                    createdRequest.getNoticeTitle().equals(NOTICE_TITLE)
                            && createdRequest.getNoticeContent().equals("<b>" + NOTICE_CONTENT + "</b>")
            ), eq(null));
        }

        @Test
        void 사진과_함께_공지사항을_생성하면_200을_반환한다() throws Exception {
            AdminNoticeCreationRequest request = new AdminNoticeCreationRequest(
                    NOTICE_TITLE,
                    NOTICE_CONTENT,
                    List.of(1)
            );
            MockMultipartFile requestPart = createJsonPart(request);
            MockMultipartFile photo = createPhotoPart();
            List<String> presignedUrls = List.of("https://cdn.test/upload-url");

            given(adminNoticeService.createNotice(any(AdminNoticeCreationRequest.class), any())).willReturn(presignedUrls);

            mockMvc.perform(multipart(BASE_URL)
                            .file(requestPart)
                            .file(photo))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(SUCCESS_CREATE_MESSAGE))
                    .andExpect(jsonPath("$.data[0]").value("https://cdn.test/upload-url"));

            then(adminNoticeService).should().createNotice(any(AdminNoticeCreationRequest.class), any());
        }

        @Test
        void 생성_요청_중_예외가_발생하면_413을_반환한다() throws Exception {
            AdminNoticeCreationRequest request = new AdminNoticeCreationRequest(
                    NOTICE_TITLE,
                    NOTICE_CONTENT,
                    List.of(1, 2, 3, 4, 5)
            );
            MockMultipartFile requestPart = createJsonPart(request);

            given(adminNoticeService.createNotice(any(AdminNoticeCreationRequest.class), eq(null)))
                    .willThrow(new NoticeException(ExceptionType.UP_TO_5_PHOTOS_CAN_BE_UPLOADED));

            mockMvc.perform(multipart(BASE_URL)
                            .file(requestPart))
                    .andExpect(status().isPayloadTooLarge())
                    .andExpect(jsonPath("$.code").value(ExceptionType.UP_TO_5_PHOTOS_CAN_BE_UPLOADED.getCode()))
                    .andExpect(jsonPath("$.message").value(ExceptionType.UP_TO_5_PHOTOS_CAN_BE_UPLOADED.getMessage()));
        }
    }

    @Nested
    class updateNotice_테스트 {

        @Test
        void 공지사항을_수정하면_200을_반환한다() throws Exception {
            UUID noticeUUID = UUID.randomUUID();
            AdminNoticeUpdateRequest request = new AdminNoticeUpdateRequest(
                    "<script>alert('xss')</script>" + NOTICE_TITLE,
                    "<script>alert('xss')</script><b>" + NOTICE_CONTENT + "</b>",
                    List.of(1)
            );
            MockMultipartFile requestPart = createJsonPart(request);
            MockMultipartFile photo = createPhotoPart();
            List<String> presignedUrls = List.of("https://cdn.test/updated-url");

            given(adminNoticeService.updateNotice(eq(noticeUUID), any(AdminNoticeUpdateRequest.class), any())).willReturn(presignedUrls);

            mockMvc.perform(multipart(BASE_URL + "/{noticeUUID}", noticeUUID)
                            .file(requestPart)
                            .file(photo)
                            .with(httpServletRequest -> {
                                httpServletRequest.setMethod("PUT");
                                return httpServletRequest;
                            }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(SUCCESS_UPDATE_MESSAGE))
                    .andExpect(jsonPath("$.data[0]").value("https://cdn.test/updated-url"));

            then(adminNoticeService).should().updateNotice(eq(noticeUUID), argThat(updatedRequest ->
                    updatedRequest.getNoticeTitle().equals(NOTICE_TITLE)
                            && updatedRequest.getNoticeContent().equals("<b>" + NOTICE_CONTENT + "</b>")
            ), any());
        }

        @Test
        void 존재하지_않는_UUID로_수정하면_404를_반환한다() throws Exception {
            UUID noticeUUID = UUID.randomUUID();
            AdminNoticeUpdateRequest request = new AdminNoticeUpdateRequest(
                    NOTICE_TITLE,
                    NOTICE_CONTENT,
                    List.of()
            );
            MockMultipartFile requestPart = createJsonPart(request);

            given(adminNoticeService.updateNotice(eq(noticeUUID), any(AdminNoticeUpdateRequest.class), eq(null)))
                    .willThrow(new NoticeException(ExceptionType.NOTICE_NOT_EXISTS));

            mockMvc.perform(multipart(BASE_URL + "/{noticeUUID}", noticeUUID)
                            .file(requestPart)
                            .with(httpServletRequest -> {
                                httpServletRequest.setMethod("PUT");
                                return httpServletRequest;
                            }))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ExceptionType.NOTICE_NOT_EXISTS.getCode()))
                    .andExpect(jsonPath("$.message").value(ExceptionType.NOTICE_NOT_EXISTS.getMessage()));
        }
    }

    @Nested
    class deleteNotice_테스트 {

        @Test
        void 공지사항을_삭제하면_200을_반환한다() throws Exception {
            UUID noticeUUID = UUID.randomUUID();

            MvcResult mvcResult = mockMvc.perform(delete(BASE_URL + "/{noticeUUID}", noticeUUID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(SUCCESS_DELETE_MESSAGE))
                    .andExpect(jsonPath("$.data").value(noticeUUID.toString()))
                    .andReturn();

            assertThat(mvcResult.getResponse().getContentAsString(StandardCharsets.UTF_8)).contains(noticeUUID.toString());
            then(adminNoticeService).should().deleteNotice(noticeUUID);
        }

        @Test
        void 존재하지_않는_UUID로_삭제하면_404를_반환한다() throws Exception {
            UUID noticeUUID = UUID.randomUUID();
            willThrow(new NoticeException(ExceptionType.NOTICE_NOT_EXISTS))
                    .given(adminNoticeService)
                    .deleteNotice(noticeUUID);

            mockMvc.perform(delete(BASE_URL + "/{noticeUUID}", noticeUUID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value(ExceptionType.NOTICE_NOT_EXISTS.getCode()))
                    .andExpect(jsonPath("$.message").value(ExceptionType.NOTICE_NOT_EXISTS.getMessage()));
        }
    }

    private MockMultipartFile createJsonPart(Object request) throws Exception {
        return new MockMultipartFile(
                REQUEST_PART_NAME,
                REQUEST_PART_NAME + ".json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(request)
        );
    }

    private MockMultipartFile createPhotoPart() {
        return new MockMultipartFile(
                PHOTOS_PART_NAME,
                PHOTO_FILE_NAME,
                PHOTO_CONTENT_TYPE,
                "image-content".getBytes(StandardCharsets.UTF_8)
        );
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @Import(AdminNoticeController.class)
    static class TestConfig {
    }
}
