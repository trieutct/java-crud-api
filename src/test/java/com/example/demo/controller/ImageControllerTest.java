package com.example.demo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.demo.dto.ImageResponse;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.exception.UnauthorizedException;
import com.example.demo.service.AuthService;
import com.example.demo.service.ImageService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(OutputCaptureExtension.class)
@WebMvcTest(value = ImageController.class, properties = "upload.dir=" + ImageControllerTest.UPLOAD_DIR)
class ImageControllerTest {

    static final String UPLOAD_DIR = "target/test-uploads";

    private static final MockMultipartFile FILE =
            new MockMultipartFile("file", "a.png", "image/png", new byte[] {1, 2, 3});
    private static final ImageResponse RESPONSE =
            new ImageResponse(1, "a.png", "/uploads/x.png", null, "image/png", 3L, Instant.EPOCH);

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImageService imageService;

    @MockitoBean
    private AuthService authService;

    @Test
    void protectedEndpointsReturn401WithoutToken() throws Exception {
        doThrow(new UnauthorizedException("Thiếu access token")).when(authService).authenticate(null, false);

        mockMvc.perform(multipart("/api/images").file(FILE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/images")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/images/1")).andExpect(status().isUnauthorized());
        verifyNoInteractions(imageService);
    }

    @Test
    void createReturns201WithSnakeCaseFields() throws Exception {
        when(imageService.create(any(MultipartFile.class))).thenReturn(RESPONSE);

        mockMvc.perform(multipart("/api/images").file(FILE))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/images/1"))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.code").value(201))
                .andExpect(jsonPath("$.data.original_name").value("a.png"))
                .andExpect(jsonPath("$.data.file_path").value("/uploads/x.png"))
                .andExpect(jsonPath("$.data.url").value("http://localhost/uploads/x.png"))
                .andExpect(jsonPath("$.data.content_type").value("image/png"))
                .andExpect(jsonPath("$.data.file_size").value(3));
    }

    @Test
    void createLogsInputAndOutputWithoutLineBreaks(CapturedOutput output) throws Exception {
        String name = "a\r\nFAKE.png";
        when(imageService.create(any(MultipartFile.class))).thenReturn(
                new ImageResponse(1, name, "/uploads/x.png", null, "image/png", 3L, Instant.EPOCH));

        mockMvc.perform(multipart("/api/images").file(new MockMultipartFile("file", name, "image/png", new byte[] {1})))
                .andExpect(status().isCreated());

        assertThat(output).contains("name=a__FAKE.png, contentType=image/png, size=1");
        assertThat(output).contains("originalName=a__FAKE.png", "url=http://localhost/uploads/x.png");
        assertThat(output).doesNotContain("\nFAKE");
    }

    @Test
    void createReturns400WhenServiceRejectsFile() throws Exception {
        when(imageService.create(any(MultipartFile.class)))
                .thenThrow(new BadRequestException("Chỉ hỗ trợ ảnh JPEG, PNG, GIF hoặc WEBP"));

        mockMvc.perform(multipart("/api/images").file(FILE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Chỉ hỗ trợ ảnh JPEG, PNG, GIF hoặc WEBP"));
    }

    @Test
    void createReturns400WhenFilePartMissing() throws Exception {
        mockMvc.perform(multipart("/api/images"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));
        verifyNoInteractions(imageService);
    }

    @Test
    void createReturns415WhenNotMultipart() throws Exception {
        mockMvc.perform(post("/api/images").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(415));
    }

    @Test
    void createReturns413WhenFileTooLarge() throws Exception {
        when(imageService.create(any(MultipartFile.class))).thenThrow(new MaxUploadSizeExceededException(1));

        mockMvc.perform(multipart("/api/images").file(FILE))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(413))
                .andExpect(jsonPath("$.message").value("File vượt quá dung lượng cho phép"));
    }

    @Test
    void createReturns400WhenMultipartBroken() throws Exception {
        when(imageService.create(any(MultipartFile.class))).thenThrow(new MultipartException("hỏng"));

        mockMvc.perform(multipart("/api/images").file(FILE))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Dữ liệu upload không hợp lệ"));
    }

    @Test
    void findAllReturnsImages() throws Exception {
        when(imageService.findAll()).thenReturn(List.of(RESPONSE));

        mockMvc.perform(get("/api/images"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].file_path").value("/uploads/x.png"))
                .andExpect(jsonPath("$.data.items[0].url").value("http://localhost/uploads/x.png"))
                .andExpect(jsonPath("$.data.total_items").value(1));
    }

    @Test
    void findByIdReturnsImage() throws Exception {
        when(imageService.findById(1)).thenReturn(RESPONSE);

        mockMvc.perform(get("/api/images/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.file_path").value("/uploads/x.png"))
                .andExpect(jsonPath("$.data.url").value("http://localhost/uploads/x.png"));
    }

    @Test
    void findByIdReturns404WhenMissing() throws Exception {
        when(imageService.findById(1)).thenThrow(new NotFoundException("Không tìm thấy"));

        mockMvc.perform(get("/api/images/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadedFileIsServedWithoutToken() throws Exception {
        byte[] bytes = {1, 2, 3};
        Path dir = Path.of(UPLOAD_DIR);
        Files.createDirectories(dir);
        Files.write(dir.resolve("served.png"), bytes);

        mockMvc.perform(get("/uploads/served.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(bytes));
        verifyNoInteractions(authService);
    }

    @Test
    void missingUploadedFileReturns404() throws Exception {
        mockMvc.perform(get("/uploads/khong-ton-tai.png"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
