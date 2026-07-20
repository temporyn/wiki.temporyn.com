package com.temporyn.wiki.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

/**
 * Stores uploaded images inside the vault, serves them back, and removes
 * orphans that no document references. Images live under the hidden
 * {@value #MEDIA_DIR} folder so they travel with the vault but stay out of the tree.
 */
@Service
public class MediaService {

    private static final Logger log = LoggerFactory.getLogger(MediaService.class);
    private static final String MEDIA_DIR = ".assets";
    private static final Pattern MEDIA_REFERENCE = Pattern.compile("/media/([^)\\s\"'>]+)");

    private final VaultPathResolver paths;

    public MediaService(VaultPathResolver paths) {
        this.paths = paths;
    }

    /** Persist image bytes and return the vault-relative path of the stored file. */
    public String store(byte[] data, String extension) {
        paths.ensureRoot();
        Path dir = paths.root().resolve(MEDIA_DIR).normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw serverError("Cannot create image folder: " + e.getMessage());
        }
        Path target = dir.resolve(UUID.randomUUID() + extension).normalize();
        if (!paths.isWithinRoot(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid path.");
        }
        try {
            Files.write(target, data);
        } catch (IOException e) {
            throw serverError("Cannot save image: " + e.getMessage());
        }
        return paths.toRelative(target);
    }

    public Path resolveFile(String relativePath) {
        return paths.resolveExistingFile(relativePath);
    }

    /** Delete media files that no document references. Returns the number removed. */
    public int cleanupOrphans() {
        Path mediaDir = paths.root().resolve(MEDIA_DIR).normalize();
        if (!Files.isDirectory(mediaDir)) {
            return 0;
        }
        Set<String> referenced = collectReferencedNames();
        List<Path> mediaFiles;
        try (Stream<Path> stream = Files.walk(mediaDir)) {
            mediaFiles = stream.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            throw serverError("Cannot read image folder: " + e.getMessage());
        }
        int deleted = 0;
        for (Path file : mediaFiles) {
            if (!referenced.contains(file.getFileName().toString())) {
                try {
                    Files.delete(file);
                    deleted++;
                } catch (IOException e) {
                    log.warn("Cannot delete orphan image: {}", file, e);
                }
            }
        }
        return deleted;
    }

    private Set<String> collectReferencedNames() {
        Set<String> names = new HashSet<>();
        try (Stream<Path> stream = Files.walk(paths.root())) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(VaultPathResolver.MARKDOWN_SUFFIX))
                    .forEach(path -> collectFrom(path, names));
        } catch (IOException e) {
            throw serverError("Cannot read documents: " + e.getMessage());
        }
        return names;
    }

    private void collectFrom(Path markdownFile, Set<String> names) {
        String content;
        try {
            content = Files.readString(markdownFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Cannot read document: {}", markdownFile, e);
            return;
        }
        Matcher matcher = MEDIA_REFERENCE.matcher(content);
        while (matcher.find()) {
            String decoded = UriUtils.decode(matcher.group(1), StandardCharsets.UTF_8);
            int slash = decoded.lastIndexOf('/');
            names.add(slash < 0 ? decoded : decoded.substring(slash + 1));
        }
    }

    private ResponseStatusException serverError(String reason) {
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, reason);
    }
}
