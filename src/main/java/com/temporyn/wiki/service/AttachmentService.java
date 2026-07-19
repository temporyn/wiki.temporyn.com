package com.temporyn.wiki.service;

import com.temporyn.wiki.domain.Attachment;
import com.temporyn.wiki.dto.AttachmentView;
import com.temporyn.wiki.repository.AttachmentRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final Path root;

    public AttachmentService(AttachmentRepository attachmentRepository,
                             @Value("${app.upload.dir}") String uploadDir) {
        this.attachmentRepository = attachmentRepository;
        this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct
    void init() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("업로드 디렉토리를 생성할 수 없습니다: " + root, e);
        }
    }

    @Transactional
    public AttachmentView store(MultipartFile file, String uploadToken) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "빈 파일입니다.");
        }
        String stored = UUID.randomUUID().toString().replace("-", "");
        try {
            file.transferTo(root.resolve(stored));
        } catch (IOException e) {
            throw new UncheckedIOException("파일 저장에 실패했습니다.", e);
        }
        String original = StringUtils.hasText(file.getOriginalFilename()) ? file.getOriginalFilename() : stored;
        Attachment attachment = attachmentRepository.save(
                Attachment.create(original, stored, file.getContentType(), file.getSize(), uploadToken));
        return toView(attachment);
    }

    @Transactional
    public void delete(Long id) {
        Attachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부파일을 찾을 수 없습니다."));
        removeFile(attachment);
        attachmentRepository.delete(attachment);
    }

    @Transactional
    public void assignToArticle(String uploadToken, Long articleId) {
        if (!StringUtils.hasText(uploadToken)) {
            return;
        }
        for (Attachment attachment : attachmentRepository.findByUploadToken(uploadToken)) {
            attachment.assignTo(articleId);
        }
    }

    @Transactional
    public void deleteForArticle(Long articleId) {
        for (Attachment attachment : attachmentRepository.findByArticleIdOrderById(articleId)) {
            removeFile(attachment);
            attachmentRepository.delete(attachment);
        }
    }

    @Transactional
    public int cleanupOrphans(LocalDateTime cutoff) {
        List<Attachment> orphans = attachmentRepository.findByArticleIdIsNullAndCreatedAtBefore(cutoff);
        for (Attachment attachment : orphans) {
            removeFile(attachment);
            attachmentRepository.delete(attachment);
        }
        return orphans.size();
    }

    public List<AttachmentView> listViews(Long articleId) {
        return attachmentRepository.findByArticleIdOrderById(articleId).stream().map(this::toView).toList();
    }

    public Attachment require(Long id) {
        return attachmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "첨부파일을 찾을 수 없습니다."));
    }

    public Path pathOf(Attachment attachment) {
        return root.resolve(attachment.getStoredFilename());
    }

    private void removeFile(Attachment attachment) {
        try {
            Files.deleteIfExists(pathOf(attachment));
        } catch (IOException e) {
            throw new UncheckedIOException("파일 삭제에 실패했습니다.", e);
        }
    }

    private AttachmentView toView(Attachment attachment) {
        return new AttachmentView(attachment.getId(), attachment.getOriginalFilename(),
                attachment.getFileSize(), humanSize(attachment.getFileSize()),
                "/attachments/" + attachment.getId());
    }

    static String humanSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        String[] units = {"KB", "MB", "GB", "TB"};
        double size = bytes;
        int index = -1;
        do {
            size /= 1024;
            index++;
        } while (size >= 1024 && index < units.length - 1);
        return String.format("%.1f %s", size, units[index]);
    }
}
