package com.temporyn.wiki.controller;

import com.temporyn.wiki.service.ArticleService;
import com.temporyn.wiki.service.VaultService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

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
        model.addAttribute("article", articleService.getArticleView(trimLeadingSlash(path)));
        return "index";
    }

    private String trimLeadingSlash(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
