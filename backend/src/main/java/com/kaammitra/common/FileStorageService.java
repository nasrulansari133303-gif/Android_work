package com.kaammitra.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
public class FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "pdf");
    private static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;

    private final Path uploadRoot;

    public FileStorageService(@Value("${app.upload.dir:uploads}") String uploadDir) {
        this.uploadRoot = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadRoot);
        } catch (IOException e) {
            throw new IllegalStateException("Could not initialize upload directory", e);
        }
    }

    public String storeFile(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Uploaded file must not be empty");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("File exceeds maximum allowed size of 5MB");
        }
        String original = file.getOriginalFilename() == null ? "file.jpg" : file.getOriginalFilename();
        String sanitized = Paths.get(original).getFileName().toString();
        String ext = "";
        int dotIdx = sanitized.lastIndexOf('.');
        if (dotIdx >= 0 && dotIdx < sanitized.length() - 1) {
            ext = sanitized.substring(dotIdx + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BusinessException("Unsupported file type. Allowed: jpg, jpeg, png, webp, pdf");
        }
        String safeFolder = folder == null ? "general" : folder.replaceAll("[^a-zA-Z0-9_-]", "");
        Path targetDir = this.uploadRoot.resolve(safeFolder).normalize();
        try {
            Files.createDirectories(targetDir);
            String filename = UUID.randomUUID() + "." + ext;
            Path targetFile = targetDir.resolve(filename).normalize();
            if (!targetFile.startsWith(this.uploadRoot)) {
                throw new BusinessException("Invalid file path");
            }
            Files.copy(file.getInputStream(), targetFile, StandardCopyOption.REPLACE_EXISTING);
            return "/uploads/" + safeFolder + "/" + filename;
        } catch (IOException e) {
            throw new BusinessException("Failed to store uploaded file");
        }
    }
}
