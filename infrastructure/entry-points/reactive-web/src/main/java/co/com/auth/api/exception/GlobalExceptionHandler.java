package co.com.auth.api.exception;

import co.com.auth.api.dto.ResponseDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.codec.DecodingException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Order(-2)
@RequiredArgsConstructor
@Log4j2
public class GlobalExceptionHandler implements ErrorWebExceptionHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        var res = exchange.getResponse();
        if (res.isCommitted()) return Mono.error(ex);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        ResponseDTO<Object> body;

        // Datos comunes
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        var now = java.time.OffsetDateTime.now().toString();

        try {

            if (ex instanceof ConstraintViolationException cve) {
                status = HttpStatus.BAD_REQUEST;
                var errors = cve.getConstraintViolations().stream()
                        .map(v -> Map.of("field", v.getPropertyPath().toString(), "message", v.getMessage()))
                        .collect(Collectors.toList());
                body = ResponseDTO.builder()
                        .success(false).message("Invalid request payload").code(status.value())
                        .response(Map.of("errors", errors, "path", path, "timestamp", now))
                        .build();
                log.warn("400 ConstraintViolation: {}", errors);
            }

            else if (ex instanceof ServerWebInputException || ex instanceof DecodingException) {
                status = HttpStatus.BAD_REQUEST;

                String reason = ex.getMessage();
                if (ex instanceof ServerWebInputException swe && swe.getReason() != null) {
                    reason = swe.getReason();
                }
                body = ResponseDTO.builder()
                        .success(false).message("Invalid request payload").code(status.value())
                        .response(Map.of(
                                "detail", safeDetail(reason),
                                "path", path,
                                "timestamp", now))
                        .build();
                log.warn("400 Bad payload: {}", reason);
            }

            else if (ex instanceof IllegalStateException ise) {
                status = HttpStatus.CONFLICT;
                body = ResponseDTO.builder()
                        .success(false).message(ise.getMessage()).code(status.value())
                        .response(Map.of("path", path, "timestamp", now))
                        .build();
                log.warn("409 Conflict: {}", ise.getMessage());
            }

            else if (ex instanceof IllegalArgumentException iae) {
                status = HttpStatus.BAD_REQUEST;
                body = ResponseDTO.builder()
                        .success(false).message(iae.getMessage()).code(status.value())
                        .response(Map.of("path", path, "timestamp", now))
                        .build();
                log.warn("400 Illegal argument: {}", iae.getMessage());
            }

            else if (ex instanceof DuplicateKeyException
                    || ex.getClass().getName().contains("R2dbcDataIntegrityViolationException")) {
                status = HttpStatus.CONFLICT;
                body = ResponseDTO.builder()
                        .success(false).message("Duplicate key / unique constraint violated").code(status.value())
                        .response(Map.of("path", path, "timestamp", now))
                        .build();
                log.warn("409 Duplicate key: {}", ex.getMessage());
            }

            else if (ex instanceof MethodNotAllowedException) {
                status = HttpStatus.METHOD_NOT_ALLOWED;
                body = ResponseDTO.builder().success(false).message("Method not allowed")
                        .code(status.value()).response(Map.of("path", path, "timestamp", now)).build();
            } else if (ex instanceof UnsupportedMediaTypeStatusException) {
                status = HttpStatus.UNSUPPORTED_MEDIA_TYPE;
                body = ResponseDTO.builder().success(false).message("Unsupported media type")
                        .code(status.value()).response(Map.of("path", path, "timestamp", now)).build();
            } else if (ex instanceof NotAcceptableStatusException) {
                status = HttpStatus.NOT_ACCEPTABLE;
                body = ResponseDTO.builder().success(false).message("Not acceptable")
                        .code(status.value()).response(Map.of("path", path, "timestamp", now)).build();
            }

            else if (ex instanceof ResponseStatusException rse) {
                status = (HttpStatus) rse.getStatusCode();
                body = ResponseDTO.builder()
                        .success(false)
                        .message(rse.getReason() != null ? rse.getReason() : status.getReasonPhrase())
                        .code(status.value())
                        .response(Map.of("path", path, "timestamp", now))
                        .build();
                log.warn("{} ResponseStatusException: {}", status.value(), rse.getReason());
            }

            else {
                status = HttpStatus.INTERNAL_SERVER_ERROR;
                body = ResponseDTO.builder()
                        .success(false).message("An unexpected error occurred.").code(status.value())
                        .response(Map.of("path", path, "timestamp", now))
                        .build();
                log.error("500 Unexpected error", ex);
            }

            res.setStatusCode(status);
            res.getHeaders().setContentType(MediaType.APPLICATION_JSON);
            if (correlationId != null) {
                res.getHeaders().set("X-Correlation-Id", correlationId);
            }

            byte[] bytes = objectMapper.writeValueAsBytes(body);
            return res.writeWith(Mono.just(res.bufferFactory().wrap(bytes)));

        } catch (Exception ser) {
            log.error("Error serializando respuesta", ser);
            res.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
            return res.setComplete();
        }
    }

    private String safeDetail(String raw) {
        if (raw == null) return null;
        var s = raw.replaceAll("\\s+", " ").trim();
        return s.length() > 300 ? s.substring(0, 300) + "…" : s;
    }
}

