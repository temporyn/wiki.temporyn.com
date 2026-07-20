package com.temporyn.wiki.controller;

import com.temporyn.wiki.service.VaultNodeService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** REST API for structural vault operations triggered from the sidebar. */
@RestController
@RequestMapping("/api")
public class VaultNodeController {

    private final VaultNodeService nodeService;

    public VaultNodeController(VaultNodeService nodeService) {
        this.nodeService = nodeService;
    }

    @PostMapping("/directories")
    public Map<String, String> createDirectory(@RequestBody CreateRequest request) {
        return Map.of("path", nodeService.createDirectory(request.parentPath(), request.name()));
    }

    @PostMapping("/articles")
    public Map<String, String> createArticle(@RequestBody CreateRequest request) {
        return viewResponse(nodeService.createArticle(request.parentPath(), request.name()));
    }

    @PostMapping("/directories/rename")
    public Map<String, String> renameDirectory(@RequestBody RenameRequest request) {
        return Map.of("path", nodeService.renameDirectory(request.path(), request.name()));
    }

    @PostMapping("/articles/rename")
    public Map<String, String> renameArticle(@RequestBody RenameRequest request) {
        return viewResponse(nodeService.renameArticle(request.path(), request.name()));
    }

    @PostMapping("/directories/move")
    public Map<String, String> moveDirectory(@RequestBody MoveRequest request) {
        return Map.of("path", nodeService.moveDirectory(request.path(), request.targetPath()));
    }

    @PostMapping("/articles/move")
    public Map<String, String> moveArticle(@RequestBody MoveRequest request) {
        return viewResponse(nodeService.moveArticle(request.path(), request.targetPath()));
    }

    @PostMapping("/directories/delete")
    public Map<String, String> deleteDirectory(@RequestBody DeleteRequest request) {
        nodeService.deleteDirectory(request.path());
        return Map.of("path", request.path());
    }

    @PostMapping("/articles/delete")
    public Map<String, String> deleteArticle(@RequestBody DeleteRequest request) {
        nodeService.deleteArticle(request.path());
        return Map.of("path", request.path());
    }

    private Map<String, String> viewResponse(String path) {
        return Map.of("path", path, "url", "/view/" + path);
    }

    public record CreateRequest(String parentPath, String name) {
    }

    public record RenameRequest(String path, String name) {
    }

    public record MoveRequest(String path, String targetPath) {
    }

    public record DeleteRequest(String path) {
    }
}
