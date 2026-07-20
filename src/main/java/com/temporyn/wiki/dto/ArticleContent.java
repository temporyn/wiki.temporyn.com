package com.temporyn.wiki.dto;

/**
 * An article's raw Markdown content plus display metadata.
 * Used for both viewing and editing; rendering happens client-side (TipTap).
 */
public record ArticleContent(String path, String title, String breadcrumb, String content) {
}
