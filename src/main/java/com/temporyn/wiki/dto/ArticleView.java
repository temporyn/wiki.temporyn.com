package com.temporyn.wiki.dto;

import java.util.List;

public record ArticleView(Long id, String title, String path, String content, List<AttachmentView> attachments) {
}
