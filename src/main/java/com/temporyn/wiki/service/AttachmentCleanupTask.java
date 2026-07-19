package com.temporyn.wiki.service;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AttachmentCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(AttachmentCleanupTask.class);

    private final AttachmentService attachmentService;
    private final long orphanRetentionHours;

    public AttachmentCleanupTask(AttachmentService attachmentService,
                                 @Value("${app.upload.orphan-retention-hours:24}") long orphanRetentionHours) {
        this.attachmentService = attachmentService;
        this.orphanRetentionHours = orphanRetentionHours;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void cleanupOrphans() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(orphanRetentionHours);
        int removed = attachmentService.cleanupOrphans(cutoff);
        if (removed > 0) {
            log.info("정리된 미저장 첨부파일: {}건", removed);
        }
    }
}
