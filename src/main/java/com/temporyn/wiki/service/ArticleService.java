package com.temporyn.wiki.service;

import com.temporyn.wiki.dto.ArticleContent;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Reads and writes the Markdown content of a single article. */
@Service
public class ArticleService {

    private final VaultPathResolver paths;

    public ArticleService(VaultPathResolver paths) {
        this.paths = paths;
    }

    public ArticleContent load(String relativePath) {
        Path file = paths.resolveArticleFile(relativePath);
        String markdown;
        try {
            markdown = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read document: " + relativePath, e);
        }
        return new ArticleContent(relativePath, titleOf(relativePath), breadcrumbOf(relativePath), markdown);
    }

    public void save(String relativePath, String markdown) {
        Path file = paths.resolveArticleFile(relativePath);
        try {
            Files.writeString(file, markdown == null ? "" : markdown, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Cannot save document: " + e.getMessage());
        }
    }

    private String titleOf(String relativePath) {
        int slash = relativePath.lastIndexOf('/');
        return slash < 0 ? relativePath : relativePath.substring(slash + 1);
    }

    private String breadcrumbOf(String relativePath) {
        int slash = relativePath.lastIndexOf('/');
        return slash < 0 ? "" : relativePath.substring(0, slash).replace("/", " / ");
    }
}
