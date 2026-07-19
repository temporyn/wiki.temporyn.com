package com.temporyn.wiki.controller;

import com.temporyn.wiki.dto.ArticleForm;
import com.temporyn.wiki.service.ArticleService;
import com.temporyn.wiki.service.AttachmentService;
import com.temporyn.wiki.service.DirectoryService;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class WikiController {

    private final DirectoryService directoryService;
    private final ArticleService articleService;
    private final AttachmentService attachmentService;

    public WikiController(DirectoryService directoryService, ArticleService articleService,
                          AttachmentService attachmentService) {
        this.directoryService = directoryService;
        this.articleService = articleService;
        this.attachmentService = attachmentService;
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

    @GetMapping("/articles/new")
    public String newArticle(@RequestParam(required = false) Long directoryId, Model model) {
        ArticleForm form = new ArticleForm();
        form.setDirectoryId(directoryId);
        form.setUploadToken(UUID.randomUUID().toString());
        model.addAttribute("tree", directoryService.buildTree());
        model.addAttribute("articleForm", form);
        model.addAttribute("directories", directoryService.listRows());
        model.addAttribute("formMode", "create");
        model.addAttribute("attachments", List.of());
        return "index";
    }

    @PostMapping("/articles")
    public String create(@ModelAttribute ArticleForm articleForm) {
        Long id = articleService.create(articleForm);
        return "redirect:/view/" + articleForm.getDirectoryId() + "/" + id;
    }

    @GetMapping("/articles/{id}/edit")
    public String editArticle(@PathVariable Long id, Model model) {
        ArticleForm form = articleService.getForm(id);
        form.setUploadToken(UUID.randomUUID().toString());
        model.addAttribute("tree", directoryService.buildTree());
        model.addAttribute("articleForm", form);
        model.addAttribute("directories", directoryService.listRows());
        model.addAttribute("formMode", "edit");
        model.addAttribute("articleId", id);
        model.addAttribute("attachments", attachmentService.listViews(id));
        return "index";
    }

    @PostMapping("/articles/{id}")
    public String update(@PathVariable Long id, @ModelAttribute ArticleForm articleForm) {
        articleService.update(id, articleForm);
        return "redirect:/view/" + articleForm.getDirectoryId() + "/" + id;
    }
}
