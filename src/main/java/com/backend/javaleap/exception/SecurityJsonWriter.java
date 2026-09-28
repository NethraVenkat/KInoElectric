package com.backend.javaleap.exception;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/** Writes the standard ApiResponse error JSON from inside the security filter chain. */
final class SecurityJsonWriter {

    private SecurityJsonWriter() { }

    static void write(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"success\":false,\"message\":\"" + message.replace("\"", "'")
                + "\",\"data\":null,\"timestamp\":\"" + Instant.now() + "\"}");
    }
}
