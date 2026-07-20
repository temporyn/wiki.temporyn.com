package com.temporyn.wiki.service;

import com.temporyn.wiki.dto.ArticleLink;
import com.temporyn.wiki.dto.DirectoryNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Builds the navigation tree shown in the sidebar by scanning the vault directory.
 * Folders sort before files, both alphabetically (Jekyll-style prefix ordering).
 */
@Service
public class VaultTreeService {

    private static final Logger log = LoggerFactory.getLogger(VaultTreeService.class);

    private final VaultPathResolver paths;

    public VaultTreeService(VaultPathResolver paths) {
        this.paths = paths;
    }

    /** Root node containing every folder and top-level article. Empty when the vault is missing. */
    public DirectoryNode buildTree() {
        if (!paths.vaultExists()) {
            log.warn("Content directory does not exist: {}", paths.root());
            return new DirectoryNode("", "", 0, List.of(), List.of());
        }
        return scan(paths.root(), "");
    }

    private DirectoryNode scan(Path dir, String relativePath) {
        List<DirectoryNode> children = new ArrayList<>();
        List<ArticleLink> articles = new ArrayList<>();

        for (Path entry : listEntries(dir)) {
            String name = entry.getFileName().toString();
            String childPath = relativePath.isEmpty() ? name : relativePath + "/" + name;
            if (Files.isDirectory(entry)) {
                children.add(scan(entry, childPath));
            } else if (name.endsWith(VaultPathResolver.MARKDOWN_SUFFIX)) {
                String title = name.substring(0, name.length() - VaultPathResolver.MARKDOWN_SUFFIX.length());
                String articlePath = relativePath.isEmpty() ? title : relativePath + "/" + title;
                articles.add(new ArticleLink(title, articlePath, paths.viewUrl(articlePath), true));
            } else {
                // Non-Markdown files are listed and can be downloaded, but not opened as documents.
                articles.add(new ArticleLink(name, childPath, paths.downloadUrl(childPath), false));
            }
        }

        int count = articles.size();
        for (DirectoryNode child : children) {
            count += child.articleCount();
        }
        String name = relativePath.isEmpty() ? "" : dir.getFileName().toString();
        return new DirectoryNode(name, relativePath, count, children, articles);
    }

    private List<Path> listEntries(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(path -> !path.getFileName().toString().startsWith("."))
                    .sorted(Comparator
                            .comparing((Path path) -> Files.isDirectory(path) ? 0 : 1)
                            .thenComparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException e) {
            log.warn("Cannot read directory: {}", dir, e);
            return List.of();
        }
    }
}
