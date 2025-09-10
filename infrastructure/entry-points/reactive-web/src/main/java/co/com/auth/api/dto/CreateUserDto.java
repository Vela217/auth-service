package co.com.auth.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "DTO para la creación de usuarios")
@Builder(toBuilder = true)
public record CreateUserDto(

        @Schema(description = "ID usuario", example = "d965e3bc-3995-4daa-a") String userId,

        @Schema(description = "Número de documento de identidad", example = "12345678") @NotBlank(message = "numberDocument is required") String numberDocument,

        @Schema(description = "Nombre del usuario", example = "Juan") @NotBlank(message = "name is required") @NotNull String name,

        @Schema(description = "Apellido del usuario", example = "Pérez") @NotBlank(message = "lastName is required") String lastName,

        @Schema(description = "Fecha de nacimiento", example = "1990-01-01") LocalDate birthDate,

        @Schema(description = "Dirección del usuario", example = "Calle 123 #45-67") String address,

        @Schema(description = "Correo electrónico del usuario", example = "juan.perez@email.com") @NotBlank(message = "Email is required") @Email(message = "Email is not valid") String email,

        @Schema(description = "Número de teléfono", example = "+573001234567") String phone,

        @Schema(description = "Contraseña") @NotBlank(message = "La contraseña es requerida") @NotNull String password,

        @NotNull
        @Schema(description = "Rol del usuario", example = "1") Long roleId,

        @Schema(description = "Salario base del usuario", example = "2500000.00", minimum = "0", maximum = "15000000.000") @DecimalMin(value = "0", inclusive = false, message = "Base salary be greater than 0") @DecimalMax(value = "15000000.00", message = "Base salary must be less than 15000000.00") @NotNull(message = "Base salary is required") BigDecimal baseSalary){

}
