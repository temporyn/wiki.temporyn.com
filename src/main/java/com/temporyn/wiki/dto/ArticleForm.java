package com.temporyn.wiki.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ArticleForm {

    private Long directoryId;
    private String title;
    private String content;
    private String uploadToken;
}
