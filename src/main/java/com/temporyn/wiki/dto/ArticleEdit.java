package com.temporyn.wiki.dto;

/** 편집 화면용. content 는 렌더링 전의 Markdown 원문. */
public record ArticleEdit(String path, String title, String breadcrumb, String content) {
}
