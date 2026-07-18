package com.temporyn.wiki.repository;

import com.temporyn.wiki.domain.Article;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {
}
