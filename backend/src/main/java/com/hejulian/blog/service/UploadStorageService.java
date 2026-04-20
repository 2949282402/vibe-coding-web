package com.hejulian.blog.service;

import com.hejulian.blog.common.SlugUtils;
import com.hejulian.blog.config.UploadProperties;
import com.hejulian.blog.dto.AdminDtos;
import com.hejulian.blog.exception.BusinessException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class UploadStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".bmp", ".avif"
    );

    private final UploadProperties uploadProperties;

    public AdminDtos.ImageUploadResponse storeImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Image file is required");
        }

        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        if (!contentType.startsWith("image/")) {
            throw new BusinessException("Only image uploads are supported");
        }
        if ("image/svg+xml".equals(contentType)) {
            throw new BusinessException("SVG uploads are not supported");
        }

        String extension = resolveExtension(file.getOriginalFilename(), contentType);
        String baseName = SlugUtils.toSlug(stripExtension(file.getOriginalFilename()));
        String filename = baseName + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12) + extension;

        LocalDate today = LocalDate.now();
        Path rootDirectory = uploadProperties.resolveDirectory();
        Path relativeDirectory = Path.of("images", String.valueOf(today.getYear()), String.format("%02d", today.getMonthValue()));
        Path targetDirectory = rootDirectory.resolve(relativeDirectory).normalize();
        Path targetFile = targetDirectory.resolve(filename).normalize();

        if (!targetFile.startsWith(rootDirectory)) {
            throw new BusinessException("Invalid upload path");
        }

        try {
            Files.createDirectories(targetDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new BusinessException("Failed to store image");
        }

        String publicUrl = "/uploads/" + relativeDirectory.resolve(filename).toString().replace('\\', '/');
        return new AdminDtos.ImageUploadResponse(publicUrl, filename);
    }

    private String resolveExtension(String originalFilename, String contentType) {
        String filename = StringUtils.hasText(originalFilename) ? originalFilename.trim() : "";
        int index = filename.lastIndexOf('.');
        if (index >= 0) {
            String extension = filename.substring(index).toLowerCase(Locale.ROOT);
            if (ALLOWED_EXTENSIONS.contains(extension)) {
                return extension;
            }
        }

        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            case "image/bmp" -> ".bmp";
            case "image/avif" -> ".avif";
            default -> ".jpg";
        };
    }

    private String stripExtension(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            return "image";
        }

        int index = originalFilename.lastIndexOf('.');
        return index >= 0 ? originalFilename.substring(0, index) : originalFilename;
    }
}
