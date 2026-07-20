package com.temporyn.wiki.dto;

import java.util.List;

/** A single folder in the vault. {@code path} is relative to the vault root. */
public record DirectoryNode(
        String name,
        String path,
        int articleCount,
        List<DirectoryNode> children,
        List<ArticleLink> articles) {
}
