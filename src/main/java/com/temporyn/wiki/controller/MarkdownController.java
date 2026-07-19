package com.temporyn.wiki.controller;

import com.temporyn.wiki.service.MarkdownRenderer;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/markdown")
public class MarkdownController {

    private final MarkdownRenderer markdownRenderer;

    public MarkdownController(MarkdownRenderer markdownRenderer) {
        this.markdownRenderer = markdownRenderer;
    }

    @PostMapping("/preview")
    public Map<String, String> preview(@RequestBody PreviewRequest request) {
        return Map.of("html", markdownRenderer.toHtml(request.markdown()));
    }

    public record PreviewRequest(String markdown) {
    }
}
