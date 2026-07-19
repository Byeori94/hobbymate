package com.byeori.hobbymate.file.storage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.byeori.hobbymate.common.exception.InvalidProfileImageException;
import com.byeori.hobbymate.common.exception.ProfileImageProcessingException;

@Component
public class ProfileImageStorage {

    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024;

    private static final Pattern STORED_FILE_NAME_PATTERN = Pattern.compile(
            "^([0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12})\\.(jpg|jpeg|png|webp)$",
            Pattern.CASE_INSENSITIVE);
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");
    private static final Map<String, String> EXPECTED_CONTENT_TYPES = Map.of(
            "jpg", MediaType.IMAGE_JPEG_VALUE,
            "jpeg", MediaType.IMAGE_JPEG_VALUE,
            "png", MediaType.IMAGE_PNG_VALUE,
            "webp", "image/webp");

    private final Path profileDirectory;

    public ProfileImageStorage(@Value("${app.upload.profile-dir:uploads/profile}") String configuredPath) {
        if (configuredPath == null || configuredPath.isBlank()) {
            throw new IllegalStateException("프로필 이미지 저장 경로가 설정되지 않았습니다.");
        }
        this.profileDirectory = Path.of(configuredPath).toAbsolutePath().normalize();
    }

    public String store(MultipartFile file) {
        validateBasicFile(file);
        String extension = extractExtension(file.getOriginalFilename());
        validateContentType(extension, file.getContentType());

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new ProfileImageProcessingException("프로필 사진을 저장할 수 없습니다.", ex);
        }
        validateDecodedImage(content, extension);

        ensureDirectory();
        String storedFileName = UUID.randomUUID() + "." + extension;
        Path target = safePath(storedFileName);
        Path temporary = null;
        try {
            temporary = Files.createTempFile(profileDirectory, ".profile-", ".tmp");
            Files.write(temporary, content, StandardOpenOption.TRUNCATE_EXISTING);
            try {
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temporary, target);
            }
            return storedFileName;
        } catch (IOException ex) {
            deleteTemporaryFile(temporary);
            throw new ProfileImageProcessingException("프로필 사진을 저장할 수 없습니다.", ex);
        }
    }

    public void delete(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return;
        }
        if (!isSafeStoredFileName(storedFileName)) {
            throw new ProfileImageProcessingException("안전하지 않은 프로필 이미지 파일명입니다.");
        }
        try {
            Files.deleteIfExists(safePath(storedFileName));
        } catch (IOException ex) {
            throw new ProfileImageProcessingException("프로필 사진 파일을 삭제할 수 없습니다.", ex);
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
        return MediaType.parseMediaType(EXPECTED_CONTENT_TYPES.get(extension));
    }

    public boolean isSafeStoredFileName(String storedFileName) {
        if (storedFileName == null || storedFileName.isBlank()) {
            return false;
        }
        Matcher matcher = STORED_FILE_NAME_PATTERN.matcher(storedFileName);
        if (!matcher.matches()) {
            return false;
        }
        try {
            UUID.fromString(matcher.group(1));
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private void validateBasicFile(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() == 0) {
            throw new InvalidProfileImageException("프로필 사진을 선택해 주세요.");
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new InvalidProfileImageException("5MB 이하의 이미지만 등록할 수 있습니다.");
        }
    }

    private String extractExtension(String originalFileName) {
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new InvalidProfileImageException("JPG, JPEG, PNG, WEBP 파일만 등록할 수 있습니다.");
        }
        String fileName = originalFileName.replace('\\', '/');
        fileName = fileName.substring(fileName.lastIndexOf('/') + 1).trim();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex <= 0 || dotIndex == fileName.length() - 1) {
            throw new InvalidProfileImageException("JPG, JPEG, PNG, WEBP 파일만 등록할 수 있습니다.");
        }
        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new InvalidProfileImageException("JPG, JPEG, PNG, WEBP 파일만 등록할 수 있습니다.");
        }
        return extension;
    }

    private void validateContentType(String extension, String contentType) {
        String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!EXPECTED_CONTENT_TYPES.get(extension).equals(normalized)) {
            throw new InvalidProfileImageException("올바른 이미지 파일이 아닙니다.");
        }
    }

    private void validateDecodedImage(byte[] content, String extension) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(content))) {
            if (input == null) {
                throw invalidImage();
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                BufferedImage decoded = reader.read(0);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (width <= 0 || height <= 0 || decoded == null || !matchesFormat(extension, format)) {
                    throw invalidImage();
                }
            } finally {
                reader.dispose();
            }
        } catch (InvalidProfileImageException ex) {
            throw ex;
        } catch (IOException | RuntimeException ex) {
            throw invalidImage();
        }
    }

    private boolean matchesFormat(String extension, String format) {
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return "jpeg".equals(format) || "jpg".equals(format);
        }
        return extension.equals(format);
    }

    private InvalidProfileImageException invalidImage() {
        return new InvalidProfileImageException("올바른 이미지 파일이 아닙니다.");
    }

    private void ensureDirectory() {
        try {
            Files.createDirectories(profileDirectory);
            if (!Files.isDirectory(profileDirectory) || !Files.isWritable(profileDirectory)) {
                throw new IOException("Profile upload directory is not writable");
            }
        } catch (IOException ex) {
            throw new ProfileImageProcessingException("프로필 사진 저장소를 사용할 수 없습니다.", ex);
        }
    }

    private Path safePath(String storedFileName) {
        Path resolved = profileDirectory.resolve(storedFileName).normalize();
        if (!resolved.getParent().equals(profileDirectory)) {
            throw new ProfileImageProcessingException("안전하지 않은 프로필 이미지 경로입니다.");
        }
        return resolved;
    }

    private void deleteTemporaryFile(Path temporary) {
        if (temporary == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporary);
        } catch (IOException ignored) {
            // 저장 실패의 원래 원인을 유지한다.
        }
    }
}
