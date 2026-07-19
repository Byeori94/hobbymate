package com.byeori.hobbymate.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.byeori.hobbymate.common.exception.InvalidProfileImageException;
import com.byeori.hobbymate.common.exception.ProfileImageProcessingException;

class ProfileImageStorageTest {

    private static final byte[] ONE_PIXEL_WEBP = Base64.getDecoder().decode(
            "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");

    @TempDir
    Path tempDirectory;

    @Test
    void storesDecodedPngWithUuidFileName() throws Exception {
        ProfileImageStorage storage = storage();
        MockMultipartFile file = new MockMultipartFile(
                "profileImage", "내사진.PNG", "image/png", image("png"));

        String storedFileName = storage.store(file);

        assertThat(storedFileName).matches(
                "[0-9a-f-]{36}\\.png");
        assertThat(storedFileName).doesNotContain("내사진");
        assertThat(storage.find(storedFileName)).isPresent();
        assertThat(Files.size(storage.find(storedFileName).orElseThrow())).isPositive();
    }

    @Test
    void acceptsJpegAndWebpThroughRealImageDecoders() throws Exception {
        ProfileImageStorage storage = storage();

        String jpeg = storage.store(new MockMultipartFile(
                "profileImage", "photo.JPEG", "image/jpeg", image("jpeg")));
        String webp = storage.store(new MockMultipartFile(
                "profileImage", "photo.webp", "image/webp", ONE_PIXEL_WEBP));

        assertThat(jpeg).endsWith(".jpeg");
        assertThat(webp).endsWith(".webp");
    }

    @Test
    void rejectsEmptyOversizedDisguisedAndFormatMismatchedFiles() throws Exception {
        ProfileImageStorage storage = storage();

        assertThatThrownBy(() -> storage.store(new MockMultipartFile(
                "profileImage", "empty.jpg", "image/jpeg", new byte[0])))
                .isInstanceOf(InvalidProfileImageException.class);
        assertThatThrownBy(() -> storage.store(new MockMultipartFile(
                "profileImage", "large.jpg", "image/jpeg",
                new byte[(int) ProfileImageStorage.MAX_FILE_SIZE + 1])))
                .isInstanceOf(InvalidProfileImageException.class)
                .hasMessage("5MB 이하의 이미지만 등록할 수 있습니다.");
        assertThatThrownBy(() -> storage.store(new MockMultipartFile(
                "profileImage", "fake.jpg", "image/jpeg", "not-image".getBytes())))
                .isInstanceOf(InvalidProfileImageException.class)
                .hasMessage("올바른 이미지 파일이 아닙니다.");
        assertThatThrownBy(() -> storage.store(new MockMultipartFile(
                "profileImage", "wrong.jpg", "image/jpeg", image("png"))))
                .isInstanceOf(InvalidProfileImageException.class);
        assertThatThrownBy(() -> storage.store(new MockMultipartFile(
                "profileImage", "page.svg", "image/svg+xml", "<svg/>".getBytes())))
                .isInstanceOf(InvalidProfileImageException.class);
        assertThatThrownBy(() -> storage.store(new MockMultipartFile(
                "profileImage", "photo.jpg.exe", "image/jpeg", image("jpeg"))))
                .isInstanceOf(InvalidProfileImageException.class);
    }

    @Test
    void blocksTraversalAndOnlyDeletesSafeStoredFiles() throws Exception {
        ProfileImageStorage storage = storage();
        String storedFileName = storage.store(new MockMultipartFile(
                "profileImage", "photo.png", "image/png", image("png")));

        assertThat(storage.find("../../application.properties")).isEmpty();
        assertThat(storage.find("..\\secret.png")).isEmpty();
        assertThatThrownBy(() -> storage.delete("../../application.properties"))
                .isInstanceOf(ProfileImageProcessingException.class);

        storage.delete(storedFileName);
        assertThat(storage.find(storedFileName)).isEmpty();
        storage.delete(storedFileName);
    }

    @Test
    void failsSafelyWhenConfiguredDirectoryIsAFile() throws Exception {
        Path filePath = tempDirectory.resolve("not-a-directory");
        Files.writeString(filePath, "file");
        ProfileImageStorage storage = new ProfileImageStorage(filePath.toString());

        assertThatThrownBy(() -> storage.store(new MockMultipartFile(
                "profileImage", "photo.png", "image/png", image("png"))))
                .isInstanceOf(ProfileImageProcessingException.class)
                .hasMessage("프로필 사진 저장소를 사용할 수 없습니다.");
    }

    private ProfileImageStorage storage() {
        return new ProfileImageStorage(tempDirectory.resolve("uploads/profile").toString());
    }

    private byte[] image(String format) throws Exception {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, Color.GREEN.getRGB());
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
