package com.temporyn.wiki.controller;

import com.temporyn.wiki.dto.DirectoryForm;
import com.temporyn.wiki.service.ArticleService;
import com.temporyn.wiki.service.DirectoryService;
import java.util.Map;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TreeController {

    private final DirectoryService directoryService;
    private final ArticleService articleService;

    public TreeController(DirectoryService directoryService, ArticleService articleService) {
        this.directoryService = directoryService;
        this.articleService = articleService;
    }

    @PostMapping("/directories")
    public Map<String, Object> createDirectory(@RequestBody DirectoryForm form) {
        Long id = directoryService.create(form);
        return Map.of("id", id);
    }

    @PostMapping("/directories/{id}/rename")
    public void renameDirectory(@PathVariable Long id, @RequestBody NameRequest request) {
        directoryService.rename(id, request.name());
    }

    @PostMapping("/directories/{id}/move")
    public void moveDirectory(@PathVariable Long id, @RequestBody MoveRequest request) {
        directoryService.move(id, request.parentId());
    }

    @PostMapping("/directories/{id}/delete")
    public void deleteDirectory(@PathVariable Long id) {
        directoryService.delete(id);
    }

    @PostMapping("/articles/{id}/rename")
    public void renameArticle(@PathVariable Long id, @RequestBody NameRequest request) {
        articleService.rename(id, request.name());
    }

    @PostMapping("/articles/{id}/move")
    public void moveArticle(@PathVariable Long id, @RequestBody MoveRequest request) {
        articleService.move(id, request.parentId());
    }

    @PostMapping("/articles/{id}/delete")
    public void deleteArticle(@PathVariable Long id) {
        articleService.delete(id);
    }

    public record NameRequest(String name) {
    }

    public record MoveRequest(Long parentId) {
    }
}
