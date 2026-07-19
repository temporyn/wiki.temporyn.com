package com.temporyn.wiki.controller;

import com.temporyn.wiki.dto.ArticleForm;
import com.temporyn.wiki.dto.DirectoryForm;
import com.temporyn.wiki.service.ArticleService;
import com.temporyn.wiki.service.DirectoryService;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ArticleService articleService;
    private final DirectoryService directoryService;

    public AdminController(ArticleService articleService, DirectoryService directoryService) {
        this.articleService = articleService;
        this.directoryService = directoryService;
    }

    @GetMapping
    public String dashboard(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        model.addAttribute("articleCount", articleService.listRows().size());
        model.addAttribute("directoryCount", directoryService.listRows().size());
        return "admin/index";
    }

    @GetMapping("/articles")
    public String articles(Model model) {
        model.addAttribute("articles", articleService.listRows());
        return "admin/articles";
    }

    @GetMapping("/articles/new")
    public String newArticle(Model model) {
        model.addAttribute("form", new ArticleForm());
        model.addAttribute("directories", directoryService.listRows());
        model.addAttribute("mode", "create");
        return "admin/article-form";
    }

    @PostMapping("/articles")
    public String create(@ModelAttribute("form") ArticleForm form) {
        articleService.create(form);
        return "redirect:/admin/articles";
    }

    @GetMapping("/articles/{id}/edit")
    public String editArticle(@PathVariable Long id, Model model) {
        model.addAttribute("form", articleService.getForm(id));
        model.addAttribute("directories", directoryService.listRows());
        model.addAttribute("mode", "edit");
        model.addAttribute("articleId", id);
        return "admin/article-form";
    }

    @PostMapping("/articles/{id}")
    public String update(@PathVariable Long id, @ModelAttribute("form") ArticleForm form) {
        articleService.update(id, form);
        return "redirect:/admin/articles";
    }

    @PostMapping("/articles/{id}/delete")
    public String delete(@PathVariable Long id) {
        articleService.delete(id);
        return "redirect:/admin/articles";
    }

    @GetMapping("/directories")
    public String directories(Model model) {
        model.addAttribute("directories", directoryService.listRows());
        return "admin/directories";
    }

    @GetMapping("/directories/new")
    public String newDirectory(Model model) {
        model.addAttribute("form", new DirectoryForm());
        model.addAttribute("directories", directoryService.listRows());
        return "admin/directory-form";
    }

    @PostMapping("/directories")
    public String createDirectory(@ModelAttribute("form") DirectoryForm form) {
        directoryService.create(form);
        return "redirect:/admin/directories";
    }

    @GetMapping("/directories/{id}/edit")
    public String editDirectory(@PathVariable Long id, Model model) {
        model.addAttribute("form", directoryService.getForm(id));
        model.addAttribute("directories", directoryService.listRows());
        model.addAttribute("mode", "edit");
        model.addAttribute("directoryId", id);
        return "admin/directory-form";
    }

    @PostMapping("/directories/{id}")
    public String updateDirectory(@PathVariable Long id, @ModelAttribute("form") DirectoryForm form,
                                  RedirectAttributes redirectAttributes) {
        try {
            directoryService.update(id, form);
        } catch (ResponseStatusException e) {
            redirectAttributes.addFlashAttribute("error", e.getReason());
        }
        return "redirect:/admin/directories";
    }

    @PostMapping("/directories/{id}/delete")
    public String deleteDirectory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            directoryService.delete(id);
        } catch (ResponseStatusException e) {
            redirectAttributes.addFlashAttribute("error", e.getReason());
        }
        return "redirect:/admin/directories";
    }
}
