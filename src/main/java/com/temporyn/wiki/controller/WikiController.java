package com.temporyn.wiki.controller;

import com.temporyn.wiki.service.ArticleService;
import com.temporyn.wiki.service.DirectoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class WikiController {

    private final DirectoryService directoryService;
    private final ArticleService articleService;

    public WikiController(DirectoryService directoryService, ArticleService articleService) {
        this.directoryService = directoryService;
        this.articleService = articleService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("tree", directoryService.buildTree());
        return "index";
    }

    @GetMapping("/view/{directoryId}/{articleId}")
    public String view(@PathVariable Long directoryId, @PathVariable Long articleId, Model model) {
        model.addAttribute("tree", directoryService.buildTree());
        model.addAttribute("article", articleService.getArticleView(directoryId, articleId));
        return "index";
    }
}
