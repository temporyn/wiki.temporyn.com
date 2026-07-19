package com.temporyn.wiki.controller;

import com.temporyn.wiki.service.VaultService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 사이드바에서 폴더/문서를 생성한다. 수정·삭제는 볼트에서 직접 처리한다. */
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
        String path = vaultService.createArticle(request.parentPath(), request.name());
        return Map.of("path", path, "url", "/view/" + path);
    }

    public record CreateRequest(String parentPath, String name) {
    }
}
