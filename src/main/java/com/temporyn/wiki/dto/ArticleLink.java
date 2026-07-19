package com.temporyn.wiki.dto;

/** 사이드바의 문서 링크. path 는 확장자를 뺀 볼트 상대 경로. */
public record ArticleLink(String title, String path, String url) {
}
