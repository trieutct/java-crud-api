package com.example.demo.exception;

import com.example.demo.dto.ApiResponse;
import com.example.demo.dto.ErrorDetail;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Xử lý chung các lỗi nghiệp vụ, trả về status do từng exception quy định */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleApiException(ApiException ex) {
        return toResponse(ex.getStatus(), ex.getMessage());
    }

    /** Hai request cùng email chạy song song có thể vượt qua bước kiểm tra, khi đó unique constraint sẽ chặn lại */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return toResponse(HttpStatus.BAD_REQUEST, "Dữ liệu bị trùng hoặc không hợp lệ");
    }

    /** Request multipart bị hỏng là lỗi của client, không phải lỗi hệ thống */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse<Void>> handleMultipart(MultipartException ex) {
        return toResponse(HttpStatus.BAD_REQUEST, "Dữ liệu upload không hợp lệ");
    }

    /** Lỗi không lường trước: ghi log đầy đủ nhưng không trả chi tiết nội bộ cho client */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Lỗi không xác định", ex);
        return toResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Lỗi hệ thống");
    }

    /** Trả về lỗi validation của @Valid theo từng trường */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<ErrorDetail> errors = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> new ErrorDetail(error.getField(), status.value(), error.getDefaultMessage()))
                .toList();

        ApiResponse<Void> body = ApiResponse.error(status.value(), "Dữ liệu không hợp lệ", errors);
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

    /** File upload vượt giới hạn spring.servlet.multipart.*, trả về 413 */
    @Override
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ApiResponse<Void> body = ApiResponse.error(status.value(), "File vượt quá dung lượng cho phép", List.of());
        return super.handleExceptionInternal(ex, body, headers, status, request);
    }

    /** Đưa các lỗi do Spring MVC tự sinh (sai kiểu id, JSON hỏng, sai method...) về cùng format */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(
            Exception ex, Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        String message = body instanceof ProblemDetail problem ? problem.getDetail() : "Yêu cầu không hợp lệ";
        ApiResponse<Void> response = ApiResponse.error(statusCode.value(), message, List.of());
        return super.handleExceptionInternal(ex, response, headers, statusCode, request);
    }

    private ResponseEntity<ApiResponse<Void>> toResponse(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(ApiResponse.error(status.value(), message, List.of()));
    }
}
