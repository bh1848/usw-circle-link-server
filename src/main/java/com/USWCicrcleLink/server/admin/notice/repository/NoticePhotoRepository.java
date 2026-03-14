package com.USWCicrcleLink.server.admin.notice.repository;

import com.USWCicrcleLink.server.admin.notice.domain.Notice;
import com.USWCicrcleLink.server.admin.notice.domain.NoticePhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NoticePhotoRepository extends JpaRepository<NoticePhoto, Long> {
    List<NoticePhoto> findByNotice(Notice notice);
}
