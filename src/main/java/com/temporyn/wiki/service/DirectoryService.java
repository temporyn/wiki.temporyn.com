package com.temporyn.wiki.service;

import com.temporyn.wiki.domain.Article;
import com.temporyn.wiki.domain.Directory;
import com.temporyn.wiki.dto.ArticleLink;
import com.temporyn.wiki.dto.DirectoryForm;
import com.temporyn.wiki.dto.DirectoryNode;
import com.temporyn.wiki.dto.DirectoryRow;
import com.temporyn.wiki.repository.ArticleRepository;
import com.temporyn.wiki.repository.DirectoryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DirectoryService {

    private static final Sort BY_ORDER = Sort.by(Sort.Direction.ASC, "sortOrder", "id");

    private final DirectoryRepository directoryRepository;
    private final ArticleRepository articleRepository;

    public DirectoryService(DirectoryRepository directoryRepository, ArticleRepository articleRepository) {
        this.directoryRepository = directoryRepository;
        this.articleRepository = articleRepository;
    }

    public List<DirectoryNode> buildTree() {
        Map<Long, List<Directory>> childrenByParent = new LinkedHashMap<>();
        for (Directory directory : directoryRepository.findAll(BY_ORDER)) {
            childrenByParent.computeIfAbsent(directory.getParentId(), key -> new ArrayList<>()).add(directory);
        }

        Map<Long, List<Article>> articlesByDirectory = new LinkedHashMap<>();
        for (Article article : articleRepository.findAll(BY_ORDER)) {
            articlesByDirectory.computeIfAbsent(article.getDirectoryId(), key -> new ArrayList<>()).add(article);
        }

        List<DirectoryNode> roots = new ArrayList<>();
        for (Directory root : childrenByParent.getOrDefault(null, List.of())) {
            roots.add(toNode(root, childrenByParent, articlesByDirectory));
        }
        return roots;
    }

    public List<DirectoryRow> listRows() {
        List<Directory> directories = directoryRepository.findAll(BY_ORDER);
        Map<Long, Directory> directoriesById = directories.stream()
                .collect(Collectors.toMap(Directory::getId, Function.identity()));
        Map<Long, Long> articleCounts = articleRepository.findAll().stream()
                .collect(Collectors.groupingBy(Article::getDirectoryId, Collectors.counting()));

        List<DirectoryRow> rows = new ArrayList<>();
        for (Directory directory : directories) {
            int count = articleCounts.getOrDefault(directory.getId(), 0L).intValue();
            rows.add(new DirectoryRow(directory.getId(), directory.getName(), pathOf(directory, directoriesById), count));
        }
        return rows;
    }

    @Transactional
    public Long create(DirectoryForm form) {
        List<Directory> siblings = form.getParentId() == null
                ? directoryRepository.findByParentIdIsNull()
                : directoryRepository.findByParentId(form.getParentId());
        Directory directory = Directory.create(form.getName(), form.getParentId(), siblings.size() + 1);
        return directoryRepository.save(directory).getId();
    }

    public DirectoryForm getForm(Long directoryId) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "디렉토리를 찾을 수 없습니다."));
        DirectoryForm form = new DirectoryForm();
        form.setName(directory.getName());
        form.setParentId(directory.getParentId());
        return form;
    }

    @Transactional
    public void update(Long directoryId, DirectoryForm form) {
        Directory directory = directoryRepository.findById(directoryId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "디렉토리를 찾을 수 없습니다."));

        Long parentId = form.getParentId();
        if (parentId != null && descendantIds(directoryId).contains(parentId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자기 자신이나 하위 디렉토리로 이동할 수 없습니다.");
        }
        directory.update(form.getName(), parentId);
    }

    private java.util.Set<Long> descendantIds(Long directoryId) {
        Map<Long, List<Directory>> childrenByParent = new LinkedHashMap<>();
        for (Directory directory : directoryRepository.findAll()) {
            childrenByParent.computeIfAbsent(directory.getParentId(), key -> new ArrayList<>()).add(directory);
        }
        java.util.Set<Long> ids = new java.util.LinkedHashSet<>();
        java.util.Deque<Long> queue = new java.util.ArrayDeque<>();
        queue.add(directoryId);
        while (!queue.isEmpty()) {
            Long current = queue.poll();
            ids.add(current);
            for (Directory child : childrenByParent.getOrDefault(current, List.of())) {
                queue.add(child.getId());
            }
        }
        return ids;
    }

    @Transactional
    public void delete(Long directoryId) {
        if (!directoryRepository.existsById(directoryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "디렉토리를 찾을 수 없습니다.");
        }
        if (!directoryRepository.findByParentId(directoryId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "하위 디렉토리가 있어 삭제할 수 없습니다.");
        }
        if (!articleRepository.findByDirectoryId(directoryId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "문서가 있어 삭제할 수 없습니다.");
        }
        directoryRepository.deleteById(directoryId);
    }

    public String breadcrumb(Long directoryId) {
        Map<Long, Directory> directoriesById = directoriesById();
        Directory directory = directoriesById.get(directoryId);
        return directory == null ? "" : pathOf(directory, directoriesById);
    }

    public Map<Long, String> breadcrumbMap() {
        Map<Long, Directory> directoriesById = directoriesById();
        Map<Long, String> paths = new LinkedHashMap<>();
        for (Directory directory : directoriesById.values()) {
            paths.put(directory.getId(), pathOf(directory, directoriesById));
        }
        return paths;
    }

    private Map<Long, Directory> directoriesById() {
        return directoryRepository.findAll().stream()
                .collect(Collectors.toMap(Directory::getId, Function.identity()));
    }

    private String pathOf(Directory directory, Map<Long, Directory> directoriesById) {
        List<String> segments = new ArrayList<>();
        Directory current = directory;
        while (current != null) {
            segments.add(0, current.getName());
            current = current.getParentId() == null ? null : directoriesById.get(current.getParentId());
        }
        return String.join(" / ", segments);
    }

    private DirectoryNode toNode(Directory directory,
                                 Map<Long, List<Directory>> childrenByParent,
                                 Map<Long, List<Article>> articlesByDirectory) {
        List<DirectoryNode> children = new ArrayList<>();
        for (Directory child : childrenByParent.getOrDefault(directory.getId(), List.of())) {
            children.add(toNode(child, childrenByParent, articlesByDirectory));
        }

        List<ArticleLink> links = new ArrayList<>();
        for (Article article : articlesByDirectory.getOrDefault(directory.getId(), List.of())) {
            links.add(new ArticleLink(article.getId(), article.getTitle(), viewUrl(directory.getId(), article.getId())));
        }

        int count = links.size();
        for (DirectoryNode child : children) {
            count += child.articleCount();
        }
        return new DirectoryNode(directory.getId(), directory.getName(), count, children, links);
    }

    private String viewUrl(Long directoryId, Long articleId) {
        return "/view/" + directoryId + "/" + articleId;
    }
}
