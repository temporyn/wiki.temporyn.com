package com.temporyn.wiki.service;

import com.temporyn.wiki.domain.Article;
import com.temporyn.wiki.domain.Directory;
import com.temporyn.wiki.dto.ArticleLink;
import com.temporyn.wiki.dto.DirectoryNode;
import com.temporyn.wiki.repository.ArticleRepository;
import com.temporyn.wiki.repository.DirectoryRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

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

    public String breadcrumb(Long directoryId) {
        Map<Long, Directory> directoriesById = directoryRepository.findAll().stream()
                .collect(Collectors.toMap(Directory::getId, Function.identity()));

        List<String> segments = new ArrayList<>();
        Directory current = directoriesById.get(directoryId);
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
