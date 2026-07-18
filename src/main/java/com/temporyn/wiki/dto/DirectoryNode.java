package com.temporyn.wiki.dto;

import java.util.List;

public record DirectoryNode(
        Long id,
        String name,
        int articleCount,
        List<DirectoryNode> children,
        List<ArticleLink> articles) {
}
