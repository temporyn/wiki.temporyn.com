package com.temporyn.wiki.controller;

import com.temporyn.wiki.service.ArticleService;
import com.temporyn.wiki.service.VaultService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;

@Controller
public class WikiController {

    private final VaultService vaultService;
    private final ArticleService articleService;

    public WikiController(VaultService vaultService, ArticleService articleService) {
        this.vaultService = vaultService;
        this.articleService = articleService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("tree", vaultService.buildRoot());
        return "index";
    }

    @GetMapping("/view/{*path}")
    public String view(@PathVariable String path, Model model) {
        model.addAttribute("tree", vaultService.buildRoot());
        model.addAttribute("article", articleService.getArticleView(normalize(path)));
        return "index";
    }

    @GetMapping("/edit/{*path}")
    public String edit(@PathVariable String path, Model model) {
        model.addAttribute("tree", vaultService.buildRoot());
        model.addAttribute("editing", articleService.getArticleEdit(normalize(path)));
        return "index";
    }

    @PostMapping("/edit/{*path}")
    public String save(@PathVariable String path, @RequestParam String content) {
        String relativePath = normalize(path);
        articleService.save(relativePath, content);
        return "redirect:/view/" + UriUtils.encodePath(relativePath, StandardCharsets.UTF_8);
    }

    private String normalize(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
