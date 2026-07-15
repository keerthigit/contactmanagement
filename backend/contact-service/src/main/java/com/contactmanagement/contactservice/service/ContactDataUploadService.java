package com.contactmanagement.contactservice.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class ContactDataUploadService {

    private static final DateTimeFormatter FILE_TIMESTAMP =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private final Path uploadDirectory;

    public ContactDataUploadService(
            @Value("${contact.upload.dir:../../data/contactdata}") String uploadDir) throws IOException {
        this.uploadDirectory = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDirectory);
    }

    public String uploadFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        String originalName = sanitizeFilename(file.getOriginalFilename());
        String timestamp = LocalDateTime.now().format(FILE_TIMESTAMP);
        String savedFilename = timestamp + "_" + originalName;
        Path destination = uploadDirectory.resolve(savedFilename);

        Files.copy(file.getInputStream(), destination);

        return savedFilename;
    }

    public List<String> listUploadedFiles() throws IOException {
        try (Stream<Path> paths = Files.list(uploadDirectory)) {
            return paths
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.reverseOrder())
                    .toList();
        }
    }

    public Path getUploadDirectory() {
        return uploadDirectory;
    }

    private String sanitizeFilename(String filename) {
        if (filename == null || filename.isBlank()) {
            return "contact-data.txt";
        }

        String sanitized = Paths.get(filename).getFileName().toString()
                .replaceAll("[^a-zA-Z0-9._-]", "_");

        return sanitized.isBlank() ? "contact-data.txt" : sanitized;
    }
}
