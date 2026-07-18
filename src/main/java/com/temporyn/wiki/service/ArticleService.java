package com.temporyn.wiki.service;

import com.temporyn.wiki.domain.Article;
import com.temporyn.wiki.dto.ArticleView;
import com.temporyn.wiki.repository.ArticleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final DirectoryService directoryService;

    public ArticleService(ArticleRepository articleRepository, DirectoryService directoryService) {
        this.articleRepository = articleRepository;
        this.directoryService = directoryService;
    }

    public ArticleView getArticleView(Long directoryId, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .filter(found -> found.getDirectoryId().equals(directoryId))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."));

        String path = directoryService.breadcrumb(article.getDirectoryId());
        return new ArticleView(article.getId(), article.getTitle(), path, article.getContent());
    }
}
