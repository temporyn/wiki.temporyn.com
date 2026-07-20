package com.temporyn.wiki.controller;

import com.temporyn.wiki.service.VaultService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 사이드바에서 폴더/문서를 만들고, 이름을 바꾸고, 옮긴다. 삭제는 볼트에서 직접 처리한다. */
@RestController
@RequestMapping("/api")
public class VaultController {

    private final VaultService vaultService;

    public VaultController(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    @PostMapping("/directories")
    public Map<String, String> createDirectory(@RequestBody CreateRequest request) {
        return Map.of("path", vaultService.createDirectory(request.parentPath(), request.name()));
    }

    @PostMapping("/articles")
    public Map<String, String> createArticle(@RequestBody CreateRequest request) {
        return viewResponse(vaultService.createArticle(request.parentPath(), request.name()));
    }

    @PostMapping("/directories/rename")
    public Map<String, String> renameDirectory(@RequestBody RenameRequest request) {
        return Map.of("path", vaultService.renameDirectory(request.path(), request.name()));
    }

    @PostMapping("/articles/rename")
    public Map<String, String> renameArticle(@RequestBody RenameRequest request) {
        return viewResponse(vaultService.renameArticle(request.path(), request.name()));
    }

    @PostMapping("/directories/move")
    public Map<String, String> moveDirectory(@RequestBody MoveRequest request) {
        return Map.of("path", vaultService.moveDirectory(request.path(), request.targetPath()));
    }

    @PostMapping("/articles/move")
    public Map<String, String> moveArticle(@RequestBody MoveRequest request) {
        return viewResponse(vaultService.moveArticle(request.path(), request.targetPath()));
    }

    @PostMapping("/directories/delete")
    public Map<String, String> deleteDirectory(@RequestBody DeleteRequest request) {
        vaultService.deleteDirectory(request.path());
        return Map.of("path", request.path());
    }

    @PostMapping("/articles/delete")
    public Map<String, String> deleteArticle(@RequestBody DeleteRequest request) {
        vaultService.deleteArticle(request.path());
        return Map.of("path", request.path());
    }

    @PostMapping("/media/cleanup")
    public Map<String, Integer> cleanupMedia() {
        return Map.of("deleted", vaultService.cleanupOrphanMedia());
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
