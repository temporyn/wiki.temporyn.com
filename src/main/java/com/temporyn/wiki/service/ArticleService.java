package com.temporyn.wiki.service;

import com.temporyn.wiki.dto.ArticleView;
import org.springframework.stereotype.Service;

@Service
public class ArticleService {

    private final VaultService vaultService;
    private final MarkdownRenderer markdownRenderer;

    public ArticleService(VaultService vaultService, MarkdownRenderer markdownRenderer) {
        this.vaultService = vaultService;
        this.markdownRenderer = markdownRenderer;
    }

    public ArticleView getArticleView(String relativePath) {
        String markdown = vaultService.readArticle(relativePath);
        return new ArticleView(relativePath, titleOf(relativePath), breadcrumbOf(relativePath),
                markdownRenderer.toHtml(markdown));
    }

    private String titleOf(String relativePath) {
        int slash = relativePath.lastIndexOf('/');
        return slash < 0 ? relativePath : relativePath.substring(slash + 1);
    }

    private String breadcrumbOf(String relativePath) {
        int slash = relativePath.lastIndexOf('/');
        return slash < 0 ? "" : relativePath.substring(0, slash).replace("/", " / ");
    }
}
