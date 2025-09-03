package co.com.auth.api;

import co.com.auth.api.dto.CreateUserDto;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

import org.springdoc.core.annotations.RouterOperation;
import org.springdoc.core.annotations.RouterOperations;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import org.springframework.web.bind.annotation.RequestMethod;
@Configuration
public class RouterRest {
    @Bean
    @RouterOperations({
            @RouterOperation(
                    path = "/api/v1/usuarios",
                    method = RequestMethod.POST,
                    beanClass = Handler.class,
                    beanMethod = "listenSaveUser",
                    operation = @Operation(
                            operationId = "crearUsuario",
                            summary = "Registrar usuario",
                            requestBody = @RequestBody(
                                    required = true,
                                    content = @Content(
                                            mediaType = "application/json",
                                            schema = @Schema(implementation = CreateUserDto.class)
                                    )
                            ),
                            responses = {
                                    @ApiResponse(
                                            responseCode = "201",
                                            description = "Usuario creado con exito",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(example = """
                                {
                                  "success": true,
                                  "message": "Usuario creado con exito",
                                  "code": 201,
                                  "response": {
                                    "userId": "5ae98718-9a56-45d5-b12b-9a2b72318264",
                                    "numberDocument": "12345678",
                                    "name": "Juan",
                                    "lastName": "Vela",
                                    "birthDate": "1990-01-01",
                                    "address": "Calle 123 #45-67",
                                    "email": "vela217@email.com",
                                    "phone": "+573001234567",
                                    "roleId": 1,
                                    "baseSalary": 2500000
                                  }
                                }
                            """)
                                            )
                                    ),
                                    @ApiResponse(
                                            responseCode = "400",
                                            description = "Error en datos de entrada",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(example = """
                                {
                                  "success": false,
                                  "message": "Invalid request payload",
                                  "code": 400,
                                  "response": {
                                    "errors": [
                                      {"message": "name is required", "field": "name"},
                                      {"message": "no debe ser nulo", "field": "name"},
                                      {"message": "numberDocument is required", "field": "numberDocument"},
                                      {"message": "lastName is required", "field": "lastName"}
                                    ]
                                  }
                                }
                            """)
                                            )
                                    ),
                                    @ApiResponse(
                                            responseCode = "409",
                                            description = "Conflicto email en uso",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(example = """
                                {
                                  "success": false,
                                  "message": "El correo ya esta en uso",
                                  "code": 409,
                                  "response": null
                                }
                            """)
                                            )
                                    ),
                                    @ApiResponse(
                                            responseCode = "500",
                                            description = "Error inesperado",
                                            content = @Content(
                                                    mediaType = "application/json",
                                                    schema = @Schema(example = """
                                {
                                  "success": false,
                                  "message": "An unexpected error occurred",
                                  "code": 500,
                                  "response": null
                                }
                            """)
                                            )
                                    )
                            }
                    )
            )
    })
    public RouterFunction<ServerResponse> routerFunction(Handler handler) {
        return route(
                POST("/api/v1/usuarios")
                        .and(RequestPredicates.accept(org.springframework.http.MediaType.APPLICATION_JSON)),
                handler::listenSaveUser

        ).andRoute(GET("/api/v1/usuarios/{document}"), handler::getByDocument);
    }
}

