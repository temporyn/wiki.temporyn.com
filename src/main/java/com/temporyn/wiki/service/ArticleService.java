package com.temporyn.wiki.service;

import com.temporyn.wiki.domain.Article;
import com.temporyn.wiki.dto.ArticleForm;
import com.temporyn.wiki.dto.ArticleView;
import com.temporyn.wiki.repository.ArticleRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final DirectoryService directoryService;
    private final MarkdownRenderer markdownRenderer;
    private final AttachmentService attachmentService;

    public ArticleService(ArticleRepository articleRepository, DirectoryService directoryService,
                          MarkdownRenderer markdownRenderer, AttachmentService attachmentService) {
        this.articleRepository = articleRepository;
        this.directoryService = directoryService;
        this.markdownRenderer = markdownRenderer;
        this.attachmentService = attachmentService;
    }

    public ArticleView getArticleView(Long directoryId, Long articleId) {
        Article article = requireArticle(articleId);
        if (!article.getDirectoryId().equals(directoryId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다.");
        }
        String path = directoryService.breadcrumb(article.getDirectoryId());
        String html = markdownRenderer.toHtml(article.getContent());
        return new ArticleView(article.getId(), article.getTitle(), path, html,
                attachmentService.listViews(article.getId()));
    }

    public ArticleForm getForm(Long articleId) {
        Article article = requireArticle(articleId);
        ArticleForm form = new ArticleForm();
        form.setDirectoryId(article.getDirectoryId());
        form.setTitle(article.getTitle());
        form.setContent(article.getContent());
        return form;
    }

    @Transactional
    public Long create(ArticleForm form) {
        int nextOrder = articleRepository.findByDirectoryId(form.getDirectoryId()).size() + 1;
        Article article = Article.create(form.getDirectoryId(), form.getTitle(), form.getContent(), nextOrder);
        Long articleId = articleRepository.save(article).getId();
        attachmentService.assignToArticle(form.getUploadToken(), articleId);
        return articleId;
    }

    @Transactional
    public void update(Long articleId, ArticleForm form) {
        Article article = requireArticle(articleId);
        article.update(form.getDirectoryId(), form.getTitle(), form.getContent());
        attachmentService.assignToArticle(form.getUploadToken(), articleId);
    }

    @Transactional
    public void rename(Long articleId, String title) {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "제목을 입력하세요.");
        }
        Article article = requireArticle(articleId);
        article.update(article.getDirectoryId(), title.strip(), article.getContent());
    }

    @Transactional
    public void move(Long articleId, Long directoryId) {
        Article article = requireArticle(articleId);
        article.update(directoryId, article.getTitle(), article.getContent());
    }

    public Long directoryIdOf(Long articleId) {
        return requireArticle(articleId).getDirectoryId();
    }

    @Transactional
    public void delete(Long articleId) {
        if (!articleRepository.existsById(articleId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다.");
        }
        attachmentService.deleteForArticle(articleId);
        articleRepository.deleteById(articleId);
    }

    private Article requireArticle(Long articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."));
    }
}
