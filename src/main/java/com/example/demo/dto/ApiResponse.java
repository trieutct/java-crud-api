package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import org.springframework.http.HttpStatus;

/** Shape response chuẩn dùng chung cho toàn bộ API, trường null sẽ không xuất hiện trong JSON */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(boolean success, int code, String message, T data, List<ErrorDetail> errors) {

    public static final String DEFAULT_SUCCESS_MESSAGE = "success";

    /** Bọc response thành công theo format { success, code, message, data } */
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, HttpStatus.OK.value(), DEFAULT_SUCCESS_MESSAGE, data, null);
    }

    /** Bọc response tạo mới thành công với code 201 */
    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(true, HttpStatus.CREATED.value(), DEFAULT_SUCCESS_MESSAGE, data, null);
    }

    /** Bọc response thành công dạng danh sách theo format { success, code, message, data: { items, total_items } } */
    public static <T> ApiResponse<ListResponse<T>> successList(List<T> items) {
        return success(new ListResponse<>(items, items.size()));
    }

    /** Bọc response lỗi theo format { success, code, message, errors } */
    public static ApiResponse<Void> error(int code, String message, List<ErrorDetail> errors) {
        return new ApiResponse<>(false, code, message, null, errors);
    }
}
