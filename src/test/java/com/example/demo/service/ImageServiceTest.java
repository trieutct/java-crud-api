package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.demo.dto.ImageResponse;
import com.example.demo.entity.Image;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.repository.ImageRepository;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    private static final byte[] PNG_BYTES = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 1, 2, 3, 4, 5};
    private static final byte[] JPEG_BYTES = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2};
    private static final byte[] GIF_BYTES = "GIF89a-noi-dung".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_BYTES = "RIFF0000WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);

    @TempDir
    private Path tempDir;

    @Mock
    private ImageRepository imageRepository;

    private Path uploadDir;
    private ImageService imageService;

    @BeforeEach
    void setUp() {
        // Thư mục con chưa tồn tại, service phải tự tạo
        uploadDir = tempDir.resolve("uploads");
        imageService = new ImageService(imageRepository, uploadDir.toString());
    }

    private static MockMultipartFile file(String name, byte[] content) {
        return new MockMultipartFile("file", name, "image/png", content);
    }

    private void saveReturnsArgument() {
        when(imageRepository.save(any(Image.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void createWritesFileAndSavesPath() throws IOException {
        saveReturnsArgument();

        ImageResponse response = imageService.create(file("ảnh đại diện.png", PNG_BYTES));

        assertThat(response.originalName()).isEqualTo("ảnh đại diện.png");
        assertThat(response.filePath()).startsWith("/uploads/").endsWith(".png");
        assertThat(response.filePath()).doesNotContain("đại diện");
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.fileSize()).isEqualTo(PNG_BYTES.length);
        String fileName = response.filePath().substring("/uploads/".length());
        assertThat(Files.readAllBytes(uploadDir.resolve(fileName))).isEqualTo(PNG_BYTES);
    }

    @Test
    void createDetectsFormatFromContentNotFromClient() {
        saveReturnsArgument();

        // Client khai báo tên .png và Content-Type image/png nhưng nội dung là JPEG
        ImageResponse jpeg = imageService.create(file("a.png", JPEG_BYTES));
        ImageResponse gif = imageService.create(file("a.png", GIF_BYTES));
        ImageResponse webp = imageService.create(file("a.png", WEBP_BYTES));

        assertThat(jpeg.contentType()).isEqualTo("image/jpeg");
        assertThat(jpeg.filePath()).endsWith(".jpg");
        assertThat(gif.contentType()).isEqualTo("image/gif");
        assertThat(gif.filePath()).endsWith(".gif");
        assertThat(webp.contentType()).isEqualTo("image/webp");
        assertThat(webp.filePath()).endsWith(".webp");
    }

    @Test
    void createStripsDirectoriesFromOriginalName() {
        saveReturnsArgument();

        ImageResponse response = imageService.create(file("..\\..\\thu-muc/a.png", PNG_BYTES));

        assertThat(response.originalName()).isEqualTo("a.png");
    }

    @Test
    void createRejectsEmptyFile() {
        assertThatThrownBy(() -> imageService.create(file("a.png", new byte[0])))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(imageRepository);
    }

    @Test
    void createRejectsNonImageContentWithImageName() {
        byte[] html = "<script>alert(1)</script>".getBytes(StandardCharsets.UTF_8);

        assertThatThrownBy(() -> imageService.create(file("a.png", html)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Chỉ hỗ trợ ảnh JPEG, PNG, GIF hoặc WEBP");
        verifyNoInteractions(imageRepository);
        assertThat(uploadDir).doesNotExist();
    }

    @Test
    void createRejectsFileShorterThanSignature() {
        assertThatThrownBy(() -> imageService.create(file("a.png", new byte[] {1})))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createRejectsTooLongName() {
        String name = "a".repeat(252) + ".png";

        assertThatThrownBy(() -> imageService.create(file(name, PNG_BYTES)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên file tối đa 255 ký tự");
    }

    @Test
    void createRejectsMissingName() {
        assertThatThrownBy(() -> imageService.create(file("", PNG_BYTES)))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Tên file không hợp lệ");
    }

    @Test
    void createDeletesFileWhenSaveFails() throws IOException {
        when(imageRepository.save(any(Image.class))).thenThrow(new IllegalStateException("database lỗi"));

        assertThatThrownBy(() -> imageService.create(file("a.png", PNG_BYTES)))
                .isInstanceOf(IllegalStateException.class);

        try (var files = Files.list(uploadDir)) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void findAllReturnsImages() {
        when(imageRepository.findAll()).thenReturn(List.of(new Image("a.png", "/uploads/x.png", "image/png", 5L)));

        List<ImageResponse> response = imageService.findAll();

        assertThat(response).hasSize(1);
        assertThat(response.getFirst().filePath()).isEqualTo("/uploads/x.png");
    }

    @Test
    void findByIdReturnsImage() {
        when(imageRepository.findById(1)).thenReturn(Optional.of(new Image("a.png", "/uploads/x.png", "image/png", 5L)));

        assertThat(imageService.findById(1).originalName()).isEqualTo("a.png");
    }

    @Test
    void findByIdRejectsInvalidId() {
        assertThatThrownBy(() -> imageService.findById(0))
                .isInstanceOf(BadRequestException.class);
        verifyNoInteractions(imageRepository);
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(imageRepository.findById(1)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> imageService.findById(1))
                .isInstanceOf(NotFoundException.class);
    }
}
