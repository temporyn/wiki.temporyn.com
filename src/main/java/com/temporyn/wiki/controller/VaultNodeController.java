package com.temporyn.wiki.controller;

import com.temporyn.wiki.service.VaultNodeService;
import java.io.IOException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** REST API for structural vault operations triggered from the sidebar. */
@RestController
@RequestMapping("/api")
public class VaultNodeController {

    private static final long MAX_UPLOAD_BYTES = 50L * 1024 * 1024;

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

    @PostMapping("/files")
    public Map<String, String> uploadFile(
            @RequestParam(value = "parentPath", required = false) String parentPath,
            @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No file provided.");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new ResponseStatusException(HttpStatus.CONTENT_TOO_LARGE, "File must be 50MB or smaller.");
        }
        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Cannot read the uploaded file.");
        }
        String name = file.getOriginalFilename();
        return Map.of("path", nodeService.uploadFile(parentPath, name, data));
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
