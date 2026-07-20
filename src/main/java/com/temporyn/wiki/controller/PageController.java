package com.temporyn.wiki.controller;

import com.temporyn.wiki.service.ArticleService;
import com.temporyn.wiki.service.VaultTreeService;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

/** Serves the server-rendered pages: the wiki shell, article view/edit, and login. */
@Controller
public class PageController {

    private final VaultTreeService treeService;
    private final ArticleService articleService;

    public PageController(VaultTreeService treeService, ArticleService articleService) {
        this.treeService = treeService;
        this.articleService = articleService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("tree", treeService.buildTree());
        return "index";
    }

    @GetMapping("/view/{*path}")
    public String view(@PathVariable String path, Model model) {
        model.addAttribute("tree", treeService.buildTree());
        model.addAttribute("article", articleService.load(normalize(path)));
        return "index";
    }

    @GetMapping("/edit/{*path}")
    public String edit(@PathVariable String path, Model model) {
        model.addAttribute("tree", treeService.buildTree());
        model.addAttribute("editing", articleService.load(normalize(path)));
        return "index";
    }

    @PostMapping("/edit/{*path}")
    public String save(@PathVariable String path, @RequestParam String content) {
        String relativePath = normalize(path);
        articleService.save(relativePath, content);
        return "redirect:/view/" + UriUtils.encodePath(relativePath, StandardCharsets.UTF_8);
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    /** Strip the leading slash that Spring keeps for {@code {*path}} captures. */
    private String normalize(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }
}
