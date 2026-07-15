package com.contactmanagement.contactservice.controller;

import com.contactmanagement.contactservice.service.ContactDataUploadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/contacts/data")
public class ContactDataUploadController {

    private final ContactDataUploadService contactDataUploadService;

    public ContactDataUploadController(ContactDataUploadService contactDataUploadService) {
        this.contactDataUploadService = contactDataUploadService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadContactDataFile(
            @RequestParam("file") MultipartFile file) throws IOException {
        String savedFilename = contactDataUploadService.uploadFile(file);

        Map<String, String> response = new LinkedHashMap<>();
        response.put("filename", savedFilename);
        response.put("message", "File uploaded successfully");

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/files")
    public ResponseEntity<List<String>> listContactDataFiles() throws IOException {
        return ResponseEntity.ok(contactDataUploadService.listUploadedFiles());
    }
}
