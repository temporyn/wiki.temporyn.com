package com.temporyn.wiki.controller;

import com.temporyn.wiki.service.VaultService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

/** Handles local image uploads into the vault and serves them back for the editor and view. */
@RestController
public class MediaController {

    private static final Map<String, String> ALLOWED = Map.of(
            "image/png", ".png",
            "image/jpeg", ".jpg",
            "image/gif", ".gif",
            "image/webp", ".webp");
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".gif", ".webp");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final VaultService vaultService;

    public MediaController(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @PostMapping("/api/images")
    public Map<String, String> upload(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "파일이 없습니다.");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "이미지는 10MB 이하만 가능합니다.");
        }
        String extension = ALLOWED.get(file.getContentType());
        if (extension == null) {
            throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 이미지 형식입니다.");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "이미지를 읽을 수 없습니다.");
        }
        String relativePath = vaultService.saveMedia(data, extension);
        return Map.of("url", "/media/" + UriUtils.encodePath(relativePath, StandardCharsets.UTF_8));
    }

    @GetMapping("/media/{*path}")
    public ResponseEntity<Resource> serve(@PathVariable String path) {
        String relativePath = path.startsWith("/") ? path.substring(1) : path;
        if (!hasAllowedExtension(relativePath)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }
        Path file = vaultService.resolveMedia(relativePath);
        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            String probed = Files.probeContentType(file);
            if (probed != null) {
                contentType = MediaType.parseMediaType(probed);
            }
        } catch (IOException ignored) {
            // Fall back to octet-stream when the type cannot be probed.
        }
        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CACHE_CONTROL, "max-age=31536000, immutable")
                .body(new FileSystemResource(file));
    }

    private boolean hasAllowedExtension(String relativePath) {
        int dot = relativePath.lastIndexOf('.');
        if (dot < 0) {
            return false;
        }
        return ALLOWED_EXTENSIONS.contains(relativePath.substring(dot).toLowerCase());
    }
}
