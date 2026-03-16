package com.USWCicrcleLink.server.admin.notice.repository;

import com.USWCicrcleLink.server.admin.admin.domain.Admin;
import com.USWCicrcleLink.server.admin.admin.repository.AdminRepository;
import com.USWCicrcleLink.server.admin.notice.domain.Notice;
import com.USWCicrcleLink.server.global.security.jwt.domain.Role;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class NoticeRepositoryTest {

    private static final String ADMIN_ACCOUNT = "admin";
    private static final String ADMIN_PASSWORD = "encoded-password";
    private static final String ADMIN_NAME = "관리자";
    private static final String NOTICE_TITLE = "공지 제목";
    private static final String NOTICE_CONTENT = "공지 내용";

    @Autowired
    private NoticeRepository noticeRepository;

    @Autowired
    private AdminRepository adminRepository;

    @MockBean
    private com.USWCicrcleLink.server.global.s3File.Service.S3FileUploadService s3FileUploadService;

    @Nested
    class findByNoticeUUID_테스트 {

        @Test
        void 저장된_UUID로_조회하면_공지사항을_반환한다() {
            Admin savedAdmin = adminRepository.save(Admin.builder()
                    .adminAccount(ADMIN_ACCOUNT)
                    .adminPw(ADMIN_PASSWORD)
                    .adminName(ADMIN_NAME)
                    .role(Role.ADMIN)
                    .build());
            UUID noticeUUID = UUID.randomUUID();
            Notice savedNotice = noticeRepository.save(Notice.builder()
                    .noticeUUID(noticeUUID)
                    .noticeTitle(NOTICE_TITLE)
                    .noticeContent(NOTICE_CONTENT)
                    .noticeCreatedAt(LocalDateTime.of(2026, 3, 17, 12, 0))
                    .admin(savedAdmin)
                    .build());

            Optional<Notice> result = noticeRepository.findByNoticeUUID(noticeUUID);

            assertThat(result).isPresent();
            assertThat(result.get().getNoticeId()).isEqualTo(savedNotice.getNoticeId());
            assertThat(result.get().getNoticeUUID()).isEqualTo(noticeUUID);
            assertThat(result.get().getNoticeTitle()).isEqualTo(NOTICE_TITLE);
            assertThat(result.get().getNoticeContent()).isEqualTo(NOTICE_CONTENT);
        }

        @Test
        void 존재하지_않는_UUID로_조회하면_빈_Optional을_반환한다() {
            Optional<Notice> result = noticeRepository.findByNoticeUUID(UUID.randomUUID());

            assertThat(result).isEmpty();
        }
    }
}
