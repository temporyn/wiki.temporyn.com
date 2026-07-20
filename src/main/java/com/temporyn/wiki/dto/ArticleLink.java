package com.temporyn.wiki.dto;

/**
 * A sidebar file entry. Markdown documents are openable ({@code openable = true}) and carry a
 * view {@code url}; other file types are shown for reference only and are not openable.
 * {@code path} is the vault-relative path (without the {@code .md} suffix for documents).
 */
public record ArticleLink(String title, String path, String url, boolean openable) {
}
