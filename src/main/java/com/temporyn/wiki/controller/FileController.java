package com.temporyn.wiki.controller;

import com.temporyn.wiki.service.VaultPathResolver;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Serves arbitrary vault files (non-Markdown) as downloads. */
@RestController
public class FileController {

    private final VaultPathResolver paths;

    public FileController(VaultPathResolver paths) {
        this.paths = paths;
    }

    @GetMapping("/download/{*path}")
    public ResponseEntity<Resource> download(@PathVariable String path) {
        String relativePath = path.startsWith("/") ? path.substring(1) : path;
        Path file = paths.resolveExistingFile(relativePath);

        MediaType contentType = MediaType.APPLICATION_OCTET_STREAM;
        try {
            String probed = Files.probeContentType(file);
            if (probed != null) {
                contentType = MediaType.parseMediaType(probed);
            }
        } catch (IOException ignored) {
            // Fall back to octet-stream when the type cannot be probed.
        }

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.getFileName().toString(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(file));
    }
}
