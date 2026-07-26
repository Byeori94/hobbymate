package com.byeori.hobbymate.file.storage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import com.byeori.hobbymate.common.exception.ClubImageException;

class ClubImageStorageTest {

    @TempDir
    Path tempDir;

    @Test
    void validPngIsStoredWithUuidFileName() throws Exception {
        ClubImageStorage storage = new ClubImageStorage(tempDir.toString());
        MockMultipartFile image = new MockMultipartFile(
                "representativeImage",
                "club.png",
                "image/png",
                png());

        String storedFileName = storage.store(image);

        assertThat(storedFileName).matches("[0-9a-f-]{36}\\.png");
        assertThat(Files.isRegularFile(tempDir.resolve(storedFileName))).isTrue();
    }

    @Test
    void forgedImageContentIsRejected() {
        ClubImageStorage storage = new ClubImageStorage(tempDir.toString());
        MockMultipartFile image = new MockMultipartFile(
                "representativeImage",
                "club.png",
                "image/png",
                "not-an-image".getBytes());

        assertThatThrownBy(() -> storage.store(image))
                .isInstanceOf(ClubImageException.class)
                .hasMessage("JPG, JPEG, PNG, WEBP 이미지 파일만 등록할 수 있습니다.");
    }

    @Test
    void oversizedImageIsRejectedBeforeWrite() {
        ClubImageStorage storage = new ClubImageStorage(tempDir.toString());
        MockMultipartFile image = new MockMultipartFile(
                "representativeImage",
                "club.png",
                "image/png",
                new byte[(int) ClubImageStorage.MAX_FILE_SIZE + 1]);

        assertThatThrownBy(() -> storage.store(image))
                .isInstanceOf(ClubImageException.class)
                .hasMessage("대표 이미지는 5MB 이하만 등록할 수 있습니다.");
    }

    private byte[] png() throws Exception {
        BufferedImage image = new BufferedImage(16, 9, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }
}
