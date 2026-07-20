package com.temporyn.wiki.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

/**
 * Single source of truth for filesystem path resolution and security inside the vault.
 *
 * <p>The vault is a plain directory tree where folders are directories and {@code *.md}
 * files are articles. Every path that comes from a request is resolved through this class
 * so that path traversal outside the vault root is impossible.
 */
@Component
public class VaultPathResolver {

    public static final String MARKDOWN_SUFFIX = ".md";

    private final Path root;

    public VaultPathResolver(@Value("${app.content.dir}") String contentDir) {
        this.root = Paths.get(contentDir).toAbsolutePath().normalize();
    }

    public Path root() {
        return root;
    }

    public boolean vaultExists() {
        return Files.isDirectory(root);
    }

    /** Create the vault root directory if it does not exist yet. */
    public void ensureRoot() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw serverError("Cannot create content directory: " + root);
        }
    }

    /** Resolve an existing article file ({@code relativePath} without the .md suffix). */
    public Path resolveArticleFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw articleNotFound();
        }
        Path file = root.resolve(relativePath + MARKDOWN_SUFFIX).normalize();
        if (!isWithinRoot(file) || !Files.isRegularFile(file)) {
            throw articleNotFound();
        }
        return file;
    }

    /** Resolve an existing directory. The vault root itself is not a valid target. */
    public Path resolveDirectory(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "The vault root cannot be modified.");
        }
        Path dir = root.resolve(relativePath).normalize();
        if (!isWithinRoot(dir) || !Files.isDirectory(dir)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Folder not found.");
        }
        return dir;
    }

    /** Resolve a not-yet-existing child inside a parent folder, guarding against name clashes. */
    public Path resolveNewChild(String parentPath, String fileName) {
        ensureRoot();
        Path parent = (parentPath == null || parentPath.isBlank())
                ? root
                : root.resolve(parentPath).normalize();
        if (!isWithinRoot(parent) || !Files.isDirectory(parent)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Parent folder not found.");
        }
        Path target = parent.resolve(fileName).normalize();
        if (!isWithinRoot(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid path.");
        }
        if (Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An item with the same name already exists.");
        }
        return target;
    }

    /** Resolve an existing regular file inside the vault (used to serve media). */
    public Path resolveExistingFile(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found.");
        }
        Path file = root.resolve(relativePath).normalize();
        if (!isWithinRoot(file) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found.");
        }
        return file;
    }

    public boolean isWithinRoot(Path path) {
        return path.normalize().startsWith(root);
    }

    public String toRelative(Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    public String stripMarkdownSuffix(String relativePath) {
        return relativePath.substring(0, relativePath.length() - MARKDOWN_SUFFIX.length());
    }

    /** Build the view URL for an article, encoding spaces and non-ASCII names safely. */
    public String viewUrl(String relativePath) {
        return "/view/" + UriUtils.encodePath(relativePath, StandardCharsets.UTF_8);
    }

    /** Build the download URL for an arbitrary vault file. */
    public String downloadUrl(String relativePath) {
        return "/download/" + UriUtils.encodePath(relativePath, StandardCharsets.UTF_8);
    }

    /** Validate a user-supplied file or folder name and return it trimmed. */
    public String validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please enter a name.");
        }
        String trimmed = name.strip();
        if (trimmed.startsWith(".") || trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "That name is not allowed.");
        }
        return trimmed;
    }

    private ResponseStatusException articleNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found.");
    }

    private ResponseStatusException serverError(String reason) {
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, reason);
    }
}
