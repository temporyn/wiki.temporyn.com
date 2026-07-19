package com.temporyn.wiki.repository;

import com.temporyn.wiki.domain.Attachment;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByArticleIdOrderById(Long articleId);

    List<Attachment> findByUploadToken(String uploadToken);

    List<Attachment> findByArticleIdIsNullAndCreatedAtBefore(LocalDateTime cutoff);
}
