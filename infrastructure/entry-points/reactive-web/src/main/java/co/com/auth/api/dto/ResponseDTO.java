package co.com.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "DTO genérico para respuestas de la API")
public class ResponseDTO<T> {

    @Schema(description = "Indica si la operación fue exitosa", example = "true")
    private Boolean success;

    @Schema(description = "Mensaje descriptivo de la respuesta", example = "Usuario creado con éxito")
    private String message;

    @Schema(description = "Código de estado HTTP", example = "201")
    private Integer code;

    @Schema(description = "Datos de respuesta de la operación")
    private T response;

}