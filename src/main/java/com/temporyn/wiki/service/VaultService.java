package com.temporyn.wiki.service;

import com.temporyn.wiki.dto.ArticleLink;
import com.temporyn.wiki.dto.DirectoryNode;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * 파일시스템 볼트 접근. 폴더 = 디렉토리, *.md = 문서이며 정렬은 파일명 순서(Jekyll 방식)를 따른다.
 * 콘텐츠는 Obsidian 등으로 외부에서 직접 관리하므로 이 서비스는 읽기 전용이다.
 */
@Service
public class VaultService {

    private static final Logger log = LoggerFactory.getLogger(VaultService.class);
    private static final String MARKDOWN_SUFFIX = ".md";

    private final Path root;

    public VaultService(@Value("${app.content.dir}") String contentDir) {
        this.root = Paths.get(contentDir).toAbsolutePath().normalize();
    }

    /** 볼트 루트를 나타내는 노드. 하위 폴더와 최상위 문서를 모두 담는다. */
    public DirectoryNode buildRoot() {
        if (!Files.isDirectory(root)) {
            log.warn("콘텐츠 디렉토리가 없습니다: {}", root);
            return new DirectoryNode("", "", 0, List.of(), List.of());
        }
        return scan(root, "");
    }

    public String readArticle(String relativePath) {
        Path file = resolveArticle(relativePath);
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("문서를 읽을 수 없습니다: " + relativePath, e);
        }
    }

    private Path resolveArticle(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw notFound();
        }
        Path file = root.resolve(relativePath + MARKDOWN_SUFFIX).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw notFound();
        }
        return file;
    }

    private ResponseStatusException notFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다.");
    }

    private DirectoryNode scan(Path dir, String relativePath) {
        List<Path> entries = listEntries(dir);

        List<DirectoryNode> children = new ArrayList<>();
        List<ArticleLink> articles = new ArrayList<>();
        for (Path entry : entries) {
            String name = entry.getFileName().toString();
            String childPath = relativePath.isEmpty() ? name : relativePath + "/" + name;
            if (Files.isDirectory(entry)) {
                children.add(scan(entry, childPath));
            } else if (name.endsWith(MARKDOWN_SUFFIX)) {
                String title = name.substring(0, name.length() - MARKDOWN_SUFFIX.length());
                String articlePath = relativePath.isEmpty() ? title : relativePath + "/" + title;
                articles.add(new ArticleLink(title, articlePath, "/view/" + articlePath));
            }
        }

        int count = articles.size();
        for (DirectoryNode child : children) {
            count += child.articleCount();
        }
        String name = relativePath.isEmpty() ? "" : dir.getFileName().toString();
        return new DirectoryNode(name, relativePath, count, children, articles);
    }

    /** 폴더 먼저, 그다음 문서. 각각 파일명 오름차순(Jekyll 스타일 접두어 정렬). */
    private List<Path> listEntries(Path dir) {
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(path -> !path.getFileName().toString().startsWith("."))
                    .filter(path -> Files.isDirectory(path) || path.getFileName().toString().endsWith(MARKDOWN_SUFFIX))
                    .sorted(Comparator
                            .comparing((Path path) -> Files.isDirectory(path) ? 0 : 1)
                            .thenComparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .toList();
        } catch (IOException e) {
            log.warn("디렉토리를 읽을 수 없습니다: {}", dir, e);
            return List.of();
        }
    }
}
