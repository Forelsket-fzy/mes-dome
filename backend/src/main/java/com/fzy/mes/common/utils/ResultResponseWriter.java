package com.fzy.mes.common.utils;

import com.fzy.mes.common.module.vo.Result;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Filter / 非 Controller 场景统一写 {@link Result} JSON。
 */
public final class ResultResponseWriter {

    private ResultResponseWriter() {
    }

    public static void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        write(response, HttpServletResponse.SC_UNAUTHORIZED, Result.unauthorized(message));
    }

    public static void writeBadRequest(HttpServletResponse response, String message) throws IOException {
        write(response, HttpServletResponse.SC_BAD_REQUEST, Result.badRequest(message));
    }

    public static void write(HttpServletResponse response, int httpStatus, Result<?> body) throws IOException {
        response.setStatus(httpStatus);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(toJson(body));
    }

    private static String toJson(Result<?> body) {
        return "{\"code\":" + body.getCode()
                + ",\"message\":\"" + escapeJson(body.getMessage()) + "\""
                + ",\"data\":null}";
    }

    private static String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

}
