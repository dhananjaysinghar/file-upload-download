package com.demo.controller;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@RestController
public class RequestController {
    public static final String UPLOAD_DIR = "uploads/";

    @PostMapping(value = "/uploader", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploader(@RequestParam("fileName") String fileName,
                                      @RequestParam("file") MultipartFile file) throws IOException {

        Path path = Paths.get(UPLOAD_DIR).toAbsolutePath().normalize();
        Files.copy(file.getInputStream(), path.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);

        return new ResponseEntity<>("file uploaded ...", HttpStatus.CREATED);
    }


    @GetMapping("/downloader")
    public ResponseEntity<?> downloader(@RequestParam String fileName) throws MalformedURLException {
        Path filePath = Paths.get(UPLOAD_DIR).resolve(fileName).normalize();
        Resource resource = new UrlResource(filePath.toUri());
        if (resource.exists()) {
            return new ResponseEntity<>(resource, HttpStatus.OK);
        } else {
            throw new RuntimeException("File not found " + fileName);
        }

    }
}


