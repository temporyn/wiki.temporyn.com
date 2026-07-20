package com.temporyn.wiki.dto;

/** A sidebar link to an article. {@code path} is the vault-relative path without the .md suffix. */
public record ArticleLink(String title, String path, String url) {
}
