package com.temporyn.wiki.dto;

public record AttachmentView(Long id, String filename, long size, String humanSize, String downloadUrl) {
}
