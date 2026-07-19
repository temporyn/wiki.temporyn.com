package com.temporyn.wiki.dto;

import java.util.List;

/** 볼트(파일시스템) 안의 폴더 한 개. path 는 볼트 루트 기준 상대 경로. */
public record DirectoryNode(
        String name,
        String path,
        int articleCount,
        List<DirectoryNode> children,
        List<ArticleLink> articles) {
}
