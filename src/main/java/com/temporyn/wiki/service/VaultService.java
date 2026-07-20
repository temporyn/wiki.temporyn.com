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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

/**
 * 파일시스템 볼트 접근. 폴더 = 디렉토리, *.md = 문서이며 정렬은 파일명 순서(Jekyll 방식)를 따른다.
 * 콘텐츠는 Obsidian 등으로 외부에서 직접 관리하므로 이 서비스는 읽기 전용이다.
 */
@Service
public class VaultService {

    private static final Logger log = LoggerFactory.getLogger(VaultService.class);
    private static final String MARKDOWN_SUFFIX = ".md";
    private static final String MEDIA_DIR = ".assets";
    private static final Pattern MEDIA_REFERENCE = Pattern.compile("/media/([^)\\s\"'>]+)");

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

    /** 폴더 생성. 반환값은 볼트 상대 경로. */
    public String createDirectory(String parentPath, String name) {
        String folderName = validatedName(name);
        Path target = resolveNewChild(parentPath, folderName);
        try {
            Files.createDirectories(target);
        } catch (IOException e) {
            throw failed("폴더를 만들 수 없습니다: " + e.getMessage());
        }
        return relativeOf(target);
    }

    /** 문서 생성. 반환값은 확장자를 뺀 볼트 상대 경로. */
    public String createArticle(String parentPath, String name) {
        String title = validatedName(name);
        Path target = resolveNewChild(parentPath, title + MARKDOWN_SUFFIX);
        try {
            Files.writeString(target, "# " + title + System.lineSeparator(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw failed("문서를 만들 수 없습니다: " + e.getMessage());
        }
        String relative = relativeOf(target);
        return relative.substring(0, relative.length() - MARKDOWN_SUFFIX.length());
    }

    /** 문서 내용 저장(덮어쓰기). */
    public void writeArticle(String relativePath, String markdown) {
        Path file = resolveArticle(relativePath);
        try {
            Files.writeString(file, markdown == null ? "" : markdown, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw failed("문서를 저장할 수 없습니다: " + e.getMessage());
        }
    }

    /** Store an uploaded image under the hidden ".assets" folder. Returns the vault-relative path. */
    public String saveMedia(byte[] data, String extension) {
        ensureRoot();
        Path dir = root.resolve(MEDIA_DIR).normalize();
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw failed("이미지 폴더를 만들 수 없습니다: " + e.getMessage());
        }
        Path target = dir.resolve(UUID.randomUUID() + extension).normalize();
        if (!target.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 경로입니다.");
        }
        try {
            Files.write(target, data);
        } catch (IOException e) {
            throw failed("이미지를 저장할 수 없습니다: " + e.getMessage());
        }
        return relativeOf(target);
    }

    /** Resolve a vault-relative media path for serving. */
    public Path resolveMedia(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }
        Path file = root.resolve(relativePath).normalize();
        if (!file.startsWith(root) || !Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
        }
        return file;
    }

    /** Delete a document (*.md). */
    public void deleteArticle(String relativePath) {
        Path file = resolveArticle(relativePath);
        try {
            Files.delete(file);
        } catch (IOException e) {
            throw failed("문서를 삭제할 수 없습니다: " + e.getMessage());
        }
    }

    /** Delete a folder and all of its contents recursively. */
    public void deleteDirectory(String relativePath) {
        Path dir = resolveDirectory(relativePath);
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                try {
                    Files.delete(path);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        } catch (IOException | UncheckedIOException e) {
            throw failed("폴더를 삭제할 수 없습니다: " + e.getMessage());
        }
    }

    /** Remove media files under ".assets" that no document references. Returns the number of files deleted. */
    public int cleanupOrphanMedia() {
        Path mediaDir = root.resolve(MEDIA_DIR).normalize();
        if (!Files.isDirectory(mediaDir)) {
            return 0;
        }
        Set<String> referenced = collectReferencedMedia();
        List<Path> mediaFiles;
        try (Stream<Path> stream = Files.walk(mediaDir)) {
            mediaFiles = stream.filter(Files::isRegularFile).toList();
        } catch (IOException e) {
            throw failed("이미지 폴더를 읽을 수 없습니다: " + e.getMessage());
        }
        int deleted = 0;
        for (Path file : mediaFiles) {
            if (!referenced.contains(file.getFileName().toString())) {
                try {
                    Files.delete(file);
                    deleted++;
                } catch (IOException e) {
                    log.warn("고아 이미지를 삭제할 수 없습니다: {}", file, e);
                }
            }
        }
        return deleted;
    }

    /** Collect media file names referenced by any markdown document in the vault. */
    private Set<String> collectReferencedMedia() {
        Set<String> names = new HashSet<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(MARKDOWN_SUFFIX))
                    .forEach(path -> collectMediaNames(path, names));
        } catch (IOException e) {
            throw failed("문서를 읽을 수 없습니다: " + e.getMessage());
        }
        return names;
    }

    private void collectMediaNames(Path markdownFile, Set<String> names) {
        String content;
        try {
            content = Files.readString(markdownFile, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("문서를 읽을 수 없습니다: {}", markdownFile, e);
            return;
        }
        Matcher matcher = MEDIA_REFERENCE.matcher(content);
        while (matcher.find()) {
            String encoded = matcher.group(1);
            String decoded = UriUtils.decode(encoded, StandardCharsets.UTF_8);
            int slash = decoded.lastIndexOf('/');
            names.add(slash < 0 ? decoded : decoded.substring(slash + 1));
        }
    }

    /** 폴더 이름 변경. 반환값은 새 상대 경로. */
    public String renameDirectory(String relativePath, String newName) {
        Path source = resolveDirectory(relativePath);
        return relativeOf(renameTo(source, validatedName(newName)));
    }

    /** 문서 이름 변경. 반환값은 확장자를 뺀 새 상대 경로. */
    public String renameArticle(String relativePath, String newName) {
        Path source = resolveArticle(relativePath);
        Path renamed = renameTo(source, validatedName(newName) + MARKDOWN_SUFFIX);
        return stripSuffix(relativeOf(renamed));
    }

    /** 폴더 이동. targetParentPath 가 비어 있으면 최상위로 옮긴다. */
    public String moveDirectory(String relativePath, String targetParentPath) {
        Path source = resolveDirectory(relativePath);
        String normalizedTarget = targetParentPath == null ? "" : targetParentPath;
        if (normalizedTarget.equals(relativePath) || normalizedTarget.startsWith(relativePath + "/")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신이나 하위 폴더로 옮길 수 없습니다.");
        }
        return relativeOf(moveInto(source, normalizedTarget, source.getFileName().toString()));
    }

    /** 문서 이동. targetParentPath 가 비어 있으면 최상위로 옮긴다. */
    public String moveArticle(String relativePath, String targetParentPath) {
        Path source = resolveArticle(relativePath);
        Path moved = moveInto(source, targetParentPath == null ? "" : targetParentPath,
                source.getFileName().toString());
        return stripSuffix(relativeOf(moved));
    }

    private Path renameTo(Path source, String newFileName) {
        Path target = source.resolveSibling(newFileName).normalize();
        return relocate(source, target);
    }

    private Path moveInto(Path source, String targetParentPath, String fileName) {
        Path parent = targetParentPath.isBlank() ? root : root.resolve(targetParentPath).normalize();
        if (!parent.startsWith(root) || !Files.isDirectory(parent)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "옮길 폴더를 찾을 수 없습니다.");
        }
        return relocate(source, parent.resolve(fileName).normalize());
    }

    private Path relocate(Path source, Path target) {
        if (!target.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 경로입니다.");
        }
        if (target.equals(source)) {
            return source;
        }
        if (Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 같은 이름이 있습니다.");
        }
        try {
            return Files.move(source, target);
        } catch (IOException e) {
            throw failed("옮길 수 없습니다: " + e.getMessage());
        }
    }

    private Path resolveDirectory(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "최상위 폴더는 변경할 수 없습니다.");
        }
        Path dir = root.resolve(relativePath).normalize();
        if (!dir.startsWith(root) || !Files.isDirectory(dir)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "폴더를 찾을 수 없습니다.");
        }
        return dir;
    }

    private String stripSuffix(String relative) {
        return relative.substring(0, relative.length() - MARKDOWN_SUFFIX.length());
    }

    private Path resolveNewChild(String parentPath, String fileName) {
        ensureRoot();

        Path parent = (parentPath == null || parentPath.isBlank())
                ? root
                : root.resolve(parentPath).normalize();
        if (!parent.startsWith(root) || !Files.isDirectory(parent)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "상위 폴더를 찾을 수 없습니다.");
        }

        Path target = parent.resolve(fileName).normalize();
        if (!target.startsWith(root)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 경로입니다.");
        }
        if (Files.exists(target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 같은 이름이 있습니다.");
        }
        return target;
    }

    /** 이름을 검증하고 앞뒤 공백을 제거해 돌려준다. */
    private String validatedName(String name) {
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이름을 입력하세요.");
        }
        String trimmed = name.strip();
        if (trimmed.startsWith(".") || trimmed.contains("/") || trimmed.contains("\\") || trimmed.contains("..")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "사용할 수 없는 이름입니다.");
        }
        return trimmed;
    }

    private void ensureRoot() {
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw failed("콘텐츠 디렉토리를 만들 수 없습니다: " + root);
        }
    }

    private String relativeOf(Path path) {
        return root.relativize(path).toString().replace('\\', '/');
    }

    /** 공백·한글이 들어간 파일명도 안전하도록 경로를 인코딩한다. */
    private String viewUrl(String relativePath) {
        return "/view/" + UriUtils.encodePath(relativePath, StandardCharsets.UTF_8);
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

    private ResponseStatusException failed(String reason) {
        return new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, reason);
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
                articles.add(new ArticleLink(title, articlePath, viewUrl(articlePath)));
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
