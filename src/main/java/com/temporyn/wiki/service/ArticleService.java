package com.temporyn.wiki.service;

import com.temporyn.wiki.dto.ArticleEdit;
import com.temporyn.wiki.dto.ArticleView;
import org.springframework.stereotype.Service;

@Service
public class ArticleService {

    private final VaultService vaultService;

    public ArticleService(VaultService vaultService) {
        this.vaultService = vaultService;
    }

    public ArticleView getArticleView(String relativePath) {
        String markdown = vaultService.readArticle(relativePath);
        return new ArticleView(relativePath, titleOf(relativePath), breadcrumbOf(relativePath), markdown);
    }

    public ArticleEdit getArticleEdit(String relativePath) {
        String markdown = vaultService.readArticle(relativePath);
        return new ArticleEdit(relativePath, titleOf(relativePath), breadcrumbOf(relativePath), markdown);
    }

    public void save(String relativePath, String markdown) {
        vaultService.writeArticle(relativePath, markdown);
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
