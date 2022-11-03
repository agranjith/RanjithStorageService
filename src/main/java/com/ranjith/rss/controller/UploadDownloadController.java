package com.ranjith.rss.controller;

import com.ranjith.rss.dto.FileUploadResponse;
import com.ranjith.rss.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


@RestController
public class UploadDownloadController {

    private FileStorageService fileStorageService;

    public UploadDownloadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/single/upload")
    FileUploadResponse singleFileUpload(@RequestParam("file") MultipartFile file) {
        String fileName = fileStorageService.storeFile(file);

        String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/download/")
                .path(fileName)
                .toUriString();
        String contentType = file.getContentType();
        FileUploadResponse response = new FileUploadResponse(fileName, contentType, url);

        return response;
    }

    @PostMapping("/multiple/upload")
    List<FileUploadResponse> multipleFileUpload(@RequestParam("files") MultipartFile[] files) {

        if (files.length > 7) {
            throw new RuntimeException("Too Many Files");
        }

        List<FileUploadResponse> fileUploadResponseList = new ArrayList<>();

        Arrays.asList(files)
                .stream()
                .forEach(file -> {
                    String fileName = fileStorageService.storeFile(file);

                    String url = ServletUriComponentsBuilder.fromCurrentContextPath()
                            .path("/download/")
                            .path(fileName)
                            .toUriString();
                    String contentType = file.getContentType();
                    FileUploadResponse response = new FileUploadResponse(fileName, contentType, url);
                    fileUploadResponseList.add(response);
                });


        return fileUploadResponseList;
    }

    @GetMapping("/download/{fileName}")
    ResponseEntity<Resource> downloadSingleFile(@PathVariable String fileName, HttpServletRequest request) {

        Resource resource = fileStorageService.downloadFile(fileName);

        String mimeType;

        try {
            mimeType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException e) {
            mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }

        mimeType = mimeType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : mimeType;


        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;fileName=" + resource.getFilename())
                .body(resource);
    }


    @GetMapping("/zipDownload")
    void zipDownload(@RequestParam("fileName") String[] files, HttpServletResponse response) throws IOException {

        try (ZipOutputStream zipOutputStream = new ZipOutputStream(response.getOutputStream())) {
            Arrays.asList(files)
                    .stream()
                    .forEach(file -> {
                        Resource resource = fileStorageService.downloadFile(file);

                        ZipEntry zipEntry = new ZipEntry(resource.getFilename());

                        try {
                            zipEntry.setSize(resource.contentLength());
                            zipOutputStream.putNextEntry(zipEntry);

                            StreamUtils.copy(resource.getInputStream(), zipOutputStream);
                            zipOutputStream.closeEntry();
                        } catch (IOException e) {
                            System.out.println("some exception while ziping");
                        }
                    });
            zipOutputStream.finish();
        }

        response.setStatus(200);
        response.addHeader(HttpHeaders.CONTENT_DISPOSITION,"attachment:fileName=zipFile");
    }
}
