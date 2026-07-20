package com.temporyn.wiki.service;

import com.temporyn.wiki.dto.SearchResult;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Case-insensitive full-text search across article titles and Markdown content. */
@Service
public class SearchService {

    private static final Logger log = LoggerFactory.getLogger(SearchService.class);

    private final VaultPathResolver paths;

    public SearchService(VaultPathResolver paths) {
        this.paths = paths;
    }

    /** Return up to {@code limit} documents whose title or body contains {@code query}. */
    public List<SearchResult> search(String query, int limit) {
        if (query == null || query.isBlank() || !paths.vaultExists()) {
            return List.of();
        }
        String needle = query.strip().toLowerCase();
        List<SearchResult> results = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(paths.root())) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(VaultPathResolver.MARKDOWN_SUFFIX))
                    .filter(path -> !isHidden(path))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
            for (Path file : files) {
                SearchResult hit = match(file, needle);
                if (hit != null) {
                    results.add(hit);
                    if (results.size() >= limit) {
                        break;
                    }
                }
            }
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Search failed: " + e.getMessage());
        }
        return results;
    }

    /** Skip dot-folders such as .assets so media and metadata never surface in results. */
    private boolean isHidden(Path file) {
        for (Path segment : paths.root().relativize(file)) {
            if (segment.toString().startsWith(".")) {
                return true;
            }
        }
        return false;
    }

    private SearchResult match(Path file, String needle) {
        String relative = paths.stripMarkdownSuffix(paths.toRelative(file));
        String title = relative.substring(relative.lastIndexOf('/') + 1);
        boolean titleHit = title.toLowerCase().contains(needle);

        String content;
        try {
            content = Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Cannot read document: {}", file, e);
            content = "";
        }
        int idx = content.toLowerCase().indexOf(needle);
        if (!titleHit && idx < 0) {
            return null;
        }
        String snippet = idx >= 0 ? buildSnippet(content, idx, needle.length()) : "";
        return new SearchResult(title, relative, paths.viewUrl(relative), snippet);
    }

    /** Build a short single-line excerpt around the first content match. */
    private String buildSnippet(String content, int index, int needleLength) {
        String flat = content.replaceAll("\\s+", " ").trim();
        int at = flat.toLowerCase().indexOf(content.substring(index, index + needleLength).toLowerCase());
        if (at < 0) {
            at = 0;
        }
        int start = Math.max(0, at - 30);
        int end = Math.min(flat.length(), at + needleLength + 40);
        String slice = flat.substring(start, end);
        return (start > 0 ? "…" : "") + slice + (end < flat.length() ? "…" : "");
    }
}
