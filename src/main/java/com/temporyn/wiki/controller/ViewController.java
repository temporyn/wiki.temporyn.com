package com.temporyn.wiki.controller;

import com.temporyn.wiki.service.DirectoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ViewController {

    private final DirectoryService directoryService;

    public ViewController(DirectoryService directoryService) {
        this.directoryService = directoryService;
    }

    @GetMapping({"/about", "/about/"})
    public String about(Model model) {
        model.addAttribute("tree", directoryService.buildTree());
        return "about";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
