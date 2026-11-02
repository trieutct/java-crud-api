package com.example.demo.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import org.springframework.web.util.WebUtils;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

@Component
public class ApiLoggingInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(ApiLoggingInterceptor.class);

    private static final String START_TIME = ApiLoggingInterceptor.class.getName() + ".startTime";
    private static final int MAX_LOG_LENGTH = 4000;
    private static final String MASK = "***";
    private static final Set<String> SENSITIVE_FIELDS =
            Set.of("password", "passwordhash", "accesstoken", "refreshtoken", "fcmtoken");

    /** Ghi lại thời điểm bắt đầu để tính thời gian xử lý */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_TIME, System.currentTimeMillis());
        return true;
    }

    /** Ghi log payload và data sau khi xử lý xong, vì Spring chỉ đọc body sau preHandle */
    @Override
    public void afterCompletion(
            HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        String api = request.getMethod() + " " + sanitize(request.getRequestURI());
        ContentCachingRequestWrapper cachedRequest =
                WebUtils.getNativeRequest(request, ContentCachingRequestWrapper.class);
        ContentCachingResponseWrapper cachedResponse =
                WebUtils.getNativeResponse(response, ContentCachingResponseWrapper.class);

        log.info("API vào: {} payload={}", api,
                toLogBody(cachedRequest == null ? new byte[0] : cachedRequest.getContentAsByteArray()));
        log.info("API ra: {} status={} durationMs={} data={}", api, response.getStatus(),
                System.currentTimeMillis() - (Long) request.getAttribute(START_TIME),
                toLogBody(cachedResponse == null ? new byte[0] : cachedResponse.getContentAsByteArray()));
    }

    /** Chỉ ghi JSON đọc được để che được trường nhạy cảm, nội dung khác (form, JSON hỏng) thì bỏ qua để tránh lộ bí mật */
    private String toLogBody(byte[] body) {
        if (body.length == 0) {
            return "(không có hoặc chưa đọc)";
        }
        try {
            JsonNode json = JsonMapper.shared().readTree(body);
            mask(json);
            String text = json.toString();
            return text.length() > MAX_LOG_LENGTH ? text.substring(0, MAX_LOG_LENGTH) + "...(đã cắt)" : text;
        } catch (JacksonException ex) {
            return "(không phải JSON hợp lệ hoặc quá lớn, không ghi nội dung)";
        }
    }

    /** Che giá trị của trường nhạy cảm ở mọi cấp, không phân biệt kiểu tên trường */
    private void mask(JsonNode node) {
        if (node instanceof ObjectNode object) {
            for (Map.Entry<String, JsonNode> field : object.properties()) {
                if (SENSITIVE_FIELDS.contains(field.getKey().replace("_", "").toLowerCase(Locale.ROOT))) {
                    object.put(field.getKey(), MASK);
                } else {
                    mask(field.getValue());
                }
            }
        } else if (node.isArray()) {
            node.forEach(this::mask);
        }
    }

    /** Thay ký tự xuống dòng để dữ liệu từ client không giả mạo được dòng log */
    private String sanitize(String value) {
        return value.replaceAll("[\\r\\n]", "_");
    }
}
