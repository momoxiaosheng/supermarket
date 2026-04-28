package com.example.supermarket2.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileUploadConfig {

    @Value("${file.upload.image-path:./upload/images/}")
    private String imageUploadPath;

    public static String IMAGE_UPLOAD_PATH;

    @PostConstruct
    public void init() {
        IMAGE_UPLOAD_PATH = this.imageUploadPath;
    }
}