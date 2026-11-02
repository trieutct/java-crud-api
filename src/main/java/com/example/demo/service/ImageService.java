package com.example.demo.service;

import com.example.demo.dto.ImageResponse;
import com.example.demo.entity.Image;
import com.example.demo.exception.BadRequestException;
import com.example.demo.exception.NotFoundException;
import com.example.demo.repository.ImageRepository;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageService {

    /** Tiền tố đường dẫn công khai của ảnh, WebConfig dùng giá trị này để phục vụ file */
    public static final String PUBLIC_PATH = "/uploads/";

    private static final Logger log = LoggerFactory.getLogger(ImageService.class);

    private static final int MAX_ORIGINAL_NAME_LENGTH = 255;
    private static final int SIGNATURE_LENGTH = 12;

    private static final ImageFormat JPEG = new ImageFormat(".jpg", "image/jpeg");
    private static final ImageFormat PNG = new ImageFormat(".png", "image/png");
    private static final ImageFormat GIF = new ImageFormat(".gif", "image/gif");
    private static final ImageFormat WEBP = new ImageFormat(".webp", "image/webp");

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] GIF87_SIGNATURE = "GIF87a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] GIF89_SIGNATURE = "GIF89a".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] RIFF_SIGNATURE = "RIFF".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] WEBP_SIGNATURE = "WEBP".getBytes(StandardCharsets.US_ASCII);

    private final ImageRepository imageRepository;
    private final Path uploadDir;

    public ImageService(ImageRepository imageRepository, @Value("${upload.dir}") String uploadDir) {
        this.imageRepository = imageRepository;
        this.uploadDir = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    /** Lấy thông tin tất cả ảnh */
    @Transactional(readOnly = true)
    public List<ImageResponse> findAll() {
        return imageRepository.findAll().stream()
                .map(ImageResponse::from)
                .toList();
    }

    /** Lấy thông tin một ảnh theo id */
    @Transactional(readOnly = true)
    public ImageResponse findById(Integer id) {
        if (id <= 0) {
            throw new BadRequestException("Id phải lớn hơn 0");
        }

        return imageRepository.findById(id)
                .map(ImageResponse::from)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy ảnh với id: " + id));
    }

    /** Lưu file ảnh lên ổ đĩa và lưu đường dẫn vào database, báo lỗi 400 nếu file rỗng, tên không hợp lệ hoặc không phải ảnh được hỗ trợ */
    @Transactional
    public ImageResponse create(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File ảnh không được để trống");
        }
        String originalName = getOriginalName(file);
        ImageFormat format = detectFormat(file);

        // Tên file do server sinh, không dùng tên từ client để tránh trùng file và path traversal
        String fileName = UUID.randomUUID() + format.extension();
        Path target = uploadDir.resolve(fileName);
        Image image = new Image(originalName, PUBLIC_PATH + fileName, format.contentType(), file.getSize());
        try {
            Files.createDirectories(uploadDir);
            file.transferTo(target);
            return ImageResponse.from(imageRepository.save(image));
        } catch (IOException ex) {
            deleteFile(target);
            throw new UncheckedIOException("Không thể lưu file ảnh", ex);
        } catch (RuntimeException ex) {
            // Database lỗi thì transaction rollback, xóa file để không còn file mồ côi trên ổ đĩa
            deleteFile(target);
            throw ex;
        }
    }

    /** Lấy tên file gốc đã bỏ phần thư mục, báo lỗi 400 nếu tên trống hoặc quá dài */
    private String getOriginalName(MultipartFile file) {
        String originalName = file.getOriginalFilename();
        String name = originalName == null ? null : StringUtils.getFilename(StringUtils.cleanPath(originalName));
        if (!StringUtils.hasText(name)) {
            throw new BadRequestException("Tên file không hợp lệ");
        }
        if (name.length() > MAX_ORIGINAL_NAME_LENGTH) {
            throw new BadRequestException("Tên file tối đa 255 ký tự");
        }
        return name;
    }

    /** Xác định định dạng từ nội dung file vì tên file và Content-Type do client gửi có thể bị giả mạo, báo lỗi 400 nếu không được hỗ trợ */
    private ImageFormat detectFormat(MultipartFile file) {
        byte[] header = readHeader(file);
        if (startsWith(header, 0, JPEG_SIGNATURE)) {
            return JPEG;
        }
        if (startsWith(header, 0, PNG_SIGNATURE)) {
            return PNG;
        }
        if (startsWith(header, 0, GIF87_SIGNATURE) || startsWith(header, 0, GIF89_SIGNATURE)) {
            return GIF;
        }
        if (startsWith(header, 0, RIFF_SIGNATURE) && startsWith(header, 8, WEBP_SIGNATURE)) {
            return WEBP;
        }
        throw new BadRequestException("Chỉ hỗ trợ ảnh JPEG, PNG, GIF hoặc WEBP");
    }

    private byte[] readHeader(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            // File ngắn hơn SIGNATURE_LENGTH được đệm thêm byte 0 để so sánh không bị vượt mảng
            return Arrays.copyOf(input.readNBytes(SIGNATURE_LENGTH), SIGNATURE_LENGTH);
        } catch (IOException ex) {
            throw new UncheckedIOException("Không thể đọc file ảnh", ex);
        }
    }

    private boolean startsWith(byte[] header, int offset, byte[] signature) {
        return Arrays.equals(header, offset, offset + signature.length, signature, 0, signature.length);
    }

    private void deleteFile(Path target) {
        try {
            Files.deleteIfExists(target);
        } catch (IOException ex) {
            log.warn("Không thể xóa file ảnh: {}", target, ex);
        }
    }

    private record ImageFormat(String extension, String contentType) {
    }
}
