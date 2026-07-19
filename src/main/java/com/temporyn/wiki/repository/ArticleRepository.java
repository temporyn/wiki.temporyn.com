package com.temporyn.wiki.repository;

import com.temporyn.wiki.domain.Article;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    List<Article> findByDirectoryId(Long directoryId);
}
