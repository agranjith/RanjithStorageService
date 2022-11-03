package com.ranjith.rss.dto;

import lombok.Data;

@Data
public class FileUploadResponse {
    private String fileName;
    private String contentType;
    private String url;

    public FileUploadResponse(String fileName, String contentType, String url) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.url = url;
    }
}
