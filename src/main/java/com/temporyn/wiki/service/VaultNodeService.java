package com.temporyn.wiki.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Structural mutations of the vault: create, rename, move and delete both
 * directories and articles. All paths are validated through {@link VaultPathResolver}.
 */
@Service
public class VaultNodeService {

    private final VaultPathResolver paths;

    public VaultNodeService(VaultPathResolver paths) {
        this.paths = paths;
    }

    /** Create an empty folder. Returns its vault-relative path. */
    public String createDirectory(String parentPath, String name) {
        Path target = paths.resolveNewChild(parentPath, paths.validateName(name));
        try {
            Files.createDirectories(target);
        } catch (IOException e) {
            throw serverError("Cannot create folder: " + e.getMessage());
        }
        return paths.toRelative(target);
    }

    /** Create a document seeded with an H1 title. Returns the path without the .md suffix. */
    public String createArticle(String parentPath, String name) {
        String title = paths.validateName(name);
        Path target = paths.resolveNewChild(parentPath, title + VaultPathResolver.MARKDOWN_SUFFIX);
        try {
            Files.writeString(target, "# " + title + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw serverError("Cannot create document: " + e.getMessage());
        }
        return paths.stripMarkdownSuffix(paths.toRelative(target));
    }

    public String renameDirectory(String relativePath, String newName) {
        Path source = paths.resolveDirectory(relativePath);
        return paths.toRelative(renameTo(source, paths.validateName(newName)));
    }

    public String renameArticle(String relativePath, String newName) {
        Path source = paths.resolveArticleFile(relativePath);
        Path renamed = renameTo(source, paths.validateName(newName) + VaultPathResolver.MARKDOWN_SUFFIX);
        return paths.stripMarkdownSuffix(paths.toRelative(renamed));
    }

    /** Move a folder. A blank target moves it to the vault root. */
    public String moveDirectory(String relativePath, String targetParentPath) {
        Path source = paths.resolveDirectory(relativePath);
        String target = targetParentPath == null ? "" : targetParentPath;
        if (target.equals(relativePath) || target.startsWith(relativePath + "/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot move a folder into itself or its own subfolder.");
        }
        return paths.toRelative(moveInto(source, target, source.getFileName().toString()));
    }

    /** Move a document. A blank target moves it to the vault root. */
    public String moveArticle(String relativePath, String targetParentPath) {
        Path source = paths.resolveArticleFile(relativePath);
        Path moved = moveInto(source, targetParentPath == null ? "" : targetParentPath,
                source.getFileName().toString());
        return paths.stripMarkdownSuffix(paths.toRelative(moved));
    }

    public void deleteArticle(String relativePath) {
        Path file = paths.resolveArticleFile(relativePath);
        try {
            Files.delete(file);
        } catch (IOException e) {
            throw serverError("Cannot delete document: " + e.getMessage());
        }
    }

    public void deleteDirectory(String relativePath) {
        Path dir = paths.resolveDirectory(relativePath);
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException | UncheckedIOException e) {
            throw serverError("Cannot delete folder: " + e.getMessage());
        }
    }

    private Path renameTo(Path source, String newFileName) {
        return relocate(source, source.resolveSibling(newFileName).normalize());
    }

    private Path moveInto(Path source, String targetParentPath, String fileName) {
        Path parent = targetParentPath.isBlank() ? paths.root() : paths.root().resolve(targetParentPath).normalize();
        if (!paths.isWithinRoot(parent) || !Files.isDirectory(parent)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Destination folder not found.");
        }
        return relocate(source, parent.resolve(fileName).normalize());
    }

    private Path relocate(Path source, Path target) {
        if (!paths.isWithinRoot(target)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid path.");
        }
        if (target.equals(source)) {
            return source;
        }
        if (Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "An item with the same name already exists.");
        }
        try {
            return Files.move(source, target);
        } catch (IOException e) {
            throw serverError("Cannot move: " + e.getMessage());
        }
    }

    private ResponseStatusException serverError(String reason) {
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, reason);
    }
}
