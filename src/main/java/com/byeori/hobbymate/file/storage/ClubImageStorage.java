package com.byeori.hobbymate.file.storage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.byeori.hobbymate.common.exception.ClubImageException;

@Component
public class ClubImageStorage {

    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Map<String, String> CONTENT_TYPES = Map.of(
            "jpg", MediaType.IMAGE_JPEG_VALUE,
            "jpeg", MediaType.IMAGE_JPEG_VALUE,
            "png", MediaType.IMAGE_PNG_VALUE,
            "webp", "image/webp");

    private final Path clubDirectory;

    public ClubImageStorage(@Value("${app.upload.club-dir:uploads/club}") String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException("모임 이미지 저장 경로가 설정되지 않았습니다.");
        }
        this.clubDirectory = Path.of(configuredPath).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new ClubImageException("올바른 대표 이미지를 선택해 주세요.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ClubImageException("대표 이미지는 5MB 이하만 등록할 수 있습니다.");
        }

        String extension = extension(file.getOriginalFilename());
        String contentType = file.getContentType() == null
                ? ""
                : file.getContentType().toLowerCase(Locale.ROOT);
        if (!CONTENT_TYPES.get(extension).equals(contentType)) {
            throw new ClubImageException("JPG, JPEG, PNG, WEBP 이미지 파일만 등록할 수 있습니다.");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new ClubImageException("대표 이미지를 저장할 수 없습니다.", ex);
        }
        validateDecodedImage(content, extension);

        try {
            Files.createDirectories(clubDirectory);
            String storedFileName = UUID.randomUUID() + "." + extension;
            Path target = safePath(storedFileName);
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
            return storedFileName;
        } catch (IOException ex) {
            throw new ClubImageException("대표 이미지를 저장할 수 없습니다.", ex);
        }
    }

    public void delete(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }
        try {
            Files.deleteIfExists(safePath(storedFileName));
        } catch (IOException | IllegalArgumentException ex) {
            throw new ClubImageException("대표 이미지 파일을 정리할 수 없습니다.", ex);
        }
    }

    public Optional<Path> find(String storedFileName) {
        if (!isSafeStoredFileName(storedFileName)) {
            return Optional.empty();
        }
        Path candidate = safePath(storedFileName);
        return Files.isRegularFile(candidate) ? Optional.of(candidate) : Optional.empty();
    }

    public MediaType mediaType(String storedFileName) {
        String extension = storedFileName.substring(storedFileName.lastIndexOf('.') + 1)
                .toLowerCase(Locale.ROOT);
        return MediaType.parseMediaType(CONTENT_TYPES.get(extension));
    }

    private String extension(String originalFileName) {
        if (originalFileName == null) {
            throw invalidType();
        }
        String fileName = originalFileName.replace('\\', '/');
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1).trim();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            throw invalidType();
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw invalidType();
        }
        return extension;
    }

    private void validateDecodedImage(byte[] content, String extension) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                throw invalidType();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidType();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                BufferedImage image = reader.read(0);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0
                        || !matches(extension, format)) {
                    throw invalidType();
                }
            } finally {
                reader.dispose();
            }
        } catch (ClubImageException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw invalidType();
        }
    }

    private boolean matches(String extension, String format) {
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return "jpg".equals(format) || "jpeg".equals(format);
        }
        return extension.equals(format);
    }

    private Path safePath(String storedFileName) {
        if (!isSafeStoredFileName(storedFileName)) {
            throw new IllegalArgumentException("Unsafe club image file name");
        }
        Path target = clubDirectory.resolve(storedFileName).normalize();
        if (!clubDirectory.equals(target.getParent())) {
            throw new IllegalArgumentException("Unsafe club image path");
        }
        return target;
    }

    private boolean isSafeStoredFileName(String storedFileName) {
        return storedFileName != null
                && storedFileName.matches("[0-9a-fA-F-]{36}\\.(jpg|jpeg|png|webp)");
    }

    private ClubImageException invalidType() {
        return new ClubImageException("JPG, JPEG, PNG, WEBP 이미지 파일만 등록할 수 있습니다.");
    }
}
