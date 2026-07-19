package com.temporyn.wiki.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "attachment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "article_id")
    private Long articleId;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "stored_filename", nullable = false, length = 100)
    private String storedFilename;

    @Column(name = "content_type", length = 150)
    private String contentType;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "upload_token", length = 64)
    private String uploadToken;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Attachment create(String originalFilename, String storedFilename,
                                    String contentType, long fileSize, String uploadToken) {
        Attachment attachment = new Attachment();
        attachment.originalFilename = originalFilename;
        attachment.storedFilename = storedFilename;
        attachment.contentType = contentType;
        attachment.fileSize = fileSize;
        attachment.uploadToken = uploadToken;
        attachment.createdAt = LocalDateTime.now();
        return attachment;
    }

    public void assignTo(Long articleId) {
        this.articleId = articleId;
        this.uploadToken = null;
    }
}
