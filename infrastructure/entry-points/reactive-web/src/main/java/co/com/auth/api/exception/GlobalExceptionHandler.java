package co.com.auth.api.exception;

import co.com.auth.api.dto.ResponseDTO;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebInputException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    // ========= VALIDACIONES =========
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ResponseDTO<Object>> handleConstraintViolation(
            ConstraintViolationException ex, ServerHttpRequest req) {

        var errors = ex.getConstraintViolations().stream()
                .map(v -> Map.of("field", v.getPropertyPath().toString(), "message", v.getMessage()))
                .collect(Collectors.toList());

        log.warn("❌ ConstraintViolation en {} -> {}", req.getPath(), errors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", Map.of("errors", errors), req);
    }

    @ExceptionHandler(WebExchangeBindException.class)
    public ResponseEntity<ResponseDTO<Object>> handleBindException(
            WebExchangeBindException ex, ServerHttpRequest req) {

        var errors = ex.getFieldErrors().stream()
                .map(fe -> Map.of("field", fe.getField(),
                        "message", Optional.ofNullable(fe.getDefaultMessage()).orElse("Invalid value")))
                .collect(Collectors.toList());

        log.warn("❌ BindException en {} -> {}", req.getPath(), errors);
        return build(HttpStatus.BAD_REQUEST, "Validation failed", Map.of("errors", errors), req);
    }

    // ========= PAYLOAD / INPUT =========
    @ExceptionHandler({ ServerWebInputException.class, DecodingException.class })
    public ResponseEntity<ResponseDTO<Object>> handleBadPayload(Exception ex, ServerHttpRequest req) {
        String detail = (ex instanceof ServerWebInputException swe && swe.getReason() != null)
                ? swe.getReason()
                : ex.getMessage();

        log.warn("🧩 Payload inválido en {} -> {}", req.getPath(), safeDetail(detail));
        return build(HttpStatus.BAD_REQUEST, "Invalid request payload", Map.of("detail", safeDetail(detail)), req);
    }

    // ========= CONFLICTOS / DUPLICADOS =========
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ResponseDTO<Object>> handleDuplicate(DuplicateKeyException ex, ServerHttpRequest req) {
        log.warn("⚠️ Duplicate key en {} -> {}", req.getPath(), safeDetail(ex.getMessage()));
        return build(HttpStatus.CONFLICT, "Duplicate key / unique constraint violated", null, req);
    }

    // ========= NOT FOUND / CONFLICT por reglas previas =========
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ResponseDTO<Object>> handleIAE(IllegalArgumentException ex, ServerHttpRequest req) {
        log.warn("🔎 404 NOT_FOUND en {} -> {}", req.getPath(), safeDetail(ex.getMessage()));
        return build(HttpStatus.NOT_FOUND, ex.getMessage(), null, req);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ResponseDTO<Object>> handleISE(IllegalStateException ex, ServerHttpRequest req) {
        log.warn("🚧 409 CONFLICT en {} -> {}", req.getPath(), safeDetail(ex.getMessage()));
        return build(HttpStatus.CONFLICT, ex.getMessage(), null, req);
    }

    // ========= ResponseStatusException =========
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ResponseDTO<Object>> handleRSE(ResponseStatusException ex, ServerHttpRequest req) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        if (status == null)
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        String msg = Optional.ofNullable(ex.getReason()).orElse(status.getReasonPhrase());

        log.warn("📣 {} ResponseStatusException en {} -> {}", status.value(), req.getPath(), safeDetail(msg));
        return build(status, msg, null, req);
    }

    // ========= WebClient (errores HTTP remotos) =========
    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ResponseDTO<Object>> handleWebClient(WebClientResponseException ex, ServerHttpRequest req) {
        HttpStatus status = HttpStatus.resolve(ex.getRawStatusCode());
        if (status == null)
            status = HttpStatus.INTERNAL_SERVER_ERROR;

        String body = safeDetail(ex.getResponseBodyAsString());
        log.error("🌐 Error HTTP remoto {} en {} -> body: {}", status.value(), req.getPath(), body);
        return build(status, "Remote HTTP error", Map.of("detail", body), req);
    }

    // ========= Fallback =========
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDTO<Object>> handleGeneral(Exception ex, ServerHttpRequest req) {
        // R2DBC constraint violations (sin dependencia directa)
        if (ex.getClass().getName().contains("R2dbcDataIntegrityViolationException")) {
            log.warn("⚠️ Duplicate key (R2DBC) en {} -> {}", req.getPath(), safeDetail(ex.getMessage()));
            return build(HttpStatus.CONFLICT, "Duplicate key / unique constraint violated", null, req);
        }

        log.error("🔥 500 Unexpected error en {} -> {}", req.getPath(), safeDetail(ex.getMessage()), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected error", null, req);
    }

    // ========= Helpers =========
    private ResponseEntity<ResponseDTO<Object>> build(HttpStatus status, String message, Object details,
            ServerHttpRequest req) {
        String correlationId = Optional.ofNullable(req.getHeaders().getFirst("X-Correlation-Id"))
                .orElse(UUID.randomUUID().toString());

        var meta = new LinkedHashMap<String, Object>();
        meta.put("path", req.getPath().value());
        meta.put("timestamp", OffsetDateTime.now().toString());
        meta.put("correlationId", correlationId);
        if (details != null)
            meta.put("details", details);

        var body = ResponseDTO.builder()
                .success(false)
                .message(message)
                .statusCode(status.value())
                .data(meta)
                .build();

        return ResponseEntity.status(status)
                .header("X-Correlation-Id", correlationId)
                .body(body);
    }

    private String safeDetail(String raw) {
        if (raw == null)
            return null;
        var s = raw.replaceAll("\\s+", " ").trim();
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}
